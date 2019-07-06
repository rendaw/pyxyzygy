package com.zarbosoft.automodel.task;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeSpec;
import com.zarbosoft.rendaw.common.Assertion;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AutoField {
  final AutoObject parent;
  CodeBlock def;
  boolean persist;
  Mutability mutability;

  public static enum Mutability {
    READONLY,
    MUTABLE,
    VERSIONED
  }

  public AutoField def(CodeBlock block) {
    def = block;
    return this;
  }

  public AutoField def(String format, Object... args) {
    def = CodeBlock.of(format, args);
    return this;
  }

  public CodeBlock generateSerialize() {
    return type.generateSerializeCode(name);
  }

  public AutoType.DeserializeCodeBuilt generateDeserializerCode(boolean lazyFinish) {
    AutoType.DeserializeCodeBuilt pre = type.generateDeserializerCode(name, "value", lazyFinish);
    AutoType.DeserializeCode out = new AutoType.DeserializeCode();
    if (!pre.record.isEmpty()) {
      out.record.add("case \"$L\": return $L;\n", name, pre.record);
    }
    if (!pre.array.isEmpty()) {
      out.array.add("case \"$L\": return $L;\n", name, pre.array);
    }
    if (!pre.value.isEmpty() || !pre.valueStatements.isEmpty()) {
      out.value.add("case \"$L\":\n", name).indent();
      if (!pre.valueStatements.isEmpty()) out.value.add(pre.valueStatements);
      if (!pre.value.isEmpty()) out.value.add("out.$L = $L;\n", name, pre.value);
      out.value.add("break;\n").unindent();
    }
    out.finish.add(pre.finish);
    return out.build();
  }

  public String name;
  public AutoType type;
  final List<String> comments;
  public AutoType key;

  public AutoField comment(String comment) {
    this.comments.add(comment);
    return this;
  }

  public AutoField(AutoObject parent, String name, AutoType type) {
    this.parent = parent;
    this.name = name;
    this.type = type;
    this.persist = false;
    this.mutability = Mutability.READONLY;
    this.comments = new ArrayList<>();
  }

  public AutoField versioned() {
    if (mutability != Mutability.READONLY) throw new Assertion();
    persist = true;
    mutability = Mutability.VERSIONED;
    return this;
  }

  public AutoField persist() {
    persist = true;
    return this;
  }

  public void addGettersInto(TypeSpec.Builder clone) {
    type.addGettersInto(clone, name);
  }

  public void extendDecRef(CodeBlock.Builder decRef) {
    type.extendDecRef(decRef, name);
  }

  public void generateChangesInto(
      Path path,
      AutoObject entry,
      CodeBlock.Builder globalChangeDeserialize,
      ClassName typeChangeStepBuilderName,
      TypeSpec.Builder typeChangeStepBuilder) {
    type.generateChangesInto(
        path,
        name,
        entry,
        globalChangeDeserialize,
        typeChangeStepBuilderName,
        typeChangeStepBuilder);
  }

  public CodeBlock generateWalk() {
    return type.generateWalk(name);
  }

  public AutoField mutable() {
    this.mutability = Mutability.MUTABLE;
    return this;
  }
}
