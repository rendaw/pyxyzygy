package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.core.model.latest.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.latest.Model;

public abstract class RowAdapterFrame {
  public abstract int length();

  public abstract void setLength(Model context, ChangeStepBuilder change, int length);

  public abstract void remove(Context context, ChangeStepBuilder change);

  public abstract void clear(Context context, ChangeStepBuilder change);

  public abstract void moveLeft(Context context, ChangeStepBuilder change);

  public abstract void moveRight(Context context, ChangeStepBuilder change);

  public abstract Object id();
}
