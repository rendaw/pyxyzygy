package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.pyxyzygy.core.model.v0.ProjectObject;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;
import static com.zarbosoft.pyxyzygy.app.Misc.unopt;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public class CustomBinding {
	public static <T> Runnable bind(Property<T> dest, HalfBinder<T> source) {
		final Consumer<T> listener = v -> {
			dest.setValue(v);
		};
		return source.addListener(listener);
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

		Optional<T> get();

		void destroy();
	}

	public static class MapBinder<T, U> implements HalfBinder<U>, Consumer<T> {
		private final Function<T, Optional<U>> forward;
		private final Runnable cleanup;
		Optional<U> last = Optional.empty();
		List<Consumer<U>> listeners = new ArrayList<>();

		public MapBinder(HalfBinder parent, Function<T, Optional<U>> forward) {
			this.forward = forward;
			cleanup = parent.addListener(this);
		}

		@Override
		public Runnable addListener(Consumer<U> listener) {
			listeners.add(listener);
			if (last.isPresent())
				listener.accept(unopt(last));
			return () -> listeners.remove(listener);
		}

		@Override
		public Optional<U> get() {
			return last;
		}

		@Override
		public void accept(T t) {
			Optional<U> v = forward.apply(t);
			if (!v.isPresent())
				return;
			last = v;
			listeners.forEach(l -> l.accept(unopt(v)));
		}

		@Override
		public void destroy() {
			cleanup.run();
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
		T last;
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
			if (Character.isUpperCase(name.charAt(0)))
				throw new Assertion();
			String camel = name.substring(0, 1).toUpperCase() + name.substring(1);
			listen = bindConsumer(base, "add" + camel + "SetListeners", Listener.ScalarSet.class);
			unlisten = bindConsumer(base, "remove" + camel + "SetListeners", Listener.ScalarSet.class);
		}

		private static <X> Consumer<X> bindConsumer(Object target, String name, Class listener) {
			Method method = uncheck(() -> target.getClass().getMethod(name, listener));
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

		@Override
		public Optional<T> get() {
			return opt(last);
		}

		@Override
		public void destroy() {

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

		public ScalarBinder(
				ProjectObject base, String name, Consumer<T> set
		) {
			super(base, name);
			this.set = set;
		}

		@Override
		public void set(T v) {
			set.accept(v);
		}
	}

	public static class ListHalfBinder<T> implements HalfBinder<List<T>> {
		private final Supplier<List<T>> get;
		private final Consumer<Listener.ListAdd> listenAdd;
		private final Consumer<Listener.ListAdd> unlistenAdd;
		private final Consumer<Listener.ListRemove> listenRemove;
		private final Consumer<Listener.ListRemove> unlistenRemove;
		private final Consumer<Listener.ListMoveTo> listenMoveTo;
		private final Consumer<Listener.ListMoveTo> unlistenMoveTo;

		public ListHalfBinder(
				Supplier<List<T>> get,
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
			if (Character.isUpperCase(prop.charAt(0)))
				throw new Assertion();
			Method getMethod = uncheck(() -> target.getClass().getMethod(prop.toLowerCase()));
			String camel = prop.substring(0, 1).toUpperCase() + prop.substring(1);
			get = () -> uncheck(() -> (List) getMethod.invoke(target));
			listenAdd = bindConsumer(target, "add" + camel + "AddListeners", Listener.ListAdd.class);
			unlistenAdd = bindConsumer(target, "remove" + camel + "AddListeners", Listener.ListAdd.class);
			listenRemove = bindConsumer(target, "add" + camel + "RemoveListeners", Listener.ListRemove.class);
			unlistenRemove = bindConsumer(target, "remove" + camel + "RemoveListeners", Listener.ListRemove.class);
			listenMoveTo = bindConsumer(target, "add" + camel + "MoveToListeners", Listener.ListMoveTo.class);
			unlistenMoveTo = bindConsumer(target, "remove" + camel + "MoveToListeners", Listener.ListMoveTo.class);
		}

		private static <X> Consumer<X> bindConsumer(
				Object target, String name, Class listener
		) {
			Method method = uncheck(() -> target.getClass().getMethod(name, listener));
			return g -> uncheck(() -> method.invoke(target, g));
		}

		@Override
		public Runnable addListener(Consumer<List<T>> listener) {
			final Listener.ListAdd listAdd = (target, at, value) -> {
				listener.accept(get.get());
			};
			listenAdd.accept(listAdd);
			final Listener.ListRemove listRemove = (target, at, count) -> {
				listener.accept(get.get());
			};
			listenRemove.accept(listRemove);
			final Listener.ListMoveTo listMoveTo = (target, source, count, dest) -> {
				listener.accept(get.get());
			};
			listenMoveTo.accept(listMoveTo);
			return () -> {
				unlistenAdd.accept(listAdd);
				unlistenRemove.accept(listRemove);
				unlistenMoveTo.accept(listMoveTo);
			};
		}

		@Override
		public Optional<List<T>> get() {
			return opt(get.get());
		}

		@Override
		public void destroy() {

		}
	}

	public static class PropertyHalfBinder<T> implements HalfBinder<T> {
		final ReadOnlyProperty<T> property;

		public PropertyHalfBinder(ReadOnlyProperty<T> property) {
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

		@Override
		public Optional<T> get() {
			return opt(property.getValue());
		}

		@Override
		public void destroy() {

		}
	}

	public static class PropertyBinder<T> extends PropertyHalfBinder<T> implements Binder<T> {
		public PropertyBinder(Property<T> property) {
			super(property);
		}

		@Override
		public void set(T v) {
			((Property<T>) property).setValue(v);
		}
	}

	public static class ListPropertyHalfBinder<T extends ObservableList> implements HalfBinder<T> {
		final T property;

		public ListPropertyHalfBinder(T property) {
			this.property = property;
		}

		@Override
		public Runnable addListener(Consumer<T> listener) {
			final InvalidationListener inner = c -> {
				listener.accept(property);
			};
			property.addListener(inner);
			listener.accept(property);
			return () -> property.removeListener(inner);
		}

		@Override
		public Optional<T> get() {
			return opt(property);
		}

		@Override
		public void destroy() {

		}
	}

	public static <T> Runnable bindBidirectional(Binder<T>... properties) {
		return new Runnable() {
			List<Runnable> cleanup = new ArrayList<>();
			boolean suppress;
			Optional<T> last = Optional.empty();

			{
				for (Binder<T> binder : properties) {
					if (last.isPresent())
						suppress = true;
					cleanup.add(binder.addListener(v -> {
						if (suppress)
							return;
						suppress = true;
						try {
							last = opt(v);
							for (Binder<T> otherBinder : properties) {
								if (otherBinder == binder)
									continue;
								otherBinder.set(v);
							}
						} finally {
							suppress = false;
						}
					}));
					suppress = false;
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
	 * Acts as half/binder for last prop/binder so addListener receives prop/binder value rather than prop/binder itself
	 * <p>
	 * T is the inner type of the final prop/binder, so for SimpleBooleanProperty it would be Boolean
	 *
	 * @param <T>
	 */
	public static class IndirectHalfBinder<T> implements HalfBinder<T> {
		private Runnable cleanup;
		private List<Consumer<T>> listeners = new ArrayList<>();
		private final Function<Object, Optional> function;
		private Optional<T> last = Optional.empty();
		protected Object base;
		private Runnable cleanupSource;

		public <U> IndirectHalfBinder(ReadOnlyProperty<U> source, Function<U, Optional> function) {
			this.function = (Function<Object, Optional>) function;
			final ChangeListener listener = (observable, oldValue, newValue) -> accept1(newValue);
			source.addListener(listener);
			accept1(source.getValue());
			cleanupSource = () -> source.removeListener(listener);
		}

		public <U> IndirectHalfBinder(HalfBinder<U> source, Function<U, Optional> function) {
			this.function = (Function<Object, Optional>) function;
			cleanupSource = ((HalfBinder) source).addListener(o -> accept1(o));
		}

		@Override
		public void destroy() {
			if (cleanupSource != null) {
				cleanupSource.run();
				cleanupSource = null;
				if (cleanup != null) {
					cleanup.run();
					cleanup = null;
				}
			}
		}

		@Override
		public Runnable addListener(Consumer<T> listener) {
			listeners.add(listener);
			if (last.isPresent())
				listener.accept(unopt(last));
			return () -> listeners.remove(listener);
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
			if (cleanup != null) {
				cleanup.run();
				cleanup = null;
			}
			Optional<Object> v2 = function.apply(v1);
			if (!v2.isPresent())
				return;
			Object v = unopt(v2);
			base = v;
			if (v == null) {
				last = Optional.empty();
			} else if (v instanceof ReadOnlyProperty) {
				final ChangeListener listener = (observable, oldValue, newValue) -> {
					accept2((T) newValue);
				};
				((ReadOnlyProperty) v).addListener(listener);
				cleanup = () -> ((ReadOnlyProperty) v).removeListener(listener);
				accept2((T) ((ReadOnlyProperty) v).getValue());
			} else if (v instanceof HalfBinder) {
				cleanup = ((HalfBinder<T>) v).addListener(optional -> {
					accept2(optional);
				});
			} else
				throw new Assertion();
		}
	}

	public static class IndirectBinder<T> extends IndirectHalfBinder<T> implements Binder<T> {
		public <U> IndirectBinder(Property<U> source, Function<U, Optional> function) {
			super(source, function);
		}

		public <U> IndirectBinder(HalfBinder<U> source, Function<U, Optional> function) {
			super(source, function);
		}

		@Override
		public void set(T v) {
			if (base == null)
				return;
			if (base instanceof Property) {
				((Property) base).setValue(v);
			} else if (base instanceof Binder) {
				((Binder) base).set(v);
			} else
				throw new Assertion();
		}
	}

	public static class DoubleHalfBinder<X, Y, T> implements HalfBinder<T> {
		private Optional<T> last = Optional.empty();
		Runnable sourceCleanup;
		Runnable baseCleanup;

		private final class Value {
			Optional last = Optional.empty();
		}

		private final Value value1 = new Value();
		private final Value value2 = new Value();
		private List<Consumer<T>> listeners = new ArrayList<>();
		private final BiFunction<X, Y, Optional> function;

		public DoubleHalfBinder(
				ReadOnlyProperty<X> source1, ReadOnlyProperty<Y> source2, BiFunction<X, Y, Optional> function
		) {
			this.function = function;
			setSource(value1, source1);
			setSource(value2, source2);
		}

		public DoubleHalfBinder(
				ReadOnlyProperty<X> source1, HalfBinder<Y> source2, BiFunction<X, Y, Optional> function
		) {
			this.function = function;
			setSource(value1, source1);
			setSource(value2, source2);
		}

		public DoubleHalfBinder(
				HalfBinder<X> source1, ReadOnlyProperty<Y> source2, BiFunction<X, Y, Optional> function
		) {
			this.function = function;
			setSource(value1, source1);
			setSource(value2, source2);
		}

		public DoubleHalfBinder(
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
				((ReadOnlyProperty) source).addListener(listener);
				accept1(value, ((ReadOnlyProperty) source).getValue());
				sourceCleanup = () -> ((ReadOnlyProperty) source).removeListener(listener);
			} else if (source instanceof HalfBinder) {
				sourceCleanup = ((HalfBinder) source).addListener(v -> {
					accept1(value, v);
				});
			} else
				throw new Assertion();
		}

		@Override
		public void destroy() {
			if (sourceCleanup != null)
				sourceCleanup.run();
			if (baseCleanup != null)
				baseCleanup.run();
		}

		private void accept1(Value value, Object newValue) {
			if (baseCleanup != null) {
				baseCleanup.run();
				baseCleanup = null;
			}
			value.last = opt(newValue);
			if (!value1.last.isPresent() || !value2.last.isPresent())
				return;
			last = function.apply((X) unopt(value1.last), (Y) unopt(value2.last));
			if (!last.isPresent())
				return;
			for (Consumer<T> c : new ArrayList<>(listeners))
				c.accept(unopt(last));
		}

		@Override
		public Runnable addListener(Consumer<T> listener) {
			listeners.add(listener);
			if (last.isPresent())
				listener.accept(unopt(last));
			return () -> listeners.remove(listener);
		}

		@Override
		public Optional<T> get() {
			return last;
		}
	}

	public static class ConstHalfBinder<T> implements HalfBinder<T> {
		final T v;

		public ConstHalfBinder(T v) {
			this.v = v;
		}

		@Override
		public Runnable addListener(Consumer<T> listener) {
			listener.accept(v);
			return () -> {
			};
		}

		@Override
		public Optional<T> get() {
			return opt(v);
		}

		@Override
		public void destroy() {

		}
	}

	public static class DoubleIndirectHalfBinder<X, Y, T> implements HalfBinder<T> {
		private Optional<T> last = Optional.empty();
		Runnable sourceCleanup;
		Optional base;
		Runnable baseCleanup;

		private final class Value {
			Optional last = Optional.empty();
		}

		private final Value value1 = new Value();
		private final Value value2 = new Value();
		private List<Consumer<T>> listeners = new ArrayList<>();
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
				((ReadOnlyProperty) source).addListener(listener);
				accept1(value, ((ReadOnlyProperty) source).getValue());
				sourceCleanup = () -> ((ReadOnlyProperty) source).removeListener(listener);
			} else if (source instanceof HalfBinder) {
				sourceCleanup = ((HalfBinder) source).addListener(v -> {
					accept1(value, v);
				});
			} else
				throw new Assertion();
		}

		@Override
		public void destroy() {
			if (sourceCleanup != null)
				sourceCleanup.run();
			if (baseCleanup != null)
				baseCleanup.run();
		}

		private void accept1(Value value, Object newValue) {
			if (baseCleanup != null) {
				baseCleanup.run();
				baseCleanup = null;
			}
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
				((ReadOnlyProperty<T>) unopt(base)).addListener(listener);
				baseCleanup = () -> ((ReadOnlyProperty<T>) unopt(base)).removeListener(listener);
				listener.changed(null, null, ((ReadOnlyProperty<T>) unopt(base)).getValue());
			} else if (unopt(base) instanceof HalfBinder) {
				baseCleanup = ((HalfBinder<T>) unopt(base)).addListener(newValue2 -> {
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
		public Runnable addListener(Consumer<T> listener) {
			listeners.add(listener);
			if (last.isPresent())
				listener.accept(unopt(last));
			return () -> listeners.remove(listener);
		}

		@Override
		public Optional<T> get() {
			return last;
		}
	}

	public static class ListElementsHalfBinder<T> implements HalfBinder<T> {
		List<Consumer<T>> listeners = new ArrayList<>();
		Optional<T> at = Optional.empty();
		final List<Runnable> cleanup;

		public <G> ListElementsHalfBinder(List<HalfBinder<G>> list, Function<List<HalfBinder<G>>, Optional<T>> function) {
			Consumer<G> listener = u0 -> {
				at = function.apply(list);
				if (at.isPresent())
				listeners.forEach(l -> l.accept(at.get()));
			};
			cleanup = list.stream().map(t -> t.addListener(listener)).collect(Collectors.toList());
			listener.accept(null);
		}

		@Override
		public Runnable addListener(Consumer<T> listener) {
			listeners.add(listener);
			if (at.isPresent()) listener.accept(at.get());
			return () -> listeners.remove(listener);
		}

		@Override
		public Optional<T> get() {
			return at;
		}

		@Override
		public void destroy() {
			cleanup.forEach(c -> c.run());
		}
	}
}
