package com.zarbosoft.automodel.task;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.zarbosoft.automodel.task.Helper.poetJoin;
import static javax.lang.model.element.Modifier.PUBLIC;

public class AutoObject implements AutoType {
  final String name;
  final AutoModelVersion model;
  AutoObject parent;
  final List<AutoObject> children = new ArrayList<>();
  boolean isAbstract;

  // Generation state
  ClassName genName;
  TypeSpec.Builder genBuilder;

  AutoObject(AutoModelVersion model, String name) {
    this.model = model;
    this.name = name;
    genName = model.name(name);
    genBuilder = TypeSpec.classBuilder(genName).addModifiers(PUBLIC);
  }

  List<AutoField> fields = new ArrayList<>();

  public AutoObject isAbstract() {
    isAbstract = true;
    return this;
  }

  @Override
  public boolean flattenPoint() {
    return true;
  }

  public AutoObject field(String name, AutoType type, Consumer<AutoField> consumer) {
    AutoField field = new AutoField(this, name, type);
    fields.add(field);
    consumer.accept(field);
    return this;
  }

  public AutoObject parent(AutoObject object) {
    this.parent = object;
    object.children.add(object);
    return this;
  }

  public Iterable<? extends AutoField> allFields() {
    List<AutoField> out = new ArrayList<>();
    AutoObject at = this;
    while (at != null) {
      out.addAll(at.fields);
      at = at.parent;
    }
    return out;
  }

  @Override
  public boolean primitive() {
    return false;
  }

  @Override
  public TypeName poet() {
    return genName;
  }

  @Override
  public CodeBlock generateSerializeCode(String expr) {
    return CodeBlock.builder()
        .add("if ($L == null)\n", expr)
        .indent()
        .add("writer.primitive(\"null\");\n")
        .unindent()
        .add("else\n")
        .indent()
        .add("writer.primitive($T.toString($L.id()));\n", Long.class, expr)
        .unindent()
        .build();
  }

  @Override
  public DeserializeCodeBuilt generateDeserializerCode(
      String name, String sourceExpr, boolean lazyFinish) {
    Function<CodeBlock, CodeBlock> resolve =
        sourceExpr1 -> CodeBlock.of("($T) context.getObject($L)", genName, sourceExpr1);
    if (lazyFinish)
      return new DeserializeCode()
          .valueStatements(
              b ->
                  b.add(
                      "map.put($S, \"null\".equals(value) ? null : $T.parseLong(($T) value));\n",
                      name,
                      Long.class,
                      String.class))
          .finish(
              b -> b.add("out.$L = $L;\n", name, resolve.apply(CodeBlock.of("map.get($S)", name))))
          .build();
    else
      return new DeserializeCode()
          .value(
              b ->
                  b.add(
                      resolve.apply(
                          CodeBlock.of(
                              "$T.parseLong(($T) $L)", Long.class, String.class, sourceExpr))))
          .build();
  }

  @Override
  public CodeBlock def() {
    return CodeBlock.of("null");
  }

  @Override
  public void addGettersInto(TypeSpec.Builder clone, String name) {
    AutoType.addScalarGettersInto(clone, name, this);
  }

  @Override
  public void extendDecRef(CodeBlock.Builder decRef, String name) {
    decRef.add("$L.decRef(context);\n", name);
  }

  @Override
  public void generateChangesInto(
      Path path,
      String name,
      AutoObject entry,
      CodeBlock.Builder versionChangeDeserialize,
      ClassName typeChangeStepBuilderName,
      TypeSpec.Builder typeChangeStepBuilder) {
    AutoType.generateScalarChangesInto(
        path,
        name,
        this,
        entry,
        versionChangeDeserialize,
        typeChangeStepBuilderName,
        typeChangeStepBuilder);
  }

  @Override
  public CodeBlock generateWalk(String name) {
    return CodeBlock.of("$T.of($L).iterator()", ImmutableList.class, name);
  }

  public static boolean isConstructorParameter(AutoField f) {
    return f.mutability == AutoField.Mutability.READONLY;
  }

  public CodeBlock forwardConstructorParameters() {
    return poetJoin(
        ", ",
        fields.stream().filter(AutoObject::isConstructorParameter).map(f -> CodeBlock.of(f.name)));
  }

  public void copyConstructorParameters(MethodSpec.Builder method) {
    fields.forEach(
        f -> {
          if (!isConstructorParameter(f)) return;
          method.addParameter(f.type.poet(), f.name);
        });
  }
}
