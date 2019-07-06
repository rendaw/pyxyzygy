package com.zarbosoft.pyxyzygy.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.rendaw.common.Pair;
import org.apache.tools.ant.Task;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import static com.zarbosoft.rendaw.common.Common.uncheck;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;

public class Helper extends Task {
  public static TypeName toPoet(
      TypeInfo type, Map<Class, Pair<ClassName, TypeSpec.Builder>> typeMap) {
    TypeName base =
        Optional.ofNullable(typeMap.get(type.type))
            .map(p -> (TypeName) p.first)
            .orElseGet(() -> TypeName.get(type.type));
    if (type.parameters != null && type.parameters.length > 0) {
      return ParameterizedTypeName.get(
          (ClassName) base,
          (TypeName[])
              Arrays.stream(type.parameters).map(p -> toPoet(p, typeMap)).toArray(TypeName[]::new));
    } else return base;
  }

  public static MethodSpec.Builder poetMethod(
      Method method, Map<Class, Pair<ClassName, TypeSpec.Builder>> typeMap) {
    MethodSpec.Builder out =
        MethodSpec.methodBuilder(method.getName()).returns(toPoet(new TypeInfo(method), typeMap));
    if (Modifier.isPublic(method.getModifiers())) out.addModifiers(PUBLIC);
    if (Modifier.isPrivate(method.getModifiers())) out.addModifiers(PRIVATE);
    if (Modifier.isProtected(method.getModifiers())) out.addModifiers(PROTECTED);
    for (Parameter parameter : method.getParameters())
      out.addParameter(toPoet(new TypeInfo(parameter), typeMap), parameter.getName());
    return out;
  }

  public static void write(Path path, ClassName name, TypeSpec spec) {
    System.out.format("Writing class %s\n", name);
    uncheck(() -> JavaFile.builder(name.packageName(), spec).build().writeTo(path));
  }
}
