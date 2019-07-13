package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.zarbosoft.automodel.lib.Listener;
import com.zarbosoft.pyxyzygy.app.CanvasHandle;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.DoubleRectangle;
import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.Wrapper;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupChild;
import com.zarbosoft.pyxyzygy.core.model.latest.ProjectLayer;
import com.zarbosoft.pyxyzygy.seed.Vector;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ToolBar;

import static com.zarbosoft.pyxyzygy.app.Misc.mirror;
import static com.zarbosoft.pyxyzygy.app.Misc.noopConsumer;
import static com.zarbosoft.pyxyzygy.app.Misc.unopt;

public class GroupNodeCanvasHandle extends CanvasHandle {
  private final Runnable layerListenCleanup;
  private final ObservableList<CanvasHandle> childHandles = FXCollections.observableArrayList();
  private final Runnable childrenRoot;
  private final Runnable overlayRoot;
  private CanvasHandle parent;
  final SimpleIntegerProperty positiveZoom = new SimpleIntegerProperty(0);
  private final Listener.ScalarSet<ProjectLayer, Vector> offsetListener;

  ToolBar toolBar = new ToolBar();
  private GroupNodeWrapper wrapper;

  public GroupNodeCanvasHandle(Context context, Window window, GroupNodeWrapper wrapper) {
    layerListenCleanup =
        mirror(
            wrapper.children,
            childHandles,
            c -> {
              final CanvasHandle canvasHandle = c.buildCanvas(context, window, this);
              canvasHandle.setViewport(context, bounds.get(), positiveZoom.get());
              return canvasHandle;
            },
            h -> h.remove(context, null),
            noopConsumer());
    childrenRoot = mirror(
        childHandles,
        paint.getChildren(),
        h -> {
          return h.getPaintWidget();
        },
        noopConsumer(),
        noopConsumer());
    overlayRoot = mirror(
        childHandles,
        overlay.getChildren(),
        h -> {
          return h.getOverlayWidget();
        },
        noopConsumer(),
        noopConsumer());
    offsetListener =
        wrapper.node.addOffsetSetListeners(
            (target, offset) -> {
              paint.setLayoutX(offset.x);
              paint.setLayoutY(offset.y);
              overlay.setLayoutX(offset.x);
              overlay.setLayoutY(offset.y);
            });
    this.wrapper = wrapper;
  }

  @Override
  public void setParent(CanvasHandle parent) {
    this.parent = parent;
  }

  @Override
  public void setViewport(Context context, DoubleRectangle newBounds, int positiveZoom) {
    this.positiveZoom.set(positiveZoom);
    this.bounds.set(newBounds);
    childHandles.forEach(c -> c.setViewport(context, newBounds, positiveZoom));
  }

  @Override
  public void setViewedTime(Context context, int outerTime) {
    this.time.set(outerTime);
    childHandles.forEach(c -> c.setViewedTime(context, toInnerTime(outerTime)));
  }

  @Override
  public void remove(Context context, Wrapper excludeSubtree) {
    wrapper.canvasHandle = null;
    childHandles.forEach(c -> c.remove(context, excludeSubtree));
    wrapper.node.removeOffsetSetListeners(offsetListener);
    layerListenCleanup.run();
    overlayRoot.run();
    childrenRoot.run();
  }

  @Override
  public Wrapper getWrapper() {
    return wrapper;
  }

  @Override
  public CanvasHandle getParent() {
    return parent;
  }

  @Override
  public DoubleVector toInnerPosition(DoubleVector outerPosition) {
    return outerPosition.minus(wrapper.node.offset());
  }

  @Override
  public int toInnerTime(int outerTime) {
    return outerTime;
  }
}
