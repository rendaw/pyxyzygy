package com.zarbosoft.automodel.task;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.zarbosoft.automodel.lib.ModelBase;

import java.nio.file.Path;

import static com.zarbosoft.automodel.task.Helper.write;
import static javax.lang.model.element.Modifier.PUBLIC;

public class GenerateTypeChangeBuilder {

  private final ClassName typeChangeStepBuilderName;
  private final TypeSpec.Builder typeChangeStepBuilder;
  private final AutoObject entry;

  public GenerateTypeChangeBuilder(AutoObject entry, Poetry.PoetryPair changeStepSpec) {
    this.entry = entry;
    typeChangeStepBuilderName = entry.model.name(entry.name + "ChangeBuilder");
    typeChangeStepBuilder =
        TypeSpec.classBuilder(typeChangeStepBuilderName)
            .addModifiers(PUBLIC)
            .addField(
                FieldSpec.builder(changeStepSpec.name, "changeStepBuilder")
                    .addModifiers(PUBLIC)
                    .build())
            .addField(FieldSpec.builder(entry.genName, "target").addModifiers(PUBLIC).build())
            .addMethod(
                MethodSpec.constructorBuilder()
                    .addModifiers(PUBLIC)
                    .addParameter(changeStepSpec.name, "changeStepBuilder")
                    .addParameter(entry.genName, "target")
                    .addCode("this.changeStepBuilder = changeStepBuilder;\n")
                    .addCode("this.target = target;\n")
                    .build());
    changeStepSpec.builder.addMethod(
        MethodSpec.methodBuilder(Helper.lowerFirst(entry.name))
            .addModifiers(PUBLIC)
            .returns(typeChangeStepBuilderName)
            .addParameter(entry.genName, "target")
            .addCode("return new $T(this, target);\n", typeChangeStepBuilderName)
            .build());
  }

  public void add(Path path, AutoField sourceField, CodeBlock.Builder globalChangeDeserialize) {
    sourceField.generateChangesInto(
        path, entry, globalChangeDeserialize, typeChangeStepBuilderName, typeChangeStepBuilder);
  }

  public void generateInto(Path path, AutoObject entry) {
    MethodSpec.Builder changeConstructor =
        MethodSpec.constructorBuilder()
            .addParameter(ModelBase.class, "contextt")
            .addParameter(entry.genName, "target")
            .addCode("this.target = target;\n");
    changeConstructor.addCode("this.target.incRef(context);\n");
    write(path, typeChangeStepBuilderName, typeChangeStepBuilder.build());
  }
}
