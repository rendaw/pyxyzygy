package com.zarbosoft.automodel.task;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.zarbosoft.automodel.lib.GeneralMapState;
import com.zarbosoft.automodel.lib.ImmediateIDMapState;
import com.zarbosoft.automodel.lib.LazyIDMapState;
import com.zarbosoft.automodel.lib.Listener;
import com.zarbosoft.automodel.lib.ModelBase;
import com.zarbosoft.automodel.lib.ProjectObject;
import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.rendaw.common.Assertion;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.lang.model.element.Modifier.PUBLIC;

public class MapType implements AutoType {
  private final AutoType key;
  private final AutoType value;

  public MapType(AutoType key, AutoType value) {
    this.key = key;
    this.value = value;
  }

  @Override
  public boolean primitive() {
    return false;
  }

  @Override
  public TypeName poet() {
    return ParameterizedTypeName.get(ClassName.get(Map.class), key.poetBoxed(), value.poetBoxed());
  }

  @Override
  public CodeBlock generateSerializeCode(String expr) {
    return CodeBlock.builder()
        .add("writer.recordBegin();\n")
        .add(
            "for (Map.Entry<$T, $T> p : $L.entrySet()) {\n",
            key.poetBoxed(),
            value.poetBoxed(),
            expr)
        .indent()
        .add("writer.key($T.toString(p.getKey()));\n", Objects.class)
        .add(value.generateSerializeCode("p.getValue()"))
        .unindent()
        .add("}\n")
        .add("writer.recordEnd();\n")
        .build();
  }

  @Override
  public DeserializeCodeBuilt generateDeserializerCode(
      String name, String sourceExpr, boolean lazyFinish) {
    DeserializeCode out =
        new DeserializeCode().value(b -> b.add("($T) $L", HashMap.class, sourceExpr));
    if (value.flattenPoint()) {
      out.record(
          b ->
              b.add(
                      "new $T(context, k -> {\n",
                      lazyFinish ? LazyIDMapState.class : ImmediateIDMapState.class)
                  .indent()
                  .add("return $L;\n", lon.generateDeserializerCode(name, "k", false).value)
                  .unindent()
                  .add("})"));
    } else {
      DeserializeCodeBuilt keyCode = key.generateDeserializerCode(name, "key", false);
      if (!keyCode.array.isEmpty() || !keyCode.record.isEmpty() || !keyCode.finish.isEmpty())
        throw new Assertion();
      DeserializeCodeBuilt valueCode = value.generateDeserializerCode(name, "value", false);
      if (!valueCode.array.isEmpty() || !valueCode.finish.isEmpty()) throw new Assertion();
      out.record(
          b ->
              b.add("new $T() {\n", GeneralMapState.class)
                  .indent()
                  .add("public void value(Object value) {\n")
                  .indent()
                  .add("data.put($L, $L);\n", keyCode.value, valueCode.value)
                  .unindent()
                  .add("}\n"));
      if (!valueCode.record.isEmpty())
        out.record
            .add("public $T record() {\n", StackReader.State.class)
            .indent()
            .add("return $L;\n", valueCode.record)
            .unindent()
            .add("}");
      out.record(b -> b.unindent().add("}"));
    }
    return out.build();
  }

  @Override
  public CodeBlock def() {
    return CodeBlock.of("new $T()", HashMap.class);
  }

  @Override
  public void addGettersInto(TypeSpec.Builder clone, String name) {
    clone.addMethod(
        MethodSpec.methodBuilder(name + "Get")
            .addModifiers(PUBLIC)
            .returns(value.poet())
            .addParameter(key.poet(), "key")
            .addCode("return $L.get(key);\n", name)
            .build());
    clone.addMethod(
        MethodSpec.methodBuilder(name + "Has")
            .addModifiers(PUBLIC)
            .returns(boolean.class)
            .addParameter(key.poet(), "key")
            .addCode("return $L.containsKey(key);\n", name)
            .build());
    clone.addMethod(
        MethodSpec.methodBuilder(name)
            .addModifiers(PUBLIC)
            .returns(poet())
            .addCode("return $T.unmodifiableMap($L);\n", Collections.class, name)
            .build());
  }

  @Override
  public void extendDecRef(CodeBlock.Builder decRef, String name) {
    if (!flattenPoint()) return;
    decRef.add(
        "for (Map.Entry<$T, $T> e : $L.entrySet()) (($T) e.getValue()).decRef(context);\n",
        key.poet(),
        value.poet(),
        name,
        ProjectObject.class);
  }

  @Override
  public void generateChangesInto(
      Path path,
      String name,
      AutoObject entry,
      CodeBlock.Builder versionChangeDeserialize,
      ClassName typeChangeStepBuilderName,
      TypeSpec.Builder typeChangeStepBuilder) {
    CodeBlock.Builder initialPutAllCode =
        CodeBlock.builder()
            .add("if (refCount > 0) throw new $T();\n", Assertion.class)
            .add("this.$L.putAll(values);\n", name);
    if (value.flattenPoint())
      initialPutAllCode.add("values.values().forEach(v -> v.incRef(context));\n");
    String initialSetName = String.format("initial%sPutAll", Helper.capFirst(name));
    entry.genBuilder.addMethod(
        MethodSpec.methodBuilder(initialSetName)
            .addModifiers(PUBLIC)
            .addParameter(ModelBase.class, "context")
            .addParameter(
                ParameterizedTypeName.get(
                    ClassName.get(Map.class), key.poetBoxed(), value.poetBoxed()),
                "values")
            .addCode(initialPutAllCode.build())
            .build());

    GenerateChange putBuilder =
        new GenerateChange(
            entry,
            name,
            "putAll",
            ParameterizedTypeName.get(
                ClassName.get(Listener.MapPutAll.class),
                entry.genName,
                key.poetBoxed(),
                value.poetBoxed()),
            typeChangeStepBuilderName);
    CodeBlock.Builder putCode =
        CodeBlock.builder()
            .add(
                "if (remove.stream().anyMatch(put::containsKey)) throw new $T();\n",
                Assertion.class /* DEBUG */)
            .add(
                "$T<$T, $T> removing = $T.concat(\n",
                Map.class,
                key.poetBoxed(),
                value.poetBoxed(),
                Stream.class)
            .indent()
            .add("remove.stream().filter(k -> target.$L.containsKey(k)),\n", name)
            .add("put.keySet().stream().filter(k -> target.$L.containsKey(k))\n", name)
            .unindent()
            .add(").collect($T.toMap(k -> k, k -> target.$L.get(k)));\n", Collectors.class, name)
            .add(
                "changeStep.add(context, new $T(context, target, removing,\n", putBuilder.getName())
            .indent()
            .add(
                "new ArrayList<>($T.difference(put.keySet(), target.$L.keySet()))\n",
                Sets.class,
                name)
            .unindent()
            .add("));\n");
    if (value.flattenPoint())
      putCode.add("removing.entrySet().forEach(e -> e.getValue().decRef(context));\n");
    putCode
        .add("remove.forEach(k -> target.$L.remove(k));\n", name)
        .add("target.$L.putAll(put);\n", name);
    CodeBlock.Builder putMergeCode =
        CodeBlock.builder()
            .add(
                "if (other.getClass() != $T.class || (($T)other).target != target) return false;\n",
                putBuilder.getName(),
                putBuilder.getName())
            .add("remove.addAll((($T)other).remove);\n", putBuilder.getName())
            .add("(($T)other).put.forEach((k, v) -> {\n", putBuilder.getName())
            .indent()
            .add("if (put.containsKey(k) || remove.contains(k)) return;\n")
            .add("put.put(k, v);\n");
    if (value.flattenPoint()) putMergeCode.add("v.incRef(context);\n");
    putMergeCode.unindent().add("});\n").add("return true;\n");
    putBuilder
        .addMapParameter(key, value, "put", true)
        .addListParameter(key, "remove", false)
        .addCode(putCode.build())
        .onAddListener("listener.accept(this, $L, $T.of());\n", name, ImmutableList.class)
        .mergeAdd(putMergeCode.build())
        .finish(path, typeChangeStepBuilder, versionChangeDeserialize);
    typeChangeStepBuilder.addMethod(
        MethodSpec.methodBuilder(String.format("%sPut", name))
            .addParameter(key.poet(), "key")
            .addParameter(value.poet(), "value")
            .addModifiers(PUBLIC)
            .addCode(
                "$LPutAll($T.of(key, value), $T.of());\n",
                name,
                ImmutableMap.class,
                ImmutableList.class)
            .build());
    GenerateChange clearBuilder =
        new GenerateChange(
            entry,
            name,
            "clear",
            ParameterizedTypeName.get(ClassName.get(Listener.Clear.class), entry.genName),
            typeChangeStepBuilderName);
    clearBuilder.addCode(
        "changeStep.add(context, new $T(context, target, new $T(target.$L), new $T()));\n",
        putBuilder.getName(),
        HashMap.class,
        name,
        ArrayList.class);
    if (value.flattenPoint())
      clearBuilder.addCode("target.$L.forEach((k, e) -> e.decRef(context));\n", name);
    clearBuilder
        .addCode("target.$L.clear();\n", name)
        .mergeAdd(
            "return other.getClass() == $T.class && (($T)other).target == target;\n",
            clearBuilder.getName(),
            clearBuilder.getName())
        .finish(path, typeChangeStepBuilder, versionChangeDeserialize);
  }

  @Override
  public CodeBlock generateWalk(String name) {
    if (!value.flattenPoint()) return null;
    return CodeBlock.of("$L.values().iterator()", name);
  }
}
