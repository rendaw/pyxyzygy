package com.zarbosoft.pyxyzygy.app.widgets.binding;

import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.value.ChangeListener;

import java.util.Optional;
import java.util.function.Consumer;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;

public class PropertyHalfBinder<T> implements HalfBinder<T> {
  final ReadOnlyProperty<T> property;

  public PropertyHalfBinder(ReadOnlyProperty<T> property) {
    this.property = property;
  }

  @Override
  public BinderRoot addListener(Consumer<T> listener) {
    final ChangeListener<T> inner =
        (observable, oldValue, newValue) -> {
          listener.accept(newValue);
        };
    property.addListener(inner);
    listener.accept(property.getValue());
    return new SimpleBinderRoot(this, inner);
  }

  @Override
  public void removeRoot(Object key) {
    property.removeListener((ChangeListener<? super T>) key);
  }

  @Override
  public Optional<T> get() {
    return opt(property.getValue());
  }
}
