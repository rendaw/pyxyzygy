package com.zarbosoft.shoedemo.deserialize;

import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.luxem.read.StackReader;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class GeneralMapState extends StackReader.State {
	private final Map data = new HashMap<>();
	private final GeneralStateBuilder builder;
	private final int at;
	private final GeneralStateBuilder.Converter convert;
	private String key;
	private String type;

	public GeneralMapState(GeneralStateBuilder builder, int at, GeneralStateBuilder.Converter convert) {
		this.builder = builder;
		this.at = at;
		this.convert = convert;
	}

	@Override
	public void key(String value) {
		key = value;
	}

	@Override
	public void value(final Object value) {
		data.put(key, convert.convert(type, value));
	}

	@Override
	public void type(final String value) {
		this.type = value;
	}

	@Override
	public Object get() {
		return data;
	}

	@Override
	public StackReader.State array() {
		return builder.next(at);
	}

	@Override
	public StackReader.State record() {
		return builder.next(at);
	}
}
