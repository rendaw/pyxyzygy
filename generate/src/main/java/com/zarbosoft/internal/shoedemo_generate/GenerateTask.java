package com.zarbosoft.internal.shoedemo_generate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.squareup.javapoet.*;
import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.interface1.Walk;
import com.zarbosoft.internal.shoedemo_seed.model.*;
import com.zarbosoft.internal.shoedemo_generate.premodel.ProjectObject;
import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;
import com.zarbosoft.internal.shoedemo_seed.deserialize.*;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zarbosoft.rendaw.common.Common.*;
import static javax.lang.model.element.Modifier.*;

/**
 * Serialization occurs on all fields and recurses collections of exactly 1 depth.  For greater depth you need to make
 * an intermediate object that contains the next container level.  This way also simplifies mutation, which all can be
 * described with a reference to an object, a field, and maybe 1 or two other values for the change.
 * <p>
 * Objects should be created either by their Deserializer or by their static create() method.
 */
public class GenerateTask extends Task {

	// ANT STUFF
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Path path;

	/**
	 * For elements with ids (mutable) serialized flat at the top output level, return true.
	 *
	 * @param type
	 * @return
	 */
	public static boolean flattenPoint(TypeInfo type) {
		if (ProjectObject.class.isAssignableFrom((Class) type.type))
			return true;
		return false;
	}

	public static String decideName(Class k) {
		return k.getSimpleName();
	}

	public void setPath(final String path) {
		this.path = Paths.get(path);
	}

	@Override
	public void execute() throws BuildException {
		uncheck(() -> Files.createDirectories(path));
		buildModel();
	}

	public static void main(final String[] args) {
		final GenerateTask t = new GenerateTask();
		t.setPath(args[0]);
		t.execute();
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static Object CHANGE_TOKEN_NAME = new Object();
	public static Reflections reflections = new Reflections("", new SubTypesScanner(false));

	public static Method sigObjIncRef = findMethod(ProjectObjectInterface.class, "incRef");
	public static Method sigObjDecRef = findMethod(ProjectObjectInterface.class, "decRef");
	public static Method sigObjSerialize = findMethod(ProjectObjectInterface.class, "serialize");
	public static Method sigChangeDebugCounts = findMethod(Change.class, "debugRefCounts");
	public static Method sigChangeMerge = findMethod(Change.class, "merge");
	public static Method sigChangeApply = findMethod(Change.class, "apply");
	public static Method sigChangeDelete = findMethod(Change.class, "delete");
	public static Method sigChangeSerialize = findMethod(Change.class, "serialize");
	public static Method sigStateValue = findMethod(StackReader.State.class, "value");
	public static Method sigStateArray = findMethod(StackReader.State.class, "array");
	public static Method sigStateRecord = findMethod(StackReader.State.class, "record");
	public static Method sigStateGet = findMethod(StackReader.State.class, "get");
	public static Method sigFinisherFinish = findMethod(ModelDeserializationContext.Finisher.class, "finish");

	public Map<Class, Pair<ClassName, TypeSpec.Builder>> typeMap = new HashMap<>();
	public ClassName deserializeHelperName = name("DeserializeHelper");
	public TypeSpec.Builder deserializeHelper = TypeSpec.classBuilder(deserializeHelperName).addModifiers(PUBLIC);
	public MethodSpec.Builder globalModelDeserialize = MethodSpec
			.methodBuilder("deserializeModel")
			.addModifiers(PUBLIC, STATIC)
			.returns(StackReader.State.class)
			.addParameter(ModelDeserializationContext.class, "context")
			.addParameter(String.class, "type");
	public MethodSpec.Builder globalChangeDeserialize = MethodSpec
			.methodBuilder("deserializeChange")
			.addModifiers(PUBLIC, STATIC)
			.returns(StackReader.State.class)
			.addParameter(ChangeDeserializationContext.class, "context")
			.addParameter(String.class, "type");

	public TypeName toPoet(TypeInfo type) {
		TypeName base = Optional
				.ofNullable(typeMap.get(type.type))
				.map(p -> (TypeName) p.first)
				.orElseGet(() -> TypeName.get(type.type));
		if (type.parameters != null && type.parameters.length > 0) {
			return ParameterizedTypeName.get((ClassName) base,
					(TypeName[]) Arrays.stream(type.parameters).map(p -> toPoet(p)).toArray(TypeName[]::new)
			);
		} else
			return base;
	}

	public ClassName name(String... parts) {
		return ClassName.get("com.zarbosoft.shoedemo.model", Arrays.stream(parts).collect(Collectors.joining("_")));
	}

	private CodeBlock generateScalarSerialize(TypeInfo fieldInfo, String key) {
		if (fieldInfo.type == String.class) {
			return CodeBlock.of("writer.primitive($L);\n", key);
		} else if (fieldInfo.type == Integer.class || fieldInfo.type == int.class) {
			return CodeBlock.of("writer.primitive($T.toString($L));\n", Integer.class, key);
		} else if (fieldInfo.type == Long.class || fieldInfo.type == long.class) {
			return CodeBlock.of("writer.primitive($T.toString($L));\n", Long.class, key);
		} else if (fieldInfo.type == Float.class || fieldInfo.type == float.class) {
			return CodeBlock.of("writer.primitive($T.toString($L));\n", Float.class, key);
		} else if (fieldInfo.type == Double.class || fieldInfo.type == double.class) {
			return CodeBlock.of("writer.primitive($T.toString($L));\n", Double.class, key);
		} else if (fieldInfo.type == Boolean.class || fieldInfo.type == boolean.class) {
			return CodeBlock.of("writer.primitive($L ? \"true\" : \"false\");\n", key);
		} else if (((Class<?>) fieldInfo.type).isEnum()) {
			CodeBlock.Builder code = CodeBlock.builder();
			code.add("switch ($L) {\n", key).indent();
			for (Pair<Enum<?>, Field> v : Walk.enumValues((Class) fieldInfo.type)) {
				code.add("case $E: writer.primitive(\"$L\"); break;\n", v.first, Walk.decideEnumName(v.first));
			}
			code.unindent().add("}\n");
			return code.build();
		} else if (((Class) fieldInfo.type).isAssignableFrom(ArrayList.class)) {
			throw new Assertion();
		} else if (((Class) fieldInfo.type).isAssignableFrom(HashSet.class)) {
			throw new Assertion();
		} else if (((Class) fieldInfo.type).isAssignableFrom(HashMap.class)) {
			throw new Assertion();
		} else if (flattenPoint(fieldInfo)) {
			return CodeBlock
					.builder()
					.add("if ($L == null)\n", key)
					.indent()
					.add("writer.primitive(\"null\");\n")
					.unindent()
					.add("else\n")
					.indent()
					.add("writer.primitive($T.toString($L.id()));\n", Long.class, key)
					.unindent()
					.build();
		} else {
			return CodeBlock.of("$L.serialize(writer);\n", key);
		}
	}

	private CodeBlock generateScalarFromString(
			TypeInfo fieldInfo, String name, CodeBlock.Builder valueCode, CodeBlock.Builder recordCode
	) {
		if (fieldInfo.type == String.class) {
			return CodeBlock.of("($T) $L", String.class, name);
		} else if (fieldInfo.type == Integer.class || fieldInfo.type == int.class) {
			return CodeBlock.of("$T.valueOf(($T) $L)", Integer.class, String.class, name);
		} else if (fieldInfo.type == Long.class || fieldInfo.type == long.class) {
			return CodeBlock.of("$T.valueOf(($T) $L)", Long.class, String.class, name);
		} else if (fieldInfo.type == Float.class || fieldInfo.type == float.class) {
			return CodeBlock.of("$T.valueOf(($T) $L)", Float.class, String.class, name);
		} else if (fieldInfo.type == Double.class || fieldInfo.type == double.class) {
			return CodeBlock.of("$T.valueOf(($T) $L)", Double.class, String.class, name);
		} else if (fieldInfo.type == Boolean.class || fieldInfo.type == boolean.class) {
			return CodeBlock.of("\"true\".equals($L)", name);
		} else if (((Class<?>) fieldInfo.type).isEnum()) {
			valueCode.add("$T converted;\n", fieldInfo.type).add("switch (($T) $L) {\n", fieldInfo.type, name).indent();
			for (Pair<Enum<?>, Field> v : Walk.enumValues((Class) fieldInfo.type))
				valueCode.add("case \"$L\": converted = $E; break;\n",
						Walk.decideEnumName(v.first),
						fieldInfo.field.getName(),
						v.first
				);
			valueCode
					.add("default: throw new RuntimeError(\"Unknown value \" + ($T)$L);\n", String.class, name)
					.unindent();
			valueCode.unindent().add("}\n");
			return CodeBlock.of("converted");
		} else if (((Class) fieldInfo.type).isAssignableFrom(ArrayList.class)) {
			throw new Assertion();
		} else if (((Class) fieldInfo.type).isAssignableFrom(HashSet.class)) {
			throw new Assertion();
		} else if (((Class) fieldInfo.type).isAssignableFrom(HashMap.class)) {
			throw new Assertion();
		} else if (flattenPoint(fieldInfo)) {
			// skip this case
			return null;
		} else {
			if (Walk.isConcrete(fieldInfo)) {
				recordCode.add("return new $T.Deserializer();\n", toPoet(fieldInfo));
			} else {
				recordCode.add("switch (key) {\n").indent();
				Walk.getDerived(reflections, fieldInfo).forEach(p -> {
					recordCode.add("case \"$L\": return new $T.Deserializer();\n", p.first, p.second);
				});
				recordCode
						.add("default: throw new $T(String.format(\"Unknown type %s\", type));\n",
								RuntimeException.class
						)
						.unindent()
						.add("}\n");
			}
			return CodeBlock.of("($T) $L", toPoet(fieldInfo), name);
		}
	}

	public MethodSpec.Builder poetMethod(Method method) {
		MethodSpec.Builder out = MethodSpec
				.methodBuilder(method.getName())
				.addAnnotation(Override.class)
				.returns(toPoet(new TypeInfo(method)));
		if (Modifier.isPublic(method.getModifiers()))
			out.addModifiers(PUBLIC);
		if (Modifier.isPrivate(method.getModifiers()))
			out.addModifiers(PRIVATE);
		if (Modifier.isProtected(method.getModifiers()))
			out.addModifiers(PROTECTED);
		for (Parameter parameter : method.getParameters())
			out.addParameter(toPoet(new TypeInfo(parameter)), parameter.getName());
		return out;
	}

	public static Method findMethod(Class klass, String name) {
		return Arrays.stream(klass.getMethods()).filter(m -> name.equals(m.getName())).findFirst().get();
	}

	public void buildModel() {
		// Prep type map
		for (Class klass : iterable(reflections
				.getSubTypesOf(ModelRootType.class)
				.stream()
				.filter(c -> c.getPackage().getName().equals("com.zarbosoft.internal.shoedemo_generate.premodel")))) {
			ClassName name = name(klass.getSimpleName());
			typeMap.put(klass, new Pair<>(name, TypeSpec.classBuilder(name).addModifiers(PUBLIC)));
		}

		// Build
		ClassName changeStepName = ClassName.get(ChangeStep.class);
		ClassName changeStepBuilderName = name("ChangeStepBuilder");
		TypeSpec.Builder changeStepBuilder = TypeSpec
				.classBuilder(changeStepBuilderName)
				.addModifiers(PUBLIC)
				.addField(FieldSpec.builder(ProjectContextBase.class, "context").addModifiers(PUBLIC, FINAL).build())
				.addField(FieldSpec.builder(changeStepName, "changeStep").addModifiers(PUBLIC, FINAL).build())
				.addMethod(MethodSpec
						.constructorBuilder()
						.addModifiers(PUBLIC)
						.addParameter(ProjectContextBase.class, "context")
						.addParameter(ChangeStep.class, "changeStep")
						.addCode("this.context = context;\n")
						.addCode("this.changeStep = changeStep;\n", ChangeStep.class, ChangeStep.CacheId.class)
						.build());

		for (Map.Entry<Class, Pair<ClassName, TypeSpec.Builder>> entry : typeMap.entrySet()) {
			Class klass = entry.getKey();
			TypeInfo classInfo = new TypeInfo(klass);

			// Set up clone core
			final ClassName cloneName = entry.getValue().first;
			TypeSpec.Builder clone = entry.getValue().second;
			CodeBlock.Builder cloneSerialize =
					CodeBlock.builder().add("writer.type(\"$L\");\n", decideName(klass)).add("writer.recordBegin();\n");
			CodeBlock.Builder cloneDeserializeValue = CodeBlock.builder();
			CodeBlock.Builder cloneDeserializeArray = CodeBlock.builder();
			CodeBlock.Builder cloneDeserializeRecord = CodeBlock.builder();
			CodeBlock.Builder cloneDeserializeFinish = CodeBlock.builder();
			CodeBlock.Builder cloneDecRef = CodeBlock
					.builder()
					.add("refCount -= 1;\n")
					.add("if (refCount > 0) return;\n")
					.add("project.objectMap.remove(id);\n");

			ClassName typeChangeStepBuilderName = name(klass.getSimpleName(), "ChangeBuilder");
			TypeSpec.Builder typeChangeStepBuilder =
					TypeSpec.classBuilder(typeChangeStepBuilderName).addModifiers(PUBLIC);
			typeChangeStepBuilder
					.addField(FieldSpec
							.builder(changeStepBuilderName, "changeStepBuilder")
							.addModifiers(PUBLIC)
							.build())
					.addField(FieldSpec.builder(cloneName, "target").addModifiers(PUBLIC).build())
					.addMethod(MethodSpec
							.constructorBuilder()
							.addModifiers(PUBLIC)
							.addParameter(changeStepBuilderName, "changeStepBuilder")
							.addParameter(cloneName, "target")
							.addCode("this.changeStepBuilder = changeStepBuilder;\n")
							.addCode("this.target = target;\n")
							.build());
			changeStepBuilder.addMethod(MethodSpec
					.methodBuilder(lowerFirst(klass.getSimpleName()))
					.addModifiers(PUBLIC)
					.returns(typeChangeStepBuilderName)
					.addParameter(cloneName, "target")
					.addCode("return new $T(this, target);\n", typeChangeStepBuilderName)
					.build());

			// Type-specific stuff
			for (Pair<Configuration, TypeInfo> pair : iterable(Walk.getFields(klass))) {
				TypeInfo fieldInfo = pair.second;
				String fieldName = pair.second.field.getName();

				class ChangeBuilder {
					private final ClassName changeName;
					private final MethodSpec.Builder changeInvoke;
					private final List<String> invokeForward = new ArrayList<>();
					private final TypeSpec.Builder change;
					private final MethodSpec.Builder changeConstructor;
					private final CodeBlock.Builder changeApply;
					private final CodeBlock.Builder changeApply2;
					private final CodeBlock.Builder changeApplyNotify;
					private final MethodSpec.Builder changeDelete;
					private final MethodSpec.Builder changeSerialize;
					private final CodeBlock.Builder changeDebugCounts;
					private final String listenersFieldName;
					private final CodeBlock.Builder listenersAdd;
					private final ClassName listenerName;
					private final TypeSpec.Builder listener;
					private final MethodSpec.Builder listenerAccept;
					private final CodeBlock.Builder merge;
					private final ClassName deserializerName;
					private final CodeBlock.Builder deserializerValue;
					private final CodeBlock.Builder deserializerRecord;
					private final CodeBlock.Builder deserializerArray;

					public ChangeBuilder(
							String fieldName, String action
					) {
						listenerName = cloneName.nestedClass(String.format("%s%sListener",
								capFirst(fieldName),
								capFirst(action)
						));
						listener = TypeSpec
								.interfaceBuilder(listenerName)
								.addModifiers(PUBLIC)
								.addAnnotation(FunctionalInterface.class);
						listenerAccept = MethodSpec
								.methodBuilder("accept")
								.addModifiers(PUBLIC, ABSTRACT)
								.addParameter(cloneName, "target");
						listenersFieldName = String.format("%s%sListeners", fieldName, capFirst(action));
						clone.addField(FieldSpec
								.builder(ParameterizedTypeName.get(ClassName.get(List.class), listenerName),
										listenersFieldName
								)
								.addModifiers(PRIVATE, FINAL)
								.initializer("new $T<>()", ArrayList.class)
								.build());
						listenersAdd = CodeBlock.builder();
						merge = CodeBlock.builder();
						deserializerName = cloneName.nestedClass(String.format("%s%sDeserializer",
								capFirst(fieldName),
								capFirst(action)
						));
						changeName = cloneName.nestedClass(String.format("%s%sChange",
								capFirst(fieldName),
								capFirst(action)
						));
						change = TypeSpec
								.classBuilder(changeName)
								.superclass(Change.class)
								.addModifiers(PUBLIC, STATIC)
								.addField(FieldSpec.builder(cloneName, "target").addModifiers(PUBLIC).build());
						String changeLuxemTypeName =
								String.format("%s-%s", cloneName.simpleName(), changeName.simpleName());
						changeSerialize = poetMethod(sigChangeSerialize)
								.addCode("writer.type(\"$L\").recordBegin();\n", changeLuxemTypeName)
								.addCode("writer.key(\"target\").primitive($T.toString((($T)target).id()));\n",
										Long.class,
										ProjectObjectInterface.class
								);
						globalChangeDeserialize.addCode("if (\"$L\".equals(type)) return new $T(context);\n",
								changeLuxemTypeName,
								deserializerName
						);
						changeConstructor = MethodSpec
								.constructorBuilder()
								.addParameter(ProjectContextBase.class, "project")
								.addParameter(cloneName, "target")
								.addCode("this.target = target;\n");
						if (flattenPoint(classInfo))
							changeConstructor.addCode("this.target.incRef(project);\n");
						changeApply = CodeBlock.builder();
						changeApply2 = CodeBlock.builder();
						changeApplyNotify = CodeBlock.builder().add(
								"for ($T listener : new $T<>(target.$L)) listener.accept(target",
								listenerName,
								ArrayList.class,
								listenersFieldName
						);
						changeDelete = poetMethod(sigChangeDelete);
						if (flattenPoint(classInfo))
							changeDelete.addCode("target.decRef(project);\n");
						changeInvoke = MethodSpec
								.methodBuilder(String.format("%s%s", fieldName, capFirst(action)))
								.addModifiers(PUBLIC)
								.returns(changeStepBuilderName);
						changeDebugCounts = CodeBlock.builder().add("increment.accept(target);\n");
						deserializerValue = CodeBlock.builder().add("if (\"target\".equals(key)) {\n").indent().add(
								"out.target = ($T) context.getObject($T.parseLong(($T) value));\n",
								cloneName,
								Long.class,
								String.class
						).add("return;\n").unindent().add("}\n");
						deserializerArray = CodeBlock.builder();
						deserializerRecord = CodeBlock.builder();
					}

					public ChangeBuilder apply(Consumer<ChangeBuilder> f) {
						f.accept(this);
						return this;
					}

					public ClassName getName() {
						return changeName;
					}

					public ClassName getListenerName() {
						return listenerName;
					}

					public ChangeBuilder addMapParameter(TypeInfo key, TypeInfo value, String name, boolean inc) {
						TypeName keyName = toPoet(key);
						TypeName valueName = toPoet(value);
						TypeName map = ParameterizedTypeName.get(ClassName.get(Map.class), keyName, valueName);
						listenerAccept.addParameter(map, name);
						change.addField(FieldSpec.builder(map, name).addModifiers(PUBLIC).build());
						changeConstructor.addParameter(map, name).addCode(String.format("this.%s = %s;\n", name, name));
						if (flattenPoint(value)) {
							Function<Boolean, CodeBlock> incDecBuilder = inc0 -> {
								CodeBlock.Builder out = CodeBlock.builder();
								out
										.add("for (Map.Entry<$T, $T> e : $L.entrySet()) {\n", keyName, valueName, name)
										.indent();
								if (inc0)
									out.add("e.getValue().incRef(project);\n");
								else
									out.add("e.getValue().decRef(project);\n");
								out.unindent().add("};\n");
								return out.build();
							};
							changeConstructor.addCode(incDecBuilder.apply(true));
							changeApply2.add(incDecBuilder.apply(inc));
							changeDelete.addCode(incDecBuilder.apply(false));
							changeDebugCounts
									.add("for (Map.Entry<$T, $T> e : $L.entrySet()) {\n", keyName, valueName, name)
									.indent()
									.add("increment.accept(e.getValue());\n")
									.unindent()
									.add("};\n");
						}
						changeApplyNotify.add(String.format(", %s", name));
						changeInvoke.addParameter(map, name);
						invokeForward.add(name);
						CodeBlock.Builder serializeBlock = CodeBlock.builder();
						serializeBlock
								.add("writer.key(\"$L\").recordBegin();\n", name)
								.add("for (Map.Entry<$T, $T> e : $L.entrySet()) {\n", keyName, valueName, name)
								.indent();
						serializeBlock.add("writer.key($T.toString(e.getKey()));\n", Objects.class);
						deserializerValue.add("if (\"$L\".equals(key)) { out.$L = ($T) value; return; }\n",
								name,
								name,
								map
						);
						deserializerRecord
								.add("if (\"$L\".equals(key)) return new $T() {\n", name, StackReader.RecordState.class)
								.indent()
								.add("@Override\n")
								.add("public void value(Object value) {\n")
								.indent();
						if (flattenPoint(value)) {
							serializeBlock.add("writer.primitive($T.toString((($T) e.getValue()).id()));\n",
									Objects.class,
									ProjectObjectInterface.class
							);
							deserializerRecord.add(
									"data.put($L, context.getObject($T.parseLong(($T) value)));\n",
									generateScalarFromString(key, "key", deserializerArray, null),
									Long.class,
									String.class
							);
						} else {
							serializeBlock.add("writer.primitive($T.toString(e.getValue()));\n", Objects.class);
							deserializerRecord.add("data.put($L, $L);\n",
									generateScalarFromString(key, "key", deserializerArray, null),
									generateScalarFromString(value, "value", deserializerArray, null)
							);
						}
						serializeBlock.unindent().add("}\n").add("writer.recordEnd();\n");
						changeSerialize.addCode(serializeBlock.build());
						deserializerRecord.unindent().add("}\n").unindent().add("};\n");
						return this;
					}

					public ChangeBuilder addListParameter(TypeInfo type, String name, boolean inc) {
						TypeName element = toPoet(type);
						TypeName list = ParameterizedTypeName.get(ClassName.get(List.class), element);
						listenerAccept.addParameter(list, name);
						change.addField(FieldSpec.builder(list, name).addModifiers(PUBLIC).build());
						changeConstructor
								.addParameter(list, name)
								.addCode(String.format("this.%s = %s;\n", name, name));
						if (flattenPoint(type)) {
							Function<Boolean, CodeBlock> incDecBuilder = inc0 -> {
								CodeBlock.Builder out = CodeBlock.builder();
								out.add("for ($T e : $L) {\n", element, name).indent();
								if (inc0)
									out.add("e.incRef(project);\n");
								else
									out.add("e.decRef(project);\n");
								out.unindent().add("};\n");
								return out.build();
							};
							changeConstructor.addCode(incDecBuilder.apply(true));
							changeApply2.add(incDecBuilder.apply(inc));
							changeDelete.addCode(incDecBuilder.apply(false));
							changeDebugCounts
									.add("for ($T e : $L) {\n", element, name)
									.indent()
									.add("increment.accept(e);\n")
									.unindent()
									.add("};\n");
						}
						changeApplyNotify.add(String.format(", %s", name));
						changeInvoke.addParameter(list, name);
						invokeForward.add(name);
						changeSerialize
								.addCode("writer.key(\"$L\").arrayBegin();\n", name)
								.addCode("for ($T e : $L) ", element, name);
						deserializerValue.add("if (\"$L\".equals(key)) { out.$L = ($T) value; return; }\n",
								name,
								name,
								list
						);
						deserializerArray
								.add("if (\"$L\".equals(key)) return new $T() {\n", name, StackReader.ArrayState.class)
								.indent()
								.add("@Override\n")
								.add("public void value(Object value) {\n")
								.indent();
						if (flattenPoint(type)) {
							changeSerialize.addCode("writer.primitive($T.toString((($T)e).id()));\n",
									Objects.class,
									ProjectObjectInterface.class
							);
							deserializerArray.add("data.add(context.getObject($T.parseLong(($T) value)));\n",
									Long.class,
									String.class
							);
						} else {
							changeSerialize.addCode("writer.primitive($T.toString(e));\n", Objects.class);
							deserializerArray.add("data.add($L);\n",
									generateScalarFromString(type, "value", deserializerArray, null)
							);
						}
						changeSerialize.addCode("writer.arrayEnd();\n");
						deserializerArray.unindent().add("}\n").unindent().add("};\n");
						return this;
					}

					public ChangeBuilder addParameter(TypeInfo type, String name, boolean inc) {
						listenerAccept.addParameter(toPoet(type), name);
						change.addField(FieldSpec.builder(toPoet(type), name).addModifiers(PUBLIC).build());
						changeConstructor
								.addParameter(toPoet(type), name)
								.addCode(String.format("this.%s = %s;\n", name, name));
						if (flattenPoint(type)) {
							Function<Boolean, CodeBlock> incDecBuilder = inc0 -> {
								CodeBlock.Builder out = CodeBlock.builder().add("if ($L != null)\n", name).indent();
								if (inc0)
									out.add("$L.incRef(project);\n", name);
								else
									out.add("$L.decRef(project);\n", name);
								return out.unindent().build();
							};
							changeConstructor.addCode(incDecBuilder.apply(true));
							changeApply2.add(incDecBuilder.apply(inc));
							changeDelete.addCode(incDecBuilder.apply(false));
							changeDebugCounts.add("if ($L != null) increment.accept($L);\n", name, name);
						}
						changeApplyNotify.add(String.format(", %s", name));
						changeInvoke.addParameter(toPoet(type), name);
						invokeForward.add(name);
						changeSerialize.addCode("writer.key(\"$L\")", name);
						deserializerValue.add("if (\"$L\".equals(key)) {\n", name).indent();
						if (flattenPoint(type)) {
							changeSerialize.addCode(".primitive($L == null ? \"null\" : $T.toString($L.id()));\n",
									name,
									Objects.class,
									name
							);
							deserializerValue.add(
									"out.$L = ($T) context.getObject(\"null\".equals(value) ? null : $T.parseLong(($T) value));\n",
									name,
									toPoet(type),
									Long.class,
									String.class
							);
						} else {
							changeSerialize.addCode(".primitive($T.toString($L));\n", Objects.class, name);
							deserializerRecord.add("if (\"$L\".equals(key)) {\n", name).indent();
							deserializerValue
									.add("out.$L = ", name)
									.add(generateScalarFromString(type, "value", deserializerValue, deserializerRecord))
									.add(";\n", name);
							deserializerRecord.unindent().add("}\n");
						}
						deserializerValue.add("return;\n").unindent().add("}\n");
						return this;
					}

					public ChangeBuilder addCode(CodeBlock code) {
						changeApply.add(code);
						return this;
					}

					public ChangeBuilder addCode(String format, Object... args) {
						changeApply.add(CodeBlock
								.builder()
								.add(format,
										Arrays.stream(args).map(a -> a == CHANGE_TOKEN_NAME ? changeName : a).toArray()
								)
								.build());
						return this;
					}

					public ChangeBuilder listenersAdd(String format, Object... args) {
						listenersAdd.add(format, args);
						return this;
					}

					public ChangeBuilder mergeAdd(String format, Object... args) {
						merge.add(format, args);
						return this;
					}

					public ChangeBuilder mergeAdd(CodeBlock code) {
						merge.add(code);
						return this;
					}

					public void finish() {
						listener.addMethod(listenerAccept.build());
						clone.addType(listener.build());
						clone.addMethod(MethodSpec
								.methodBuilder(String.format("add%s", capFirst(listenersFieldName)))
								.returns(listenerName)
								.addModifiers(PUBLIC)
								.addParameter(listenerName, "listener")
								.addCode("$L.add(listener);\n", listenersFieldName)
								.addCode(listenersAdd.build())
								.addCode("return listener;\n")
								.build());
						clone.addMethod(MethodSpec
								.methodBuilder(String.format("remove%s", capFirst(listenersFieldName)))
								.addModifiers(PUBLIC)
								.addParameter(listenerName, "listener")
								.addCode("$L.remove(listener);\n", listenersFieldName)
								.build());
						CodeBlock mergeBuilt = merge.build();
						change.addMethod(poetMethod(sigChangeMerge)
								.addCode(mergeBuilt.isEmpty() ?
										CodeBlock.builder().add("return false;\n").build() :
										mergeBuilt)
								.build());
						change
								.addMethod(MethodSpec.constructorBuilder().build())
								.addMethod(changeConstructor.build())
								.addMethod(poetMethod(sigChangeApply)
										.addCode(changeApply.build())
										.addCode(changeApply2.build())
										.addCode(changeApplyNotify.add(");\n").build())
										.build())
								.addMethod(changeSerialize.addCode("writer.recordEnd();\n").build())
								.addMethod(changeDelete.build());
						if (flattenPoint(classInfo))
							change.addMethod(poetMethod(sigChangeDebugCounts)
									.addCode(changeDebugCounts.build())
									.build());
						clone.addType(change.build());
						clone.addType(TypeSpec
								.classBuilder(deserializerName)
								.addModifiers(STATIC)
								.superclass(StackReader.RecordState.class)
								.addField(FieldSpec.builder(ChangeDeserializationContext.class, "context").build())
								.addField(FieldSpec
										.builder(changeName, "out")
										.initializer("new $T()", changeName)
										.build())
								.addMethod(MethodSpec
										.constructorBuilder()
										.addParameter(ChangeDeserializationContext.class, "context")
										.addCode("this.context = context;\n")
										.build())
								.addMethod(poetMethod(sigStateValue).addCode(deserializerValue.add(
										"throw new $T(String.format(\"Key (%s) is unknown\", key));\n",
										RuntimeException.class
								).build()).build())
								.addMethod(poetMethod(sigStateArray).addCode(deserializerArray.add(
										"throw new $T(String.format(\"Key (%s) is unknown or is not an array\", key));\n",
										RuntimeException.class
								).build()).build())
								.addMethod(poetMethod(sigStateRecord).addCode(deserializerRecord.add(
										"throw new $T(String.format(\"Key (%s) is unknown or is not a record\", key));\n",
										RuntimeException.class
								).build()).build())
								.addMethod(poetMethod(sigStateGet).addCode("return out;\n").build())
								.build());
						typeChangeStepBuilder.addMethod(changeInvoke
								.addCode("$T change = new $T(changeStepBuilder.context, target$L);\n",
										changeName,
										changeName,
										invokeForward
												.stream()
												.map(n -> String.format(", %s", n))
												.collect(Collectors.joining(""))
								)
								.addCode("change.apply(changeStepBuilder.context, changeStepBuilder.changeStep);\n")
								.addCode("change.delete(changeStepBuilder.context);\n")
								.addCode("return changeStepBuilder;\n")
								.build());
					}

					public ChangeBuilder addCodeIf(boolean condition, String format, Object... args) {
						if (!condition)
							return this;
						return addCode(format, args);
					}

					public ChangeBuilder indent() {
						changeApply.indent();
						return this;
					}

					public ChangeBuilder unindent() {
						changeApply.unindent();
						return this;
					}
				}
				class GenerateScalar {
					public void applyFieldOrigin(TypeInfo field) {
						// Getter
						MethodSpec.Builder getter =
								MethodSpec.methodBuilder(fieldName).returns(toPoet(field)).addModifiers(PUBLIC);
						clone.addMethod(getter.addCode("return $L;\n", fieldName).build());

						// Mutation
						if (!Stream.of("id", "refCount").anyMatch(fieldName::equals)) {
							ChangeBuilder setBuilder = new ChangeBuilder(fieldName, "set");
							CodeBlock.Builder changeMerge = CodeBlock.builder().add(
									"if (other.getClass() != getClass() || (($T)other).target == target) return false;\n",
									setBuilder.changeName
							);
							if (flattenPoint(field))
								changeMerge.add("value.decRef(context);\n");
							changeMerge.add("value = (($T)other).value;\n", setBuilder.changeName);
							if (flattenPoint(field))
								changeMerge.add("value.incRef(context);\n");
							changeMerge.add("return true;\n");
							setBuilder
									.addParameter(field, "value", false)
									.addCode("if ($T.equals(value, target.$L)) return;\n", Objects.class, fieldName)
									.addCode(
											"changeStep.add(project, new $T(project, target, target.$L));\n",
											CHANGE_TOKEN_NAME,
											fieldName
									)
									.addCode("target.$L = value;\n", fieldName)
									.listenersAdd("listener.accept(this, $L);\n", fieldName)
									.mergeAdd(changeMerge.build())
									.finish();
							CodeBlock.Builder initialSetCode = CodeBlock
									.builder()
									.add("if (refCount > 0) throw new $T();\n", Assertion.class)
									.add("this.$L = value;\n", fieldName);
							if (flattenPoint(field))
								initialSetCode.add("value.incRef(project);\n");
							String initialSetName = String.format("initial%sSet", capFirst(fieldName));
							clone.addMethod(MethodSpec
									.methodBuilder(initialSetName)
									.addModifiers(PUBLIC)
									.addParameter(ProjectContextBase.class, "project")
									.addParameter(toPoet(field), "value")
									.addCode(initialSetCode.build())
									.build());
						}
					}

					public void applyLeaf(TypeInfo field) {
						// Serialize
						cloneSerialize.add(generateScalarSerialize(field, fieldName));

						// Deserialize
						{
							CodeBlock value = generateScalarFromString(field,
									"value",
									cloneDeserializeValue,
									cloneDeserializeRecord
							);
							if (value != null)
								cloneDeserializeValue.add("out.$L = ", fieldName).add(value).add(";\n");
						}

						// Dec ref
						if (flattenPoint(field)) {
							cloneDecRef.add("(($T) $L).decRef(project);\n", ProjectObjectInterface.class, fieldName);
						}
					}
				}
				GenerateScalar generateScalar = new GenerateScalar();

				FieldSpec.Builder field = FieldSpec.builder(toPoet(fieldInfo), fieldName);
				cloneSerialize.add("writer.key(\"$L\");\n", fieldName);

				// Create initializer, getters + mutators, changes
				if (pair.second.field.getDeclaringClass() == klass) {
					if (fieldInfo.type == String.class) {
						generateScalar.applyFieldOrigin(fieldInfo);
					} else if (fieldInfo.type == Integer.class ||
							fieldInfo.type == int.class ||
							fieldInfo.type == SimpleIntegerProperty.class ||
							fieldInfo.type == Long.class ||
							fieldInfo.type == long.class ||
							fieldInfo.type == SimpleLongProperty.class ||
							fieldInfo.type == Float.class ||
							fieldInfo.type == float.class ||
							fieldInfo.type == SimpleDoubleProperty.class ||
							fieldInfo.type == Double.class ||
							fieldInfo.type == double.class ||
							fieldInfo.type == Boolean.class ||
							fieldInfo.type == boolean.class ||
							((Class<?>) fieldInfo.type).isEnum()) {
						generateScalar.applyFieldOrigin(fieldInfo);
					} else if (((Class) fieldInfo.type).isAssignableFrom(ArrayList.class)) {
						field.initializer("new $T()", ArrayList.class);

						// Getters
						clone.addMethod(MethodSpec
								.methodBuilder(fieldName + "Get")
								.addModifiers(PUBLIC)
								.returns(toPoet(fieldInfo.parameters[0]))
								.addParameter(Integer.class, "index")
								.addCode("return $L.get(index);\n", fieldName)
								.build());
						clone.addMethod(MethodSpec
								.methodBuilder(fieldName + "Length")
								.addModifiers(PUBLIC)
								.returns(int.class)
								.addCode("return $L.size();\n", fieldName)
								.build());
						clone.addMethod(MethodSpec
								.methodBuilder(fieldName)
								.addModifiers(PUBLIC)
								.returns(toPoet(fieldInfo))
								.addCode("return $T.unmodifiableList($L);\n", Collections.class, fieldName)
								.build());

						// Mutation
						TypeInfo elementType = fieldInfo.parameters[0];

						CodeBlock.Builder initialAddCode = CodeBlock
								.builder()
								.add("if (refCount > 0) throw new $T();\n", Assertion.class)
								.add("this.$L.addAll(values);\n", fieldName);
						if (flattenPoint(elementType))
							initialAddCode.add("values.forEach(v -> v.incRef(project));\n");
						String initialSetName = String.format("initial%sAdd", capFirst(fieldName));
						clone.addMethod(MethodSpec
								.methodBuilder(initialSetName)
								.addModifiers(PUBLIC)
								.addParameter(ProjectContextBase.class, "project")
								.addParameter(
										ParameterizedTypeName.get(ClassName.get(List.class), toPoet(elementType)),
										"values"
								)
								.addCode(initialAddCode.build())
								.build());

						ChangeBuilder addBuilder = new ChangeBuilder(fieldName, "add");
						ChangeBuilder removeBuilder = new ChangeBuilder(fieldName, "remove");
						addBuilder
								.addParameter(new TypeInfo(int.class), "at", false)
								.addListParameter(elementType, "value", true)
								.addCode("target.$L.addAll(at, value);\n", fieldName)
								.addCode(
										"changeStep.add(project, new $T(project, target, at, value.size()));\n",
										removeBuilder.getName()
								)
								.listenersAdd("listener.accept(this, 0, $L);\n", fieldName)
								.finish();
						TypeName listType = ParameterizedTypeName.get(ClassName.get(List.class), toPoet(elementType));
						removeBuilder
								.addParameter(new TypeInfo(int.class), "at", false)
								.addParameter(new TypeInfo(int.class), "count", false)
								.addCode("final $T sublist = target.$L.subList(at, at + count);\n", listType, fieldName)
								.addCode(
										"changeStep.add(project, new $T(project, target, at, new $T(sublist)));\n",
										addBuilder.getName(),
										ArrayList.class
								);
						if (flattenPoint(elementType))
							removeBuilder.addCode("sublist.forEach(e -> e.decRef(project));\n");
						removeBuilder.addCode("sublist.clear();\n").finish();
						ChangeBuilder clearBuilder = new ChangeBuilder(fieldName, "clear").addCode(
								"changeStep.add(project, new $T(project, target, 0, new $T(target.$L)));\n",
								addBuilder.getName(),
								ArrayList.class,
								fieldName
						);
						if (flattenPoint(elementType))
							clearBuilder.addCode("target.$L.forEach(e -> e.decRef(project));\n", fieldName);
						clearBuilder.addCode("target.$L.clear();\n", fieldName).mergeAdd(
								"return other.getClass() == $T.class && (($T)other).target == target;\n",
								clearBuilder.changeName,
								clearBuilder.changeName
						);
						clearBuilder.finish();
						typeChangeStepBuilder.addMethod(MethodSpec
								.methodBuilder(String.format("%sAdd", fieldName))
								.addModifiers(PUBLIC)
								.returns(changeStepBuilderName)
								.addParameter(int.class, "at")
								.addParameter(toPoet(elementType), "value")
								.addCode("return $LAdd(at, $T.<$T>of(value));\n",
										fieldName,
										ImmutableList.class,
										toPoet(elementType)
								)
								.build());
						typeChangeStepBuilder.addMethod(MethodSpec
								.methodBuilder(String.format("%sAdd", fieldName))
								.addModifiers(PUBLIC)
								.returns(changeStepBuilderName)
								.addParameter(toPoet(elementType), "value")
								.addCode("return $LAdd(target.$LLength(), value);\n", fieldName, fieldName)
								.build());
						ChangeBuilder moveToBuilder = new ChangeBuilder(fieldName, "moveTo")
								.addParameter(new TypeInfo(int.class), "source", false)
								.addParameter(new TypeInfo(int.class), "count", false)
								.addParameter(new TypeInfo(int.class), "dest", false)
								.addCode("if (source == dest) return;\n")
								.addCode(
										"if (count >= target.$L.size()) throw new $T(\"Count is greater than size.\");\n",
										fieldName,
										Assertion.class
								)
								.addCode("source = $T.min(source, target.$L.size() - count);\n", Math.class, fieldName)
								.addCode(
										"changeStep.add(project, new $T(project, target, dest, count, source));\n",
										CHANGE_TOKEN_NAME
								)
								.addCode("$T sublist = target.$L.subList(source, source + count);\n",
										listType,
										fieldName
								)
								.addCode("for ($T e : sublist) (($T) e).incRef(project);\n",
										toPoet(fieldInfo.parameters[0]),
										ProjectObjectInterface.class
								)
								.addCode("$T readd = new $T(sublist);\n", listType, ArrayList.class)
								.addCode("sublist.clear();\n")
								.addCode("target.$L.addAll(dest, readd);\n", fieldName);
						moveToBuilder.finish();
						typeChangeStepBuilder.addMethod(MethodSpec
								.methodBuilder(String.format("%sMoveUp", capFirst(fieldName)))
								.returns(changeStepBuilderName)
								.addParameter(int.class, "at")
								.addParameter(int.class, "count")
								.addCode("if (at == 0) return changeStepBuilder;\n")
								.addCode("return $LMoveTo(at, count, at - 1);\n", fieldName)
								.build());
						typeChangeStepBuilder.addMethod(MethodSpec
								.methodBuilder(String.format("%sMoveDown", capFirst(fieldName)))
								.returns(changeStepBuilderName)
								.addParameter(int.class, "at")
								.addParameter(int.class, "count")
								.addCode("if (at == target.$L.size() - 1) return changeStepBuilder;\n", fieldName)
								.addCode("return $LMoveTo(at, count, at + 1);\n", fieldName)
								.build());

						clone.addMethod(MethodSpec
								.methodBuilder(String.format("mirror%s", capFirst(fieldName)))
								.returns(Runnable.class)
								.addTypeVariable(TypeVariableName.get("T"))
								.addModifiers(PUBLIC)
								.addParameter(ParameterizedTypeName.get(ClassName.get(List.class),
										TypeVariableName.get("T")
								), "list")
								.addParameter(
										ParameterizedTypeName.get(ClassName.get(Function.class),
												toPoet(elementType),
												TypeVariableName.get("T")
										),
										"create"
								)
								.addParameter(ParameterizedTypeName.get(ClassName.get(Consumer.class),
										TypeVariableName.get("T")
								), "remove")
								.addParameter(ParameterizedTypeName.get(Consumer.class,
										Integer.class /* Index of first change */
								), "change")
								.addCode(CodeBlock
										.builder()
										.add("return new $T() {\n", Runnable.class)
										.indent()
										.add("boolean dead = false;\n")
										.add("\n")
										.add(
												"$T addListener = add$LAddListeners((target, at, values) -> {\n",
												addBuilder.getListenerName(),
												capFirst(fieldName)
										)
										.indent()
										.add("if (dead) return;\n")
										.add(
												"list.addAll(at, values.stream().map(create).collect($T.toList()));\n",
												Collectors.class
										)
										.add("change.accept(at);\n")
										.unindent()
										.add("});\n")
										.add(
												"$T removeListener = add$LRemoveListeners((target, at, count) -> {\n",
												removeBuilder.getListenerName(),
												capFirst(fieldName)
										)
										.indent()
										.add("if (dead) return;\n")
										.add("List<$T> sublist = list.subList(at, at + count);\n",
												TypeVariableName.get("T")
										)
										.add("sublist.forEach(remove);\n")
										.add("sublist.clear();\n")
										.add("change.accept(at);\n")
										.unindent()
										.add("});\n")
										.add("$T clearListener = add$LClearListeners((target) -> {\n",
												clearBuilder.getListenerName(),
												capFirst(fieldName)
										)
										.indent()
										.add("if (dead) return;\n")
										.add("list.clear();\n")
										.add("change.accept(0);\n")
										.unindent()
										.add("});\n")
										.add(
												"$T moveToListener = add$LMoveToListeners((target, source, count, dest) -> {\n",
												moveToBuilder.getListenerName(),
												capFirst(fieldName)
										)
										.indent()
										.add("if (dead) return;\n")
										.add(
												"List<$T> sublist = list.subList(source, source + count);\n",
												TypeVariableName.get("T")
										)
										.add("List<$T> temp = new ArrayList(sublist);\n", TypeVariableName.get("T"))
										.add("sublist.clear();\n")
										.add("list.addAll(dest, temp);\n")
										.add("change.accept($T.min(source, dest));\n", Math.class)
										.unindent()
										.add("});\n")
										.add("\n")
										.add("@$T\n", Override.class)
										.add("public void run() {\n")
										.indent()
										.add("dead = true;\n")
										.add("remove$LAddListeners(addListener);\n", capFirst(fieldName))
										.add("remove$LRemoveListeners(removeListener);\n", capFirst(fieldName))
										.add("remove$LMoveToListeners(moveToListener);\n", capFirst(fieldName))
										.add("remove$LClearListeners(clearListener);\n", capFirst(fieldName))
										.unindent()
										.add("}\n")
										.unindent()
										.add("};\n")
										.build())
								.build());
					} else if (((Class) fieldInfo.type).isAssignableFrom(HashSet.class)) {
						field.initializer("new $T()", HashSet.class);

						// Getters
						clone.addMethod(MethodSpec
								.methodBuilder(fieldName + "Has")
								.addModifiers(PUBLIC)
								.returns(boolean.class)
								.addParameter(toPoet(fieldInfo.parameters[0]), "value")
								.addCode("return $L.contains(value);\n", fieldName)
								.build());
						clone.addMethod(MethodSpec
								.methodBuilder(fieldName + "Length")
								.addModifiers(PUBLIC)
								.returns(int.class)
								.addCode("return $L.size();\n", fieldName)
								.build());
						clone.addMethod(MethodSpec
								.methodBuilder(fieldName)
								.addModifiers(PUBLIC)
								.returns(toPoet(fieldInfo))
								.addCode("return $T.unmodifiableSet($L);\n", Collections.class, fieldName)
								.build());

						// Mutation
						ChangeBuilder addBuilder = new ChangeBuilder(fieldName, "add");
						ChangeBuilder removeBuilder = new ChangeBuilder(fieldName, "remove");
						TypeInfo elementType = fieldInfo.parameters[0];
						addBuilder
								.addCode("if (target.$L.contains(value)) return;\n")
								.addListParameter(elementType, "value", true)
								.addCode("target.$L.addAll(value);\n", fieldName)
								.addCode(("changeStep.add(project, new $T(project, target, value));\n"),
										removeBuilder.getName()
								)
								.mergeAdd(CodeBlock
										.builder()
										.add(
												"if (other.getClass() != $T.class || (($T)other).target != target) return false;\n",
												addBuilder.changeName,
												addBuilder.changeName
										)
										.add("(($T)other).value.forEach(e -> {\n")
										.indent()
										.add("if (value.contains(e)) return;\n")
										.add("e.incRef(context);\n")
										.add("value.add(e);\n")
										.unindent()
										.add("}\n")
										.build())
								.finish();
						removeBuilder
								.addCode("if (!target.$L.remove(value)) return;\n", fieldName)
								.addListParameter(elementType, "value", false)
								.addCode(("changeStep.add(project, new $T(project, target, value));\n"),
										addBuilder.getName()
								)
								.finish();
						ChangeBuilder clearBuilder = new ChangeBuilder(fieldName, "clear").addCode(
								"changeStep.add(project, new $T(project, target, 0, new $T($L)));\n",
								addBuilder.getName(),
								ArrayList.class
						);
						if (flattenPoint(elementType))
							clearBuilder.addCode("target.$L.forEach(e -> e.decRef(project));\n");
						clearBuilder.addCode("target.$L.clear();\n", fieldName).mergeAdd(
								"return other.getClass() == $T.class && (($T)other).target == target;\n",
								clearBuilder.changeName,
								clearBuilder.changeName
						).finish();
						typeChangeStepBuilder.addMethod(MethodSpec
								.methodBuilder(String.format("%sAdd"))
								.addModifiers(PUBLIC)
								.addParameter(toPoet(elementType), "value")
								.addCode("return $LAdd($T.of(value));\n", fieldName, ImmutableList.class)
								.build());
						typeChangeStepBuilder.addMethod(MethodSpec
								.methodBuilder(String.format("%sRemove"))
								.addModifiers(PUBLIC)
								.addParameter(toPoet(elementType), "value")
								.addCode("return $LRemove($T.of(value));\n", fieldName, ImmutableList.class)
								.build());
					} else if (((Class) fieldInfo.type).isAssignableFrom(HashMap.class)) {
						field.initializer("new $T()", HashMap.class);

						// Getters
						clone.addMethod(MethodSpec
								.methodBuilder(fieldName + "Get")
								.addModifiers(PUBLIC)
								.returns(toPoet(fieldInfo.parameters[1]))
								.addParameter(fieldInfo.parameters[0].type, "key")
								.addCode("return $L.get(key);\n", fieldName)
								.build());
						clone.addMethod(MethodSpec
								.methodBuilder(fieldName + "Has")
								.addModifiers(PUBLIC)
								.returns(boolean.class)
								.addParameter(toPoet(fieldInfo.parameters[0]), "key")
								.addCode("return $L.containsKey(key);\n", fieldName)
								.build());
						clone.addMethod(MethodSpec
								.methodBuilder(fieldName)
								.addModifiers(PUBLIC)
								.returns(toPoet(fieldInfo))
								.addCode("return $T.unmodifiableMap($L);\n", Collections.class, fieldName)
								.build());

						// Mutation
						CodeBlock.Builder initialPutAllCode = CodeBlock
								.builder()
								.add("if (refCount > 0) throw new $T();\n", Assertion.class)
								.add("this.$L.putAll(values);\n", fieldName);
						if (flattenPoint(fieldInfo.parameters[1]))
							initialPutAllCode.add("values.values().forEach(v -> v.incRef(project));\n");
						String initialSetName = String.format("initial%sPutAll", capFirst(fieldName));
						clone.addMethod(MethodSpec
								.methodBuilder(initialSetName)
								.addModifiers(PUBLIC)
								.addParameter(ProjectContextBase.class, "project")
								.addParameter(ParameterizedTypeName.get(ClassName.get(Map.class),
										toPoet(fieldInfo.parameters[0]),
										toPoet(fieldInfo.parameters[1])
								), "values")
								.addCode(initialPutAllCode.build())
								.build());

						ChangeBuilder putBuilder = new ChangeBuilder(fieldName, "putAll");
						CodeBlock.Builder putCode = CodeBlock
								.builder()
								.add("if (remove.stream().anyMatch(put::containsKey)) throw new $T();\n",
										Assertion.class /* DEBUG */
								)
								.add("$T<$T, $T> removing = $T.concat(\n",
										Map.class,
										toPoet(fieldInfo.parameters[0]),
										toPoet(fieldInfo.parameters[1]),
										Stream.class
								)
								.indent()
								.add("remove.stream().filter(k -> target.$L.containsKey(k)),\n", fieldName)
								.add("put.keySet().stream().filter(k -> target.$L.containsKey(k))\n", fieldName)
								.unindent()
								.add(").collect($T.toMap(k -> k, k -> target.$L.get(k)));\n",
										Collectors.class,
										fieldName
								)
								.add("changeStep.add(project, new $T(project, target, removing,\n",
										putBuilder.getName()
								)
								.indent()
								.add("new ArrayList<>($T.difference(put.keySet(), target.$L.keySet()))\n",
										Sets.class,
										fieldName
								)
								.unindent()
								.add("));\n");
						if (flattenPoint(fieldInfo.parameters[1]))
							putCode.add("removing.entrySet().forEach(e -> e.getValue().decRef(project));\n");
						putCode
								.add("remove.forEach(k -> target.$L.remove(k));\n", fieldName)
								.add("target.$L.putAll(put);\n", fieldName);
						CodeBlock.Builder putMergeCode = CodeBlock
								.builder()
								.add(
										"if (other.getClass() != $T.class || (($T)other).target != target) return false;\n",
										putBuilder.changeName,
										putBuilder.changeName
								)
								.add("remove.addAll((($T)other).remove);\n", putBuilder.changeName)
								.add("(($T)other).put.forEach((k, v) -> {\n", putBuilder.changeName)
								.indent()
								.add("if (put.containsKey(k) || remove.contains(k)) return;\n")
								.add("put.put(k, v);\n");
						if (flattenPoint(fieldInfo.parameters[1]))
							putMergeCode.add("v.incRef(context);\n");
						putMergeCode.unindent().add("});\n").add("return true;\n");
						putBuilder
								.addMapParameter(fieldInfo.parameters[0], fieldInfo.parameters[1], "put", true)
								.addListParameter(fieldInfo.parameters[0], "remove", false)
								.addCode(putCode.build())
								.listenersAdd("listener.accept(this, $L, $T.of());\n", fieldName, ImmutableList.class)
								.mergeAdd(putMergeCode.build())
								.finish();
						typeChangeStepBuilder.addMethod(MethodSpec
								.methodBuilder(String.format("%sPut", fieldName))
								.addParameter(toPoet(fieldInfo.parameters[0]), "key")
								.addParameter(toPoet(fieldInfo.parameters[1]), "value")
								.addModifiers(PUBLIC)
								.addCode("$LPutAll($T.of(key, value), $T.of());\n",
										fieldName,
										ImmutableMap.class,
										ImmutableList.class
								)
								.build());
						ChangeBuilder clearBuilder = new ChangeBuilder(fieldName, "clear");
						clearBuilder.addCode(
								"changeStep.add(project, new $T(project, target, new $T(target.$L), new $T()));\n",
								putBuilder.getName(),
								HashMap.class,
								fieldName,
								ArrayList.class
						).addCode("target.$L.clear();\n", fieldName).mergeAdd(
								"return other.getClass() == $T.class && (($T)other).target == target;\n",
								clearBuilder.changeName,
								clearBuilder.changeName
						).finish();
					} else if (flattenPoint(fieldInfo)) {
						generateScalar.applyFieldOrigin(fieldInfo);
					} else {
						generateScalar.applyFieldOrigin(fieldInfo);
					}

					clone.addField(field.build());
				}

				// Create serialize, deserialize
				if (Walk.isConcrete(classInfo)) {
					cloneDeserializeValue.add("if (\"$L\".equals(key)) {\n", fieldName).indent();
					if (fieldInfo.type == String.class) {
						generateScalar.applyLeaf(fieldInfo);
					} else if (fieldInfo.type == Integer.class || fieldInfo.type == int.class) {
						generateScalar.applyLeaf(fieldInfo);
					} else if (fieldInfo.type == Long.class || fieldInfo.type == long.class) {
						generateScalar.applyLeaf(fieldInfo);
					} else if (fieldInfo.type == Float.class || fieldInfo.type == float.class) {
						generateScalar.applyLeaf(fieldInfo);
					} else if (fieldInfo.type == Double.class || fieldInfo.type == double.class) {
						generateScalar.applyLeaf(fieldInfo);
					} else if (fieldInfo.type == Boolean.class || fieldInfo.type == boolean.class) {
						generateScalar.applyLeaf(fieldInfo);
					} else if (((Class<?>) fieldInfo.type).isEnum()) {
						generateScalar.applyLeaf(fieldInfo);
					} else if (((Class) fieldInfo.type).isAssignableFrom(ArrayList.class)) {
						// Serialize
						cloneSerialize
								.add("writer.arrayBegin();\n")
								.add("for ($T e : $L) {\n", toPoet(fieldInfo.parameters[0]), fieldName)
								.indent()
								.add(generateScalarSerialize(fieldInfo.parameters[0], "e"))
								.unindent()
								.add("}\n")
								.add("writer.arrayEnd();\n");

						// Deserialize
						cloneDeserializeArray.add("if (\"$L\".equals(key)) return new ", fieldName);
						if (flattenPoint(fieldInfo.parameters[0])) {
							cloneDeserializeArray.add("$T(context);\n", IDListState.class);
							cloneDecRef.add("for ($T e : $L) (($T) e).decRef(project);\n",
									toPoet(fieldInfo.parameters[0]),
									fieldName,
									ProjectObjectInterface.class
							);
						} else {
							cloneDeserializeArray
									.add("$T() {\n", GeneralListState.class)
									.indent()
									.add("public void value(Object value) {\n")
									.indent();
							CodeBlock.Builder valueCode = CodeBlock.builder();
							CodeBlock.Builder recordCode = CodeBlock.builder();
							CodeBlock value =
									generateScalarFromString(fieldInfo.parameters[0], "value", valueCode, recordCode);
							cloneDeserializeArray.add("data.add(").add(value).add(");\n").unindent().add("}\n");
							CodeBlock recordCode1 = recordCode.build();
							if (!recordCode1.isEmpty())
								cloneDeserializeArray
										.add("public $T record() {\n", StackReader.State.class)
										.indent()
										.add(recordCode1)
										.unindent()
										.add("}\n");
							cloneDeserializeArray.add("};\n");
						}
						cloneDeserializeValue.add("out.$L = ($T) value;\n", fieldName, toPoet(fieldInfo));
					} else if (((Class) fieldInfo.type).isAssignableFrom(HashSet.class)) {
						// Serialize
						cloneSerialize
								.add("writer.arrayBegin();\n")
								.add("for ($T e : $L) {\n", toPoet(fieldInfo.parameters[0]), fieldName)
								.indent()
								.add(generateScalarSerialize(fieldInfo.parameters[0], "e"))
								.unindent()
								.add("}\n")
								.add("writer.arrayEnd();\n");

						// Deserialize
						cloneDeserializeArray.add("if (\"$L\".equals(key)) return new ", fieldName);
						if (flattenPoint(fieldInfo.parameters[0])) {
							cloneDeserializeArray.add("$T(context);\n", IDSetState.class);
							cloneDecRef.add("for ($T e : $L) (($T) e).decRef(project);\n",
									toPoet(fieldInfo.parameters[0]),
									fieldName,
									ProjectObjectInterface.class
							);
						} else {
							cloneDeserializeArray
									.add("$T() {\n", GeneralSetState.class)
									.indent()
									.add("public void value(Object value) {\n")
									.indent();
							CodeBlock.Builder valueCode = CodeBlock.builder();
							CodeBlock.Builder recordCode = CodeBlock.builder();
							CodeBlock value =
									generateScalarFromString(fieldInfo.parameters[0], "value", valueCode, recordCode);
							cloneDeserializeArray.add("data.add(").add(value).add(");\n").unindent().add("}\n");
							CodeBlock recordCode1 = recordCode.build();
							if (!recordCode1.isEmpty())
								cloneDeserializeArray
										.add("public $T record() {\n", StackReader.State.class)
										.indent()
										.add(recordCode1)
										.unindent()
										.add("}\n");
							cloneDeserializeArray.add("};\n");
						}
						cloneDeserializeValue.add("out.$L = ($T) value;\n", fieldName, toPoet(fieldInfo));
					} else if (((Class) fieldInfo.type).isAssignableFrom(HashMap.class)) {
						// Serialize
						cloneSerialize
								.add("writer.recordBegin();\n")
								.add("for (Map.Entry<$T, $T> p : $L.entrySet()) {\n",
										toPoet(fieldInfo.parameters[0]),
										toPoet(fieldInfo.parameters[1]),
										fieldName
								)
								.indent()
								.add("writer.key($T.toString(p.getKey()));\n", Objects.class)
								.add(generateScalarSerialize(fieldInfo.parameters[1], "p.getValue()"))
								.unindent()
								.add("}\n")
								.add("writer.recordEnd();\n");

						// Deserialize
						cloneDeserializeRecord.add("if (\"$L\".equals(key)) return ", fieldName);
						if (flattenPoint(fieldInfo.parameters[1])) {
							cloneDeserializeRecord.add("new $T(context, k -> {\n", IDMapState.class).indent().add(
									"return $L;\n",
									generateScalarFromString(fieldInfo.parameters[0], "k", cloneDeserializeRecord, null)
							).unindent().add("});\n");
							cloneDecRef.add(
									"for (Map.Entry<$T, $T> e : $L.entrySet()) (($T) e.getValue()).decRef(project);\n",
									toPoet(fieldInfo.parameters[0]),
									toPoet(fieldInfo.parameters[1]),
									fieldName,
									ProjectObjectInterface.class
							);
						} else {
							cloneDeserializeRecord
									.add("new $T() {\n", GeneralMapState.class)
									.indent()
									.add("public void value(Object value) {\n")
									.indent();
							CodeBlock.Builder valueCode = CodeBlock.builder();
							CodeBlock.Builder recordCode = CodeBlock.builder();
							CodeBlock key =
									generateScalarFromString(fieldInfo.parameters[0], "key", valueCode, recordCode);
							CodeBlock value =
									generateScalarFromString(fieldInfo.parameters[1], "value", valueCode, recordCode);
							cloneDeserializeRecord
									.add("data.put(")
									.add(key)
									.add(", ")
									.add(value)
									.add(");\n")
									.unindent()
									.add("}\n");
							CodeBlock recordCode1 = recordCode.build();
							if (!recordCode1.isEmpty())
								cloneDeserializeRecord
										.add("public $T record() {\n", StackReader.State.class)
										.indent()
										.add(recordCode1)
										.unindent()
										.add("}\n");
							cloneDeserializeRecord.unindent().add("};\n");
						}
						cloneDeserializeValue.add("out.$L = ($T) value;\n", fieldName, toPoet(fieldInfo));
					} else if (flattenPoint(fieldInfo)) {
						generateScalar.applyLeaf(fieldInfo);

						// Deserialize
						cloneDeserializeValue.add(
								"if (!\"null\".equals(value)) map.put(\"$L\", $T.parseLong(($T) value));\n",
								fieldName,
								Long.class,
								String.class
						);
						cloneDeserializeFinish.add("out.$L = ($T) context.getObject(map.get(\"$L\"));\n",
								fieldName,
								toPoet(fieldInfo),
								fieldName
						);
					} else {
						cloneDeserializeRecord.add("if (\"$L\".equals(key)) {\n", fieldName).indent();
						generateScalar.applyLeaf(fieldInfo);
						cloneDeserializeRecord.unindent().add("}\n");
					}
					cloneDeserializeValue.add("return;\n").unindent().add("};\n");
				}
			}

			write(typeChangeStepBuilderName, typeChangeStepBuilder.build());
			clone.addMethod(MethodSpec.constructorBuilder().build());
			if (Walk.isConcrete(classInfo)) {
				ClassName deserializerName = cloneName.nestedClass("Deserializer");
				ClassName finisherName = deserializerName.nestedClass("Finisher");
				TypeSpec.Builder deserializerBuilder = TypeSpec
						.classBuilder(deserializerName)
						.addModifiers(PUBLIC, STATIC)
						.superclass(StackReader.RecordState.class)
						.addType(TypeSpec
								.classBuilder(finisherName)
								.superclass(ModelDeserializationContext.Finisher.class)
								.addModifiers(PUBLIC)
								.addMethod(poetMethod(sigFinisherFinish)
										.addCode(cloneDeserializeFinish.build())
										.build())
								.build())
						.addField(ModelDeserializationContext.class, "context", FINAL, PRIVATE)
						.addField(ParameterizedTypeName.get(Map.class, String.class, Long.class), "map", FINAL, PRIVATE)
						.addField(cloneName, "out", FINAL, PRIVATE)
						.addMethod(MethodSpec
								.constructorBuilder()
								.addModifiers(PUBLIC)
								.addParameter(ModelDeserializationContext.class, "context")
								.addCode("this.context = context;\n")
								.addCode("context.finishers.add(new Finisher());\n")
								.addCode("map = new $T();\n", HashMap.class)
								.addCode("out = new $T();\n", cloneName)
								.build())
						.addMethod(poetMethod(sigStateValue)
								.addCode(cloneDeserializeValue.build())
								.addCode("throw new $T(String.format(\"Unknown key (%s)\", key));\n",
										RuntimeException.class
								)
								.build())
						.addMethod(poetMethod(sigStateArray).addCode(cloneDeserializeArray.build()).addCode(
								"throw new $T(String.format(\"Key (%s) is unknown or is not an array\", key));\n",
								RuntimeException.class
						).build())
						.addMethod(poetMethod(sigStateRecord).addCode(cloneDeserializeRecord.build()).addCode(
								"throw new $T(String.format(\"Key (%s) is unknown or is not a record\", key));\n",
								RuntimeException.class
						).build())
						.addMethod(poetMethod(sigStateGet)
								.addCode("if (context.objectMap.containsKey(out.id())) throw new $T();\n",
										Assertion.class
								)
								.addCode("context.objectMap.put(out.id(), out);\n", ProjectObjectInterface.class)
								.addCode("return out;\n")
								.build());
				clone
						.addMethod(MethodSpec
								.methodBuilder("create")
								.addModifiers(PUBLIC, STATIC)
								.returns(cloneName)
								.addParameter(ProjectContextBase.class, "project")
								.addCode("$T out = new $T();\n", cloneName, cloneName)
								.addCode("out.id = project.nextId++;\n")
								.addCode("return out;\n")
								.build())
						.addMethod(poetMethod(sigObjIncRef)
								.addCode("refCount += 1;\n")
								.addCode(CodeBlock
										.builder()
										.add("if (refCount == 1) {\n")
										.indent()
										.add("if (project.objectMap.containsKey(id)) throw new $T();\n",
												Assertion.class
										)
										.add("project.objectMap.put(id, this);\n")
										.unindent()
										.add("}\n")
										.build())
								.build())
						.addMethod(poetMethod(sigObjDecRef).addCode(cloneDecRef.build()).build())
						.addType(deserializerBuilder.build())
						.addMethod(poetMethod(sigObjSerialize)
								.addCode(cloneSerialize.add("writer.recordEnd();\n").build())
								.build());
				globalModelDeserialize.addCode("if (\"$L\".equals(type)) return new $T.Deserializer(context);\n",
						decideName(klass),
						cloneName
				);
			}
			if (klass.getSuperclass() != Object.class)
				clone.superclass(toPoet(new TypeInfo(klass.getSuperclass())));
			for (Class interface1 : klass.getInterfaces())
				clone.addSuperinterface(interface1);
			if (!Walk.isConcrete(classInfo))
				clone.addModifiers(ABSTRACT);
			write(cloneName, clone.addModifiers(PUBLIC).addSuperinterface(ProjectObjectInterface.class).build());
		}

		write(changeStepBuilderName, changeStepBuilder.build());
		write(
				deserializeHelperName,
				deserializeHelper
						.addMethod(globalModelDeserialize
								.addCode(
										"throw new $T(String.format(\"Unknown type %s\", type));\n",
										RuntimeException.class
								)
								.build())
						.addMethod(globalChangeDeserialize.addCode(
								"throw new $T(String.format(\"Unknown change type %s\", type));\n",
								RuntimeException.class
						).build())
						.build()
		);
	}

	public void write(ClassName name, TypeSpec spec) {
		System.out.format("Writing class %s\n", name);
		uncheck(() -> JavaFile.builder(name.packageName(), spec).build().writeTo(path));
	}

	public static List<String> splitNames(final String names) {
		final ArrayList<String> out = new ArrayList();
		List<String> split = Arrays.asList(names.split("::|\\."));
		out.addAll(sublist(split, 0, -1).stream().map(n -> n + "_").collect(Collectors.toList()));
		out.add(last(split));
		return out;
	}

	public static String capFirst(final String v) {
		return v.substring(0, 1).toUpperCase() + v.substring(1);
	}

	public static String lowerFirst(final String v) {
		return v.substring(0, 1).toLowerCase() + v.substring(1);
	}

}
