package com.zarbosoft.pyxyzygy.app.widgets.binding;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public interface HalfBinder<T> {
	/**
	 * @param listener Immediately called with latest value
	 * @return
	 */
	BinderRoot addListener(Consumer<T> listener);

	void removeRoot(Object key);

	default <U> HalfBinder<U> map(Function<T, Optional<U>> function) {
		return new MapBinder(this, function);
	}

	Optional<T> get();
}
