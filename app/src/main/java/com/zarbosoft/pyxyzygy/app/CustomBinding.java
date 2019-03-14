package com.zarbosoft.pyxyzygy.app;

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
		Runnable addListener(Consumer<Optional<T>> listener);
	}

	public interface Binder<T> extends HalfBinder<T> {
		void set(T v);

		default <U> Binder<U> convert(Function<T, U> there, Function<U, T> back) {
			Binder<T> this1 = this;
			return new Binder<U>() {
				@Override
				public void set(U v) {
					this1.set(back.apply(v));
				}

				@Override
				public Runnable addListener(Consumer<Optional<U>> listener) {
					return this1.addListener(v -> listener.accept(v.map(there)));
				}
			};
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

		@Override
		public Runnable addListener(Consumer<Optional<T>> listener) {
			final Listener.ScalarSet<Object, T> inner = (t, v) -> {
				listener.accept(Optional.of(v));
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
			listenAdd = bindConsumer(target, "add" + prop + "AddListener", Listener.ListAdd.class);
			unlistenAdd = bindConsumer(target, "remove" + prop + "AddListener", Listener.ListAdd.class);
			listenRemove = bindConsumer(target, "add" + prop + "RemoveListener", Listener.ListRemove.class);
			unlistenRemove = bindConsumer(target, "remove" + prop + "RemoveListener", Listener.ListRemove.class);
			listenMoveTo = bindConsumer(target, "add" + prop + "MoveToListener", Listener.ListMoveTo.class);
			unlistenMoveTo = bindConsumer(target, "remove" + prop + "MoveToListener", Listener.ListMoveTo.class);
		}

		private static <X> Consumer<X> bindConsumer(Object target, String name, Class<X> arg) {
			Method method = uncheck(() -> target.getClass().getDeclaredMethod(name));
			return g -> uncheck(() -> method.invoke(target, g));
		}

		@Override
		public Runnable addListener(Consumer<Optional<T>> listener) {
			final Listener.ListAdd listAdd = (target, at, value) -> {
				listener.accept(Optional.of((T) get.get()));
			};
			listenAdd.accept(listAdd);
			final Listener.ListRemove listRemove = (target, at, count) -> {
				listener.accept(Optional.of((T) get.get()));
			};
			listenRemove.accept(listRemove);
			final Listener.ListMoveTo listMoveTo = (target, source, count, dest) -> {
				listener.accept(Optional.of((T) get.get()));
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
		public Runnable addListener(Consumer<Optional<T>> listener) {
			final ChangeListener<T> inner = (observable, oldValue, newValue) -> {
				listener.accept(Optional.ofNullable(newValue));
			};
			property.addListener(inner);
			return () -> {
				property.removeListener(inner);
			};
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

	/*
	public static class IndirectHalfBinder<T> implements HalfBinder<T>, Indirect<T> {
		private final Function function;
		private List<Consumer<Optional<T>>> listeners = new ArrayList<>();

		public <X> IndirectHalfBinder(IndirectBase<X> indirect, Function<X, Optional<T>> function) {
			indirect.next.add(this);
			this.function = function;
		}

		@Override
		public Runnable addListener(Consumer<Optional<T>> listener) {
			listeners.add(listener);
			return () -> listeners.remove(listener);
		}

		@Override
		public void parentChanged(Object source, Object value) {
			Optional<T> value2 = (Optional<T>) function.apply(value);
			listeners.forEach(l -> l.accept(value2));
		}
	}

	public static class IndirectBinder<T> extends IndirectHalfBinder<T> implements Binder<T> {
		private final Consumer<T> set;

		public <X> IndirectBinder(IndirectBase indirect, Function<X, Optional<T>> function, Consumer<T> set) {
			super(indirect, function);
			this.set = set;
		}

		@Override
		public void set(T v) {
			this.set.accept(v);
		}
	}

	public static class DoubleIndirectHalfBinder<X, Y, T> extends IndirectBase<T> implements HalfBinder<T> {
		private final BiFunction<X, Y, Optional<T>> function;
		private final Object indirect1;
		private final Object indirect2;
		private List<Consumer<Optional<T>>> listeners = new ArrayList<>();
		X last1;
		Y last2;
		private Optional<T> lastT;

		public DoubleIndirectHalfBinder(Object indirect1, Object indirect2, BiFunction<X, Y, Optional<T>> function) {
			this.indirect1 = indirect1;
			this.indirect2 = indirect2;
			this.function = function;
			bind(indirect1);
			bind(indirect2);
		}

		private void bind(Object indirect) {
			if (indirect instanceof IndirectBase) {
				((IndirectBase) indirect).next.add(this);
			} else if (indirect instanceof HalfBinder) {
				((HalfBinder<Y>) indirect).addListener(v -> {
					if (!v.isPresent())
						return;
					this.parentChanged(indirect, v.get());
				});
			} else
				throw new Assertion();
		}

		@Override
		public Runnable addListener(Consumer<Optional<T>> listener) {
			listeners.add(listener);
			return () -> listeners.remove(listener);
		}

		@Override
		public void parentChanged(Object source, Object value) {
			if (source == indirect1)
				last1 = (X) value;
			else if (source == indirect2)
				last2 = (Y) value;
			else
				throw new Assertion();
			thisChanged();
		}

		private void thisChanged() {
			lastT = (Optional<T>) function.apply(last1, last2);
			listeners.forEach(l -> l.accept(lastT));
			lastT.ifPresent(t -> next.forEach(n -> n.parentChanged(this, t)));
		}
	}

	public static class DoubleIndirectBinder<X, Y, T> extends DoubleIndirectHalfBinder<X, Y, T> implements Binder<T> {
		private final Consumer<T> set;

		public DoubleIndirectBinder(
				IndirectBase indirect1,
				IndirectBase indirect2,
				BiFunction<X, Y, Optional<T>> function,
				Consumer<T> set
		) {
			super(indirect1, indirect2, function);
			this.set = set;
		}

		@Override
		public void set(T v) {
			this.set.accept(v);
		}
	}
	*/

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
							if (!v.isPresent())
								return;
							last = v;
							for (Binder<T> otherBinder : properties) {
								if (otherBinder == binder)
									continue;
								otherBinder.set(last.get());
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
	 * Binds something that returns a "wrapper" (property, binder) and unifies changes to that.
	 *
	 * @param <T>
	 */
	public static class IndirectHalfBinder<T> implements HalfBinder<T>, Consumer<Optional> {
		Runnable cleanup;
		private List<Consumer<Optional<T>>> listeners = new ArrayList<>();
		private final Function<Object, Optional<T>> function;

		public <U> IndirectHalfBinder(Property source, Function<U, Optional<T>> function) {
			this.function = (Function<Object, Optional<T>>) function;
			source.addListener((observable, oldValue, newValue) -> accept(Optional.of(newValue)));
		}

		public <U> IndirectHalfBinder(HalfBinder source, Function<U, Optional<T>> function) {
			this.function = (Function<Object, Optional<T>>) function;
			source.addListener(this);
		}

		@Override
		public Runnable addListener(Consumer<Optional<T>> listener) {
			listeners.add(listener);
			return () -> listeners.remove(listener);
		}

		private void accept2(Object v) {
			Optional<T> v2 = function.apply(v);
			if (!v2.isPresent())
				return;
			new ArrayList<>(listeners).forEach(c -> c.accept(v2));
		}

		@Override
		public void accept(Optional u) {
			u.ifPresent(v -> {
				if (cleanup != null) {
					cleanup.run();
					cleanup = null;
				}
				if (v instanceof Property) {
					final ChangeListener listener = (observable, oldValue, newValue) -> {
						accept2(newValue);
					};
					((Property) v).addListener(listener);
					cleanup = () -> ((Property) v).removeListener(listener);
					accept2(((Property) v).getValue());
				} else if (v instanceof HalfBinder) {
					cleanup = ((HalfBinder) v).addListener(badType -> {
						Optional optional = (Optional) badType;
						if (!optional.isPresent())
							return;
						accept2(optional.get());
					});
				}
			});
		}
	}

	public static class IndirectBinder<T> extends IndirectHalfBinder<T> implements Binder<T> {
		final BiConsumer<Object, T> set;
		private Object base;

		public <U> IndirectBinder(
				Property source, Function<U, Optional<T>> function, BiConsumer<Object,T> set
		) {
			super(source, function);
			this.set = set;
		}

		public <U> IndirectBinder(
				HalfBinder source, Function<U, Optional<T>> function, BiConsumer<Object,T> set
		) {
			super(source, function);
			this.set = set;
		}

		@Override
		public void accept(Optional u) {
			if (u.isPresent()) {
				base = u.get();
			}
			super.accept(u);
		}

		@Override
		public void set(T v) {
			set.accept(base,v);
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
		private List<Consumer<Optional<T>>> listeners = new ArrayList<>();
		private final BiFunction<X, Y, Optional<T>> function;

		public DoubleIndirectHalfBinder(Object source1, Object source2, BiFunction<X, Y, Optional<T>> function) {
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
				listener.changed(null,null,((Property) source).getValue());
			} else if (source instanceof HalfBinder) {
				((HalfBinder) source).addListener(optional -> {
					value.base = (Optional) optional;
					accept(value);
				});
			} else throw new Assertion();
		}

		private void accept(Value value) {
			if (!value.base.isPresent()) return;
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
				value.cleanup = ()->((Property) t).removeListener(listener);
				listener.changed(null,null,((Property) t).getValue());
			} else if (t instanceof HalfBinder) {
				value.cleanup = ((HalfBinder<Object>) t).addListener(optional -> {
					if (!optional.isPresent());
					value.last = (Optional) optional;
					valueChanged();
				});
			}
		}

		private void valueChanged() {
			if (!value1.last.isPresent() || !value2.last.isPresent()) return;
			Optional<T> v = function.apply((X)value1.last.get(),(Y)value2.last.get());
			if (!v.isPresent()) return;
			listeners.forEach(l -> l.accept(v));
		}

		@Override
		public Runnable addListener(Consumer<Optional<T>> listener) {
			listeners.add(listener);
			return () -> listeners.remove(listener);
		}
	}

	public static class IndirectBuilder<T, RootT> {
		private final List<Function> chain;

		public IndirectBuilder() {
			this.chain = new ArrayList<>();
		}

		public IndirectBuilder(List<Function> chain) {
			this.chain = chain;
		}

		public <U> IndirectBuilder<U, RootT> t(Function<T, U> function) {
			chain.add(function);
			return new IndirectBuilder<U, RootT>(chain);
		}

		public <X> HalfBinder<X> done(Property<RootT> source) {
			HalfBinder last = null;
			for (Function f : chain) {
				if (last == null)
					last = new IndirectHalfBinder(source, f);
				else
					last = new IndirectHalfBinder(last, f);
			}
			return last;
		}
	}
}
