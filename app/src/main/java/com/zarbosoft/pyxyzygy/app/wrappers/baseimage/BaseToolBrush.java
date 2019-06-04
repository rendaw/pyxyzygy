package com.zarbosoft.pyxyzygy.app.wrappers.baseimage;

import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.config.BaseBrush;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.CircleCursor;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectObject;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.beans.value.ChangeListener;
import javafx.scene.input.KeyCode;

public abstract class BaseToolBrush<F extends ProjectObject, L> extends Tool {
  private final BaseBrush brush;
  public final BaseImageNodeWrapper<?, F, ?, L> wrapper;
  private final ChangeListener<Integer> brushSizeListener;
  private final ChangeListener<Number> zoomListener;
  private DoubleVector lastEnd;

  public BaseToolBrush(Window window, BaseImageNodeWrapper<?, F, ?, L> wrapper, BaseBrush brush) {
    this.wrapper = wrapper;
    this.brush = brush;
    brushSizeListener = (observable, oldValue, newValue) -> updateCursor(window);
    zoomListener = (observable, oldValue, newValue) -> updateCursor(window);
    brush.size.addListener(brushSizeListener);
    window.editor.zoomFactor.addListener(zoomListener);
    updateCursor(window);
  }

  @Override
  public void markStart(
      ProjectContext context, Window window, DoubleVector start, DoubleVector globalStart) {}

  @Override
  public void mark(
      ProjectContext context,
      Window window,
      DoubleVector start,
      DoubleVector end,
      DoubleVector globalStart,
      DoubleVector globalEnd) {
    if (false) {
      throw new Assertion();
    } else if (window.pressed.contains(KeyCode.SHIFT)) {
      if (lastEnd == null) lastEnd = end;
      strokeInner(context, null, lastEnd, end);
    } else strokeInner(context, new ProjectContext.Tuple(brush, "stroke"), start, end);
  }

  private void strokeInner(
      ProjectContext context,
      ProjectContext.Tuple changeUnique,
      DoubleVector start,
      DoubleVector end) {
    final double startRadius = brush.size.get() / 20.0;
    final double endRadius = brush.size.get() / 20.0;
    final DoubleRectangle bounds =
        new BoundsBuilder().circle(start, startRadius).circle(end, endRadius).build();
    context.change(
        changeUnique,
        c -> {
          wrapper.modify(
              context,
              c,
              bounds,
              (image, corner) -> {
                stroke(
                    context, image, start.minus(corner), startRadius, end.minus(corner), endRadius);
              });
        });
    lastEnd = end;
  }

  protected abstract void stroke(
      ProjectContext context,
      L canvas,
      DoubleVector start,
      double startRadius,
      DoubleVector end,
      double endRadius);

  private void updateCursor(Window window) {
    double zoom = window.editor.zoomFactor.get();
    window.editorCursor.set(this, CircleCursor.create(brush.sizeInPixels() * zoom));
  }

  @Override
  public void remove(ProjectContext context, Window window) {
    brush.size.removeListener(brushSizeListener);
    window.editor.zoomFactor.removeListener(zoomListener);
    window.editorCursor.clear(this);
  }

  @Override
  public void cursorMoved(ProjectContext context, Window window, DoubleVector position) {}
}
