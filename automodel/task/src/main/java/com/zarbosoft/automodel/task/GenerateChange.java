package com.zarbosoft.automodel.task;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.zarbosoft.automodel.lib.Change;
import com.zarbosoft.automodel.lib.ModelBase;
import com.zarbosoft.automodel.lib.ProjectObject;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zarbosoft.automodel.task.Helper.capFirst;
import static com.zarbosoft.automodel.task.Helper.poetMethod;
import static com.zarbosoft.automodel.task.Helper.write;
import static javax.lang.model.element.Modifier.PUBLIC;

class GenerateChange {
  public static Object CHANGE_TOKEN_NAME = new Object();
  static Method sigChangeDebugCounts = Helper.findMethod(Change.class, "debugRefCounts");
  static Method sigChangeMerge = Helper.findMethod(Change.class, "merge");
  static Method sigChangeApply = Helper.findMethod(Change.class, "apply");
  static Method sigChangeDelete = Helper.findMethod(Change.class, "delete");
  static Method sigChangeSerialize = Helper.findMethod(Change.class, "serialize");
  private final ClassName changeName;
  private final MethodSpec.Builder changeInvoke;
  private final List<String> invokeForward = new ArrayList<>();
  private final TypeSpec.Builder change;
  private final MethodSpec.Builder changeConstructor;
  private final CodeBlock.Builder changeApply;
  private final CodeBlock.Builder changeApply2;
  private final MethodSpec.Builder changeDelete;
  private final MethodSpec.Builder changeSerialize;
  private final CodeBlock.Builder changeDebugCounts;
  private final CodeBlock.Builder merge;
  private final GenerateDeserializer deserializer = new GenerateDeserializer(false);
  private final String changeLuxemTypeName;
  private final GenerateNotifier generateNotifier;

  GenerateChange(
      AutoObject entry,
      String fieldName,
      String action,
      ParameterizedTypeName listenerName,
      ClassName typeChangeStepBuilderName) {
    generateNotifier =
        new GenerateNotifier(
            entry,
            "target",
            String.format("%s%sListeners", fieldName, Helper.capFirst(action)),
            listenerName);
    generateNotifier.addNotifyArgument("target");
    merge = CodeBlock.builder();
    changeName = entry.model.name(entry.name, capFirst(fieldName), capFirst(action));
    change =
        TypeSpec.classBuilder(changeName)
            .superclass(Change.class)
            .addModifiers(PUBLIC)
            .addField(FieldSpec.builder(entry.genName, "target").addModifiers(PUBLIC).build());
    changeLuxemTypeName =
        String.format(
            "%s-%sChange", entry.genName.simpleName(), capFirst(fieldName) + capFirst(action));
    changeSerialize =
        poetMethod(sigChangeSerialize)
            .addCode("writer.type(\"$L\").recordBegin();\n", changeLuxemTypeName)
            .addCode(
                "writer.key(\"target\").primitive($T.toString((($T)target).id()));\n",
                Long.class,
                ProjectObject.class);
    changeConstructor =
        MethodSpec.constructorBuilder()
            .addParameter(ModelBase.class, "context")
            .addParameter(entry.genName, "target")
            .addCode("this.target = target;\n")
            .addCode("this.target.incRef(context);\n");
    changeApply = CodeBlock.builder();
    changeApply2 = CodeBlock.builder();
    changeDelete = poetMethod(sigChangeDelete).addCode("target.decRef(context);\n");
    changeInvoke =
        MethodSpec.methodBuilder(String.format("%s%s", fieldName, Helper.capFirst(action)))
            .addModifiers(PUBLIC)
            .returns(typeChangeStepBuilderName);
    changeDebugCounts = CodeBlock.builder().add("increment.accept(target);\n");
    deserializer.add(new AutoField(null, "target", entry));
  }

  public GenerateChange apply(Consumer<GenerateChange> f) {
    f.accept(this);
    return this;
  }

  public ClassName getName() {
    return changeName;
  }

  public GenerateChange addMapParameter(AutoType key, AutoType value, String name, boolean inc) {
    AutoField changeField = new AutoField(null, name, new MapType(key, value));

    TypeName map =
        ParameterizedTypeName.get(ClassName.get(Map.class), key.poetBoxed(), value.poetBoxed());
    change.addField(FieldSpec.builder(map, name).addModifiers(PUBLIC).build());
    changeConstructor.addParameter(map, name).addCode(String.format("this.%s = %s;\n", name, name));
    if (value.flattenPoint()) {
      Function<Boolean, CodeBlock> incDecBuilder =
          inc0 -> {
            CodeBlock.Builder out = CodeBlock.builder();
            out.add(
                    "for (Map.Entry<$T, $T> e : $L.entrySet()) {\n",
                    key.poetBoxed(),
                    value.poetBoxed(),
                    name)
                .indent();
            if (inc0) out.add("e.getValue().incRef(context);\n");
            else out.add("e.getValue().decRef(context);\n");
            out.unindent().add("};\n");
            return out.build();
          };
      changeConstructor.addCode(incDecBuilder.apply(true));
      changeApply2.add(incDecBuilder.apply(inc));
      changeDelete.addCode(incDecBuilder.apply(false));
      changeDebugCounts
          .add(
              "for (Map.Entry<$T, $T> e : $L.entrySet()) {\n",
              key.poetBoxed(),
              value.poetBoxed(),
              name)
          .indent()
          .add("increment.accept(e.getValue());\n")
          .unindent()
          .add("};\n");
    }
    generateNotifier.addNotifyArgument(name);
    changeInvoke.addParameter(map, name);
    invokeForward.add(name);

    deserializer.add(changeField);
    changeSerialize.addCode(changeField.generateSerialize());
    return this;
  }

  public GenerateChange addListParameter(AutoType type, String name, boolean inc) {
    AutoField changeField = new AutoField(null, name, new ListType(type));

    TypeName list = ParameterizedTypeName.get(ClassName.get(List.class), type.poetBoxed());
    change.addField(FieldSpec.builder(list, name).addModifiers(PUBLIC).build());
    changeConstructor
        .addParameter(list, name)
        .addCode(String.format("this.%s = %s;\n", name, name));
    if (type.flattenPoint()) {
      Function<Boolean, CodeBlock> incDecBuilder =
          inc0 -> {
            CodeBlock.Builder out = CodeBlock.builder();
            out.add("for ($T e : $L) {\n", type.poet(), name).indent();
            if (inc0) out.add("e.incRef(context);\n");
            else out.add("e.decRef(context);\n");
            out.unindent().add("};\n");
            return out.build();
          };
      changeConstructor.addCode(incDecBuilder.apply(true));
      changeApply2.add(incDecBuilder.apply(inc));
      changeDelete.addCode(incDecBuilder.apply(false));
      changeDebugCounts
          .add("for ($T e : $L) {\n", type.poet(), name)
          .indent()
          .add("increment.accept(e);\n")
          .unindent()
          .add("};\n");
    }
    generateNotifier.addNotifyArgument(name);
    changeInvoke.addParameter(list, name);
    invokeForward.add(name);

    deserializer.add(changeField);
    changeSerialize.addCode(changeField.generateSerialize());
    return this;
  }

  public GenerateChange addParameter(AutoType type, String name) {
    AutoField changeField = new AutoField(null, name, type);

    change.addField(FieldSpec.builder(type.poet(), name).addModifiers(PUBLIC).build());
    changeConstructor
        .addParameter(type.poet(), name)
        .addCode(String.format("this.%s = %s;\n", name, name));
    if (type.flattenPoint()) {
      Function<Boolean, CodeBlock> incDecBuilder =
          inc0 -> {
            CodeBlock.Builder out = CodeBlock.builder().add("if ($L != null)\n", name).indent();
            if (inc0) out.add("$L.incRef(context);\n", name);
            else out.add("$L.decRef(context);\n", name);
            return out.unindent().build();
          };
      changeConstructor.addCode(incDecBuilder.apply(true));
      changeDelete.addCode(incDecBuilder.apply(false));
      changeDebugCounts.add("if ($L != null) increment.accept($L);\n", name, name);
    }
    generateNotifier.addNotifyArgument(name);
    changeInvoke.addParameter(type.poet(), name);
    invokeForward.add(name);

    deserializer.add(changeField);
    changeSerialize.addCode(changeField.generateSerialize());
    return this;
  }

  public GenerateChange addCode(CodeBlock code) {
    changeApply.add(code);
    return this;
  }

  public GenerateChange addCode(String format, Object... args) {
    changeApply.add(
        CodeBlock.builder()
            .add(
                format,
                Arrays.stream(args).map(a -> a == CHANGE_TOKEN_NAME ? changeName : a).toArray())
            .build());
    return this;
  }

  public GenerateChange onAddListener(String format, Object... args) {
    generateNotifier.onAddListener(format, args);
    return this;
  }

  public GenerateChange mergeAdd(String format, Object... args) {
    merge.add(format, args);
    return this;
  }

  public GenerateChange mergeAdd(CodeBlock code) {
    merge.add(code);
    return this;
  }

  public void finish(
      Path path,
      TypeSpec.Builder typeChangeStepBuilder,
      CodeBlock.Builder globalChangeDeserialize) {
    CodeBlock mergeBuilt = merge.build();
    change.addMethod(
        poetMethod(sigChangeMerge)
            .addCode(
                mergeBuilt.isEmpty()
                    ? CodeBlock.builder().add("return false;\n").build()
                    : mergeBuilt)
            .build());
    generateNotifier.generate();
    change
        .addMethod(MethodSpec.constructorBuilder().build())
        .addMethod(changeConstructor.build())
        .addMethod(
            poetMethod(sigChangeApply)
                .addCode(changeApply.build())
                .addCode(changeApply2.build())
                .addCode(generateNotifier.generateNotify())
                .build())
        .addMethod(changeSerialize.addCode("writer.recordEnd();\n").build())
        .addMethod(changeDelete.build());
    change.addMethod(poetMethod(sigChangeDebugCounts).addCode(changeDebugCounts.build()).build());
    typeChangeStepBuilder.addMethod(
        changeInvoke
            .addCode(
                "$T change = new $T(changeStepBuilder.context, target$L);\n",
                changeName,
                changeName,
                invokeForward.stream()
                    .map(n -> String.format(", %s", n))
                    .collect(Collectors.joining("")))
            .addCode("change.apply(changeStepBuilder.context, changeStepBuilder.changeStep);\n")
            .addCode("change.delete(changeStepBuilder.context);\n")
            .addCode("return this;\n")
            .build());
    deserializer.generateInto(
        changeLuxemTypeName, changeName, change, globalChangeDeserialize, ModelBase.class);
    write(path, changeName, change.build());
  }

  public GenerateChange addCodeIf(boolean condition, String format, Object... args) {
    if (!condition) return this;
    return addCode(format, args);
  }

  public GenerateChange indent() {
    changeApply.indent();
    return this;
  }

  public GenerateChange unindent() {
    changeApply.unindent();
    return this;
  }
}
