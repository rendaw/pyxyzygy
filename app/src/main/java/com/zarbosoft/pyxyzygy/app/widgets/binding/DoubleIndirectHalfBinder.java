package com.zarbosoft.pyxyzygy.app.widgets.binding;

import com.zarbosoft.rendaw.common.Assertion;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;
import static com.zarbosoft.pyxyzygy.app.Misc.unopt;

public class DoubleIndirectHalfBinder<X, Y, T> implements HalfBinder<T> {
	private Optional<T> last = Optional.empty();
	Optional base;

	private final class Value {
		Optional last = Optional.empty();
		Object sourceRoot; // GC root
		Object intermediateRoot; // GC root
	}

	private final Value value1 = new Value();
	private final Value value2 = new Value();
	private WeakList<Consumer<T>> listeners = new WeakList<>();
	private final BiFunction<X, Y, Optional> function;

	public DoubleIndirectHalfBinder(
			ReadOnlyProperty<X> source1, ReadOnlyProperty<Y> source2, BiFunction<X, Y, Optional> function
	) {
		this.function = function;
		setSource(value1, source1);
		setSource(value2, source2);
	}

	public DoubleIndirectHalfBinder(
			ReadOnlyProperty<X> source1, HalfBinder<Y> source2, BiFunction<X, Y, Optional> function
	) {
		this.function = function;
		setSource(value1, source1);
		setSource(value2, source2);
	}

	public DoubleIndirectHalfBinder(
			HalfBinder<X> source1, ReadOnlyProperty<Y> source2, BiFunction<X, Y, Optional> function
	) {
		this.function = function;
		setSource(value1, source1);
		setSource(value2, source2);
	}

	public DoubleIndirectHalfBinder(
			HalfBinder<X> source1, HalfBinder<Y> source2, BiFunction<X, Y, Optional> function
	) {
		this.function = function;
		setSource(value1, source1);
		setSource(value2, source2);
	}

	private void setSource(Value value, Object source) {
		if (source instanceof ReadOnlyProperty) {
			final ChangeListener listener = (observable, oldValue, newValue) -> {
				accept1(value, newValue);
			};
			((ReadOnlyProperty) source).addListener(new WeakChangeListener(listener));
			accept1(value, ((ReadOnlyProperty) source).getValue());
			value.sourceRoot = listener;
		} else if (source instanceof HalfBinder) {
			value.sourceRoot = ((HalfBinder) source).addListener(v -> {
				accept1(value, v);
			});
		} else
			throw new Assertion();
	}

	private void accept1(Value value, Object newValue) {
		value.intermediateRoot = null;
		value.last = opt(newValue);
		if (!value1.last.isPresent() || !value2.last.isPresent())
			return;
		base = function.apply((X) unopt(value1.last), (Y) unopt(value2.last));
		if (!base.isPresent())
			return;
		if (unopt(base) == null) {
			last = Optional.empty();
		} else if (unopt(base) instanceof ReadOnlyProperty) {
			final ChangeListener<T> listener = (observable, oldValue, newValue2) -> {
				accept2(newValue2);
			};
			((ReadOnlyProperty<T>) unopt(base)).addListener(new WeakChangeListener<>(listener));
			value.intermediateRoot = listener;
			listener.changed(null, null, ((ReadOnlyProperty<T>) unopt(base)).getValue());
		} else if (unopt(base) instanceof HalfBinder) {
			value.intermediateRoot = ((HalfBinder<T>) unopt(base)).addListener(newValue2 -> {
				accept2(newValue2);
			});
		} else
			throw new Assertion();
	}

	private void accept2(T newValue) {
		last = opt(newValue);
		for (Consumer<T> c : new ArrayList<>(listeners))
			c.accept(newValue);
	}

	@Override
	public BinderRoot addListener(Consumer<T> listener) {
		listeners.add(listener);
		if (last.isPresent())
			listener.accept(unopt(last));
		return new SimpleBinderRoot(this, listener);
	}

	@Override
	public void removeRoot(Object key) {
		listeners.remove(key);
	}

	@Override
	public Optional<T> get() {
		return last;
	}
}
