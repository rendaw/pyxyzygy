package com.zarbosoft.pyxyzygy.app.widgets.binding;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;

public class VariableBinder<T> implements Binder<T> {
	T value;
	WeakList<Consumer<T>> listeners = new WeakList<>();

	public VariableBinder(T value) {
		this.value = value;
	}

	@Override
	public void set(T v) {
		value = v;
		for (Consumer<T> listener : new ArrayList<>(listeners)) {
			listener.accept(v);
		}
	}

	@Override
	public BinderRoot addListener(Consumer<T> listener) {
		listeners.add(listener);
		listener.accept(value);
		return new SimpleBinderRoot(this, listener);
	}

	@Override
	public void removeRoot(Object key) {
		listeners.remove(key);
	}

	@Override
	public Optional<T> get() {
		return opt(value);
	}
}
