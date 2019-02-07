package com.zarbosoft.pyxyzygy.generate;

import com.squareup.javapoet.*;
import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.pyxyzygy.generate.premodel.ProjectObject;
import com.zarbosoft.rendaw.common.Pair;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.zarbosoft.rendaw.common.Common.*;
import static javax.lang.model.element.Modifier.*;

public class Helper extends Task {

	/**
	 * For elements with ids (mutable) serialized flat at the top output level, return true.
	 *
	 * @param type
	 * @return
	 */
	public static boolean flattenPoint(TypeInfo type) {
		if (ProjectObject.class.isAssignableFrom((Class) type.type))
			return true;
		return false;
	}

	public static String decideName(Class k) {
		return k.getSimpleName();
	}

	public static TypeName toPoet(TypeInfo type, Map<Class, Pair<ClassName, TypeSpec.Builder>> typeMap) {
		TypeName base = Optional
				.ofNullable(typeMap.get(type.type))
				.map(p -> (TypeName) p.first)
				.orElseGet(() -> TypeName.get(type.type));
		if (type.parameters != null && type.parameters.length > 0) {
			return ParameterizedTypeName.get((ClassName) base,
					(TypeName[]) Arrays.stream(type.parameters).map(p -> toPoet(p, typeMap)).toArray(TypeName[]::new)
			);
		} else
			return base;
	}

	public static MethodSpec.Builder poetMethod(Method method, Map<Class, Pair<ClassName, TypeSpec.Builder>> typeMap) {
		MethodSpec.Builder out = MethodSpec
				.methodBuilder(method.getName())
				.returns(toPoet(new TypeInfo(method), typeMap));
		if (Modifier.isPublic(method.getModifiers()))
			out.addModifiers(PUBLIC);
		if (Modifier.isPrivate(method.getModifiers()))
			out.addModifiers(PRIVATE);
		if (Modifier.isProtected(method.getModifiers()))
			out.addModifiers(PROTECTED);
		for (Parameter parameter : method.getParameters())
			out.addParameter(toPoet(new TypeInfo(parameter), typeMap), parameter.getName());
		return out;
	}

	public static Method findMethod(Class klass, String name) {
		return Arrays.stream(klass.getMethods()).filter(m -> name.equals(m.getName())).findFirst().get();
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

}
