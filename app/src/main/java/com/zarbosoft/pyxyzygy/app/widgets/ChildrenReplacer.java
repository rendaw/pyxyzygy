package com.zarbosoft.pyxyzygy.app.widgets;

import java.util.List;

public abstract class ChildrenReplacer<T> {
  private Object key;

  public void set(Object key, List<T> content) {
    set(key, null, content);
  }

  public void set(Object key, String title, List<T> content) {
    if (key != this.key) innerClear();
    this.key = key;
    innerSet(title, content);
  }

  protected abstract void innerSet(String title, List<T> content);

  protected abstract void innerClear();

  public void clear(Object key) {
    if (key != this.key) return;
    this.key = null;
    innerClear();
  }
}
