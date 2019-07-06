package com.zarbosoft.pyxyzygy.app.modelmirror;

import com.zarbosoft.automodel.lib.ProjectObject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeItem;

public abstract class ObjectMirror {
  public int parentIndex;
  public final SimpleObjectProperty<TreeItem<ObjectMirror>> tree = new SimpleObjectProperty<>();

  public void setParentIndex(int index) {
    this.parentIndex = index;
  }

  public abstract ObjectMirror getParent();

  public abstract ProjectObject getValue();

  public abstract void remove(com.zarbosoft.pyxyzygy.app.Context context);

  public abstract static class Context {
    public abstract ObjectMirror create(
      com.zarbosoft.pyxyzygy.app.Context context, ObjectMirror parent, ProjectObject object);
  }
}
