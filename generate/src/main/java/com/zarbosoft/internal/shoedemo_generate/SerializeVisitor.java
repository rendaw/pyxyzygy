package com.zarbosoft.internal.shoedemo_generate;

import com.squareup.javapoet.CodeBlock;
import com.zarbosoft.interface1.StopWalk;
import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.interface1.TypeVisitor;
import com.zarbosoft.interface1.Walk;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;
import com.zarbosoft.shoedemo.deserialize.DeserializationContext;
import com.zarbosoft.shoedemo.model.ProjectObjectInterface;
import com.zarbosoft.shoedemo.model.Tile;
import com.zarbosoft.shoedemo.model.Vector;

import java.util.List;
import java.util.Objects;

class SerializeVisitor implements TypeVisitor<Integer, CodeBlock> {
	public CodeBlock visitPrimitive(int argument, TypeInfo field) {
		return CodeBlock.of(
				"writer.primitive($T.toString($L));\n",
				Objects.class,
				argument > 0 ? String.format("e%s", argument) : field.field.getName()
		);
	}

	@Override
	public CodeBlock visitString(Integer argument, TypeInfo field) {
		return visitPrimitive(argument, field);
	}

	@Override
	public CodeBlock visitLong(Integer argument, TypeInfo field) {
		return visitPrimitive(argument, field);
	}

	@Override
	public CodeBlock visitDouble(Integer argument, TypeInfo field) {
		return visitPrimitive(argument, field);
	}

	@Override
	public CodeBlock visitBoolean(Integer argument, TypeInfo field) {
		return visitPrimitive(argument, field);
	}

	@Override
	public CodeBlock visitEnum(Integer argument, TypeInfo field) {
		return CodeBlock.of(
				"writer.primitive($T.decideEnumName($L));\n",
				Walk.class,
				argument > 0 ? String.format("e%s", argument) : field.field.getName()
		);
	}

	@Override
	public Object visitListBegin(Integer argument, TypeInfo field) {
		return argument + 1;
	}

	@Override
	public CodeBlock visitListEnd(Integer argument, TypeInfo field, CodeBlock inner) {
		String name = String.format("e%s", argument + 1);
		return CodeBlock.builder().add(
				"for ($T $L : $L) {\n",
				field.parameters[0].type,
				name,
				argument > 0 ? String.format("e%s", argument) : field.field.getName()
		).indent().add(inner).unindent().add("}\n").build();
	}

	@Override
	public Object visitSetBegin(Integer argument, TypeInfo field) {
		return argument + 1;
	}

	@Override
	public CodeBlock visitSetEnd(Integer argument, TypeInfo field, CodeBlock inner) {
		String name = String.format("e%s", argument + 1);
		return CodeBlock.builder().add(
				"for ($T $L : $L) {\n",
				field.parameters[0].type,
				name,
				argument > 0 ? String.format("e%s", argument) : field.field.getName()
		).indent().add(inner).unindent().add("}\n").build();
	}

	@Override
	public Object visitMapBegin(Integer argument, TypeInfo field) {
		return argument + 1;
	}

	@Override
	public CodeBlock visitMapEnd(Integer argument, TypeInfo field, CodeBlock innerKey, CodeBlock innerValue) {
		String name = String.format("e%s", argument + 1);
		return CodeBlock.builder().add(
				"for (Map.Entry<$T, $T> $L : $L.entrySet()) {\n",
				field.parameters[0].type,
				field.parameters[1].type,
				name,
				argument > 0 ? String.format("e%s", argument) : field.field.getName()
		).indent().add("writer.key($L.getKey());\n", name).add(innerValue).unindent().add("}\n").build();
	}

	@Override
	public Object visitAbstractBegin(Integer argument, TypeInfo field) {
		String name = argument > 0 ? String.format("e%s", argument) : field.field.getName();
		return new StopWalk<CodeBlock>(CodeBlock
				.builder()
				.add("writer.type($T.decideName($L.getClass()));\n", DeserializationContext.class, name)
				.add("(($T)$L).serialize(writer);\n", ProjectObjectInterface.class, name)
				.build());
	}

	@Override
	public CodeBlock visitAbstractEnd(
			Integer argument, TypeInfo field, List<Pair<Class<?>, CodeBlock>> derived
	) {
		throw new Assertion();
	}

	@Override
	public Object visitConcreteBegin(Integer argument, TypeInfo field) {
		String name = argument > 0 ? String.format("e%s", argument) : field.field.getName();
		if (DeserializationContext.flattenPoint(field)) {
			return new StopWalk<>(CodeBlock.of("writer.primitive($T.toString($L.id));\n", Objects.class, name));
		}
		return new StopWalk<>(CodeBlock.of("(($T)$L).serialize(writer);\n", ProjectObjectInterface.class, name));
	}

	@Override
	public CodeBlock visitConcreteEnd(
			Integer argument, TypeInfo field, List<Pair<TypeInfo, CodeBlock>> childFields
	) {
		throw new Assertion();
	}

	@Override
	public CodeBlock visitOther(Integer argument, TypeInfo field) {
		if (field.type == Vector.class) {
			String name = argument > 0 ? String.format("e%s", argument) : field.field.getName();
			return CodeBlock.of("(($T)$L).serialize(writer);", ProjectObjectInterface.class, name);
		}
		if (field.type == Tile.class) {
			return CodeBlock.of("writer.primitive($T.toString($L.id));\n", Objects.class, field.field.getName());
		}
		return TypeVisitor.super.visitOther(argument, field);
	}
}
