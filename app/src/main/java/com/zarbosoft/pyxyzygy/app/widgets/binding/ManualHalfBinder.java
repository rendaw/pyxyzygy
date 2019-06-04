package com.zarbosoft.pyxyzygy.app.widgets.binding;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Consumer;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;
import static com.zarbosoft.pyxyzygy.app.Misc.unopt;

public class ManualHalfBinder<T> implements HalfBinder<T> {
  Optional<T> value = Optional.empty();
  WeakList<Consumer<T>> listeners = new WeakList<>();

  public void set(T value) {
    this.value = opt(value);
    for (Consumer<T> listener : new ArrayList<>(listeners)) {
      listener.accept(value);
    }
  }

  public void clear() {
    this.value = Optional.empty();
  }

  @Override
  public BinderRoot addListener(Consumer<T> listener) {
    listeners.add(listener);
    if (value.isPresent()) listener.accept(unopt(value));
    return new SimpleBinderRoot(this, listener);
  }

  @Override
  public void removeRoot(Object key) {
    listeners.remove(key);
  }

  @Override
  public Optional<T> get() {
    return value;
  }
}
