package com.zarbosoft.pyxyzygy.app.widgets.binding;

import javafx.beans.property.Property;

public class PropertyBinder<T> extends PropertyHalfBinder<T> implements Binder<T> {
	public PropertyBinder(Property<T> property) {
		super(property);
	}

	@Override
	public void set(T v) {
		((Property<T>) property).setValue(v);
	}
}
