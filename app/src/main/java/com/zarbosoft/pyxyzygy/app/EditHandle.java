package com.zarbosoft.pyxyzygy.app;

public abstract class EditHandle {
  /**
   * @param context
   * @param window
   * @param vector In flipped/zoomed coordinates relative to canvas global origin
   */
  public abstract void cursorMoved(Context context, Window window, DoubleVector vector);

  public abstract Wrapper getWrapper();

  public abstract void remove(Context context, Window window);

  /**
   * @param context
   * @param window
   * @param start In flipped/zoomed coordinates relative to canvas global origin
   * @param end In flipped/zoomed coordinates relative to canvas global origin
   */
  public abstract void mark(Context context, Window window, DoubleVector start, DoubleVector end);

  /**
   * @param context
   * @param window
   * @param start In flipped/zoomed coordinates relative to canvas global origin
   */
  public abstract void markStart(Context context, Window window, DoubleVector start);

  public abstract CanvasHandle getCanvas();
}
