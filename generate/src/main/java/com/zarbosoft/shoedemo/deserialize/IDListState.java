package com.zarbosoft.shoedemo.deserialize;

import com.zarbosoft.luxem.read.StackReader.ArrayState;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class IDListState extends ArrayState {
	final List<Long> temp = new ArrayList<>();
	DeserializationContext.Finisher finisher = new DeserializationContext.Finisher() {
		@Override
		public void finish(DeserializationContext context) {
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
