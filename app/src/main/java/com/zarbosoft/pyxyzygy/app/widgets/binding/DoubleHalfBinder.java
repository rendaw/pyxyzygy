package com.zarbosoft.pyxyzygy.app.widgets.binding;

import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;
import static com.zarbosoft.pyxyzygy.app.Misc.unopt;

/**
 * Create a value binding whose value changes when both sub-value bindings are present and change.
 * @param <X>
 * @param <Y>
 */
public class DoubleHalfBinder<X, Y> implements HalfBinder<Pair<X, Y>> {
	private Optional<Pair<X, Y>> last = Optional.empty();

	private final class Value {
		Optional last = Optional.empty();
		Object sourceRoot; // GC root
	}

	private final Value value1 = new Value();
	private final Value value2 = new Value();
	private WeakList<Consumer<Pair<X, Y>>> listeners = new WeakList<>();

	public DoubleHalfBinder(
			ReadOnlyProperty<X> source1, ReadOnlyProperty<Y> source2
	) {
		setSource(value1, source1);
		setSource(value2, source2);
	}

	public DoubleHalfBinder(
			ReadOnlyProperty<X> source1, HalfBinder<Y> source2
	) {
		setSource(value1, source1);
		setSource(value2, source2);
	}

	public DoubleHalfBinder(
			HalfBinder<X> source1, ReadOnlyProperty<Y> source2
	) {
		setSource(value1, source1);
		setSource(value2, source2);
	}

	public DoubleHalfBinder(
			HalfBinder<X> source1, HalfBinder<Y> source2
	) {
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
		value.last = opt(newValue);
		if (!value1.last.isPresent() || !value2.last.isPresent())
			return;
		last = Optional.of(new Pair<X, Y>((X) unopt(value1.last), (Y) unopt(value2.last)));
		for (Consumer<Pair<X, Y>> c : new ArrayList<>(listeners))
			c.accept(unopt(last));
	}

	@Override
	public BinderRoot addListener(Consumer<Pair<X, Y>> listener) {
		listeners.add(listener);
		if (last.isPresent())
			listener.accept(unopt(last));
		return new SimpleBinderRoot(this, listener);
	}

	@Override
	public void removeRoot(Object key) {
		listeners.remove(key);
	}

	public BinderRoot addListener(BiConsumer<X, Y> listener) {
		return addListener(p -> listener.accept(p.first, p.second));
	}

	@Override
	public Optional<Pair<X, Y>> get() {
		return last;
	}

	public <U> HalfBinder<U> map(BiFunction<X, Y, Optional<U>> function) {
		return map(p -> function.apply(p.first, p.second));
	}
}
