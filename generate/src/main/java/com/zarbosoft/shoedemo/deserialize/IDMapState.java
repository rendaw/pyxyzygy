package com.zarbosoft.shoedemo.deserialize;

import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class IDMapState extends StackReader.State {
	protected final Map data = new HashMap<>();
	private String key;
	private final List<Pair<String, Long>> temp = new ArrayList<>();
	ModelDeserializationContext.Finisher finisher = new ModelDeserializationContext.Finisher() {
		@Override
		public void finish(ModelDeserializationContext context) {
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
