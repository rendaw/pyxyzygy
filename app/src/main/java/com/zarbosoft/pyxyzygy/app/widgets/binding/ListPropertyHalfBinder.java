package com.zarbosoft.pyxyzygy.app.widgets.binding;

import javafx.beans.InvalidationListener;
import javafx.collections.ObservableList;

import java.util.Optional;
import java.util.function.Consumer;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;

public class ListPropertyHalfBinder<T extends ObservableList> implements HalfBinder<T> {
  final T property;

  public ListPropertyHalfBinder(T property) {
    this.property = property;
  }

  @Override
  public BinderRoot addListener(Consumer<T> listener) {
    final InvalidationListener inner =
        c -> {
          listener.accept(property);
        };
    property.addListener(inner);
    listener.accept(property);
    return new SimpleBinderRoot(this, inner);
  }

  @Override
  public void removeRoot(Object key) {
    property.removeListener((InvalidationListener) key);
  }

  @Override
  public Optional<T> get() {
    return opt(property);
  }
}
