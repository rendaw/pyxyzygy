package com.zarbosoft.pyxyzygy.app;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.Node;

public abstract class CanvasHandle {
  public final SimpleIntegerProperty frameNumber = new SimpleIntegerProperty(0);
  public final SimpleIntegerProperty previousFrame = new SimpleIntegerProperty(-1);
  public final SimpleIntegerProperty nextFrame = new SimpleIntegerProperty(-1);
  public final SimpleObjectProperty<DoubleRectangle> bounds =
      new SimpleObjectProperty<>(new DoubleRectangle(0, 0, 0, 0));

  public abstract DoubleVector toInner(DoubleVector vector);

  public final Group paint = new Group();
  public final Group overlay = new Group();

  public final Node getPaintWidget() {
    return paint;
  }

  public final Node getOverlayWidget() {
    return overlay;
  }

  public abstract void setViewport(Context context, DoubleRectangle newBounds, int positiveZoom);

  public abstract void setFrame(Context context, int frameNumber);

  public abstract void remove(Context context, Wrapper excludeSubtree);

  public abstract Wrapper getWrapper();

  public abstract CanvasHandle getParent();

  public abstract void setParent(CanvasHandle parent);
}
