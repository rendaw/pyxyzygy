package com.zarbosoft.internal.cloudformationmodel_generate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import com.squareup.javapoet.*;
import com.zarbosoft.cloudformationmodel.manual.FieldToMatch;
import com.zarbosoft.cloudformationmodel.manual.Policy;
import com.zarbosoft.cloudformationmodel.manual.ResourceProperties;
import com.zarbosoft.cloudformationmodel.manual.StringFunction;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zarbosoft.rendaw.common.Common.*;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;

public class GenerateTask extends Task {

	public Path path;

	public void setPath(final String path) {
		this.path = Paths.get(path);
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

	public static TypeName parsePrimType(final String field, final String typeSpec) {
		switch (typeSpec.toLowerCase()) {
			case "string":
				return TypeName.get(Object.class);
			case "boolean":
				return TypeName.get(Boolean.class);
			case "integer":
				return TypeName.get(Integer.class);
			case "double":
				return TypeName.get(Integer.class);
			case "long":
				return TypeName.get(Long.class);
			case "timestamp":
				return TypeName.get(String.class);
			case "json":
				if (Stream.of("Policy").anyMatch(field::contains)) {
					return TypeName.get(Policy.class);
				}
				return TypeName.get(JsonNode.class);
			default:
				throw new Assertion(String.format("Unknown primitive type spec %s", typeSpec));
		}
	}

	public static List<String> findTypePath(String typeSpec) {
		final List<String> names = splitNames(typeSpec);
		final List<String> packageNames = names.subList(0, names.size() - 1);
		return Streams.concat(packageNames.stream(), Stream.of(last(names) + "_")).collect(Collectors.toList());
	}

	public static OutTypeRef findType(final List<Map> parents, final String typeSpec) {
		switch (typeSpec.toLowerCase()) {
			case "fieldtomatch":
				return new OutTypeRef(Arrays.asList(FieldToMatch.class.getPackage().getName().split("\\.")),
						ClassName.get(FieldToMatch.class),
						null
				);
			default: {
				for (final Map parent : ImmutableList.copyOf(parents).reverse()) {
					final Object found = parent.get(typeSpec + "_");
					if (found != null)
						return (OutTypeRef) ((Map) found).get(null);
				}
			}
		}
		throw new Assertion(String.format("Couldn't find reference for field type %s%s",
				typeSpec,
				parents.stream().map(p -> String.format("\n\t--%s",
						p.keySet().stream().map(pp -> String.format("\n\t\t%s", pp)).collect(Collectors.joining())
				)).collect(Collectors.joining())
		));
	}

	public static class OutTypeRef {
		final List<String> packageNames;
		final ClassName className;
		final TypeSpec.Builder type;

		public OutTypeRef(final List<String> packageNames, final ClassName className, final TypeSpec.Builder type) {
			this.packageNames = packageNames;
			this.className = className;
			this.type = type;
		}
	}

	public static void prepType(final Map typeTree, final Map.Entry<String, JsonNode> entry) {
		final List<String> names = splitNames(entry.getKey());
		final List<String> packageNames = Streams
				.concat(Stream.of("com", "zarbosoft", "cloudformationmodel"),
						names.subList(0, names.size() - 1).stream()
				)
				.collect(Collectors.toList());
		final ClassName outName = ClassName.get(packageNames.stream().collect(Collectors.joining(".")), last(names));
		final TypeSpec.Builder outType = TypeSpec.classBuilder(outName).addModifiers(PUBLIC);

		Map parent = typeTree;
		for (final String name : findTypePath(entry.getKey())) {
			parent = (Map) parent.computeIfAbsent(name, n -> new HashMap());
		}
		parent.put(null, new OutTypeRef(packageNames, outName, outType));
	}

	public static void buildType(
			final Map typeTree, final Path outRoot, final Map.Entry<String, JsonNode> entry, final Class<?> base
	) {
		try {
			final List<Map> parents = new ArrayList<>();
			parents.add(typeTree);
			for (final String name : findTypePath(entry.getKey())) {
				parents.add((Map) last(parents).get(name));
			}
			final OutTypeRef foundClass = (OutTypeRef) last(parents).get(null);

			final ClassName outName = foundClass.className;
			final TypeSpec.Builder outType = foundClass.type;

			outType.addAnnotation(AnnotationSpec
					.builder(JsonTypeInfo.class)
					.addMember("use", "$T.$L", JsonTypeInfo.Id.class, JsonTypeInfo.Id.NONE.name())
					.build());

			final MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder().addModifiers(PUBLIC);
			if (base != null) {
				outType.superclass(base);
				final Constructor<?> superConstructor = base.getConstructors()[0];
				Arrays
						.stream(superConstructor.getParameters())
						.forEach(p -> constructorBuilder.addParameter(p.getType(), p.getName()));
				constructorBuilder.addStatement("super($L)",
						Arrays
								.stream(superConstructor.getParameters())
								.map(p -> p.getName())
								.collect(Collectors.joining(", "))
				);

				if (base == ResourceProperties.class) {
					outType.addMethod(MethodSpec
							.methodBuilder("getType")
							.addModifiers(PUBLIC)
							.addAnnotation(Override.class)
							.returns(String.class)
							.addStatement("return $S", entry.getKey())
							.build());
				}
			}

			for (final Map.Entry<String, JsonNode> subprop : iterable(entry.getValue().get("Properties").fields())) {
				final TypeName type;
				final String rawName = subprop.getKey();
				final String fieldName = rawName + "_";
				final boolean required = subprop.getValue().get("required") != null &&
						subprop.getValue().get("required").asBoolean(false);
				if (subprop.getValue().has("PrimitiveType")) {
					type = parsePrimType(rawName, subprop.getValue().get("PrimitiveType").asText());
					if (!required) {
						class CreateMethods {
							public void accept(final TypeName useType, final String suffix) {
								outType.addMethod(MethodSpec
										.methodBuilder(String.format("set%s%s", capFirst(rawName), suffix))
										.addModifiers(PUBLIC)
										.returns(outName)
										.addParameter(ParameterSpec.builder(useType, "value").build())
										.addStatement("this.$L = value", fieldName)
										.addStatement("return this")
										.build());
								outType.addMethod(MethodSpec
										.methodBuilder(String.format("set%sIf%s", capFirst(rawName), suffix))
										.addModifiers(PUBLIC)
										.returns(outName)
										.addParameter(ParameterSpec.builder(boolean.class, "cond").build())
										.addParameter(ParameterSpec.builder(
												ParameterizedTypeName.get(ClassName.get(Supplier.class), useType),
												"value"
										).build())
										.addStatement("if (cond) this.$L = value.get()", fieldName)
										.addStatement("return this")
										.build());
							}
						}
						final CreateMethods createMethods = new CreateMethods();
						if (type.equals(TypeName.get(Object.class)) /* String */) {
							createMethods.accept(TypeName.get(String.class), "");
							createMethods.accept(TypeName.get(StringFunction.class), "F");
						} else {
							createMethods.accept(type, "");
						}
					}
				} else {
					final String typeSpec = subprop.getValue().get("Type").asText();
					if (typeSpec.equals("List")) {
						final TypeName itemType;
						if (subprop.getValue().has("PrimitiveItemType")) {
							itemType = parsePrimType(rawName, subprop.getValue().get("PrimitiveItemType").asText());
						} else if (subprop.getValue().has("ItemType")) {
							final OutTypeRef found = findType(parents, subprop.getValue().get("ItemType").asText());
							itemType = found.className;
						} else
							throw new Assertion();
						type = ParameterizedTypeName.get(ClassName.get(List.class), itemType);
						if (!required) {
							outType.addMethod(MethodSpec
									.methodBuilder(String.format("add%s", capFirst(rawName)))
									.addModifiers(PUBLIC)
									.returns(outName)
									.addParameter(ParameterSpec
											.builder(ParameterizedTypeName.get(ClassName.get(Stream.class), itemType),
													"value"
											)
											.build())
									.addStatement("value.forEach(this.$L::add)", fieldName)
									.addStatement("return this")
									.build());
							class CreateMethods {
								public void accept(final TypeName useType, final String suffix) {
									outType.addMethod(MethodSpec
											.methodBuilder(String.format("add%s%s", capFirst(rawName), suffix))
											.addModifiers(PUBLIC)
											.returns(outName)
											.addParameter(ParameterSpec.builder(useType, "value").build())
											.addStatement("this.$L.add(value)", fieldName)
											.addStatement("return this")
											.build());
									outType.addMethod(MethodSpec
											.methodBuilder(String.format("add%sIf%s", capFirst(rawName), suffix))
											.addModifiers(PUBLIC)
											.returns(outName)
											.addParameter(ParameterSpec.builder(boolean.class, "cond").build())
											.addParameter(ParameterSpec.builder(
													ParameterizedTypeName.get(ClassName.get(Supplier.class), useType),
													"value"
											).build())
											.addStatement("if (cond) this.$L.add(value.get())", fieldName)
											.addStatement("return this")
											.build());
								}
							}
							final CreateMethods createMethods = new CreateMethods();
							if (itemType.equals(TypeName.get(Object.class)) /* String */) {
								createMethods.accept(TypeName.get(String.class), "");
								createMethods.accept(TypeName.get(StringFunction.class), "F");
							} else {
								createMethods.accept(itemType, "");
							}
						}
						constructorBuilder.addStatement("this.$L = new $T()", fieldName, ArrayList.class);
					} else if (typeSpec.equals("Map")) {
						final TypeName itemType;
						if (subprop.getValue().has("PrimitiveItemType")) {
							itemType = parsePrimType(rawName, subprop.getValue().get("PrimitiveItemType").asText());
						} else if (subprop.getValue().has("ItemType")) {
							final OutTypeRef found = findType(parents, subprop.getValue().get("ItemType").asText());
							itemType = found.className;
						} else
							throw new Assertion();
						type = ParameterizedTypeName.get(ClassName.get(Map.class),
								TypeName.get(String.class),
								itemType
						);
						outType.addMethod(MethodSpec
								.methodBuilder(String.format("put%s", capFirst(rawName)))
								.addModifiers(PUBLIC)
								.returns(outName)
								.addParameter(ParameterSpec.builder(
										ParameterizedTypeName.get((ClassName) TypeName.get(Stream.class),
												ParameterizedTypeName.get(ClassName.get(Pair.class),
														TypeName.get(String.class),
														itemType
												)
										),
										"value"
								).build())
								.addStatement("value.forEach(p -> this.$L.put(p.first, p.second))", fieldName)
								.addStatement("return this")
								.build());
						class CreateMethods {
							public void accept(final TypeName useType, final String suffix) {
								outType.addMethod(MethodSpec
										.methodBuilder(String.format("put%s%s", capFirst(rawName), suffix))
										.addModifiers(PUBLIC)
										.returns(outName)
										.addParameter(ParameterSpec.builder(String.class, "key").build())
										.addParameter(ParameterSpec.builder(useType, "value").build())
										.addStatement("this.$L.put(key, value)", fieldName)
										.addStatement("return this")
										.build());
								outType.addMethod(MethodSpec
										.methodBuilder(String.format("put%sIf%s", capFirst(rawName), suffix))
										.addModifiers(PUBLIC)
										.returns(outName)
										.addParameter(ParameterSpec.builder(boolean.class, "cond").build())
										.addParameter(ParameterSpec
												.builder(ParameterizedTypeName.get(ClassName.get(Supplier.class),
														TypeName.get(String.class)
												), "key")
												.build())
										.addParameter(ParameterSpec.builder(
												ParameterizedTypeName.get(ClassName.get(Supplier.class), useType),
												"value"
										).build())
										.addStatement("if (cond) this.$L.put(key.get(), value.get())", fieldName)
										.addStatement("return this")
										.build());
							}
						}
						final CreateMethods createMethods = new CreateMethods();
						if (itemType.equals(TypeName.get(Object.class)) /* String */) {
							createMethods.accept(TypeName.get(String.class), "");
							createMethods.accept(TypeName.get(StringFunction.class), "F");
						} else {
							createMethods.accept(itemType, "");
						}
						constructorBuilder.addStatement("this.$L = new $T()", fieldName, HashMap.class);
					} else {
						final OutTypeRef found = findType(parents, typeSpec);
						type = found.className;
						outType.addMethod(MethodSpec
								.methodBuilder(String.format("set%s", capFirst(rawName)))
								.addModifiers(PUBLIC)
								.returns(outName)
								.addParameter(ParameterSpec.builder(type, "value").build())
								.addStatement("this.$L = value", fieldName)
								.addStatement("return this")
								.build());
						outType.addMethod(MethodSpec
								.methodBuilder(String.format("set%sIf", capFirst(rawName)))
								.addModifiers(PUBLIC)
								.returns(outName)
								.addParameter(ParameterSpec.builder(boolean.class, "cond").build())
								.addParameter(ParameterSpec
										.builder(ParameterizedTypeName.get(ClassName.get(Supplier.class), type),
												"value"
										)
										.build())
								.addStatement("if (cond) this.$L = value.get()", fieldName)
								.addStatement("return this")
								.build());
					}
				}
				outType.addField((
						required ?
								FieldSpec.builder(type, fieldName, PUBLIC, FINAL) :
								FieldSpec.builder(type, fieldName, PUBLIC)
				)
						.addAnnotation(AnnotationSpec
								.builder(JsonProperty.class)
								.addMember("value", "$S", subprop.getKey())
								.build())
						.build());
				if (required)
					constructorBuilder.addStatement("this.$L = $L", type, fieldName);
			}

			outType.addMethod(constructorBuilder.build());

			JavaFile.builder(outName.packageName(), outType.build()).build().writeTo(outRoot);
		} catch (final Exception e) {
			throw new RuntimeException(String.format("Error building type %s", entry.getKey()), e);
		}
	}

	@Override
	public void execute() throws BuildException {
		uncheck(() -> Files.createDirectories(path));
		final ObjectMapper jackson = new ObjectMapper();
		final JsonNode tree = uncheck(() -> jackson.readTree(this
				.getClass()
				.getResourceAsStream("CloudFormationResourceSpecification.json")));
		final Map outTree = new HashMap();
		for (final Map.Entry<String, JsonNode> prop : iterable(tree.get("PropertyTypes").fields())) {
			prepType(outTree, prop);
		}
		for (final Map.Entry<String, JsonNode> prop : iterable(tree.get("PropertyTypes").fields())) {
			buildType(outTree, path, prop, null);
		}
		for (final Map.Entry<String, JsonNode> prop : iterable(tree.get("ResourceTypes").fields())) {
			prepType(outTree, prop);
		}
		for (final Map.Entry<String, JsonNode> prop : iterable(tree.get("ResourceTypes").fields())) {
			buildType(outTree, path, prop, ResourceProperties.class);
		}
	}

	public static void main(final String[] args) {
		final GenerateTask t = new GenerateTask();
		t.setPath(args[0]);
		t.execute();
	}
}
