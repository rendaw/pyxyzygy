package com.zarbosoft.shoedemo.deserialize;

import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.rendaw.common.Assertion;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class GeneralListState extends StackReader.State {
	protected final List data = new ArrayList();
	protected String type;

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
		throw new Assertion();
	}

	@Override
	public StackReader.State record() {
		throw new Assertion();
	}
}
