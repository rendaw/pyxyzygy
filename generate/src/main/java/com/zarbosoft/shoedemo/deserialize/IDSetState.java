package com.zarbosoft.shoedemo.deserialize;

import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.luxem.tree.Typed;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.shoedemo.model.Tile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class IDSetState extends StackReader.State {
	protected final Set data = new HashSet<>();
	final List<Long> temp = new ArrayList<>();
	DeserializationContext.Finisher finisher = new DeserializationContext.Finisher() {
		@Override
		public void finish(DeserializationContext context) {
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
