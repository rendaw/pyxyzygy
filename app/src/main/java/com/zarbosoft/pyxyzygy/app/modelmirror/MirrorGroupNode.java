package com.zarbosoft.pyxyzygy.app.modelmirror;

import com.zarbosoft.automodel.lib.ProjectObject;
import com.zarbosoft.pyxyzygy.app.Misc;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupLayer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

public class MirrorGroupNode extends ObjectMirror {
  private final ObjectMirror parent;
  private final GroupLayer node;
  private final ObservableList<ObjectMirror> children = FXCollections.observableArrayList();
  private final Runnable childrenListenCleanup;

  public MirrorGroupNode(
    com.zarbosoft.pyxyzygy.app.Context context,
    Context mirrorContext,
    ObjectMirror parent,
    GroupLayer node) {
    this.parentIndex = -1;
    this.parent = parent;
    this.node = node;

    tree.set(new TreeItem<>(this));
    tree.get().setExpanded(true);

    childrenListenCleanup =
        node.mirrorChildren(
            children,
            child -> {
              return mirrorContext.create(context, this, child);
            },
            child -> child.remove(context),
            at -> {
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
        Misc.noopConsumer(),
        Misc.noopConsumer());
  }

  @Override
  public ObjectMirror getParent() {
    return parent;
  }

  @Override
  public ProjectObject getValue() {
    return node;
  }

  @Override
  public void remove(com.zarbosoft.pyxyzygy.app.Context context) {
    childrenListenCleanup.run();
  }
}
