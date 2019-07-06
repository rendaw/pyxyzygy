package com.zarbosoft.pyxyzygy.app.modelmirror;

import com.zarbosoft.automodel.lib.ProjectObject;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteImageLayer;
import javafx.scene.control.TreeItem;

public class MirrorPaletteImageNode extends ObjectMirror {
  private final PaletteImageLayer node;
  private final ObjectMirror parent;

  public MirrorPaletteImageNode(ObjectMirror parent, PaletteImageLayer node) {
    this.node = node;
    this.parent = parent;
    this.parentIndex = -1;
    tree.set(new TreeItem<>(this));
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
  public void remove(com.zarbosoft.pyxyzygy.app.Context context) {}
}
