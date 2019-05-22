package com.zarbosoft.pyxyzygy.app.widgets.binding;

import java.util.Optional;
import java.util.function.Function;

import static com.zarbosoft.pyxyzygy.app.Misc.unopt;

public class BimapBinder<T, U> extends MapBinder<T, U> implements Binder<U> {
	private final Function<U, Optional<T>> back;
	private final Binder<T> parent;

	public BimapBinder(Binder<T> parent, Function<T, Optional<U>> forward, Function<U, Optional<T>> back) {
		super(parent, forward);
		this.back = back;
		this.parent = parent;
	}

	@Override
	public void set(U v) {
		Optional<T> newV = back.apply(v);
		if (!newV.isPresent()) return;
		parent.set(unopt(newV));
	}
}
