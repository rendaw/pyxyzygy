package com.zarbosoft.pyxyzygy.app.widgets.binding;

import com.zarbosoft.automodel.lib.Listener;
import com.zarbosoft.automodel.lib.ProjectObject;
import com.zarbosoft.rendaw.common.Assertion;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Consumer;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public class ScalarHalfBinder<T> implements HalfBinder<T> {
  T last;
  private final Consumer<Listener.ScalarSet> listen;
  private final Consumer<Listener.ScalarSet> unlisten;

  public ScalarHalfBinder(
      Consumer<Listener.ScalarSet> listen, Consumer<Listener.ScalarSet> unlisten) {
    this.listen = listen;
    this.unlisten = unlisten;
  }

  public ScalarHalfBinder(ProjectObject base, String name) {
    if (Character.isUpperCase(name.charAt(0))) throw new Assertion();
    String camel = name.substring(0, 1).toUpperCase() + name.substring(1);
    listen = bindConsumer(base, "add" + camel + "SetListeners", Listener.ScalarSet.class);
    unlisten = bindConsumer(base, "remove" + camel + "SetListeners", Listener.ScalarSet.class);
  }

  private static <X> Consumer<X> bindConsumer(Object target, String name, Class listener) {
    Method method = uncheck(() -> target.getClass().getMethod(name, listener));
    return g -> uncheck(() -> method.invoke(target, g));
  }

  @Override
  public BinderRoot addListener(Consumer<T> listener) {
    final Listener.ScalarSet<Object, T> inner =
        (t, v) -> {
          listener.accept(v);
        };
    this.listen.accept(inner);
    return new SimpleBinderRoot(this, inner);
  }

  @Override
  public void removeRoot(Object key) {
    this.unlisten.accept((Listener.ScalarSet) key);
  }

  @Override
  public Optional<T> get() {
    return opt(last);
  }
}
