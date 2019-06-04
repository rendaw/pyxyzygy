package com.zarbosoft.pyxyzygy.app.widgets.binding;

public class SimpleBinderRoot implements BinderRoot {
  private final HalfBinder binder;
  private final Object key;

  public SimpleBinderRoot(HalfBinder binder, Object key) {
    this.binder = binder;
    this.key = key;
  }

  @Override
  public void destroy() {
    binder.removeRoot(key);
  }
}
