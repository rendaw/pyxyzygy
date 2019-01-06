package com.zarbosoft.shoedemo.deserialize;

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
	ModelDeserializationContext.Finisher finisher = new ModelDeserializationContext.Finisher() {
		@Override
		public void finish(ModelDeserializationContext context) {
			data.addAll(temp
					.stream()
					.map(e -> context.objectMap.get(e))
					.collect(Collectors.toCollection(HashSet::new)));
		}
	};

	@Override
	public void value(Object value) {
		temp.add((Long) value);
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
