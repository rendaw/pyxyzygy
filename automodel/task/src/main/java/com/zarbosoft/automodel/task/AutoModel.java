package com.zarbosoft.automodel.task;

import com.google.common.collect.ImmutableMap;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.zarbosoft.automodel.lib.ModelBase;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Common;

import javax.lang.model.element.Modifier;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collector;

import static com.zarbosoft.automodel.task.GenerateModelVersion.sigConstructor;
import static com.zarbosoft.automodel.task.Helper.name;
import static com.zarbosoft.automodel.task.Helper.poetCopyParameters;
import static com.zarbosoft.automodel.task.Helper.poetForward;
import static com.zarbosoft.automodel.task.Helper.write;
import static com.zarbosoft.rendaw.common.Common.last;

public class AutoModel {
  public final String packag;
  final List<AutoModelVersion> versions = new ArrayList<>();

  public AutoModel(String packag) {
    this.packag = packag;
  }

  public AutoModelVersion version(String vid) {
    if (!versions.isEmpty() && !Common.isOrdered(vid, last(versions).vid))
      throw new Assertion("Latest version must be first!");
    AutoModelVersion version = new AutoModelVersion(this, vid, versions.isEmpty());
    versions.add(version);
    return version;
  }

  public AutoModel version(String vid, Consumer<AutoModelVersion> consumer) {
    AutoModelVersion v = version(vid);
    consumer.accept(v);
    return this;
  }

  public void generate(Path path) {
    Common.deleteTree(path);
    List<GenerateModelVersion.Result> versionTypes = new ArrayList<>();
    for (AutoModelVersion version : versions) {
      versionTypes.add(GenerateModelVersion.generate(version, path));
    }
    GenerateModelVersion.Result latest = last(versionTypes);
    ClassName name = name(packag, "ModelVersions");
    Map<String, CodeBlock> createAutoParams =
        ImmutableMap.<String, CodeBlock>builder()
            .put("vid", CodeBlock.of(""))
            .put("nextId", CodeBlock.of("$L", 0))
            .put("objectMap", CodeBlock.of("new $T<>()", HashMap.class))
            .put("undoHistory", CodeBlock.of("new $T()", ArrayList.class))
            .put("redoHistory", CodeBlock.of("new $T()", ArrayList.class))
            .put("activeChange", CodeBlock.of("null"))
            .build();
    MethodSpec.Builder create =
        MethodSpec.methodBuilder("create")
            .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
            .returns(latest.version)
            .addCode(
                CodeBlock.builder()
                    .add(
                        "$T out = new $T($L);\n",
                        latest.version,
                        latest.version,
                        poetForward(sigConstructor, createAutoParams))
                    .add(
                        "$T root = $T.create(out, $L);\n",
                        latest.source.root.genName,
                        latest.source.root.genName,
                        latest.source.root.forwardConstructorParameters())
                    .add("out.root = root;\n")
                    .add("initialize.accept(out);\n")
                    .add("out.root.incRef(out);\n")
                    .add("out.commitCurrentChange();\n")
                    .add("out.setDirty(out);\n")
                    .add("return out;\n")
                    .build());
    poetCopyParameters(sigConstructor, createAutoParams.keySet(), create);
    latest.source.root.copyConstructorParameters(create);
    create.addParameter(
        ParameterizedTypeName.get(ClassName.get(Consumer.class), latest.version), "initialize");
    write(
        path,
        name,
        TypeSpec.classBuilder(name)
            .addModifiers(Modifier.PUBLIC)
            .addMethod(create.build())
            .addMethod(
                MethodSpec.methodBuilder("deserialize")
                    .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                    .addParameter(ParameterSpec.builder(Path.class, "path").build())
                    .addParameter(ParameterSpec.builder(int.class, "maxUndo").build())
                    .returns(ModelBase.DeserializeResult.class)
                    .addCode(
                        CodeBlock.builder()
                            .add("return $T.deserialize(\n", ModelBase.class)
                            .indent()
                            .add("path,\n")
                            .add("version -> {\n")
                            .indent()
                            .add("switch (version) {\n")
                            .indent()
                            .add(
                                versionTypes.stream()
                                    .map(
                                        v ->
                                            CodeBlock.of(
                                                "case $S: return new $T(path, maxUndo);\n",
                                                v.source.vid,
                                                v.deserializer))
                                    .collect(
                                        Collector.of(
                                            CodeBlock::builder,
                                            (builder, block) -> builder.add(block),
                                            (a, b) -> {
                                              throw new Assertion();
                                            }))
                                    .build())
                            .add(
                                "default: throw new $T($T.format(\"Unknown project version [%s]\", version));\n",
                                IllegalStateException.class, String.class)
                            .unindent()
                            .add("}\n")
                            .unindent()
                            .add("}\n")
                            .unindent()
                            .add(");\n")
                            .build())
                    .build())
            .build());
  }
}
