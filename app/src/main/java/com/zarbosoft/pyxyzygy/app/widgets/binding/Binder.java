package com.zarbosoft.pyxyzygy.app.widgets.binding;

import java.util.Optional;
import java.util.function.Function;

public interface Binder<T> extends HalfBinder<T> {
  void set(T v);

  /**
   * For both forward and backward mappings, return a new value, or Optional.empty() if it is not
   * possible to determine if the state of the value has changed.
   *
   * @param forward
   * @param back
   * @param <U>
   * @return
   */
  default <U> Binder<U> bimap(Function<T, Optional<U>> forward, Function<U, Optional<T>> back) {
    return new BimapBinder<T, U>(this, forward, back);
  }
}
