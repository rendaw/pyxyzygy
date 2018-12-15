package com.zarbosoft.internal.shoedemo_generate;

import com.squareup.javapoet.CodeBlock;
import com.zarbosoft.interface1.DefaultTypeVisitor;
import com.zarbosoft.interface1.StopWalk;
import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;
import com.zarbosoft.shoedemo.deserialize.DeserializationContext;

import java.util.List;

class DeserializerFinishVisitor extends DefaultTypeVisitor<Void, CodeBlock> {
	public StopWalk impl(TypeInfo field) {
		if (!DeserializationContext.flattenPoint(field))
			return new StopWalk(CodeBlock.builder().build());
		return new StopWalk(CodeBlock.of("out.$L = context.objectMap.get(map.get(\"$L\"));", field.field.getName(), field.field.getName()));
	}

	@Override
	public Object visitAbstractBegin(Void _i, TypeInfo field) {
		return impl(field);
	}

	@Override
	public Object visitConcreteBegin(Void _i, TypeInfo field) {
		return impl(field);
	}
}
