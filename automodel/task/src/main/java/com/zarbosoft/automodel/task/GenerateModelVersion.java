package com.zarbosoft.automodel.task;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.zarbosoft.automodel.lib.ChangeStep;
import com.zarbosoft.automodel.lib.ModelBase;
import com.zarbosoft.automodel.lib.ModelVersionDeserializer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;

import static com.zarbosoft.automodel.task.Helper.capFirst;
import static com.zarbosoft.automodel.task.Helper.poetForward;
import static com.zarbosoft.automodel.task.Helper.poetMethod;
import static com.zarbosoft.automodel.task.Helper.write;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;

class GenerateModelVersion {
  static Method sigDeserializeChange = Helper.findMethod(ModelBase.class, "deserializeChange");
  static Method sigDeserializeObject =
      Helper.findMethod(ModelVersionDeserializer.class, "deserializeObject");
  static Method sigGenerateModel = Helper.findMethod(ModelVersionDeserializer.class, "generate");
  static Method sigNeedsMigrate = Helper.findMethod(ModelBase.class, "needsMigrate");
  static Method sigMigrate = Helper.findMethod(ModelBase.class, "migrate");
  static Constructor sigConstructor;

  public static class Result {
    public final AutoModelVersion source;
    public final TypeName deserializer;
    public final TypeName version;

    public Result(AutoModelVersion source, TypeName deserializer, TypeName version) {
      this.source = source;
      this.deserializer = deserializer;
      this.version = version;
    }
  }

  static Result generate(AutoModelVersion version, Path path) {
    CodeBlock.Builder versionObjectDeserialize =
        CodeBlock.builder().add("switch (type) {\n").indent();
    CodeBlock.Builder versionChangeDeserialize =
        CodeBlock.builder().add("switch (type) {\n").indent();

    ClassName changeStepName = ClassName.get(ChangeStep.class);
    ClassName changeStepBuilderName = version.name("ChangeStepBuilder");
    Poetry.PoetryPair changeStepBuilderSpec =
        new Poetry.PoetryPair(
            changeStepBuilderName,
            TypeSpec.classBuilder(changeStepBuilderName)
                .addModifiers(PUBLIC)
                .addField(
                    FieldSpec.builder(ModelBase.class, "context")
                        .addModifiers(PUBLIC, FINAL)
                        .build())
                .addField(
                    FieldSpec.builder(changeStepName, "changeStep")
                        .addModifiers(PUBLIC, FINAL)
                        .build())
                .addMethod(
                    MethodSpec.constructorBuilder()
                        .addModifiers(PUBLIC)
                        .addParameter(ModelBase.class, "context")
                        .addParameter(ChangeStep.class, "changeStep")
                        .addCode("this.context = context;\n")
                        .addCode(
                            "this.changeStep = changeStep;\n",
                            ChangeStep.class,
                            ChangeStep.CacheId.class)
                        .build()));

    for (AutoObject entry : version.newObjects) {
      GenerateType.generate(
          path, changeStepBuilderSpec, entry, versionObjectDeserialize, versionChangeDeserialize);
    }

    ClassName contextName = version.name("Model");

    sigConstructor = ModelBase.class.getConstructors()[0];
    write(
        path,
        contextName,
        TypeSpec.classBuilder(contextName)
            .addModifiers(PUBLIC)
            .superclass(ModelBase.class)
            .addMethod(
                poetMethod(sigConstructor, ImmutableSet.of("vid"))
                    .addCode(
                        "super($L);\n",
                        Helper.poetForward(
                            sigConstructor,
                            ImmutableMap.<String, CodeBlock>builder()
                                .put("vid", CodeBlock.of("$S", version.vid))
                                .build()))
                    .build())
            .addMethod(
                poetMethod(sigDeserializeChange)
                    .addCode("Model context = this;\n")
                    .addCode(
                        versionChangeDeserialize
                            .add(
                                "default: throw new $T(String.format(\"Unknown change type %s\", type));\n",
                                RuntimeException.class)
                            .unindent()
                            .add("}\n")
                            .build())
                    .build())
            .addMethod(poetMethod(sigNeedsMigrate).addCode("return $L;\n", !version.latest).build())
            .addMethod(
                poetMethod(sigMigrate)
                    .addCode(
                        "throw new $T($S);\n",
                        RuntimeException.class,
                        "Model migration not implemented yet!")
                    .build())
            .build());
    ClassName deserializerName =
        version.name(String.format("Model%sDeserializer", capFirst(version.vid)));
    Constructor deserializeContextConstructor = ModelVersionDeserializer.class.getConstructors()[0];
    write(
        path,
        deserializerName,
        TypeSpec.classBuilder(deserializerName)
            .addModifiers(PUBLIC)
            .superclass(ModelVersionDeserializer.class)
            .addMethod(
                poetMethod(deserializeContextConstructor)
                    .addCode(
                        "super($L);\n",
                        Helper.poetForward(deserializeContextConstructor, ImmutableMap.of()))
                    .build())
            .addMethod(
                poetMethod(sigDeserializeObject)
                    .addCode(
                        versionObjectDeserialize
                            .add(
                                "default: throw new $T(String.format(\"Unknown type %s\", type));\n",
                                RuntimeException.class)
                            .unindent()
                            .add("}\n")
                            .build())
                    .build())
            .addMethod(
                poetMethod(sigGenerateModel)
                    .addCode(
                        "return new $T($L);\n",
                        contextName,
                        poetForward(sigConstructor, ImmutableMap.of("vid", CodeBlock.of(""))))
                    .build())
            .build());
    write(path, changeStepBuilderName, changeStepBuilderSpec.builder.build());

    return new Result(version, deserializerName, contextName);
  }
}
