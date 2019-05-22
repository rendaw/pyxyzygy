package com.zarbosoft.pyxyzygy.app.widgets.binding;

import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;
import javafx.beans.binding.Binding;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;
import static com.zarbosoft.pyxyzygy.app.Misc.unopt;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public class CustomBinding {

	public static <T> BinderRoot bind(Property<T> dest, HalfBinder<T> source) {
		final Consumer<T> listener = v -> {
			dest.setValue(v);
		};
		return source.addListener(listener);
	}

	public static <T> BinderRoot bindBidirectional(Binder<T>... properties) {
		return new BinderRoot() {
			List<BinderRoot> cleanup = new ArrayList<>();
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
			public void destroy() {
				cleanup.forEach(BinderRoot::destroy);
			}
		};
	}

	public static Binding absInt(ObservableValue<Number> a) {
		return Bindings.createIntegerBinding(() -> Math.abs(a.getValue().intValue()), a);
	}

	public static Binding bindAbs(ObservableValue<Number> a) {
		return Bindings.createDoubleBinding(() -> Math.abs(a.getValue().doubleValue()), a);
	}

}
