package com.zarbosoft.pyxyzygy.app.wrappers;

import com.zarbosoft.automodel.lib.History;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.Tool;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.Wrapper;
import com.zarbosoft.pyxyzygy.core.model.latest.ProjectLayer;
import com.zarbosoft.pyxyzygy.seed.Vector;

import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.centerCursor;

public class ToolMove extends Tool {
  protected DoubleVector markStart;
  private Vector markStartOffset;
  private final Wrapper wrapper;

  public ToolMove(Window window, Wrapper wrapper) {
    this.wrapper = wrapper;
    window.editorCursor.set(this, centerCursor("cursor-move32.png"));
  }

  @Override
  public void markStart(
    Context context, Window window, DoubleVector start, DoubleVector globalStart) {
    this.markStart = globalStart;
    this.markStartOffset = ((ProjectLayer) wrapper.getValue()).offset();
  }

  @Override
  public void mark(
      Context context,
      Window window,
      DoubleVector start,
      DoubleVector end,
      DoubleVector globalStart,
      DoubleVector globalEnd) {
    context.change(
        new History.Tuple(wrapper, "move"),
        c ->
            c.projectLayer((ProjectLayer) wrapper.getValue())
                .offsetSet(globalEnd.minus(markStart).plus(markStartOffset).toInt()));
  }

  @Override
  public void remove(Context context, Window window) {
    window.editorCursor.clear(this);
  }

  @Override
  public void cursorMoved(Context context, Window window, DoubleVector position) {}
}
