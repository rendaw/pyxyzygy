package com.zarbosoft.pyxyzygy.app.wrappers.camera;

import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.Tool;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.core.model.latest.Camera;
import javafx.scene.ImageCursor;

import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.icon;

public class ToolViewport extends Tool {
  private final CameraWrapper wrapper;
  boolean adjustHorizontal;
  boolean adjustPositive;
  int startValue;
  private DoubleVector markStart;
  public Quadrant lastCursorQuad;

  public ToolViewport(CameraWrapper wrapper) {
    this.wrapper = wrapper;
  }

  private static class Quadrant {
    final boolean horizontal;
    final boolean positive;

    private Quadrant(boolean horizontal, boolean positive) {
      this.horizontal = horizontal;
      this.positive = positive;
    }
  }

  public Quadrant calculateQuadrant(DoubleVector start) {
    Camera node = (Camera) wrapper.node;
    double xyRatio = (double) node.width() / node.height();
    boolean horizontal = Math.abs(start.x) >= Math.abs(start.y * xyRatio);
    boolean positive;
    if (horizontal) {
      positive = start.x >= 0;
    } else {
      positive = start.y >= 0;
    }
    return new Quadrant(horizontal, positive);
  }

  @Override
  public void markStart(
      Context context,
      Window window,
      DoubleVector localStart,
      DoubleVector localStartWithOffset,
      DoubleVector globalStart) {
    Camera node = (Camera) wrapper.node;
    markStart = localStart;
    Quadrant quadrant = calculateQuadrant(markStart);
    adjustHorizontal = quadrant.horizontal;
    adjustPositive = quadrant.positive;
    if (adjustHorizontal) {
      startValue = node.width();
    } else {
      startValue = node.height();
    }
  }

  @Override
  public void mark(
      Context context,
      Window window,
      DoubleVector localStart,
      DoubleVector localEnd,
      DoubleVector localStartWithOffset,
      DoubleVector localEndWithOffset,
      DoubleVector globalStart,
      DoubleVector globalEnd) {
    double negative = adjustPositive ? 2 : -2;
    if (adjustHorizontal) {
      wrapper.width.set((int) (startValue + (localEnd.x - markStart.x) * negative));
    } else {
      wrapper.height.set((int) (startValue + (localEnd.y - markStart.y) * negative));
    }
  }

  @Override
  public void remove(Context context, Window window) {
    window.editorCursor.clear(this);
  }

  @Override
  public void cursorMoved(Context context, Window window, DoubleVector position) {
    Quadrant quadrant = calculateQuadrant(position);
    if (lastCursorQuad != null
        && lastCursorQuad.horizontal == quadrant.horizontal
        && lastCursorQuad.positive == quadrant.positive) return;
    lastCursorQuad = quadrant;
    if (quadrant.horizontal) {
      if (quadrant.positive) {
        window.editorCursor.set(this, new ImageCursor(icon("arrow-collapse-right32.png")));
      } else {
        window.editorCursor.set(this, new ImageCursor(icon("arrow-collapse-left32.png")));
      }
    } else {
      if (quadrant.positive) {
        window.editorCursor.set(this, new ImageCursor(icon("arrow-collapse-down32.png")));
      } else {
        window.editorCursor.set(this, new ImageCursor(icon("arrow-collapse-up32.png")));
      }
    }
  }
}
