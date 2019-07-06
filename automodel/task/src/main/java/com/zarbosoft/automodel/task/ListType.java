package com.zarbosoft.automodel.task;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.zarbosoft.automodel.lib.GeneralListState;
import com.zarbosoft.automodel.lib.ImmediateIDListState;
import com.zarbosoft.automodel.lib.LazyIDListState;
import com.zarbosoft.automodel.lib.Listener;
import com.zarbosoft.automodel.lib.ModelBase;
import com.zarbosoft.automodel.lib.ProjectObject;
import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.rendaw.common.Assertion;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zarbosoft.automodel.task.GenerateChange.CHANGE_TOKEN_NAME;
import static javax.lang.model.element.Modifier.PUBLIC;

public class ListType implements AutoType {
  private final AutoType value;

  public ListType(AutoType value) {
    this.value = value;
  }

  @Override
  public boolean primitive() {
    return false;
  }

  @Override
  public TypeName poet() {
    return ParameterizedTypeName.get(ClassName.get(List.class), value.poetBoxed());
  }

  @Override
  public CodeBlock generateSerializeCode(String expr) {
    return CodeBlock.builder()
        .add("writer.arrayBegin();\n")
        .add("for ($T e : $L) {\n", value.poet(), expr)
        .indent()
        .add(value.generateSerializeCode("e"))
        .unindent()
        .add("}\n")
        .add("writer.arrayEnd();\n")
        .build();
  }

  @Override
  public DeserializeCodeBuilt generateDeserializerCode(String name, String sourceExpr, boolean lazyFinish) {
    DeserializeCode out =
        new DeserializeCode().value(b -> b.add("($T) $L", ArrayList.class, sourceExpr));
    if (value.flattenPoint()) {
      out.array(b -> b.add("new $T(context)", lazyFinish ? LazyIDListState.class : ImmediateIDListState.class));
    } else {
      DeserializeCodeBuilt valueCode = value.generateDeserializerCode(name, "value", false);
      if (!valueCode.array.isEmpty() || !valueCode.finish.isEmpty()) throw new Assertion();
      out.array(
          b ->
              b.add("new $T() {\n", GeneralListState.class)
                  .indent()
                  .add("public void value(Object value) {\n")
                  .indent()
                  .add("data.add($L);\n", valueCode.value)
                  .unindent()
                  .add("}\n"));
      if (!valueCode.record.isEmpty())
        out.record
            .add("public $T record() {\n", StackReader.State.class)
            .indent()
            .add("return $L;\n", valueCode.record)
            .unindent()
            .add("}\n");
      out.array(b -> b.unindent().add("}"));
    }
    return out.build();
  }

  @Override
  public CodeBlock def() {
    return CodeBlock.of("new $T()", ArrayList.class);
  }

  @Override
  public void addGettersInto(TypeSpec.Builder clone, String name) {
    clone.addMethod(
        MethodSpec.methodBuilder(name + "Get")
            .addModifiers(PUBLIC)
            .returns(value.poet())
            .addParameter(Integer.class, "index")
            .addCode("return $L.get(index);\n", name)
            .build());
    clone.addMethod(
        MethodSpec.methodBuilder(name + "Length")
            .addModifiers(PUBLIC)
            .returns(int.class)
            .addCode("return $L.size();\n", name)
            .build());
    clone.addMethod(
        MethodSpec.methodBuilder(name)
            .addModifiers(PUBLIC)
            .returns(poet())
            .addCode("return $T.unmodifiableList($L);\n", Collections.class, name)
            .build());
  }

  @Override
  public void extendDecRef(CodeBlock.Builder decRef, String name) {
    if (!value.flattenPoint()) return;
    decRef.add(
        "for ($T e : $L) (($T) e).decRef(context);\n",
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
    CodeBlock.Builder initialAddCode =
        CodeBlock.builder()
            .add("if (refCount > 0) throw new $T();\n", Assertion.class)
            .add("this.$L.addAll(values);\n", name);
    if (value.flattenPoint()) initialAddCode.add("values.forEach(v -> v.incRef(context));\n");
    String initialSetName = String.format("initial%sAdd", Helper.capFirst(name));
    entry.genBuilder.addMethod(
        MethodSpec.methodBuilder(initialSetName)
            .addModifiers(PUBLIC)
            .addParameter(ModelBase.class, "context")
            .addParameter(
                ParameterizedTypeName.get(ClassName.get(List.class), value.poet()), "values")
            .addCode(initialAddCode.build())
            .build());

    final ParameterizedTypeName addListener =
        ParameterizedTypeName.get(
            ClassName.get(Listener.ListAdd.class), entry.genName, value.poetBoxed());
    GenerateChange addBuilder =
        new GenerateChange(entry, name, "add", addListener, typeChangeStepBuilderName);
    final ParameterizedTypeName removeListener =
        ParameterizedTypeName.get(ClassName.get(Listener.ListRemove.class), entry.genName);
    GenerateChange removeBuilder =
        new GenerateChange(entry, name, "remove", removeListener, typeChangeStepBuilderName);
    addBuilder
        .addParameter(AutoType.integer, "at")
        .addListParameter(value, "value", true)
        .addCode("target.$L.addAll(at, value);\n", name)
        .addCode(
            "changeStep.add(context, new $T(context, target, at, value.size()));\n",
            removeBuilder.getName())
        .onAddListener("listener.accept(this, 0, $L);\n", name)
        .finish(path, typeChangeStepBuilder, versionChangeDeserialize);
    TypeName listType = ParameterizedTypeName.get(ClassName.get(List.class), value.poet());
    removeBuilder
        .addParameter(AutoType.integer, "at")
        .addParameter(AutoType.integer, "count")
        .addCode("final $T sublist = target.$L.subList(at, at + count);\n", listType, name)
        .addCode(
            "changeStep.add(context, new $T(context, target, at, new $T(sublist)));\n",
            addBuilder.getName(),
            ArrayList.class);
    if (value.flattenPoint()) removeBuilder.addCode("sublist.forEach(e -> e.decRef(context));\n");
    removeBuilder
        .addCode("sublist.clear();\n")
        .finish(path, typeChangeStepBuilder, versionChangeDeserialize);
    final ParameterizedTypeName clearListener =
        ParameterizedTypeName.get(ClassName.get(Listener.Clear.class), entry.genName);
    GenerateChange clearBuilder =
        new GenerateChange(entry, name, "clear", clearListener, typeChangeStepBuilderName)
            .addCode(
                "changeStep.add(context, new $T(context, target, 0, new $T(target.$L)));\n",
                addBuilder.getName(),
                ArrayList.class,
                name);
    if (value.flattenPoint())
      clearBuilder.addCode("target.$L.forEach(e -> e.decRef(context));\n", name);
    clearBuilder
        .addCode("target.$L.clear();\n", name)
        .mergeAdd(
            "return other.getClass() == $T.class && (($T)other).target == target;\n",
            clearBuilder.getName(),
            clearBuilder.getName());
    clearBuilder.finish(path, typeChangeStepBuilder, versionChangeDeserialize);
    typeChangeStepBuilder.addMethod(
        MethodSpec.methodBuilder(String.format("%sAdd", name))
            .addModifiers(PUBLIC)
            .returns(typeChangeStepBuilderName)
            .addParameter(int.class, "at")
            .addParameter(value.poet(), "value")
            .addCode(
                "return $LAdd(at, $T.<$T>of(value));\n", name, ImmutableList.class, value.poet())
            .build());
    typeChangeStepBuilder.addMethod(
        MethodSpec.methodBuilder(String.format("%sAdd", name))
            .addModifiers(PUBLIC)
            .returns(typeChangeStepBuilderName)
            .addParameter(value.poet(), "value")
            .addCode("return $LAdd(target.$LLength(), value);\n", name, name)
            .build());
    final ParameterizedTypeName moveToListener =
        ParameterizedTypeName.get(ClassName.get(Listener.ListMoveTo.class), entry.genName);
    GenerateChange moveToBuilder =
        new GenerateChange(entry, name, "moveTo", moveToListener, typeChangeStepBuilderName)
            .addParameter(AutoType.integer, "source")
            .addParameter(AutoType.integer, "count")
            .addParameter(AutoType.integer, "dest")
            .addCode("if (source == dest) return;\n")
            .addCode(
                "if (count >= target.$L.size()) throw new $T(\"Count is greater than size.\");\n",
                name,
                Assertion.class)
            .addCode("source = $T.min(source, target.$L.size() - count);\n", Math.class, name)
            .addCode(
                "changeStep.add(context, new $T(context, target, dest, count, source));\n",
                CHANGE_TOKEN_NAME)
            .addCode("$T sublist = target.$L.subList(source, source + count);\n", listType, name)
            .addCode("$T readd = new $T(sublist);\n", listType, ArrayList.class)
            .addCode("sublist.clear();\n")
            .addCode("target.$L.addAll(dest, readd);\n", name);
    moveToBuilder.finish(path, typeChangeStepBuilder, versionChangeDeserialize);
    typeChangeStepBuilder.addMethod(
        MethodSpec.methodBuilder(String.format("%sMoveUp", name))
            .returns(typeChangeStepBuilderName)
            .addParameter(int.class, "at")
            .addParameter(int.class, "count")
            .addCode("if (at == 0) return this;\n")
            .addCode("return $LMoveTo(at, count, at - 1);\n", name)
            .build());
    typeChangeStepBuilder.addMethod(
        MethodSpec.methodBuilder(String.format("%sMoveDown", name))
            .returns(typeChangeStepBuilderName)
            .addParameter(int.class, "at")
            .addParameter(int.class, "count")
            .addCode("if (at == target.$L.size() - 1) return this;\n", name)
            .addCode("return $LMoveTo(at, count, at + 1);\n", name)
            .build());

    entry.genBuilder.addMethod(
        MethodSpec.methodBuilder(String.format("mirror%s", Helper.capFirst(name)))
            .returns(Runnable.class)
            .addTypeVariable(TypeVariableName.get("T"))
            .addModifiers(PUBLIC)
            .addParameter(
                ParameterizedTypeName.get(ClassName.get(List.class), TypeVariableName.get("T")),
                "list")
            .addParameter(
                ParameterizedTypeName.get(
                    ClassName.get(Function.class), value.poetBoxed(), TypeVariableName.get("T")),
                "create")
            .addParameter(
                ParameterizedTypeName.get(ClassName.get(Consumer.class), TypeVariableName.get("T")),
                "remove")
            .addParameter(
                ParameterizedTypeName.get(
                    Consumer.class, Integer.class /* Index of first change */),
                "change")
            .addCode(
                CodeBlock.builder()
                    .add("return new $T() {\n", Runnable.class)
                    .indent()
                    .add("boolean dead = false;\n")
                    .add("\n")
                    .add(
                        "$T addListener = add$LAddListeners((target, at, values) -> {\n",
                        addListener,
                        Helper.capFirst(name))
                    .indent()
                    .add("if (dead) return;\n")
                    .add(
                        "list.addAll(at, values.stream().map(create).collect($T.toList()));\n",
                        Collectors.class)
                    .add("change.accept(at);\n")
                    .unindent()
                    .add("});\n")
                    .add(
                        "$T removeListener = add$LRemoveListeners((target, at, count) -> {\n",
                        removeListener,
                        Helper.capFirst(name))
                    .indent()
                    .add("if (dead) return;\n")
                    .add(
                        "List<$T> sublist = list.subList(at, at + count);\n",
                        TypeVariableName.get("T"))
                    .add("sublist.forEach(remove);\n")
                    .add("sublist.clear();\n")
                    .add("change.accept(at);\n")
                    .unindent()
                    .add("});\n")
                    .add(
                        "$T clearListener = add$LClearListeners((target) -> {\n",
                        clearListener,
                        Helper.capFirst(name))
                    .indent()
                    .add("if (dead) return;\n")
                    .add("list.clear();\n")
                    .add("change.accept(0);\n")
                    .unindent()
                    .add("});\n")
                    .add(
                        "$T moveToListener = add$LMoveToListeners((target, source, count, dest) -> {\n",
                        moveToListener,
                        Helper.capFirst(name))
                    .indent()
                    .add("if (dead) return;\n")
                    .add(
                        "List<$T> sublist = list.subList(source, source + count);\n",
                        TypeVariableName.get("T"))
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
                    .add("remove$LAddListeners(addListener);\n", Helper.capFirst(name))
                    .add("remove$LRemoveListeners(removeListener);\n", Helper.capFirst(name))
                    .add("remove$LMoveToListeners(moveToListener);\n", Helper.capFirst(name))
                    .add("remove$LClearListeners(clearListener);\n", Helper.capFirst(name))
                    .add("list.forEach(remove);\n")
                    .add("list.clear();\n")
                    .unindent()
                    .add("}\n")
                    .unindent()
                    .add("};\n")
                    .build())
            .build());
  }

  @Override
  public CodeBlock generateWalk(String name) {
    if (!value.flattenPoint()) return null;
    return CodeBlock.of("$L.iterator()", name);
  }
}
