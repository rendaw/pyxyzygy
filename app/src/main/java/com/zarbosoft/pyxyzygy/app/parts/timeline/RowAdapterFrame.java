package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.core.model.v0.ChangeStepBuilder;

public abstract class RowAdapterFrame {
  public abstract int length();

  public abstract void setLength(ProjectContext context, ChangeStepBuilder change, int length);

  public abstract void remove(ProjectContext context, ChangeStepBuilder change);

  public abstract void clear(ProjectContext context, ChangeStepBuilder change);

  public abstract void moveLeft(ProjectContext context, ChangeStepBuilder change);

  public abstract void moveRight(ProjectContext context, ChangeStepBuilder change);

  public abstract Object id();
}
