package com.zarbosoft.pyxyzygy.app;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.Node;

import java.util.ArrayList;
import java.util.List;

public abstract class CanvasHandle {
  public final SimpleIntegerProperty time = new SimpleIntegerProperty(0);
  public final SimpleObjectProperty<DoubleRectangle> bounds =
      new SimpleObjectProperty<>(new DoubleRectangle(0, 0, 0, 0));
  public List<FrameMapEntry> timeMap = new ArrayList<>();

  /**
   * Convert vector in parent space to local space at the current time
   * @param outerPosition
   * @return
   */
  public abstract DoubleVector toInnerPosition(DoubleVector outerPosition);

  /**
   * Convert time in parent timespace to local time
   * @param outerTime
   * @return
   */
  public abstract int toInnerTime(int outerTime);

  public final Group paint = new Group();
  public final Group overlay = new Group();

  public final Node getPaintWidget() {
    return paint;
  }

  public final Node getOverlayWidget() {
    return overlay;
  }

  public abstract void setViewport(Context context, DoubleRectangle newBounds, int positiveZoom);

  public abstract void setViewedTime(Context context, int outerTime);

  public abstract void remove(Context context, Wrapper excludeSubtree);

  public abstract Wrapper getWrapper();

  public abstract CanvasHandle getParent();

  public abstract void setParent(CanvasHandle parent);
}
