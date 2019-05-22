package com.zarbosoft.pyxyzygy.app.widgets.binding;

import com.zarbosoft.rendaw.common.Assertion;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.WeakChangeListener;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;
import static com.zarbosoft.pyxyzygy.app.Misc.unopt;

/**
 * Binder expressing (prop/binder -> value -> function -> prop/binder)
 * Acts as half/binder for last prop/binder so addListener receives prop/binder value rather than prop/binder itself
 * <p>
 * T is the inner type of the final prop/binder, so for SimpleBooleanProperty it would be Boolean
 *
 * The function may return a non-prop/binder value of type T.  In this case the IndirectHalfBinder acts like a standard
 * .map call.
 *
 * @param <T>
 */
public class IndirectHalfBinder<T> implements HalfBinder<T> {
	private WeakList<Consumer<T>> listeners = new WeakList<>();
	private final Function<Object, Optional> function;
	private Optional<T> last = Optional.empty();
	protected Object base;
	private Object rootSource; // GC root
	private Object rootIntermediate; // GC root

	public <U> IndirectHalfBinder(ReadOnlyProperty<U> source, Function<U, Optional> function) {
		this.function = (Function<Object, Optional>) function;
		final ChangeListener listener = (observable, oldValue, newValue) -> accept1(newValue);
		source.addListener(new WeakChangeListener<>(listener));
		accept1(source.getValue());
		rootSource = listener;
	}

	public <U> IndirectHalfBinder(HalfBinder<U> source, Function<U, Optional> function) {
		this.function = (Function<Object, Optional>) function;
		rootSource = ((HalfBinder) source).addListener(o -> accept1(o));
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

	private void accept2(T v) {
		last = opt(v);
		new ArrayList<>(listeners).forEach(c -> c.accept(v));
	}

	public void accept1(Object v1) {
		rootIntermediate = null;
		Optional<Object> v2 = function.apply(v1);
		if (!v2.isPresent())
			return;
		Object v = unopt(v2);
		if (v instanceof ReadOnlyProperty) {
			base = v;
			final ChangeListener listener = (observable, oldValue, newValue) -> {
				accept2((T) newValue);
			};
			((ReadOnlyProperty) v).addListener(new WeakChangeListener(listener));
			rootIntermediate = listener;
			accept2((T) ((ReadOnlyProperty) v).getValue());
		} else if (v instanceof HalfBinder) {
			base = v;
			rootIntermediate = ((HalfBinder<T>) v).addListener(optional -> {
				accept2(optional);
			});
		} else {
			last = (Optional<T>) v2;
			new ArrayList<>(listeners).forEach(c -> c.accept((T)v));
		}
	}
}
