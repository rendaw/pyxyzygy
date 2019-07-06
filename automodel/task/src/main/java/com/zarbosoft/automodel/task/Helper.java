package com.zarbosoft.automodel.task;

import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.rendaw.common.Pair;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zarbosoft.rendaw.common.Common.iterable;
import static com.zarbosoft.rendaw.common.Common.last;
import static com.zarbosoft.rendaw.common.Common.sublist;
import static com.zarbosoft.rendaw.common.Common.uncheck;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PROTECTED;
import static javax.lang.model.element.Modifier.PUBLIC;

public class Helper {

  public static String decideName(Class k) {
    return k.getSimpleName();
  }

  public static TypeName toPoet(TypeInfo type) {
    TypeName base = TypeName.get(type.type);
    if (type.parameters != null && type.parameters.length > 0) {
      return ParameterizedTypeName.get(
          (ClassName) base,
          (TypeName[]) Arrays.stream(type.parameters).map(p -> toPoet(p)).toArray(TypeName[]::new));
    } else return base;
  }

  public static MethodSpec.Builder poetMethod(Executable method) {
    return poetMethod(method, ImmutableSet.of());
  }

  public static MethodSpec.Builder poetMethod(Executable method, Set<String> drop) {
    MethodSpec.Builder out =
        method instanceof Constructor
            ? MethodSpec.constructorBuilder()
            : MethodSpec.methodBuilder(method.getName());
    if (method instanceof Method) out.returns(toPoet(new TypeInfo((Method) method)));
    if (Modifier.isPublic(method.getModifiers())) out.addModifiers(PUBLIC);
    if (Modifier.isPrivate(method.getModifiers())) out.addModifiers(PRIVATE);
    if (Modifier.isProtected(method.getModifiers())) out.addModifiers(PROTECTED);
    poetCopyParameters(method, drop, out);
    return out;
  }

  public static void poetCopyParameters(
      Executable method, Set<String> drop, MethodSpec.Builder out) {
    for (Parameter parameter : method.getParameters()) {
      if (drop.contains(parameter.getName())) continue;
      out.addParameter(toPoet(new TypeInfo(parameter)), parameter.getName());
    }
  }

  public static Method findMethod(Class klass, String name) {
    return Arrays.stream(klass.getMethods())
        .filter(m -> name.equals(m.getName()))
        .findFirst()
        .get();
  }

  public static List<String> splitNames(final String names) {
    final ArrayList<String> out = new ArrayList();
    List<String> split = Arrays.asList(names.split("::|\\."));
    out.addAll(sublist(split, 0, -1).stream().map(n -> n + "_").collect(Collectors.toList()));
    out.add(last(split));
    return out;
  }

  public static String capFirst(final String v) {
    return v.substring(0, 1).toUpperCase() + v.substring(1);
  }

  public static String lowerFirst(final String v) {
    return v.substring(0, 1).toLowerCase() + v.substring(1);
  }

  public static void write(Path path, ClassName name, TypeSpec spec) {
    System.out.format("Writing class %s\n", name);
    uncheck(() -> JavaFile.builder(name.packageName(), spec).build().writeTo(path));
  }

  static ClassName name(String packag, String... parts) {
    return ClassName.get(packag, Arrays.stream(parts).collect(Collectors.joining("_")));
  }

  public static CodeBlock poetForward(Executable executable, Map<String, CodeBlock> insert) {
    return poetJoin(
        ", ",
        Arrays.stream(executable.getParameters())
            .map(p -> new Pair<>(p, insert.get(p.getName())))
            .filter(p -> p.second == null || !p.second.isEmpty())
            .map(p -> p.second != null ? p.second : CodeBlock.of(p.first.getName())));
  }

  public static CodeBlock poetJoin(String sep, Stream<CodeBlock> stream) {
    CodeBlock.Builder out = CodeBlock.builder();
    boolean first = true;
    for (CodeBlock b : iterable(stream)) {
      if (first) {
        first = false;
      } else {
        out.add(sep);
      }
      out.add(b);
    }
    return out.build();
  }
}
