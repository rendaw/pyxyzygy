package com.zarbosoft.internal.shoedemo_generate;

import com.squareup.javapoet.CodeBlock;
import com.zarbosoft.interface1.DefaultTypeVisitor;
import com.zarbosoft.interface1.StopWalk;
import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.interface1.Walk;
import com.zarbosoft.rendaw.common.Pair;
import com.zarbosoft.shoedemo.deserialize.DeserializationContext;

import java.lang.reflect.Field;
import java.util.List;

import static com.zarbosoft.shoedemo.deserialize.DeserializationContext.decideName;

public class DeserializerNestVisitor extends DefaultTypeVisitor<Void, CodeBlock> {
	@Override
	public CodeBlock visitString(Void _i, TypeInfo field) {
		return CodeBlock.of(".convert((t, v) -> ($T)v)", String.class);
	}

	@Override
	public CodeBlock visitLong(Void _i, TypeInfo field) {
		return CodeBlock.of(".convert((t, v) -> $T.parseLong(($T)v))", Long.class, String.class);
	}

	@Override
	public CodeBlock visitDouble(Void _i, TypeInfo field) {
		return CodeBlock.of(".convert((t, v) -> $T.parseDouble(($T)v))", Double.class, String.class);
	}

	@Override
	public CodeBlock visitBoolean(Void _i, TypeInfo field) {
		return CodeBlock.of(".convert((t, v) -> \"true\".equals(($T)v))", Long.class, String.class);
	}

	@Override
	public CodeBlock visitEnum(Void _i, TypeInfo field) {
		CodeBlock.Builder block =
				CodeBlock.builder().add(".convert((t, v) -> { switch (($T)v) {", String.class).indent();
		for (Pair<Enum<?>, Field> v : Walk.enumValues((Class) field.type))
			block.add("case \"$L\": return $E;", Walk.decideEnumName(v.first), v.first);
		return block
				.add("default: throw new RuntimeError(\"Unknown value \" + ($T)value);", String.class)
				.unindent()
				.add("} })")
				.build();
	}

	@Override
	public Object visitListBegin(Void _i, TypeInfo field) {
		if (DeserializationContext.flattenPoint(field.parameters[0]))
			return new StopWalk(CodeBlock.of(".idList()"));
		return null;
	}

	@Override
	public CodeBlock visitListEnd(Void _i, TypeInfo field, CodeBlock inner) {
		return CodeBlock.builder().add(".list()").add(inner).build();
	}

	@Override
	public Object visitSetBegin(Void _i, TypeInfo field) {
		if (DeserializationContext.flattenPoint(field.parameters[0]))
			return new StopWalk(CodeBlock.of(".idSet()"));
		return null;
	}

	@Override
	public CodeBlock visitSetEnd(Void _i, TypeInfo field, CodeBlock inner) {
		return CodeBlock.builder().add(".set()").add(inner).build();
	}

	@Override
	public Object visitMapBegin(Void _i, TypeInfo field) {
		if (DeserializationContext.flattenPoint(field.parameters[1]))
			return new StopWalk(CodeBlock.of(".idMap()"));
		return null;
	}

	@Override
	public CodeBlock visitMapEnd(
			Void _i, TypeInfo field, CodeBlock keyInner, CodeBlock valueInner
	) {
		return CodeBlock.builder().add(".map()").add(valueInner).build();
	}

	/*
	 * Generate record method - this only will trigger with non-flattened member objects.
	 */
	@Override
	public CodeBlock visitAbstractEnd(
			Void argument, TypeInfo field, List<Pair<Class<?>, CodeBlock>> derived
	) {
		CodeBlock.Builder block = CodeBlock.builder();
		for (Pair<Class<?>, CodeBlock> child : derived)
			block.add("\"$L\".equals(type) ? ", decideName(child.first)).add(child.second).add(" : ");
		return block.add("null").build();
	}

	@Override
	public Object visitConcreteBegin(Void argument, TypeInfo field) {
		return new StopWalk(CodeBlock.of("new $T.Deserializer(context)", field.type));
	}
}
