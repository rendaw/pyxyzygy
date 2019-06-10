package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;

@FunctionalInterface
public interface Garb {
  public void destroy(ProjectContext context, Window window);
}
