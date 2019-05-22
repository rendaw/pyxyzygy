package com.zarbosoft.pyxyzygy.app.widgets.binding;

import com.zarbosoft.rendaw.common.Assertion;
import javafx.beans.property.Property;

import java.util.Optional;
import java.util.function.Function;

public class IndirectBinder<T> extends IndirectHalfBinder<T> implements Binder<T> {
	public <U> IndirectBinder(Property<U> source, Function<U, Optional> function) {
		super(source, function);
	}

	public <U> IndirectBinder(HalfBinder<U> source, Function<U, Optional> function) {
		super(source, function);
	}

	@Override
	public void set(T v) {
		if (base == null)
			return;
		if (base instanceof Property) {
			((Property) base).setValue(v);
		} else if (base instanceof Binder) {
			((Binder) base).set(v);
		} else
			throw new Assertion();
	}
}
