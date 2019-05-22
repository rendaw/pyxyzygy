package com.zarbosoft.pyxyzygy.app.widgets.binding;

import com.zarbosoft.pyxyzygy.seed.model.Listener;
import com.zarbosoft.rendaw.common.Assertion;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public class ListHalfBinder<T> implements HalfBinder<List<T>> {
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
	public BinderRoot addListener(Consumer<List<T>> listener) {
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
		return new SimpleBinderRoot(this, new Object[] {listAdd, listRemove, listMoveTo});
	}

	@Override
	public void removeRoot(Object key0) {
		Object[] key = (Object[]) key0;
		unlistenAdd.accept((Listener.ListAdd) key[0]);
		unlistenRemove.accept((Listener.ListRemove) key[1]);
		unlistenMoveTo.accept((Listener.ListMoveTo) key[2]);
	}

	@Override
	public Optional<List<T>> get() {
		return opt(get.get());
	}
}
