package com.zarbosoft.automodel.task;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.zarbosoft.automodel.lib.ModelVersionDeserializer;
import com.zarbosoft.luxem.read.StackReader;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static com.zarbosoft.automodel.task.Helper.poetMethod;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

public class GenerateDeserializer {
  static Method sigStateValue = Helper.findMethod(StackReader.State.class, "value");
  static Method sigStateArray = Helper.findMethod(StackReader.State.class, "array");
  static Method sigStateRecord = Helper.findMethod(StackReader.State.class, "record");
  static Method sigStateGet = Helper.findMethod(StackReader.State.class, "get");
  static Method sigFinisherFinish =
      Helper.findMethod(ModelVersionDeserializer.Finisher.class, "finish");
  private final boolean lazyFinish;
  private final CodeBlock.Builder valueCode = CodeBlock.builder().add("switch (key) {\n").indent();
  private final CodeBlock.Builder arrayCode = CodeBlock.builder().add("switch (key) {\n").indent();
  private final CodeBlock.Builder recordCode = CodeBlock.builder().add("switch (key) {\n").indent();
  private final CodeBlock.Builder finishCode = CodeBlock.builder();
  private final CodeBlock.Builder getCode = CodeBlock.builder();

  public GenerateDeserializer(boolean lazyFinish) {
    this.lazyFinish = lazyFinish;
  }

  public void add(AutoField sourceField) {
    AutoType.DeserializeCodeBuilt fieldCode = sourceField.generateDeserializerCode(lazyFinish);
    valueCode.add(fieldCode.value);
    arrayCode.add(fieldCode.array);
    recordCode.add(fieldCode.record);
    finishCode.add(fieldCode.finish);
  }

  public void generateInto(
      AutoObject entry, CodeBlock.Builder versionDeserialize, Class contextClass) {
    generateInto(entry.name, entry.genName, entry.genBuilder, versionDeserialize, contextClass);
  }

  void generateInto(
      String name,
      ClassName parentName,
      TypeSpec.Builder parentBuilder,
      CodeBlock.Builder versionDeserialize,
      Class contextClass) {
    ClassName deserializerName = parentName.nestedClass("Deserializer");
    // TODO drop Finisher if finisher code is empty
    TypeSpec.Builder deserializerSpec =
        TypeSpec.classBuilder(deserializerName)
            .addModifiers(PUBLIC, STATIC)
            .superclass(StackReader.RecordState.class)
            .addField(contextClass, "context", FINAL, PRIVATE)
            .addField(
                ParameterizedTypeName.get(Map.class, String.class, Long.class),
                "map",
                FINAL,
                PRIVATE)
            .addField(parentName, "out", FINAL, PRIVATE)
            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(PUBLIC)
                    .addParameter(contextClass, "context")
                    .addCode("this.context = context;\n")
                    .addCode(lazyFinish ? "context.finishers.add(new Finisher());\n" : "")
                    .addCode("map = new $T();\n", HashMap.class)
                    .addCode("out = new $T();\n", parentName)
                    .build())
            .addMethod(
                poetMethod(sigStateValue)
                    .addCode(
                        valueCode
                            .add(
                                "default: throw new $T(String.format(\"Unknown key (%s)\", key));\n",
                                RuntimeException.class)
                            .unindent()
                            .add("}\n")
                            .build())
                    .build())
            .addMethod(
                poetMethod(sigStateArray)
                    .addCode(
                        arrayCode
                            .add(
                                "default: throw new $T(String.format(\"Key (%s) is unknown or is not an array\", key));\n",
                                RuntimeException.class)
                            .unindent()
                            .add("}\n")
                            .build())
                    .build())
            .addMethod(
                poetMethod(sigStateRecord)
                    .addCode(
                        recordCode
                            .add(
                                "default: throw new $T(String.format(\"Key (%s) is unknown or is not a record\", key));\n",
                                RuntimeException.class)
                            .unindent()
                            .add("}\n")
                            .build())
                    .build())
            .addMethod(
                poetMethod(sigStateGet).addCode(getCode.add("return out;\n").build()).build());
    if (lazyFinish) {
      deserializerSpec.addType(
          TypeSpec.classBuilder(deserializerName.nestedClass("Finisher"))
              .superclass(ModelVersionDeserializer.Finisher.class)
              .addModifiers(PUBLIC)
              .addMethod(poetMethod(sigFinisherFinish).addCode(finishCode.build()).build())
              .build());
    }
    parentBuilder.addType(deserializerSpec.build());
    versionDeserialize.add("case \"$L\": return new $T.Deserializer(context);\n", name, parentName);
  }

  public GenerateDeserializer addFinish(CodeBlock build) {
    getCode.add(build);
    return this;
  }
}
