package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.rendaw.common.Common;
import com.zarbosoft.rendaw.common.Pair;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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

	public static class Binder<T> {
		final Property property;
		final Supplier<Optional<T>> supplier;
		final Consumer<T> consumer;

		public Binder(Property property, Supplier<Optional<T>> supplier, Consumer<T> consumer) {
			this.property = property;
			this.supplier = supplier;
			this.consumer = consumer;
		}
	}

	public static <T> Runnable bindBidirectionalMultiple(Binder<T>... properties) {
		List<Pair<Property, ChangeListener>> cleanup = new ArrayList<>();
		Common.Mutable<Boolean> suppress = new Common.Mutable<>(false);
		for (Binder<T> binder : properties) {
			final ChangeListener listener = (observable, oldValue, newValue) -> {
				if (suppress.value)
					return;
				suppress.value = true;
				try {
					Optional<T> v = binder.supplier.get();
					if (!v.isPresent())
						return;
					for (Binder<T> otherBinder : properties) {
						if (otherBinder == binder)
							continue;
						otherBinder.consumer.accept(v.get());
					}
				} finally {
					suppress.value = false;
				}
			};
			binder.property.addListener(listener);
			cleanup.add(new Pair<>(binder.property, listener));
		}
		return () -> cleanup.forEach(pair -> pair.first.removeListener(pair.second));
	}

	public static Binding absInt(ObservableValue<Number> a) {
		return Bindings.createIntegerBinding(() -> Math.abs(a.getValue().intValue()), a);
	}

	public static Binding bindAbs(ObservableValue<Number> a) {
		return Bindings.createDoubleBinding(() -> Math.abs(a.getValue().doubleValue()), a);
	}
}
