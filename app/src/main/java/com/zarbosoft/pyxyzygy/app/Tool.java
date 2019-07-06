package com.zarbosoft.pyxyzygy.app;

public abstract class Tool {

  /**
   * @param context
   * @param window
   * @param start local coords
   * @param globalStart local coords
   */
  public abstract void markStart(
    Context context, Window window, DoubleVector start, DoubleVector globalStart);

  /**
   * @param context
   * @param window
   * @param start local coords
   * @param end
   * @param globalStart local coords
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
   * @param position local coords
   */
  public abstract void cursorMoved(Context context, Window window, DoubleVector position);
}
