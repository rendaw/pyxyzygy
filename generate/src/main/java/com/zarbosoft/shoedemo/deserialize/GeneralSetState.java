package com.zarbosoft.shoedemo.deserialize;

import com.zarbosoft.luxem.read.StackReader;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

public class GeneralSetState extends StackReader.State {
	private final Set data = new HashSet();
	private final GeneralStateBuilder builder;
	private final int at;
	private final GeneralStateBuilder.Converter convert;
	private String type;

	public GeneralSetState(GeneralStateBuilder builder, int at, GeneralStateBuilder.Converter convert) {
		this.builder = builder;
		this.at = at;
		this.convert = convert;
	}

	@Override
	public void value(final Object value) {
		data.add(convert.convert(type, value));
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
