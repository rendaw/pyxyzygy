package com.zarbosoft.pyxyzygy.app.widgets.binding;

import javafx.beans.value.ChangeListener;
import javafx.scene.control.SelectionModel;

import java.util.Optional;
import java.util.function.Consumer;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;

public class SelectionModelBinder<T> implements Binder<T> {
  final SelectionModel<T> model;
  private ChangeListener<T> listener1;

  public SelectionModelBinder(SelectionModel<T> model) {
    this.model = model;
  }

  @Override
  public void set(T v) {
    model.clearSelection();
    model.select(v);
  }

  @Override
  public BinderRoot addListener(Consumer<T> listener) {
    this.listener1 =
        (observable, oldValue, newValue) -> {
          listener.accept(newValue);
        };
    model.selectedItemProperty().addListener(listener1);
    return new SimpleBinderRoot(this, listener1);
  }

  @Override
  public void removeRoot(Object key) {
    model.selectedItemProperty().removeListener(listener1);
  }

  @Override
  public Optional<T> get() {
    return opt(model.selectedItemProperty().get());
  }
}
