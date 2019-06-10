package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;

public abstract class Tool {

  /**
   * @param context
   * @param window
   * @param start local coords
   * @param globalStart local coords
   */
  public abstract void markStart(
      ProjectContext context, Window window, DoubleVector start, DoubleVector globalStart);

  /**
   * @param context
   * @param window
   * @param start local coords
   * @param end
   * @param globalStart local coords
   * @param globalEnd
   */
  public abstract void mark(
      ProjectContext context,
      Window window,
      DoubleVector start,
      DoubleVector end,
      DoubleVector globalStart,
      DoubleVector globalEnd);

  public abstract void remove(ProjectContext context, Window window);

  /**
   * @param context
   * @param window
   * @param position local coords
   */
  public abstract void cursorMoved(ProjectContext context, Window window, DoubleVector position);
}
