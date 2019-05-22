package com.zarbosoft.pyxyzygy.app.widgets.binding;

import java.util.Optional;
import java.util.function.Function;

public interface Binder<T> extends HalfBinder<T> {
	void set(T v);

	default <U> Binder<U> bimap(Function<T, Optional<U>> forward, Function<U, Optional<T>> back) {
		return new BimapBinder<T, U>(this, forward, back);
	}
}
