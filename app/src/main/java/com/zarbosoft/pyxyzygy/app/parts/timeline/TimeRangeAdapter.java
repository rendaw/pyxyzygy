package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.core.model.latest.ChangeStepBuilder;

public abstract class TimeRangeAdapter {
  public abstract int getOuterAt();

  public abstract int getInnerStart();

  public abstract int getInnerLength();

  public abstract void changeStart(ChangeStepBuilder change, int value);

  public abstract void changeLength(ChangeStepBuilder change, int value);

  /**
   * Used for change category uniqueness
   *
   * @return
   */
  public abstract Object getData();
}
