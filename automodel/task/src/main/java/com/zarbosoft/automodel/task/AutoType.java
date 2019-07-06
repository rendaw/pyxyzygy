package com.zarbosoft.automodel.task;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.zarbosoft.automodel.lib.Listener;
import com.zarbosoft.automodel.lib.ModelBase;
import com.zarbosoft.rendaw.common.Assertion;

import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;

import static com.zarbosoft.automodel.task.GenerateChange.CHANGE_TOKEN_NAME;
import static javax.lang.model.element.Modifier.PUBLIC;

public interface AutoType {
  boolean primitive();

  TypeName poet();

  default TypeName poetBoxed() {
    return poet();
  }

  default boolean flattenPoint() {
    return false;
  }

  CodeBlock generateSerializeCode(String expr);

  DeserializeCodeBuilt generateDeserializerCode(String name, String sourceExpr, boolean lazyFinish);

  CodeBlock def();

  void addGettersInto(TypeSpec.Builder clone, String name);

  void extendDecRef(CodeBlock.Builder decRef, String name);

  void generateChangesInto(
      Path path,
      String name,
      AutoObject entry,
      CodeBlock.Builder versionChangeDeserialize,
      ClassName typeChangeStepBuilderName,
      TypeSpec.Builder typeChangeStepBuilder);

  CodeBlock generateWalk(String name);

  static class DeserializeCode {
    public final CodeBlock.Builder value = CodeBlock.builder();
    public final CodeBlock.Builder valueStatements = CodeBlock.builder();
    public final CodeBlock.Builder array = CodeBlock.builder();
    public final CodeBlock.Builder record = CodeBlock.builder();
    public final CodeBlock.Builder finish = CodeBlock.builder();

    public DeserializeCode value(Consumer<CodeBlock.Builder> consumer) {
      consumer.accept(value);
      return this;
    }

    public DeserializeCode valueStatements(Consumer<CodeBlock.Builder> consumer) {
      consumer.accept(valueStatements);
      return this;
    }

    public DeserializeCode array(Consumer<CodeBlock.Builder> consumer) {
      consumer.accept(array);
      return this;
    }

    public DeserializeCode record(Consumer<CodeBlock.Builder> consumer) {
      consumer.accept(record);
      return this;
    }

    public DeserializeCode finish(Consumer<CodeBlock.Builder> consumer) {
      consumer.accept(finish);
      return this;
    }

    public DeserializeCodeBuilt build() {
      return new DeserializeCodeBuilt(
          value.build(), valueStatements.build(), array.build(), record.build(), finish.build());
    }
  }

  static class DeserializeCodeBuilt {
    public final CodeBlock value;
    public final CodeBlock valueStatements;
    public final CodeBlock array;
    public final CodeBlock record;
    public final CodeBlock finish;

    public DeserializeCodeBuilt(
        CodeBlock value,
        CodeBlock valueStatements,
        CodeBlock array,
        CodeBlock record,
        CodeBlock finish) {
      this.value = value;
      this.valueStatements = valueStatements;
      this.array = array;
      this.record = record;
      this.finish = finish;
    }
  }

  static void addScalarGettersInto(TypeSpec.Builder clone, String name, AutoType type) {
    MethodSpec.Builder getter =
        MethodSpec.methodBuilder(name).returns(type.poet()).addModifiers(PUBLIC);
    clone.addMethod(getter.addCode("return $L;\n", name).build());
  }

  static void generateScalarChangesInto(
      Path path,
      String name,
      AutoType type,
      AutoObject entry,
      CodeBlock.Builder versionChangeDeserialize,
      ClassName typeChangeStepBuilderName,
      TypeSpec.Builder typeChangeStepBuilder) {
    GenerateChange setBuilder =
        new GenerateChange(
            entry,
            name,
            "set",
            ParameterizedTypeName.get(
                ClassName.get(Listener.ScalarSet.class), entry.genName, type.poetBoxed()),
            typeChangeStepBuilderName);
    CodeBlock.Builder changeMerge =
        CodeBlock.builder()
            .add(
                "if (other.getClass() != getClass() || (($T)other).target == target) return false;\n",
                setBuilder.getName());
    if (type.flattenPoint()) changeMerge.add("value.decRef(context);\n");
    changeMerge.add("value = (($T)other).value;\n", setBuilder.getName());
    if (type.flattenPoint()) changeMerge.add("value.incRef(context);\n");
    changeMerge.add("return true;\n");
    setBuilder
        .addParameter(type, "value")
        .addCode("if ($T.equals(value, target.$L)) return;\n", Objects.class, name)
        .addCode(
            "changeStep.add(context, new $T(context, target, target.$L));\n",
            CHANGE_TOKEN_NAME,
            name);
    if (type.flattenPoint())
      setBuilder.addCode("if (target.$L != null) target.$L.decRef(context);\n", name, name);
    setBuilder.addCode("target.$L = value;\n", name);
    if (type.flattenPoint()) setBuilder.addCode("if (value != null) value.incRef(context);\n");
    setBuilder
        .onAddListener("listener.accept(this, $L);\n", name)
        .mergeAdd(changeMerge.build())
        .finish(path, typeChangeStepBuilder, versionChangeDeserialize);

    CodeBlock.Builder initialSetCode =
        CodeBlock.builder()
            .add("if (refCount > 0) throw new $T();\n", Assertion.class)
            .add("this.$L = value;\n", name);
    if (type.flattenPoint()) initialSetCode.add("value.incRef(context);\n");
    entry.genBuilder.addMethod(
        MethodSpec.methodBuilder(String.format("initial%sSet", Helper.capFirst(name)))
            .addModifiers(PUBLIC)
            .addParameter(ModelBase.class, "context")
            .addParameter(type.poet(), "value")
            .addCode(initialSetCode.build())
            .build());
    if (!type.flattenPoint()) {
      entry.genBuilder.addMethod(
          MethodSpec.methodBuilder(String.format("forceInitial%sSet", Helper.capFirst(name)))
              .addModifiers(PUBLIC)
              .addParameter(type.poet(), "value")
              .addCode(CodeBlock.builder().add("this.$L = value;\n", name).build())
              .build());
    }
  }

  public static AutoType string =
      new AutoType() {
        @Override
        public boolean primitive() {
          return true;
        }

        @Override
        public TypeName poet() {
          return TypeName.get(String.class);
        }

        @Override
        public CodeBlock generateSerializeCode(String expr) {
          return CodeBlock.of("writer.primitive($L);\n", expr);
        }

        @Override
        public DeserializeCodeBuilt generateDeserializerCode(
            String name, String sourceExpr, boolean lazyFinish) {
          return new DeserializeCode()
              .value(b -> b.add("($T) $L", String.class, sourceExpr))
              .build();
        }

        @Override
        public CodeBlock def() {
          return CodeBlock.of("null");
        }

        @Override
        public void addGettersInto(TypeSpec.Builder clone, String name) {
          addScalarGettersInto(clone, name, this);
        }

        @Override
        public void extendDecRef(CodeBlock.Builder decRef, String name) {}

        @Override
        public void generateChangesInto(
            Path path,
            String name,
            AutoObject entry,
            CodeBlock.Builder versionChangeDeserialize,
            ClassName typeChangeStepBuilderName,
            TypeSpec.Builder typeChangeStepBuilder) {
          generateScalarChangesInto(
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
          return null;
        }
      };
  public static AutoType integer =
      new AutoType() {
        @Override
        public boolean primitive() {
          return true;
        }

        @Override
        public TypeName poet() {
          return TypeName.INT;
        }

        @Override
        public TypeName poetBoxed() {
          return TypeName.get(Integer.class);
        }

        @Override
        public CodeBlock generateSerializeCode(String expr) {
          return CodeBlock.of("writer.primitive($T.toString($L));\n", Integer.class, expr);
        }

        @Override
        public DeserializeCodeBuilt generateDeserializerCode(
            String name, String sourceExpr, boolean lazyFinish) {
          return new DeserializeCode()
              .value(b -> b.add("$T.valueOf(($T) $L)", Integer.class, String.class, sourceExpr))
              .build();
        }

        @Override
        public CodeBlock def() {
          return CodeBlock.of("$L", 0);
        }

        @Override
        public void addGettersInto(TypeSpec.Builder clone, String name) {
          addScalarGettersInto(clone, name, this);
        }

        @Override
        public void extendDecRef(CodeBlock.Builder decRef, String name) {}

        @Override
        public void generateChangesInto(
            Path path,
            String name,
            AutoObject entry,
            CodeBlock.Builder versionChangeDeserialize,
            ClassName typeChangeStepBuilderName,
            TypeSpec.Builder typeChangeStepBuilder) {
          generateScalarChangesInto(
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

          return null;
        }
      };
  public static AutoType lon =
      new AutoType() {
        @Override
        public boolean primitive() {
          return true;
        }

        @Override
        public TypeName poet() {
          return TypeName.LONG;
        }

        @Override
        public TypeName poetBoxed() {
          return TypeName.get(Long.class);
        }

        @Override
        public CodeBlock generateSerializeCode(String expr) {
          return CodeBlock.of("writer.primitive($T.toString($L));\n", Long.class, expr);
        }

        @Override
        public DeserializeCodeBuilt generateDeserializerCode(
            String name, String sourceExpr, boolean lazyFinish) {
          return new DeserializeCode()
              .value(b -> b.add("$T.valueOf(($T) $L)", Long.class, String.class, sourceExpr))
              .build();
        }

        @Override
        public CodeBlock def() {
          return CodeBlock.of("$L", 0l);
        }

        @Override
        public void addGettersInto(TypeSpec.Builder clone, String name) {
          addScalarGettersInto(clone, name, this);
        }

        @Override
        public void extendDecRef(CodeBlock.Builder decRef, String name) {}

        @Override
        public void generateChangesInto(
            Path path,
            String name,
            AutoObject entry,
            CodeBlock.Builder versionChangeDeserialize,
            ClassName typeChangeStepBuilderName,
            TypeSpec.Builder typeChangeStepBuilder) {
          generateScalarChangesInto(
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

          return null;
        }
      };
  public static AutoType bool =
      new AutoType() {
        @Override
        public boolean primitive() {
          return true;
        }

        @Override
        public TypeName poet() {
          return TypeName.BOOLEAN;
        }

        @Override
        public TypeName poetBoxed() {
          return TypeName.get(Boolean.class);
        }

        @Override
        public CodeBlock generateSerializeCode(String expr) {
          return CodeBlock.of("writer.primitive($L ? \"true\" : \"false\");\n", expr);
        }

        @Override
        public DeserializeCodeBuilt generateDeserializerCode(
            String name, String sourceExpr, boolean lazyFinish) {
          return new DeserializeCode().value(b -> b.add("\"true\".equals($L)", sourceExpr)).build();
        }

        @Override
        public CodeBlock def() {
          return CodeBlock.of("$L", false);
        }

        @Override
        public void addGettersInto(TypeSpec.Builder clone, String name) {
          addScalarGettersInto(clone, name, this);
        }

        @Override
        public void extendDecRef(CodeBlock.Builder decRef, String name) {}

        @Override
        public void generateChangesInto(
            Path path,
            String name,
            AutoObject entry,
            CodeBlock.Builder versionChangeDeserialize,
            ClassName typeChangeStepBuilderName,
            TypeSpec.Builder typeChangeStepBuilder) {
          generateScalarChangesInto(
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
          return null;
        }
      };

  public static AutoType of(Class klass) {
    return new AutoType() {
      @Override
      public boolean primitive() {
        return false;
      }

      @Override
      public TypeName poet() {
        return TypeName.get(klass);
      }

      @Override
      public CodeBlock generateSerializeCode(String expr) {
        return CodeBlock.of("$L.serialize(writer);\n", expr);
      }

      @Override
      public DeserializeCodeBuilt generateDeserializerCode(
          String name, String sourceExpr, boolean lazyFinish) {
        return new DeserializeCode()
            .record(b -> b.add("new $T.Deserializer()", TypeName.get(klass)))
            .value(b -> b.add("($T) $L", TypeName.get(klass), sourceExpr))
            .build();
      }

      @Override
      public CodeBlock def() {
        return null;
      }

      @Override
      public void addGettersInto(TypeSpec.Builder clone, String name) {
        addScalarGettersInto(clone, name, this);
      }

      @Override
      public void extendDecRef(CodeBlock.Builder decRef, String name) {}

      @Override
      public void generateChangesInto(
          Path path,
          String name,
          AutoObject entry,
          CodeBlock.Builder versionChangeDeserialize,
          ClassName typeChangeStepBuilderName,
          TypeSpec.Builder typeChangeStepBuilder) {
        generateScalarChangesInto(
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

        return null;
      }
    };
  }

  public static AutoType map(AutoType key, AutoType value) {
    return new MapType(key, value);
  }

  public static AutoType list(AutoType value) {
    return new ListType(value);
  }
}
