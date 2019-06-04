package com.zarbosoft.pyxyzygy.app.modelmirror;

import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectObject;
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

  public abstract void remove(ProjectContext context);

  public abstract static class Context {
    public abstract ObjectMirror create(
        ProjectContext context, ObjectMirror parent, ProjectObject object);
  }
}
