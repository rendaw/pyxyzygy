package com.zarbosoft.internal.shoedemo_generate;

import com.squareup.javapoet.*;
import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.interface1.Walk;
import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.luxem.write.RawWriter;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;
import com.zarbosoft.shoedemo.deserialize.DeserializationContext;
import com.zarbosoft.shoedemo.deserialize.GeneralStateBuilder;
import com.zarbosoft.shoedemo.model.*;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.zarbosoft.rendaw.common.Common.*;
import static com.zarbosoft.shoedemo.deserialize.DeserializationContext.decideName;
import static com.zarbosoft.shoedemo.deserialize.DeserializationContext.flattenPoint;
import static javax.lang.model.element.Modifier.*;

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
		whatever();
	}

	public static void main(final String[] args) {
		final GenerateTask t = new GenerateTask();
		t.setPath(args[0]);
		t.execute();
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	public static Object CHANGE_TOKEN_NAME = new Object();

	public ClassName deserializeHelperName = name("DeserializeHelper");
	public TypeSpec.Builder deserializeHelper = TypeSpec.classBuilder(deserializeHelperName).addModifiers(PUBLIC);
	public MethodSpec.Builder deserializeModel = MethodSpec
			.methodBuilder("deserializeModel")
			.addModifiers(PUBLIC, STATIC)
			.returns(StackReader.State.class)
			.addParameter(DeserializationContext.class, "context")
			.addParameter(String.class, "type");
	public MethodSpec.Builder deserializeChange = MethodSpec
			.methodBuilder("deserializeChange")
			.addModifiers(PUBLIC, STATIC)
			.returns(StackReader.State.class)
			.addParameter(DeserializationContext.class, "context")
			.addParameter(String.class, "type");

	public class ChangeBuilder {
		private final ClassName name;
		private final MethodSpec.Builder invoke;
		private final List<String> invokeForward = new ArrayList<>();
		private final TypeSpec.Builder changeTypeSpec;
		private final MethodSpec.Builder constructor;
		private final MethodSpec.Builder apply;
		private final CodeBlock.Builder applyCode;
		private final MethodSpec.Builder serialize;
		private final TypeSpec.Builder deserializerTypeSpec;

		public ChangeBuilder(
				ClassName changeStepName, ClassName target, Field field, String action
		) {
			name = ClassName.get("com.zarbosoft.shoedemo.model",
					String.format("%s_%s_%s", target.simpleName(), field.getName(), capFirst(action))
			);
			changeTypeSpec = TypeSpec
					.classBuilder(name)
					.superclass(Change.class)
					.addModifiers(PUBLIC)
					.addField(FieldSpec.builder(target, "target").addModifiers(PUBLIC).build());
			serialize = MethodSpec
					.methodBuilder("serialize")
					.addAnnotation(Override.class)
					.addModifiers(PUBLIC)
					.addParameter(RawWriter.class, "writer")
					.addCode("writer.type(\"$L\").recordBegin();\n", name.simpleName())
					.addCode("writer.key(\"target\").primitive($T.toString(target.id));\n", Objects.class);
			deserializeChange.addCode("if (\"$L\".equals(type)) return new $T.Deserializer();\n",
					name.simpleName(),
					name
			);
			deserializerTypeSpec = TypeSpec
					.classBuilder(name.nestedClass("Deserializer"))
					.addModifiers(STATIC)
					.superclass(StackReader.RecordState.class);
			constructor =
					MethodSpec.constructorBuilder().addParameter(target, "target").addCode("this.target = target;\n");
			apply = MethodSpec
					.methodBuilder("apply")
					.addAnnotation(Override.class)
					.addModifiers(PUBLIC)
					.addParameter(ChangeStep.class, "changeStep");
			applyCode = CodeBlock.builder();
			invoke = MethodSpec
					.methodBuilder(String.format("%s%s", action, capFirst(field.getName())))
					.addModifiers(PUBLIC)
					.returns(changeStepName);
		}

		public ClassName getName() {
			return name;
		}

		public ChangeBuilder addParameter(Type type, String name) {
			changeTypeSpec.addField(FieldSpec.builder(type, name).addModifiers(PUBLIC).build());
			constructor.addParameter(type, name).addCode(String.format("this.%s = %s;\n", name, name));
			invoke.addParameter(type, name);
			invokeForward.add(name);
			serialize.addCode("writer.key(\"$L\")", name);
			if (ProjectNode.class.isAssignableFrom((Class) type) || Tile.class.isAssignableFrom((Class) type))
				serialize.addCode(".primitive($T.toString($L.id));\n", Objects.class, name);
			else
				serialize.addCode(".primitive($T.toString($L));\n", Objects.class, name);
			return this;
		}

		public ChangeBuilder addCode(CodeBlock code) {
			applyCode.add(code);
			return this;
		}

		public ChangeBuilder addCode(String format, Object... args) {
			applyCode.add(CodeBlock
					.builder()
					.add(format, Arrays.stream(args).map(a -> a == CHANGE_TOKEN_NAME ? name : a).toArray())
					.build());
			return this;
		}

		public void finish(TypeSpec.Builder typeChangeBuilder) {
			changeTypeSpec
					.addMethod(constructor.build())
					.addMethod(apply.addCode(applyCode.build()).build())
					.addMethod(serialize.addCode("writer.recordEnd();\n").build())
					.addType(deserializerTypeSpec.build());
			write(name, changeTypeSpec.build());
			typeChangeBuilder.addMethod(invoke.addCode("new $T(target, $L).apply(changeStep);\n",
					name,
					invokeForward.stream().collect(Collectors.joining(", "))
			).addCode("return changeStep;\n").build());
		}

		public ChangeBuilder addCodeIf(boolean condition, String format, Object... args) {
			if (!condition)
				return this;
			return addCode(format, args);
		}

		public ChangeBuilder indent() {
			applyCode.indent();
			return this;
		}

		public ChangeBuilder unindent() {
			applyCode.unindent();
			return this;
		}
	}

	public ClassName name(String... parts) {
		return ClassName.get("com.zarbosoft.shoedemo.model", Arrays.stream(parts).collect(Collectors.joining("_")));
	}

	public void whatever() {
		Reflections reflections = new Reflections("com.zarbosoft.internal.shoedemo_generate.premodel");

		// Prep type map
		Map<Class, Pair<ClassName, TypeSpec.Builder>> typeMap = new HashMap<>();
		for (Class klass : reflections.getSubTypesOf(ProjectObject.class)) {
			ClassName name = name(klass.getSimpleName());
			typeMap.put(klass, new Pair<>(name, TypeSpec.classBuilder(ClassName.get(klass)).addModifiers(PUBLIC)));
		}

		// Clone types
		for (Map.Entry<Class, Pair<ClassName, TypeSpec.Builder>> entry : typeMap.entrySet()) {
			Class klass = entry.getKey();
			final ClassName className = entry.getValue().first;
			TypeSpec.Builder builder = entry.getValue().second;
			builder.addModifiers(PUBLIC).addSuperinterface(ProjectObjectInterface.class);
			if (klass.getSuperclass() != Object.class)
				builder.superclass(klass.getSuperclass());
			for (Class interface1 : klass.getInterfaces())
				builder.addSuperinterface(interface1);

			deserializeModel.addCode("if (\"$L\".equals(type)) return new $T.Deserializer(context);\n",
					decideName(klass),
					className
			);
			MethodSpec.Builder deserializerValue = MethodSpec
					.methodBuilder("value")
					.addAnnotation(Override.class)
					.addModifiers(PUBLIC)
					.addParameter(Object.class, "value");
			MethodSpec.Builder deserializerArray = MethodSpec
					.methodBuilder("array")
					.addAnnotation(Override.class)
					.addModifiers(PUBLIC)
					.returns(StackReader.State.class);
			MethodSpec.Builder deserializerRecord = MethodSpec
					.methodBuilder("record")
					.addAnnotation(Override.class)
					.addModifiers(PUBLIC)
					.returns(StackReader.State.class);
			MethodSpec.Builder deserializerFinish = MethodSpec
					.methodBuilder("finish")
					.addAnnotation(Override.class)
					.addModifiers(PUBLIC)
					.addParameter(DeserializationContext.class, "context");

			MethodSpec.Builder serialize = MethodSpec
					.methodBuilder("serialize")
					.addAnnotation(Override.class)
					.addModifiers(PUBLIC)
					.addParameter(RawWriter.class, "writer")
					.addCode("writer.type(\"$L\").recordBegin();\n", decideName(klass));

			CodeBlock.Builder refDecCode = null;
			if (flattenPoint(new TypeInfo(klass)))
				refDecCode = CodeBlock
						.builder()
						.add("refCount -= 1;\n")
						.add("if (refCount == 0) {\n")
						.indent()
						.add("project.objectMap.remove(id);");
			for (Pair<Configuration, TypeInfo> pair : iterable(Walk.getFields(klass))) {
				System.out.format("field %s\n", pair.second);
				Field field = pair.second.field;
				TypeInfo fieldInfo = pair.second;
				TypeName fieldType;
				TypeName baseType = Optional
						.ofNullable(typeMap.get(fieldInfo.type))
						.map(o -> (TypeName) o.first)
						.orElseGet(() -> ClassName.get(fieldInfo.type));
				if (fieldInfo.parameters != null)
					fieldType = ParameterizedTypeName.get((ClassName) baseType,
							Arrays.stream(fieldInfo.parameters).map(p -> TypeName.get(p.type)).<TypeName>toArray(
									TypeName[]::new)
					);
				else
					fieldType = baseType;
				if (field.getDeclaringClass() == klass)
					builder.addField(FieldSpec.builder(fieldType, field.getName()).build());
				builder.addMethod(MethodSpec
						.methodBuilder(field.getName())
						.addModifiers(PUBLIC)
						.returns(field.getType())
						.addCode("return $L;\n", field.getName())
						.build());
				serialize.addCode(Walk.walkType(reflections, fieldInfo, 0, new SerializeVisitor()));
				deserializerValue.addCode(CodeBlock
						.builder()
						.add("if (key.equals(\"$L\")) {\n", field.getName())
						.indent()
						.add(Walk.walkType(reflections, new TypeInfo(field), null, new DeserializerValueVisitor()))
						.add("return;\n")
						.unindent()
						.add("}\n")
						.build());
				if (field.getType().isAssignableFrom(ArrayList.class) ||
						field.getType().isAssignableFrom(HashSet.class))
					deserializerArray.addCode(CodeBlock
							.builder()
							.add("if (key.equals(\"$L\")) return new $T()", field.getName(), GeneralStateBuilder.class)
							.add(Walk.walkType(reflections, fieldInfo, null, new DeserializerNestVisitor()))
							.add(".build();\n")
							.build());
				if (field.getType().getAnnotation(Configuration.class) != null &&
						!DeserializationContext.flattenPoint(fieldInfo))
					deserializerRecord.addCode(CodeBlock
							.builder()
							.add("if (key.equals(\"$L\")) return new $T()", field.getName(), GeneralStateBuilder.class)
							.add(Walk.walkType(reflections, fieldInfo, null, new DeserializerNestVisitor()))
							.add(".build();\n")
							.build());
				if (DeserializationContext.flattenPoint(fieldInfo))
					deserializerFinish.addCode(Walk.walkType(reflections,
							fieldInfo,
							null,
							new DeserializerFinishVisitor()
					));
			}
			ClassName deserializerName = className.nestedClass("Deserializer");
			ClassName finisherName = deserializerName.nestedClass("Finisher");
			TypeSpec.Builder deserializerBuilder = TypeSpec
					.classBuilder(deserializerName)
					.addModifiers(PUBLIC, STATIC)
					.superclass(StackReader.RecordState.class)
					.addType(TypeSpec
							.classBuilder(finisherName)
							.superclass(DeserializationContext.Finisher.class)
							.addModifiers(PUBLIC)
							.addMethod(deserializerFinish.build())
							.build())
					.addField(DeserializationContext.class, "context", FINAL, PRIVATE)
					.addField(Map.class, "map", FINAL, PRIVATE)
					.addField(className, "out", FINAL, PRIVATE)
					.addMethod(MethodSpec
							.constructorBuilder()
							.addModifiers(PUBLIC)
							.addParameter(DeserializationContext.class, "context")
							.addCode("this.context = context;\n")
							.addCode("context.finishers.add(new Finisher());\n")
							.addCode("map = new $T();\n", HashMap.class)
							.addCode("out = new $T();\n", className)
							.build())
					.addMethod(deserializerValue.addCode("throw new $T();\n", Assertion.class).build())
					.addMethod(deserializerArray.addCode("throw new $T();\n", Assertion.class).build())
					.addMethod(deserializerRecord.addCode("throw new $T();\n", Assertion.class).build());
			if (ProjectNode.class.isAssignableFrom(klass)) {
				serialize.addCode("writer.key(\"id\").primitive($T.toString(id));\n", Objects.class);
				deserializerBuilder.addMethod(MethodSpec
						.methodBuilder("get")
						.addModifiers(PUBLIC)
						.addAnnotation(Override.class)
						.returns(Object.class)
						.addCode("context.objectMap.put(out.id, out);\n")
						.addCode("return super.get();\n")
						.build());
				builder
						.addMethod(MethodSpec.constructorBuilder().build())
						.addMethod(MethodSpec
								.methodBuilder("create")
								.addModifiers(PUBLIC, STATIC)
								.returns(className)
								.addParameter(DeserializationContext.class, "context")
								.addCode("$T out = new $T();\n", className, className)
								.addCode("out.id = context.nextId++;\n")
								.addCode("return out;\n")
								.build())
						.addMethod(MethodSpec
								.methodBuilder("refDec")
								.addAnnotation(Override.class)
								.addParameter(ProjectBase.class, "project")
								.addCode(refDecCode.unindent().add("}\n").build()).build());
			}
			builder.addType(deserializerBuilder.build()).addMethod(serialize.addCode("writer.recordEnd();\n").build());
			write(className, builder.build());
		}

		// Create change set/builder
		ClassName changeStepName = ClassName.get(ChangeStep.class);
		ClassName changeStepBuilderName = name("ChangeStepBuilder");
		TypeSpec.Builder changeStepBuilder = TypeSpec
				.classBuilder(changeStepBuilderName)
				.addModifiers(PUBLIC)
				.addField(FieldSpec.builder(changeStepName, "changeStep").addModifiers(PUBLIC).build())
				.addMethod(MethodSpec
						.constructorBuilder()
						.addModifiers(PUBLIC)
						.addCode("this.changeStep = new $T();", ChangeStep.class)
						.build());

		for (Map.Entry<Class, Pair<ClassName, TypeSpec.Builder>> entry : typeMap.entrySet()) {
			Class klass = entry.getKey();
			final ClassName className = entry.getValue().first;

			// Create sub change builder for this klass
			ClassName typeChangeName = name(klass.getSimpleName(), "ChangeBuilder");
			TypeSpec.Builder typeChangeBuilder = TypeSpec.classBuilder(typeChangeName).addModifiers(PUBLIC);
			typeChangeBuilder.addField(FieldSpec.builder(changeStepName, "changeStep").addModifiers(PUBLIC).build());
			typeChangeBuilder.addField(FieldSpec.builder(className, "target").addModifiers(PUBLIC).build());
			typeChangeBuilder.addMethod(MethodSpec
					.constructorBuilder()
					.addModifiers(PUBLIC)
					.addParameter(changeStepName, "changeStep")
					.addParameter(className, "target")
					.addCode("this.changeStep = changeStep;\n")
					.addCode("this.target = target;\n")
					.build());

			// Sub change builder state change
			changeStepBuilder.addMethod(MethodSpec
					.methodBuilder(klass.getSimpleName())
					.addModifiers(PUBLIC)
					.returns(typeChangeName)
					.addParameter(className, "target")
					.addCode("return new $T(changeStep, target);\n", typeChangeName)
					.build());

			// Create methods for modifying each field
			Arrays.stream(klass.getFields()).forEach(field -> {
				TypeInfo fieldInfo = new TypeInfo(field);
				if (field.getAnnotation(Configuration.class) == null)
					return;
				if (field.getName().equals("id") && field.getDeclaringClass() == ProjectNode.class)
					return;
				if (List.class.isAssignableFrom(field.getType())) {
					ChangeBuilder addBuilder = new ChangeBuilder(changeStepName, className, field, "add");
					ChangeBuilder removeBuilder = new ChangeBuilder(changeStepName, className, field, "remove");
					addBuilder
							.addParameter(int.class, "at")
							.addParameter(fieldInfo.parameters[0].type, "value")
							.addCode("target.$L.add(at, value);\n", field.getName())
							.addCodeIf(flattenPoint(fieldInfo.parameters[0]), "value.refInc(project, 2);\n")
							.addCode(("changeStep.add(new $T(target, at));\n"), removeBuilder.getName())
							.finish(typeChangeBuilder);
					removeBuilder
							.addParameter(int.class, "at")
							.addCode("$T temp = target.$L.remove(at);\n", fieldInfo.parameters[0].type, field.getName())
							.addCode(("changeStep.add(new $T(target, at, value));\n"), addBuilder.getName())
							.finish(typeChangeBuilder);
					new ChangeBuilder(changeStepName, className, field, "moveTo")
							.addParameter(int.class, "source")
							.addParameter(int.class, "dest")
							.addCode("if (source == dest) return;\n")
							.addCode("changeStep.add(new $T(target, dest, source));\n", CHANGE_TOKEN_NAME)
							.addCode("target.$L.add(dest, target.$L.remove(source));\n",
									field.getName(),
									field.getName()
							)
							.finish(typeChangeBuilder);
					typeChangeBuilder.addMethod(MethodSpec
							.methodBuilder(String.format("moveUp%s", capFirst(field.getName())))
							.addParameter(int.class, "at")
							.addCode("if (at == 0) return;\n")
							.addCode("moveTo(at, at - 1);\n")
							.build());
					typeChangeBuilder.addMethod(MethodSpec
							.methodBuilder(String.format("moveDown%s", capFirst(field.getName())))
							.addParameter(int.class, "at")
							.addCode("if (at == target.$L.size() - 1) return;\n", field.getName())
							.addCode("moveTo(at, at - 1);\n")
							.build());
					// ODO sort methods?
				} else if (Set.class.isAssignableFrom(field.getType())) {
					ChangeBuilder addBuilder = new ChangeBuilder(changeStepName, className, field, "add");
					ChangeBuilder removeBuilder = new ChangeBuilder(changeStepName, className, field, "remove");
					addBuilder
							.addParameter(fieldInfo.parameters[0].type, "value")
							.addCode("if (target.$L.contains(value)) return;\n", field.getName())
							.addCode("target.$L.add(value);\n", field.getName())
							.addCode(("changeStep.add(new $T(target, value));\n"), removeBuilder.getName())
							.addCodeIf(flattenPoint(fieldInfo.parameters[0]), "value.refInc(project, 2);\n")
							.finish(typeChangeBuilder);
					removeBuilder
							.addParameter(fieldInfo.parameters[0].type, "value")
							.addCode("if (!target.$L.contains(value)) return;\n", field.getName())
							.addCode("target.$L.add(value);\n", field.getName())
							.addCode(("changeStep.add(new $T(target, at, value));\n"), addBuilder.getName())
							.finish(typeChangeBuilder);
				} else if (Map.class.isAssignableFrom(field.getType())) {
					ChangeBuilder putBuilder = new ChangeBuilder(changeStepName, className, field, "put");
					ChangeBuilder removeBuilder = new ChangeBuilder(changeStepName, className, field, "remove");
					putBuilder
							.addParameter(fieldInfo.parameters[0].type, "key")
							.addParameter(fieldInfo.parameters[1].type, "value")
							.addCode("$T old = target.$L.put(key, value);\n",
									fieldInfo.parameters[1].type,
									field.getName()
							)
							.addCode("if (old == null) changeStep.add(new $T(key));\n", removeBuilder.getName())
							.addCode("else changeStep.add(new $T(target, key, old));\n", putBuilder.getName())
							.addCodeIf(flattenPoint(fieldInfo.parameters[1]), "value.refInc(project, 2);\n")
							.finish(typeChangeBuilder);
					removeBuilder
							.addParameter(fieldInfo.parameters[0].type, "key")
							.addCode("$T old = target.$L.remove(key);\n", fieldInfo.parameters[1].type, field.getName())
							.addCode("if (old == null) return;\n")
							.addCode("changeStep.add(new $T(target, key, old));\n", removeBuilder.getName())
							.finish(typeChangeBuilder);
				} else {
					new ChangeBuilder(changeStepName, className, field, "set")
							.addParameter(field.getType(), "value")
							.addCode("changeStep.add(new $T(target, target.$L));\n", CHANGE_TOKEN_NAME, field.getName())
							.addCode("target.$L = value;\n", field.getName())
							.addCodeIf(flattenPoint(fieldInfo), "value.refInc(project, 1);\n")
							.finish(typeChangeBuilder);
				}
			});

			TypeSpec typeChange = typeChangeBuilder.build();
			write(typeChangeName, typeChange);
		}

		write(changeStepBuilderName, changeStepBuilder.build());
		write(deserializeHelperName,
				deserializeHelper
						.addMethod(deserializeModel.addCode("throw new $T();\n", Assertion.class).build())
						.addMethod(deserializeChange.addCode("throw new $T();\n", Assertion.class).build())
						.build()
		);
	}

	public void write(ClassName name, TypeSpec spec) {
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
