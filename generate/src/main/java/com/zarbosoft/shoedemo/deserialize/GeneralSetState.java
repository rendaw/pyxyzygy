package com.zarbosoft.shoedemo.deserialize;

import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.rendaw.common.Assertion;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;

public abstract class GeneralSetState extends StackReader.State {
	private final Set data = new HashSet();
	private String type;

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
