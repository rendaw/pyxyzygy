package com.zarbosoft.internal.shoedemo_seed.deserialize;

import com.zarbosoft.internal.shoedemo_seed.deserialize.ModelDeserializationContext.Finisher;
import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.rendaw.common.Assertion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class IDSetState extends StackReader.State {
	protected final Set data = new HashSet<>();
	final List<Long> temp = new ArrayList<>();
	Finisher finisher = new ModelDeserializationContext.Finisher() {
		@Override
		public void finish(ModelDeserializationContext context) {
			data.addAll(temp
					.stream()
					.map(e -> context.objectMap.get(e))
					.collect(Collectors.toCollection(HashSet::new)));
		}
	};

	public IDSetState(ModelDeserializationContext context) {
		context.finishers.add(finisher);
	}

	@Override
	public void value(Object value) {
		temp.add(Long.parseLong((String) value));
	}

	@Override
	public void type(String value) {
		throw new Assertion();
	}

	@Override
	public Object get() {
		return data;
	}
}
