package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.core.model.v0.PaletteColor;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectNode;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectObject;
import com.zarbosoft.pyxyzygy.seed.model.v0.TrueColor;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Misc {
	public static void moveTo(List list, int source, int count, int dest) {
		if (list.get(0) instanceof Wrapper)
			throw new Assertion(); // DEBUG
		List temp0 = list.subList(source, source + count);
		List temp1 = new ArrayList(temp0);
		temp0.clear();
		list.addAll(dest, temp1);
	}

	public static void moveWrapperTo(List<Wrapper> list, int source, int count, int dest) {
		List temp0 = list.subList(source, source + count);
		List temp1 = new ArrayList(temp0);
		temp0.clear();
		list.addAll(dest, temp1);
		for (int i = Math.min(source, dest); i < list.size(); ++i) {
			list.get(i).setParentIndex(i);
		}
	}

	public static Runnable nodeFormFields(
			ProjectContext context, WidgetFormBuilder builder, Wrapper wrapper
	) {
		ProjectNode node = (ProjectNode) wrapper.getValue();
		return new Runnable() {
			private Runnable opacityCleanup;
			private Runnable nameCleanup;

			{
				builder.text("Name", t -> {
					this.nameCleanup =
							CustomBinding.bindBidirectional(new CustomBinding.ScalarBinder<>(node::addNameSetListeners,
									node::removeNameSetListeners,
									v -> context.change(new ProjectContext.Tuple(wrapper, "name"), c -> c.projectNode(node).nameSet(v))
							), new CustomBinding.PropertyBinder<>(t.textProperty()));
				});
				builder.slider("Opacity", 0, Global.opacityMax, slider -> {
					slider.setValue(node.opacity());
					opacityCleanup = CustomBinding.bindBidirectional(
							new CustomBinding.ScalarBinder<Integer>(
									node,
									"opacity",
									v -> context.change(new ProjectContext.Tuple(wrapper, "opacity"), c -> c.projectNode(node).opacitySet(v))
							),
							new CustomBinding.PropertyBinder<>(slider.valueProperty()).bimap(
									d -> Optional.of((int) (double) d), i -> (double) (int) i
					));
				});
			}

			@Override
			public void run() {
				nameCleanup.run();
				opacityCleanup.run();
			}
		};
	}

	public static <T, R> Runnable mirror(
			ObservableList<T> source, List<R> target, Function<T, R> add, Consumer<R> remove, Consumer<Integer> update
	) {
		return new Runnable() {
			private ListChangeListener<T> listener;
			private boolean dead = false;

			{
				listener = c -> {
					if (dead)
						return;
					while (c.next()) {
						if (c.wasAdded()) {
							target.addAll(c.getFrom(),
									c.getAddedSubList().stream().map(add).collect(Collectors.toList())
							);
						} else if (c.wasRemoved()) {
							List<R> removing = target.subList(c.getFrom(), c.getFrom() + c.getRemovedSize());
							removing.forEach(remove);
							removing.clear();
						} else if (c.wasPermutated()) {
							throw new Assertion();
						} else if (c.wasUpdated()) {
							throw new Assertion();
						}
						update.accept(c.getFrom());
					}
				};
				source.addListener(listener);
				target.addAll(source.stream().map(add).collect(Collectors.toList()));
				update.accept(0);
			}

			@Override
			public void run() {
				dead = true;
				source.removeListener(listener);
				target.forEach(remove);
			}
		};
	}

	public static Consumer noopConsumer = t -> {};
	public static <T> Consumer<T> noopConsumer() {
		return noopConsumer;
	}

	private static Object notReallyNull = new Object();

	public static <T> Optional<T> opt(T v) {
		if (v == null)
			return (Optional<T>) Optional.of(notReallyNull);
		else
			return Optional.of(v);
	}

	public static <T> T unopt(Optional<T> v) {
		Object out = v.get();
		if (out == notReallyNull)
			return null;
		else
			return (T) out;
	}
}
