package com.zarbosoft.shoedemo.deserialize;

import com.zarbosoft.luxem.read.StackReader.ArrayState;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IDListState extends ArrayState {
	final List<Long> temp = new ArrayList<>();
	ModelDeserializationContext.Finisher finisher = new ModelDeserializationContext.Finisher() {
		@Override
		public void finish(ModelDeserializationContext context) {
			data.addAll(temp
					.stream()
					.map(e -> context.objectMap.get(e))
					.collect(Collectors.toCollection(ArrayList::new)));
		}
	};

	@Override
	public void value(Object value) {
		temp.add(Long.parseLong((String) value));
	}
}
