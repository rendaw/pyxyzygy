package com.zarbosoft.automodel.task;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.zarbosoft.automodel.lib.WeakList;

import java.util.ArrayList;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;

public class GenerateNotifier {
  private final AutoObject entry;
  private final TypeName listenerName;
  private boolean notifyFirstArg;
  private CodeBlock.Builder changeApplyNotify;
  private final String listenersFieldName;
  private final CodeBlock.Builder onAddListener;

  public GenerateNotifier(
      AutoObject object, String objectExpr, String listenersFieldName, TypeName listenerName) {
    this.entry = object;
    this.listenersFieldName = listenersFieldName;
    this.listenerName = listenerName;
    object.genBuilder.addField(
        FieldSpec.builder(
                ParameterizedTypeName.get(ClassName.get(WeakList.class), listenerName),
                listenersFieldName)
            .addModifiers(FINAL)
            .initializer("new $T<>()", WeakList.class)
            .build());
    object.genBuilder.addMethod(
        MethodSpec.methodBuilder(String.format("remove%s", Helper.capFirst(listenersFieldName)))
            .addModifiers(PUBLIC)
            .addParameter(listenerName, "listener")
            .addCode("$L.remove(listener);\n", listenersFieldName)
            .build());
    onAddListener = CodeBlock.builder();
    notifyFirstArg = true;
    changeApplyNotify =
        CodeBlock.builder()
            .add(
                "for ($T listener : new $T<>($L.$L)) listener.accept(",
                listenerName,
                ArrayList.class,
                objectExpr,
                listenersFieldName);
  }

  public CodeBlock generateNotify() {
    return changeApplyNotify.add(");\n").build();
  }

  public void generate() {
    entry.genBuilder.addMethod(
        MethodSpec.methodBuilder(String.format("add%s", Helper.capFirst(listenersFieldName)))
            .returns(listenerName)
            .addModifiers(PUBLIC)
            .addParameter(listenerName, "listener")
            .addCode("$L.add(listener);\n", listenersFieldName)
            .addCode(onAddListener.build())
            .addCode("return listener;\n")
            .build());
  }

  public void onAddListener(String format, Object[] args) {
    onAddListener.add(format, args);
  }

  public void addNotifyArgument(String name) {
    changeApplyNotify.add(String.format(notifyFirstArg ? "%s" : ", %s", name));
    notifyFirstArg = false;
  }
}
