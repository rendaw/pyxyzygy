package com.zarbosoft.pyxyzygy.generate;

import com.google.common.collect.ImmutableMap;
import com.squareup.javapoet.*;
import com.sun.prism.Graphics;
import com.sun.prism.Texture;

import javax.lang.model.element.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.stream.Collectors;

public class GenerateGraphicsProxy extends TaskBase{
	@Override
	public void run() {
		Class graphics = com.sun.prism.Graphics.class;
		ClassName name = ClassName.get("com.zarbosoft.pyxyzygy.nearestneighborimageview", "NearestNeighborGraphics");
		TypeSpec.Builder b = TypeSpec.classBuilder(name).addSuperinterface(Graphics.class).addModifiers(Modifier.PUBLIC);
		b.addField(FieldSpec.builder(Graphics.class, "target").addModifiers(Modifier.PRIVATE, Modifier.FINAL).build());
		b.addMethod(MethodSpec
				.constructorBuilder()
				.addModifiers(Modifier.PUBLIC)
				.addParameter(ParameterSpec.builder(Graphics.class, "target").build())
				.addCode("this.target = target;\n")
				.build());
		Arrays.stream(graphics.getMethods()).forEach(method -> {
			CodeBlock.Builder code = CodeBlock.builder();
			for (Parameter p : method.getParameters()) {
				if (p.getType() == Texture.class) {
					code.add("$L.setLinearFiltering(false);\n", p.getName());
				}
			}
			if (method.getReturnType() != void.class)
				code.add("return ");
			code.add(
					"target.$L($L);\n",
					method.getName(),
					Arrays.stream(method.getParameters()).map(p -> p.getName()).collect(Collectors.joining(", "))
			);
			b.addMethod(Helper.poetMethod(method, ImmutableMap.of()).addCode(code.build()).build());
		});
		Helper.write(path, name, b.build());
	}
}
