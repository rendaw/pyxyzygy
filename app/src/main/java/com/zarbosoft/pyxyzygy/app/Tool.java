package com.zarbosoft.pyxyzygy.app;

public abstract class Tool {

  /**
   * @param context
   * @param window
   * @param start local coords (including frame/layer offsets)
   * @param globalStart global coords
   */
  public abstract void markStart(
    Context context, Window window, DoubleVector start, DoubleVector globalStart);

  /**
   * @param context
   * @param window
   * @param start local coords (including frame/layer offsets)
   * @param end
   * @param globalStart global coords
   * @param globalEnd
   */
  public abstract void mark(
    Context context,
    Window window,
    DoubleVector start,
    DoubleVector end,
    DoubleVector globalStart,
    DoubleVector globalEnd);

  public abstract void remove(Context context, Window window);

  /**
   * @param context
   * @param window
   * @param position relative to edit origin (not factoring in frame/layer offsets)
   */
  public abstract void cursorMoved(Context context, Window window, DoubleVector position);
}
