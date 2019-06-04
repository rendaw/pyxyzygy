package com.zarbosoft.pyxyzygy.app.widgets.binding;

import java.util.Optional;
import java.util.function.Consumer;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;

public class ConstHalfBinder<T> implements HalfBinder<T> {
  final T v;

  public ConstHalfBinder(T v) {
    this.v = v;
  }

  @Override
  public BinderRoot addListener(Consumer<T> listener) {
    listener.accept(v);
    return new BinderRoot() {
      @Override
      public void destroy() {}
    };
  }

  @Override
  public void removeRoot(Object key) {}

  @Override
  public Optional<T> get() {
    return opt(v);
  }
}
