package com.zarbosoft.shoedemo.deserialize;

import com.zarbosoft.luxem.read.StackReader;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class GeneralListState extends StackReader.State {
	private final List data = new ArrayList();
	private final GeneralStateBuilder builder;
	private final int at;
	private final GeneralStateBuilder.Converter convert;
	private String type;

	public GeneralListState(GeneralStateBuilder builder, int at, GeneralStateBuilder.Converter convert) {
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
