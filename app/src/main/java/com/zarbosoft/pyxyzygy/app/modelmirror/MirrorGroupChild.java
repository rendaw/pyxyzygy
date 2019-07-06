package com.zarbosoft.pyxyzygy.app.modelmirror;

import com.zarbosoft.automodel.lib.Listener;
import com.zarbosoft.automodel.lib.ProjectObject;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupChild;
import com.zarbosoft.pyxyzygy.core.model.latest.ProjectLayer;

public class MirrorGroupChild extends ObjectMirror {
  private final ObjectMirror parent;
  private final GroupChild node;
  private final Listener.ScalarSet<GroupChild, ProjectLayer> innerSetListener;
  private ObjectMirror child;

  public MirrorGroupChild(
    com.zarbosoft.pyxyzygy.app.Context context,
    Context mirrorContext,
    ObjectMirror parent,
    GroupChild node) {
    this.parent = parent;
    this.parentIndex = -1;
    this.node = node;

    this.innerSetListener =
        node.addInnerSetListeners(
            (target, value) -> {
              if (child != null) {
                tree.unbind();
                child.remove(context);
              }
              if (value != null) {
                child = mirrorContext.create(context, MirrorGroupChild.this, value);
                child.setParentIndex(parentIndex);
                tree.bind(child.tree);
              }
            });
  }

  @Override
  public void setParentIndex(int index) {
    super.setParentIndex(index);
    if (child != null) child.setParentIndex(index);
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
    node.removeInnerSetListeners(innerSetListener);
  }
}
