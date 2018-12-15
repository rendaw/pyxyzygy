package com.zarbosoft.internal.shoedemo_generate;

import com.squareup.javapoet.CodeBlock;
import com.zarbosoft.interface1.StopWalk;
import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.interface1.TypeVisitor;
import com.zarbosoft.interface1.Walk;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.DeadCode;
import com.zarbosoft.rendaw.common.Pair;
import com.zarbosoft.shoedemo.deserialize.DeserializationContext;
import com.zarbosoft.shoedemo.model.Tile;
import com.zarbosoft.shoedemo.model.Vector;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class DeserializerValueVisitor implements TypeVisitor<Void, CodeBlock> {
	Set recordClauses;

	public DeserializerValueVisitor() {
		recordClauses = new HashSet();
	}

	@Override
	public CodeBlock visitString(Void _i, TypeInfo field) {
		return CodeBlock.of("out.$L = ($T)value;\n", field.field.getName(), String.class);
	}

	@Override
	public CodeBlock visitLong(Void _i, TypeInfo field) {
		boolean isInt = field.type == int.class || field.type == Integer.class;
		return CodeBlock.of(
				"out.$L = $T.parse$L(($T)value);\n",
				field.field.getName(),
				isInt ? Integer.class : Long.class,
				isInt ? "Int" : "Long",
				String.class
		);
	}

	@Override
	public CodeBlock visitDouble(Void _i, TypeInfo field) {
		return CodeBlock.of("out.$L = Double.parseDouble(($T)value);\n", field.field.getName(), String.class);
	}

	@Override
	public CodeBlock visitBoolean(Void _i, TypeInfo field) {
		return CodeBlock.of("out.$L = value.equals(\"true\");\n", field.field.getName());
	}

	@Override
	public CodeBlock visitEnum(Void _i, TypeInfo field) {
		CodeBlock.Builder block = CodeBlock.builder().add("switch (value) {").indent();
		for (Pair<Enum<?>, Field> v : Walk.enumValues((Class) field.type)) {
			block
					.add("case \"$L\": {\n", Walk.decideEnumName(v.first))
					.indent()
					.add("out.$L = $E;\n", field.field.getName(), v.first)
					.add("return;\n")
					.unindent()
					.add("}\n");
		}
		return block
				.add("default: throw new RuntimeError(\"Unknown value \" + ($T)value);\n", String.class)
				.unindent()
				.build();
	}

	@Override
	public Object visitListBegin(Void _i, TypeInfo field) {
		return new StopWalk<>(CodeBlock.of("out.$L = ($T)value;\n", field.field.getName(), field.type));
	}

	@Override
	public CodeBlock visitListEnd(Void _i, TypeInfo field, CodeBlock inner) {
		throw new DeadCode();
	}

	@Override
	public Object visitSetBegin(Void _i, TypeInfo field) {
		return new StopWalk<>(CodeBlock.of("out.$L = ($T)value;\n", field.field.getName(), field.type));
	}

	@Override
	public CodeBlock visitSetEnd(Void _i, TypeInfo field, CodeBlock inner) {
		throw new DeadCode();
	}

	@Override
	public Object visitMapBegin(Void _i, TypeInfo field) {
		return new StopWalk<>(CodeBlock.of("out.$L = ($T)value;\n", field.field.getName(), field.type));
	}

	@Override
	public CodeBlock visitMapEnd(
			Void _i, TypeInfo field, CodeBlock keyInner, CodeBlock valueInner
	) {
		throw new DeadCode();
	}

	@Override
	public Object visitAbstractBegin(Void _i, TypeInfo field) {
		if (DeserializationContext.flattenPoint(field))
			return new StopWalk<>(CodeBlock.of("map.put(\"$L\", value);\n", field.field.getName()));
		return new StopWalk<>(CodeBlock.of("out.$L = ($T)value;\n", field.field.getName(), field.type));
	}

	@Override
	public CodeBlock visitAbstractEnd(
			Void _i, TypeInfo field, List<Pair<Class<?>, CodeBlock>> derived
	) {
		throw new Assertion();
	}

	@Override
	public Object visitConcreteBegin(Void _i, TypeInfo field) {
		if (DeserializationContext.flattenPoint(field))
			return new StopWalk<>(CodeBlock.of("map.put(\"$L\", value);\n", field.field.getName()));
		return new StopWalk<>(CodeBlock.of("out.$L = ($T)value;\n", field.field.getName(), field.type));
	}

	@Override
	public CodeBlock visitConcreteEnd(
			Void _i, TypeInfo field, List<Pair<TypeInfo, CodeBlock>> fields
	) {
		throw new Assertion();
	}

	@Override
	public CodeBlock visitOther(Void argument, TypeInfo field) {
		if (field.type == Vector.class) {
			return CodeBlock.of("out.$L = ($T)value;\n", field.field.getName(), field.type);
		} else if (field.type == Tile.class) {
			CodeBlock.of("map.put(\"$L\", value);\n", field.field.getName());
		}
		return TypeVisitor.super.visitOther(argument, field);
	}
}
