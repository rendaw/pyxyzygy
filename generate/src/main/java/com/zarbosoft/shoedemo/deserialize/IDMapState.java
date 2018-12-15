package com.zarbosoft.shoedemo.deserialize;

import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;
import com.zarbosoft.shoedemo.model.Tile;

import java.util.*;
import java.util.stream.Collectors;

public class IDMapState extends StackReader.State {
	protected final Map data = new HashMap<>();
	private String key;
	private final List<Pair<String, Long>> temp = new ArrayList<>();
	DeserializationContext.Finisher finisher = new DeserializationContext.Finisher() {
		@Override
		public void finish(DeserializationContext context) {
			data.putAll(temp
					.stream()
					.collect(Collectors.toMap(e -> e.first, e -> context.objectMap.get(e.second), null, HashMap::new)));
		}
	};

	@Override
	public void key(String value) {
		key = value;
	}

	@Override
	public void value(Object value) {
		temp.add(new Pair<>(key, (Long) value));
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
