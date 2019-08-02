package com.zarbosoft.pyxyzygy.app.widgets.binders;

import com.zarbosoft.automodel.lib.Listener;
import com.zarbosoft.automodel.lib.ProjectObject;
import com.zarbosoft.javafxbinders.Binder;

import java.util.function.Consumer;

public class ScalarBinder<T> extends ScalarHalfBinder<T> implements Binder<T> {
  private final Consumer<T> set;

  public ScalarBinder(
    Consumer<Listener.ScalarSet> listen, Consumer<Listener.ScalarSet> unlisten, Consumer<T> set) {
    super(listen, unlisten);
    this.set = set;
  }

  public ScalarBinder(ProjectObject base, String name, Consumer<T> set) {
    super(base, name);
    this.set = set;
  }

  @Override
  public void set(T v) {
    set.accept(v);
  }
}
