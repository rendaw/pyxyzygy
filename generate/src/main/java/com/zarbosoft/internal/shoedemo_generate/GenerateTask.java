package com.zarbosoft.internal.shoedemo_generate;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.*;
import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.interface1.Walk;
import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;
import com.zarbosoft.shoedemo.deserialize.*;
import com.zarbosoft.shoedemo.model.*;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zarbosoft.rendaw.common.Common.*;
import static com.zarbosoft.shoedemo.deserialize.DeserializationContext.decideName;
import static com.zarbosoft.shoedemo.deserialize.DeserializationContext.flattenPoint;
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
	public static Method sigChangeApply = findMethod(Change.class, "apply");
	public static Method sigChangeDelete = findMethod(Change.class, "delete");
	public static Method sigChangeSerialize = findMethod(Change.class, "serialize");
	public static Method sigStateValue = findMethod(StackReader.State.class, "value");
	public static Method sigStateArray = findMethod(StackReader.State.class, "array");
	public static Method sigStateRecord = findMethod(StackReader.State.class, "record");
	public static Method sigStateGet = findMethod(StackReader.State.class, "get");
	public static Method sigFinisherFinish = findMethod(DeserializationContext.Finisher.class, "finish");

	public Map<Class, Pair<ClassName, TypeSpec.Builder>> typeMap = new HashMap<>();
	public ClassName deserializeHelperName = name("DeserializeHelper");
	public TypeSpec.Builder deserializeHelper = TypeSpec.classBuilder(deserializeHelperName).addModifiers(PUBLIC);
	public MethodSpec.Builder globalModelDeserialize = MethodSpec
			.methodBuilder("deserializeModel")
			.addModifiers(PUBLIC, STATIC)
			.returns(StackReader.State.class)
			.addParameter(DeserializationContext.class, "context")
			.addParameter(String.class, "type");
	public MethodSpec.Builder globalChangeDeserialize = MethodSpec
			.methodBuilder("deserializeChange")
			.addModifiers(PUBLIC, STATIC)
			.returns(StackReader.State.class)
			.addParameter(DeserializationContext.class, "context")
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
			return CodeBlock.of("writer.primitive($T.toString($L));\n", Objects.class, key);
		} else if (fieldInfo.type == Long.class || fieldInfo.type == long.class) {
			return CodeBlock.of("writer.primitive($T.toString($L));\n", Objects.class, key);
		} else if (fieldInfo.type == Float.class || fieldInfo.type == float.class) {
			return CodeBlock.of("writer.primitive($T.toString($L));\n", Objects.class, key);
		} else if (fieldInfo.type == Double.class || fieldInfo.type == double.class) {
			return CodeBlock.of("writer.primitive($T.toString($L));\n", Objects.class, key);
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
			return CodeBlock.of("writer.primitive($T.toString((($T)$L).id()));\n",
					Objects.class,
					ProjectObjectInterface.class,
					key
			);
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
			return CodeBlock.of("($T) $L;\n", toPoet(fieldInfo), name);
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
				.getSubTypesOf(ProjectObject.class)
				.stream()
				.filter(c -> c.getPackage().getName().equals("com.zarbosoft.internal.shoedemo_generate.premodel")))) {
			ClassName name = name(klass.getSimpleName());
			typeMap.put(klass, new Pair<>(name, TypeSpec.classBuilder(ClassName.get(klass)).addModifiers(PUBLIC)));
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
						.addCode("this.context = context;\n")
						.addCode("this.changeStep = new $T(context.nextId++);\n", ChangeStep.class)
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
			CodeBlock.Builder cloneDecRef =
					CodeBlock.builder().add("refCount -= 1;\n").add("if (refCount > 0) return;\n");

			globalModelDeserialize.addCode("if (\"$L\".equals(type)) return new $T.Deserializer(context);\n",
					decideName(klass),
					cloneName
			);

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
					private final CodeBlock.Builder changeApplyNotify;
					private final MethodSpec.Builder changeDelete;
					private final MethodSpec.Builder changeSerialize;
					private final ClassName listenerName;
					private final TypeSpec.Builder listener;
					private final MethodSpec.Builder listenerAccept;
					private final ClassName deserializerName;

					public ChangeBuilder(
							boolean flattenBase, String fieldName, String action
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
						String listenersFieldName = String.format("%s%sListeners", fieldName, capFirst(action));
						clone.addField(FieldSpec
								.builder(ParameterizedTypeName.get(ClassName.get(List.class), listenerName),
										listenersFieldName
								)
								.addModifiers(PRIVATE, FINAL)
								.initializer("new $T<>()", ArrayList.class)
								.build());
						clone.addMethod(MethodSpec
								.methodBuilder(String.format("add%s", capFirst(listenersFieldName)))
								.addModifiers(PUBLIC)
								.addParameter(listenerName, "listener")
								.addCode("$L.add(listener);\n", listenersFieldName)
								.build());
						clone.addMethod(MethodSpec
								.methodBuilder(String.format("remove%s", capFirst(listenersFieldName)))
								.addModifiers(PUBLIC)
								.addParameter(listenerName, "listener")
								.addCode("$L.remove(listener);\n", listenersFieldName)
								.build());
						// TODO deserializer
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
								.addModifiers(STATIC)
								.addField(FieldSpec.builder(cloneName, "target").addModifiers(PUBLIC).build());
						changeSerialize = poetMethod(sigChangeSerialize)
								.addCode("writer.type(\"$L\").recordBegin();\n", changeName.simpleName())
								.addCode("writer.key(\"target\").primitive($T.toString((($T)target).id()));\n",
										Objects.class,
										ProjectObjectInterface.class
								);
						globalChangeDeserialize.addCode("if (\"$L\".equals(type)) return new $T();\n",
								changeName.simpleName(),
								deserializerName
						);
						changeConstructor = MethodSpec
								.constructorBuilder()
								.addParameter(ProjectContextBase.class, "project")
								.addParameter(cloneName, "target")
								.addCode("this.target = target;\n");
						if (flattenBase)
							changeConstructor.addCode("this.target.incRef(project);\n");
						changeApply = CodeBlock.builder();
						changeApplyNotify = CodeBlock.builder().add(
								"for ($T listener : target.$L) listener.accept(target",
								listenerName,
								listenersFieldName
						);
						changeDelete = poetMethod(sigChangeDelete);
						if (flattenBase)
							changeDelete.addCode("(($T) target).decRef(project);\n", ProjectObjectInterface.class);
						changeInvoke = MethodSpec
								.methodBuilder(String.format("%s%s", fieldName, capFirst(action)))
								.addModifiers(PUBLIC)
								.returns(changeStepBuilderName);
					}

					public ChangeBuilder apply(Consumer<ChangeBuilder> f) {
						f.accept(this);
						return this;
					}

					public ClassName getName() {
						return changeName;
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
							changeDelete
									.addCode("for ($T e : $L) ", element, name)
									.addCode("(($T) e).decRef(project);\n", ProjectObjectInterface.class);
							if (inc)
								changeApply
										.add("for ($T e : $L) {", element, name)
										.indent()
										.add("(($T) e).incRef(project);\n", ProjectObjectInterface.class)
										.add("(($T) e).incRef(project);\n", ProjectObjectInterface.class)
										.unindent()
										.add("};\n");
						}
						changeApplyNotify.add(String.format(", %s", name));
						changeInvoke.addParameter(list, name);
						invokeForward.add(name);
						changeSerialize
								.addCode("writer.key(\"$L\").arrayBegin();\n", name)
								.addCode("for ($T e : $L) ", element, name);
						if (ProjectNode.class.isAssignableFrom((Class) type.type) ||
								Tile.class.isAssignableFrom((Class) type.type))
							changeSerialize.addCode("writer.primitive($T.toString((($T)e).id()));\n",
									Objects.class,
									ProjectObjectInterface.class
							);
						else
							changeSerialize.addCode("writer.primitive($T.toString(e));\n", Objects.class);
						changeSerialize.addCode("writer.arrayEnd();\n");
						return this;
					}

					public ChangeBuilder addParameter(TypeInfo type, String name, boolean inc) {
						listenerAccept.addParameter(toPoet(type), name);
						change.addField(FieldSpec.builder(toPoet(type), name).addModifiers(PUBLIC).build());
						changeConstructor
								.addParameter(toPoet(type), name)
								.addCode(String.format("this.%s = %s;\n", name, name));
						if (flattenPoint(type)) {
							changeDelete.addCode("(($T) $L).decRef(project);\n", ProjectObjectInterface.class, name);
							if (inc) {
								changeApply.add("(($T) $L).incRef(project);\n", ProjectObjectInterface.class, name);
								changeApply.add("(($T) $L).incRef(project);\n", ProjectObjectInterface.class, name);
							}
						}
						changeApplyNotify.add(String.format(", %s", name));
						changeInvoke.addParameter(toPoet(type), name);
						invokeForward.add(name);
						changeSerialize.addCode("writer.key(\"$L\")", name);
						if (ProjectNode.class.isAssignableFrom((Class) type.type) ||
								Tile.class.isAssignableFrom((Class) type.type))
							changeSerialize.addCode(".primitive($T.toString((($T)$L).id()));\n",
									Objects.class,
									ProjectObjectInterface.class,
									name
							);
						else
							changeSerialize.addCode(".primitive($T.toString($L));\n", Objects.class, name);
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

					public void finish() {
						listener.addMethod(listenerAccept.build());
						clone.addType(listener.build());
						change
								.addMethod(changeConstructor.build())
								.addMethod(poetMethod(sigChangeApply)
										.addCode(changeApply.build())
										.addCode(changeApplyNotify.add(");\n").build())
										.build())
								.addMethod(changeSerialize.addCode("writer.recordEnd();\n").build())
								.addMethod(changeDelete.build());
						clone.addType(change.build());
						clone.addType(TypeSpec
								.classBuilder(deserializerName)
								.addModifiers(STATIC)
								.superclass(StackReader.RecordState.class)
								.build());
						typeChangeStepBuilder.addMethod(changeInvoke.addCode(
								"new $T(changeStepBuilder.context, target$L).apply(changeStepBuilder.context, changeStepBuilder.changeStep);\n",
								changeName,
								invokeForward
										.stream()
										.map(n -> String.format(", %s", n))
										.collect(Collectors.joining(""))
						).addCode("return changeStepBuilder;\n").build());
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
					public void apply(TypeInfo field) {
						// Getter
						MethodSpec.Builder getter =
								MethodSpec.methodBuilder(fieldName).returns(toPoet(field)).addModifiers(PUBLIC);
						clone.addMethod(getter.addCode("return $L;\n", fieldName).build());

						// Mutation
						if (!Stream.of("id", "refCount").anyMatch(fieldName::equals)) {
							new ChangeBuilder(flattenPoint(classInfo), fieldName, "set")
									.addParameter(field, "value", false)
									.addCode("changeStep.add(new $T(project, target, target.$L));\n",
											CHANGE_TOKEN_NAME,
											fieldName
									)
									.addCode("target.$L = value;\n", fieldName)
									.finish();
						}

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
				cloneDeserializeValue.add("if (\"$L\".equals(key)) {\n", fieldName).indent();
				if (fieldInfo.type == String.class) {
					generateScalar.apply(fieldInfo);
				} else if (fieldInfo.type == Integer.class || fieldInfo.type == int.class) {
					generateScalar.apply(fieldInfo);
				} else if (fieldInfo.type == Long.class || fieldInfo.type == long.class) {
					generateScalar.apply(fieldInfo);
				} else if (fieldInfo.type == Float.class || fieldInfo.type == float.class) {
					generateScalar.apply(fieldInfo);
				} else if (fieldInfo.type == Double.class || fieldInfo.type == double.class) {
					generateScalar.apply(fieldInfo);
				} else if (fieldInfo.type == Boolean.class || fieldInfo.type == boolean.class) {
					generateScalar.apply(fieldInfo);
				} else if (((Class<?>) fieldInfo.type).isEnum()) {
					generateScalar.apply(fieldInfo);
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
					ChangeBuilder addBuilder = new ChangeBuilder(flattenPoint(classInfo), fieldName, "add");
					ChangeBuilder removeBuilder = new ChangeBuilder(flattenPoint(classInfo), fieldName, "remove");
					addBuilder
							.addParameter(new TypeInfo(int.class), "at", false)
							.addListParameter(elementType, "value", true)
							.addCode("target.$L.addAll(at, value);\n", fieldName)
							.addCode("changeStep.add(new $T(project, target, at, value.size()));\n",
									removeBuilder.getName()
							)
							.finish();
					TypeName listType = ParameterizedTypeName.get(ClassName.get(List.class), toPoet(elementType));
					removeBuilder
							.addParameter(new TypeInfo(int.class), "at", false)
							.addParameter(new TypeInfo(int.class), "count", false)
							.addCode("final $T sublist = target.$L.subList(at, at + count);\n", listType, fieldName)
							.addCode("changeStep.add(new $T(project, target, at, new $T(sublist)));\n",
									addBuilder.getName(),
									ArrayList.class
							)
							.addCode("sublist.clear();\n")
							.finish();
					new ChangeBuilder(flattenPoint(classInfo), fieldName, "clear").addCode(
							"changeStep.add(new $T(project, target, 0, new $T(target.$L)));\n",
							addBuilder.getName(),
							ArrayList.class,
							fieldName
					).addCode("target.$L.clear();\n", fieldName).finish();
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
					new ChangeBuilder(flattenPoint(classInfo), fieldName, "moveTo")
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
							.addCode("changeStep.add(new $T(project, target, dest, count, source));\n",
									CHANGE_TOKEN_NAME
							)
							.addCode("$T sublist = target.$L.subList(source, source + count);\n", listType, fieldName)
							.addCode("for ($T e : sublist) (($T) e).incRef(project);\n",
									toPoet(fieldInfo.parameters[0]),
									ProjectObjectInterface.class
							)
							.addCode("$T readd = new $T(sublist);\n", listType, ArrayList.class)
							.addCode("sublist.clear();\n")
							.addCode("target.$L.addAll(dest, readd);\n", fieldName)
							.finish();
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
						cloneDeserializeArray.add("$T();\n", IDListState.class);
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
					ChangeBuilder addBuilder = new ChangeBuilder(flattenPoint(classInfo), fieldName, "add");
					ChangeBuilder removeBuilder = new ChangeBuilder(flattenPoint(classInfo), fieldName, "remove");
					TypeInfo elementType = fieldInfo.parameters[0];
					addBuilder
							.addCode("if (target.$L.contains(value)) return;\n")
							.addListParameter(elementType, "value", true)
							.addCode("target.$L.addAll(value);\n", fieldName)
							.addCode(("changeStep.add(new $T(project, target, value));\n"), removeBuilder.getName())
							.finish();
					removeBuilder
							.addCode("if (!target.$L.remove(value)) return;\n", fieldName)
							.addListParameter(elementType, "value", false)
							.addCode(("changeStep.add(new $T(project, target, value));\n"), addBuilder.getName())
							.finish();
					new ChangeBuilder(flattenPoint(classInfo), fieldName, "clear").addCode(
							"changeStep.add(new $T(project, target, 0, new $T($L)));\n",
							addBuilder.getName(),
							ArrayList.class
					).addCode("target.$L.clear();\n", fieldName).finish();
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
						cloneDeserializeArray.add("$T();\n", IDSetState.class);
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
					ChangeBuilder putBuilder = new ChangeBuilder(flattenPoint(classInfo), fieldName, "put");
					ChangeBuilder removeBuilder = new ChangeBuilder(flattenPoint(classInfo), fieldName, "remove");
					putBuilder
							.addParameter(fieldInfo.parameters[0], "key", false)
							.addParameter(fieldInfo.parameters[1], "value", true)
							.addCode(CodeBlock
									.builder()
									.add("if (target.$L.containsKey(key)) {\n", fieldName)
									.indent()
									.add("$T oldValue = target.$L.put(key, value);\n",
											toPoet(fieldInfo.parameters[1]),
											fieldName
									)
									.add(("changeStep.add(new $T(project, target, key, oldValue));\n"),
											putBuilder.getName()
									)
									.unindent()
									.add("} else {\n")
									.indent()
									.add("target.$L.put(key, value);\n", fieldName)
									.add(("changeStep.add(new $T(project, target, key));\n"),
											removeBuilder.getName()
									)
									.unindent()
									.add("}\n")
									.build())
							.finish();
					removeBuilder
							.addParameter(fieldInfo.parameters[0], "key", false)
							.addCode("if (!target.$L.containsKey(key)) return;\n", fieldName)
							.addCode("$T oldValue = target.$L.remove(key);\n",
									toPoet(fieldInfo.parameters[1]),
									fieldName
							)
							.addCode(("changeStep.add(new $T(project, target, key, oldValue));\n"),
									putBuilder.getName()
							)
							.finish();

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
					cloneDeserializeArray.add("if (\"$L\".equals(key)) return ", fieldName);
					if (flattenPoint(fieldInfo.parameters[1])) {
						cloneDeserializeArray.add("new $T();\n", IDMapState.class);
						cloneDecRef.add(
								"for (Map.Entry<$T, $T> e : $L.entrySet()) (($T) e.getValue()).decRef(project);\n",
								toPoet(fieldInfo.parameters[0]),
								toPoet(fieldInfo.parameters[1]),
								fieldName,
								ProjectObjectInterface.class
						);
					} else {
						cloneDeserializeArray
								.add("$T() {\n", GeneralMapState.class)
								.indent()
								.add("public void value(Object value) {\n")
								.indent();
						CodeBlock.Builder valueCode = CodeBlock.builder();
						CodeBlock.Builder recordCode = CodeBlock.builder();
						CodeBlock key = generateScalarFromString(fieldInfo.parameters[0], "key", valueCode, recordCode);
						CodeBlock value =
								generateScalarFromString(fieldInfo.parameters[1], "value", valueCode, recordCode);
						cloneDeserializeArray
								.add("data.put(")
								.add(key)
								.add(", ")
								.add(value)
								.add(");\n")
								.unindent()
								.add("}\n");
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
				} else if (flattenPoint(fieldInfo)) {
					generateScalar.apply(fieldInfo);

					// Deserialize
					cloneDeserializeValue.add("map.put(\"$L\", ($T)value);\n", fieldName, String.class);
					cloneDeserializeFinish.add("out.$L = ($T) context.objectMap.get(map.get(\"$L\"));",
							fieldName,
							toPoet(fieldInfo),
							fieldName
					);
				} else {
					cloneDeserializeRecord.add("if (\"$L\".equals(key)) {\n", fieldName).indent();
					generateScalar.apply(fieldInfo);
					cloneDeserializeRecord.unindent().add("}\n");
				}

				cloneDeserializeValue.add("return;\n").unindent().add("};\n");
				clone.addField(field.build());
			}

			write(typeChangeStepBuilderName, typeChangeStepBuilder.build());
			ClassName deserializerName = cloneName.nestedClass("Deserializer");
			ClassName finisherName = deserializerName.nestedClass("Finisher");
			TypeSpec.Builder deserializerBuilder = TypeSpec
					.classBuilder(deserializerName)
					.addModifiers(PUBLIC, STATIC)
					.superclass(StackReader.RecordState.class)
					.addType(TypeSpec
							.classBuilder(finisherName)
							.superclass(DeserializationContext.Finisher.class)
							.addModifiers(PUBLIC)
							.addMethod(poetMethod(sigFinisherFinish).addCode(cloneDeserializeFinish.build()).build())
							.build())
					.addField(DeserializationContext.class, "context", FINAL, PRIVATE)
					.addField(Map.class, "map", FINAL, PRIVATE)
					.addField(cloneName, "out", FINAL, PRIVATE)
					.addMethod(MethodSpec
							.constructorBuilder()
							.addModifiers(PUBLIC)
							.addParameter(DeserializationContext.class, "context")
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
							"throw new $T(String.format(\"Key (%s) is unknown or is not an record\", key));\n",
							RuntimeException.class
					).build());
			if (ProjectObject.class.isAssignableFrom(klass)) {
				deserializerBuilder.addMethod(poetMethod(sigStateGet)
						.addCode("context.objectMap.put((($T)out).id(), out);\n", ProjectObjectInterface.class)
						.addCode("return super.get();\n")
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
						.addMethod(poetMethod(sigObjIncRef).addCode("refCount += 1;\n").build())
						.addMethod(poetMethod(sigObjDecRef).addCode(cloneDecRef.build()).build());
			}
			if (klass.getSuperclass() != Object.class)
				clone.superclass(klass.getSuperclass());
			for (Class interface1 : klass.getInterfaces())
				clone.addSuperinterface(interface1);
			write(cloneName,
					clone
							.addModifiers(PUBLIC)
							.addSuperinterface(ProjectObjectInterface.class)
							.addType(deserializerBuilder.build())
							.addMethod(poetMethod(sigObjSerialize)
									.addCode(cloneSerialize.add("writer.recordEnd();\n").build())
									.build())
							.build()
			);
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
