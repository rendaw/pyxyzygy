package com.zarbosoft.automodel.task;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.zarbosoft.automodel.lib.Listener;
import com.zarbosoft.automodel.lib.ModelBase;
import com.zarbosoft.automodel.lib.ModelVersionDeserializer;
import com.zarbosoft.automodel.lib.ProjectObject;
import com.zarbosoft.rendaw.common.Assertion;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static com.zarbosoft.automodel.task.Helper.poetMethod;
import static com.zarbosoft.automodel.task.Helper.write;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

class GenerateType {
  static Method sigObjIncRef = Helper.findMethod(ProjectObject.class, "incRef");
  static Method sigObjDecRef = Helper.findMethod(ProjectObject.class, "decRef");
  static Method sigObjSerialize = Helper.findMethod(ProjectObject.class, "serialize");
  static Method sigWalk = Helper.findMethod(ProjectObject.class, "walk");

  static void generate(
      Path path,
      Poetry.PoetryPair changeStepSpec,
      AutoObject object,
      CodeBlock.Builder versionObjectDeserialize,
      CodeBlock.Builder versionChangeDeserialize) {
    GenerateNotifier destroyNotifier =
        new GenerateNotifier(
            object, "this", "destroyListeners", TypeName.get(Listener.Destroy.class));
    destroyNotifier.addNotifyArgument("context");
    destroyNotifier.addNotifyArgument("this");
    destroyNotifier.generate();
    CodeBlock.Builder decRef =
        CodeBlock.builder()
            .add("refCount -= 1;\n")
            .add("if (refCount > 0) return;\n")
            .add("context.objectMap.remove(id);\n")
            .add(destroyNotifier.generateNotify());

    MethodSpec.Builder create =
        MethodSpec.methodBuilder("create")
            .addModifiers(PUBLIC, STATIC)
            .returns(object.genName)
            .addParameter(ModelBase.class, "context")
            .addCode("$T out = new $T();\n", object.genName, object.genName)
            .addCode("out.id = takeId(context);\n");

    GenerateTypeChangeBuilder generateTypeChangeBuilder =
        new GenerateTypeChangeBuilder(object, changeStepSpec);
    AutoField refCount = new AutoField(null, "refCount", AutoType.integer);
    AutoField id = new AutoField(null, "id", AutoType.lon);
    final CodeBlock.Builder cloneSerialize =
        CodeBlock.builder()
            .add("writer.type(\"$L\");\n", object.name)
            .add("writer.recordBegin();\n")
            .add(refCount.generateSerialize())
            .add(id.generateSerialize());
    final GenerateDeserializer generateDeserializer =
        new GenerateDeserializer(true)
            .addFinish(
                CodeBlock.builder()
                    .add(
                        "if (context.objectMap.containsKey(out.id())) throw new $T();\n",
                        Assertion.class)
                    .add("context.objectMap.put(out.id(), out);\n")
                    .build());
    generateDeserializer.add(refCount);
    generateDeserializer.add(id);
    CodeBlock.Builder walkCode = CodeBlock.builder();

    for (AutoField sourceField : object.allFields()) {
      FieldSpec.Builder field =
          FieldSpec.builder(sourceField.type.poet(), sourceField.name)
              .addJavadoc("$L", sourceField.comments.stream().collect(Collectors.joining("\n")));
      if (sourceField.def == null && sourceField.type.def() == null)
        throw Assertion.format(
            "Default value not set for [%s] in [%s]\n", sourceField.name, sourceField.parent.name);
      field.initializer(sourceField.def == null ? sourceField.type.def() : sourceField.def);
      sourceField.extendDecRef(decRef);
      sourceField.addGettersInto(object.genBuilder);
      CodeBlock walkStatement = sourceField.generateWalk();
      if (walkStatement != null) walkCode.add("queue.add($L);\n", walkStatement);

      if (!object.isAbstract && sourceField.persist) {
        cloneSerialize.add(sourceField.generateSerialize());
        generateDeserializer.add(sourceField);
      }

      // Create initializer, getters + mutators, changes
      if (sourceField.parent == object) {
        switch (sourceField.mutability) {
          case READONLY:
            break;
          case MUTABLE:
            // TODO listeners for everything
            // TODO change mutators w/ listeners
            break;
          case VERSIONED:
            generateTypeChangeBuilder.add(path, sourceField, versionChangeDeserialize);
            break;
          default:
            throw new Assertion();
        }
        object.genBuilder.addField(field.build());
      }
      if (sourceField.mutability == AutoField.Mutability.READONLY && sourceField.persist) {
        create.addParameter(sourceField.type.poet(), sourceField.name);
        create.addCode("out.$L = $L;\n", sourceField.name, sourceField.name);
      }
    }

    generateTypeChangeBuilder.generateInto(path, object);
    object.genBuilder.addMethod(MethodSpec.constructorBuilder().addModifiers(PROTECTED).build());
    if (!object.isAbstract) {
      generateDeserializer.generateInto(
          object, versionObjectDeserialize, ModelVersionDeserializer.class);
      object
          .genBuilder
          .addMethod(create.addCode("return out;\n").build())
          .addMethod(poetMethod(sigWalk).addCode(walkCode.build()).build())
          .addMethod(
              poetMethod(sigObjIncRef)
                  .addCode("refCount += 1;\n")
                  .addCode(
                      CodeBlock.builder()
                          .add("if (refCount == 1) {\n")
                          .indent()
                          .add(
                              "if (context.objectMap.containsKey(id)) throw new $T();\n",
                              Assertion.class)
                          .add("context.objectMap.put(id, this);\n")
                          .unindent()
                          .add("}\n")
                          .build())
                  .build())
          .addMethod(poetMethod(sigObjDecRef).addCode(decRef.build()).build())
          .addMethod(
              poetMethod(sigObjSerialize)
                  .addCode(cloneSerialize.add("writer.recordEnd();\n").build())
                  .build());
    }
    object.genBuilder.superclass(
        object.parent == null ? ClassName.get(ProjectObject.class) : object.parent.genName);
    // for (Class interface1 : entry.getInterfaces()) clone.addSuperinterface(interface1);
    if (object.isAbstract) object.genBuilder.addModifiers(ABSTRACT);
    write(path, object.genName, object.genBuilder.addModifiers(PUBLIC).build());
  }
}
