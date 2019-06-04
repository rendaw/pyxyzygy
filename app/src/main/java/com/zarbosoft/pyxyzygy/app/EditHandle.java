package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;

public abstract class EditHandle {
  /**
   * @param context
   * @param window
   * @param vector In flipped/zoomed coordinates relative to canvas global origin
   */
  public abstract void cursorMoved(ProjectContext context, Window window, DoubleVector vector);

  public abstract Wrapper getWrapper();

  public abstract void remove(ProjectContext context, Window window);

  /**
   * @param context
   * @param window
   * @param start In flipped/zoomed coordinates relative to canvas global origin
   * @param end In flipped/zoomed coordinates relative to canvas global origin
   */
  public abstract void mark(
      ProjectContext context, Window window, DoubleVector start, DoubleVector end);

  /**
   * @param context
   * @param window
   * @param start In flipped/zoomed coordinates relative to canvas global origin
   */
  public abstract void markStart(ProjectContext context, Window window, DoubleVector start);

  public abstract CanvasHandle getCanvas();
}
