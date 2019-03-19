package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.pyxyzygy.core.model.v0.ProjectObject;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Common;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.*;

import static com.zarbosoft.rendaw.common.Common.uncheck;

public class CustomBinding {
	public static <A, B> Runnable bindBidirectional(
			Property<A> propertyA,
			Property<B> propertyB,
			Function<A, Optional<B>> updateB,
			Function<B, Optional<A>> updateA
	) {
		Common.Mutable<Boolean> suppress = new Common.Mutable<>(false);
		updateB.apply(propertyA.getValue()).ifPresent(value -> propertyB.setValue(value));
		final ChangeListener<A> aListener = (observable, oldValue, newValue) -> {
			if (suppress.value)
				return;
			try {
				suppress.value = true;
				Optional<B> value = updateB.apply(newValue);
				if (!value.isPresent())
					return;
				propertyB.setValue(value.get());
			} finally {
				suppress.value = false;
			}
		};
		propertyA.addListener(aListener);
		final ChangeListener<B> bListener = (observable, oldValue, newValue) -> {
			if (suppress.value)
				return;
			try {
				suppress.value = true;
				Optional<A> value = updateA.apply(newValue);
				if (!value.isPresent())
					return;
				propertyA.setValue(value.get());
			} finally {
				suppress.value = false;
			}
		};
		propertyB.addListener(bListener);
		return () -> {
			propertyA.removeListener(aListener);
			propertyB.removeListener(bListener);
		};
	}

	public static <A, B> void bind(Property<A> propertyA, Property<B> propertyB, Function<A, Optional<B>> updateB) {
		updateB.apply(propertyA.getValue()).ifPresent(value -> propertyB.setValue(value));
		propertyA.addListener(new ChangeListener<A>() {
			private boolean alreadyCalled = false;

			@Override
			public void changed(ObservableValue<? extends A> observable, A oldValue, A newValue) {
				if (alreadyCalled)
					return;
				try {
					alreadyCalled = true;
					Optional<B> value = updateB.apply(newValue);
					if (!value.isPresent())
						return;
					propertyB.setValue(value.get());
				} finally {
					alreadyCalled = false;
				}
			}
		});
	}

	public static void bindMultiple(Runnable update, Property... dependencies) {
		ChangeListener listener = (observable, oldValue, newValue) -> {
			update.run();
		};
		listener.changed(null, null, null);
		for (Property dep : dependencies)
			dep.addListener(listener);
	}

	public interface HalfBinder<T> {
		/**
		 * @param listener Immediately called with latest value
		 * @return
		 */
		Runnable addListener(Consumer<T> listener);

		default <U> HalfBinder<U> map(Function<T, Optional<U>> function) {
			return new MapBinder(this, function);
		}
	}

	public static class MapBinder<T, U> implements HalfBinder<U>, Consumer<T> {
		private final Function<T, Optional<U>> forward;
		Optional<U> last = Optional.empty();
		List<Consumer<U>> listeners = new ArrayList<>();

		public MapBinder(HalfBinder parent, Function<T, Optional<U>> forward) {
			this.forward = forward;
			parent.addListener(this);
		}

		@Override
		public Runnable addListener(Consumer<U> listener) {
			listeners.add(listener);
			if (last.isPresent()) listener.accept(last.get());
			return () -> listeners.remove(listener);
		}

		@Override
		public void accept(T t) {
			Optional<U> v = forward.apply(t);
			if (!v.isPresent())
				return;
			last = v;
			listeners.forEach(l -> l.accept(v.get()));
		}
	}

	public interface Binder<T> extends HalfBinder<T> {
		void set(T v);

		default <U> Binder<U> bimap(Function<T, Optional<U>> forward, Function<U, T> back) {
			return new BimapBinder<T, U>(this, forward, back);
		}
	}

	public static class BimapBinder<T, U> extends MapBinder<T, U> implements Binder<U> {
		private final Function<U, T> back;
		private final Binder<T> parent;

		public BimapBinder(Binder<T> parent, Function<T, Optional<U>> forward, Function<U, T> back) {
			super(parent, forward);
			this.back = back;
			this.parent = parent;
		}

		@Override
		public void set(U v) {
			parent.set(back.apply(v));
		}
	}

	public static class ScalarHalfBinder<T> implements HalfBinder<T> {
		private final Consumer<Listener.ScalarSet> listen;
		private final Consumer<Listener.ScalarSet> unlisten;

		public ScalarHalfBinder(
				Consumer<Listener.ScalarSet> listen, Consumer<Listener.ScalarSet> unlisten
		) {
			this.listen = listen;
			this.unlisten = unlisten;
		}

		public ScalarHalfBinder(
				ProjectObject base, String name
		) {
			listen = bindConsumer(base, "add" + name + "SetListener");
			unlisten = bindConsumer(base, "remove" + name + "SetListener");
		}

		private static <X> Consumer<X> bindConsumer(Object target, String name) {
			Method method = uncheck(() -> target.getClass().getDeclaredMethod(name));
			return g -> uncheck(() -> method.invoke(target, g));
		}

		@Override
		public Runnable addListener(Consumer<T> listener) {
			final Listener.ScalarSet<Object, T> inner = (t, v) -> {
				listener.accept(v);
			};
			this.listen.accept(inner);
			return () -> {
				this.unlisten.accept(inner);
			};
		}
	}

	public static class ScalarBinder<T> extends ScalarHalfBinder<T> implements Binder<T> {
		private final Consumer<T> set;

		public ScalarBinder(
				Consumer<Listener.ScalarSet> listen, Consumer<Listener.ScalarSet> unlisten, Consumer<T> set
		) {
			super(listen, unlisten);
			this.set = set;
		}

		@Override
		public void set(T v) {
			set.accept(v);
		}
	}

	public static class ListHalfBinder<T> implements HalfBinder<T> {
		private final Supplier<List> get;
		private final Consumer<Listener.ListAdd> listenAdd;
		private final Consumer<Listener.ListAdd> unlistenAdd;
		private final Consumer<Listener.ListRemove> listenRemove;
		private final Consumer<Listener.ListRemove> unlistenRemove;
		private final Consumer<Listener.ListMoveTo> listenMoveTo;
		private final Consumer<Listener.ListMoveTo> unlistenMoveTo;

		public ListHalfBinder(
				Supplier<List> get,
				Consumer<Listener.ListAdd> listenAdd,
				Consumer<Listener.ListAdd> unlistenAdd,
				Consumer<Listener.ListRemove> listenRemove,
				Consumer<Listener.ListRemove> unlistenRemove,
				Consumer<Listener.ListMoveTo> listenMoveTo,
				Consumer<Listener.ListMoveTo> unlistenMoveTo
		) {
			this.get = get;
			this.listenAdd = listenAdd;
			this.unlistenAdd = unlistenAdd;
			this.listenRemove = listenRemove;
			this.unlistenRemove = unlistenRemove;
			this.listenMoveTo = listenMoveTo;
			this.unlistenMoveTo = unlistenMoveTo;
		}

		/**
		 * @param target
		 * @param prop   First letter caps
		 */
		public ListHalfBinder(Object target, String prop) {
			Method getMethod = uncheck(() -> target.getClass().getDeclaredMethod(prop.toLowerCase()));
			get = () -> uncheck(() -> (List) getMethod.invoke(target));
			listenAdd = bindConsumer(target, "add" + prop + "AddListener");
			unlistenAdd = bindConsumer(target, "remove" + prop + "AddListener");
			listenRemove = bindConsumer(target, "add" + prop + "RemoveListener");
			unlistenRemove = bindConsumer(target, "remove" + prop + "RemoveListener");
			listenMoveTo = bindConsumer(target, "add" + prop + "MoveToListener");
			unlistenMoveTo = bindConsumer(target, "remove" + prop + "MoveToListener");
		}

		private static <X> Consumer<X> bindConsumer(Object target, String name) {
			Method method = uncheck(() -> target.getClass().getDeclaredMethod(name));
			return g -> uncheck(() -> method.invoke(target, g));
		}

		@Override
		public Runnable addListener(Consumer<T> listener) {
			final Listener.ListAdd listAdd = (target, at, value) -> {
				listener.accept((T) get.get());
			};
			listenAdd.accept(listAdd);
			final Listener.ListRemove listRemove = (target, at, count) -> {
				listener.accept((T) get.get());
			};
			listenRemove.accept(listRemove);
			final Listener.ListMoveTo listMoveTo = (target, source, count, dest) -> {
				listener.accept((T) get.get());
			};
			listenMoveTo.accept(listMoveTo);
			return () -> {
				unlistenAdd.accept(listAdd);
				unlistenRemove.accept(listRemove);
				unlistenMoveTo.accept(listMoveTo);
			};
		}
	}

	public static class PropertyHalfBinder<T> implements HalfBinder<T> {
		final Property<T> property;

		public PropertyHalfBinder(Property<T> property) {
			this.property = property;
		}

		@Override
		public Runnable addListener(Consumer<T> listener) {
			final ChangeListener<T> inner = (observable, oldValue, newValue) -> {
				listener.accept(newValue);
			};
			property.addListener(inner);
			listener.accept(property.getValue());
			return () -> property.removeListener(inner);
		}
	}

	public static class PropertyBinder<T> extends PropertyHalfBinder<T> implements Binder<T> {
		public PropertyBinder(Property<T> property) {
			super(property);
		}

		@Override
		public void set(T v) {
			property.setValue(v);
		}
	}

	public static <T> Runnable bindBidirectionalMultiple(Binder<T>... properties) {
		return new Runnable() {
			List<Runnable> cleanup = new ArrayList<>();
			boolean suppress;
			Optional<T> last = Optional.empty();

			{
				for (Binder<T> binder : properties) {
					Optional<T> oldLast = last;
					cleanup.add(binder.addListener(v -> {
						if (suppress)
							return;
						suppress = true;
						try {
							last = Optional.of(v);
							for (Binder<T> otherBinder : properties) {
								if (otherBinder == binder)
									continue;
								otherBinder.set(v);
							}
						} finally {
							suppress = false;
						}
					}));
					if (last != oldLast)
						binder.set(last.get());
				}
			}

			@Override
			public void run() {
				cleanup.forEach(Runnable::run);
			}
		};
	}

	public static Binding absInt(ObservableValue<Number> a) {
		return Bindings.createIntegerBinding(() -> Math.abs(a.getValue().intValue()), a);
	}

	public static Binding bindAbs(ObservableValue<Number> a) {
		return Bindings.createDoubleBinding(() -> Math.abs(a.getValue().doubleValue()), a);
	}

	/**
	 * Binder expressing (prop/binder -> value -> function -> prop/binder)
	 * <p>
	 * T is the inner type of the prop/binder
	 *
	 * @param <T>
	 */
	public static class IndirectHalfBinder<T> implements HalfBinder<T> {
		Runnable cleanup;
		private List<Consumer<T>> listeners = new ArrayList<>();
		private final Function<Object, Optional> function;
		Optional<T> last = Optional.empty();
		protected Object base;

		public <U> IndirectHalfBinder(Property source, Function<U, Optional> function) {
			this.function = (Function<Object, Optional>) function;
			source.addListener((observable, oldValue, newValue) -> accept1(newValue));
			accept1(source.getValue());
		}

		public <U> IndirectHalfBinder(HalfBinder<U> source, Function<U, Optional> function) {
			this.function = (Function<Object, Optional>) function;
			source.addListener(o -> accept1(o));
		}

		@Override
		public Runnable addListener(Consumer<T> listener) {
			listeners.add(listener);
			if (last.isPresent())
				listener.accept(last.get());
			return () -> listeners.remove(listener);
		}

		private void accept2(T v) {
			last = Optional.of(v);
			new ArrayList<>(listeners).forEach(c -> c.accept(v));
		}

		public void accept1(Object v1) {
			if (cleanup != null) {
				cleanup.run();
				cleanup = null;
			}
			Optional<Object> v2 = function.apply(v1);
			if (!v2.isPresent())
				return;
			Object v = v2.get();
			base = v;
			if (v instanceof Property) {
				final ChangeListener listener = (observable, oldValue, newValue) -> {
					accept2((T) newValue);
				};
				((Property) v).addListener(listener);
				cleanup = () -> ((Property) v).removeListener(listener);
				accept2((T) ((Property) v).getValue());
			} else if (v instanceof HalfBinder) {
				cleanup = ((HalfBinder<T>) v).addListener(optional -> {
					accept2(optional);
				});
			} else
				throw new Assertion();
		}
	}

	public static class IndirectBinder<T> extends IndirectHalfBinder<T> implements Binder<T> {
		final BiConsumer<Object, T> set;

		public <U> IndirectBinder(
				Property source, Function<U, Optional> function, BiConsumer<Object, T> set
		) {
			super(source, function);
			this.set = set;
		}

		public <U> IndirectBinder(
				HalfBinder source, Function<U, Optional> function, BiConsumer<Object, T> set
		) {
			super(source, function);
			this.set = set;
		}

		@Override
		public void set(T v) {
			set.accept(base, v);
		}
	}

	public static class DoubleIndirectHalfBinder<X, Y, T> implements HalfBinder<T> {
		private final class Value {
			Optional base = Optional.empty();
			Runnable cleanup;
			Optional last;
		}

		private final Value value1 = new Value();
		private final Value value2 = new Value();
		private List<Consumer<T>> listeners = new ArrayList<>();
		private final BiFunction<X, Y, Optional> function;

		public DoubleIndirectHalfBinder(Object source1, Object source2, BiFunction<X, Y, Optional> function) {
			this.function = function;
			setSource(value1, source1);
			setSource(value2, source2);
		}

		private void setSource(Value value, Object source) {
			if (source instanceof Property) {
				final ChangeListener listener = (observable, oldValue, newValue) -> {
					value.base = Optional.of(newValue);
					accept(value);
				};
				((Property) source).addListener(listener);
				listener.changed(null, null, ((Property) source).getValue());
			} else if (source instanceof HalfBinder) {
				((HalfBinder) source).addListener(optional -> {
					value.base = (Optional) optional;
					accept(value);
				});
			} else
				throw new Assertion();
		}

		private void accept(Value value) {
			if (!value.base.isPresent())
				return;
			if (value.cleanup != null) {
				value.cleanup.run();
				value.cleanup = null;
			}
			Object t = value.base.get();
			if (t instanceof Property) {
				final ChangeListener listener = (observable, oldValue, newValue) -> {
					value.last = Optional.of(newValue);
					valueChanged();
				};
				((Property) t).addListener(listener);
				value.cleanup = () -> ((Property) t).removeListener(listener);
				listener.changed(null, null, ((Property) t).getValue());
			} else if (t instanceof HalfBinder) {
				value.cleanup = ((HalfBinder<Object>) t).addListener(optional -> {
					value.last = Optional.of(optional);
					valueChanged();
				});
			}
		}

		private void valueChanged() {
			if (!value1.last.isPresent() || !value2.last.isPresent())
				return;
			Optional<T> v = function.apply((X) value1.last.get(), (Y) value2.last.get());
			if (!v.isPresent())
				return;
			listeners.forEach(l -> l.accept(v.get()));
		}

		@Override
		public Runnable addListener(Consumer<T> listener) {
			listeners.add(listener);
			Optional<T> v = function.apply((X) value1.last.get(), (Y) value2.last.get());
			if (v.isPresent())
				listener.accept(v.get());
			return () -> listeners.remove(listener);
		}
	}
}
