package com.zarbosoft.pyxyzygy.app;

public abstract class Tool {

  /**
   * @param context
   * @param window
   * @param localStart
   * @param localStartWithOffset local coords (including frame/layer offsets)
   * @param globalStart global coords
   */
  public abstract void markStart(
    Context context, Window window, DoubleVector localStart, DoubleVector localStartWithOffset, DoubleVector globalStart
  );

  /**
   * @param context
   * @param window
   * @param localStart
   * @param localEnd
   * @param localStartWithOffset local coords (including frame/layer offsets)
   * @param localEndWithOffset
   * @param globalStart global coords
   * @param globalEnd
   */
  public abstract void mark(
    Context context,
    Window window, DoubleVector localStart, DoubleVector localEnd, DoubleVector localStartWithOffset,
    DoubleVector localEndWithOffset,
    DoubleVector globalStart,
    DoubleVector globalEnd
  );

  public abstract void remove(Context context, Window window);

  /**
   * @param context
   * @param window
   * @param position relative to edit origin (not factoring in frame/layer offsets)
   */
  public abstract void cursorMoved(Context context, Window window, DoubleVector position);
}
