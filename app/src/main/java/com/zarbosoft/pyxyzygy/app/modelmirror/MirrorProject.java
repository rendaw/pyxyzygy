package com.zarbosoft.pyxyzygy.app.modelmirror;

import com.zarbosoft.automodel.lib.ProjectObject;
import com.zarbosoft.pyxyzygy.app.Misc;
import com.zarbosoft.pyxyzygy.core.model.latest.Project;
import com.zarbosoft.rendaw.common.Common;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

public class MirrorProject extends ObjectMirror {
  private final ObjectMirror parent;
  private final Project object;
  private final ObservableList<ObjectMirror> children = FXCollections.observableArrayList();
  private final Runnable topListenCleanup;

  public MirrorProject(
      com.zarbosoft.pyxyzygy.app.Context context,
      Context mirrorContext,
      ObjectMirror parent,
      Project object) {
    this.parent = parent;
    this.object = object;

    tree.set(new TreeItem<>(this));

    topListenCleanup =
        object.mirrorTop(
            children,
            layer -> {
              return mirrorContext.create(context, this, layer);
            },
            child -> child.remove(context),
            (at, end) -> {
              for (int i = at; i < children.size(); ++i) children.get(i).setParentIndex(i);
            });
    Misc.mirror(
        children,
        tree.get().getChildren(),
        child -> {
          child.tree.addListener(
              (observable, oldValue, newValue) -> {
                tree.get().getChildren().set(child.parentIndex, newValue);
              });
          return child.tree.get();
        },
        Common.noopConsumer,
        Common.noopConsumer);
  }

  @Override
  public ObjectMirror getParent() {
    return this.parent;
  }

  @Override
  public ProjectObject getValue() {
    return object;
  }

  @Override
  public void remove(com.zarbosoft.pyxyzygy.app.Context context) {
    topListenCleanup.run();
  }
}
