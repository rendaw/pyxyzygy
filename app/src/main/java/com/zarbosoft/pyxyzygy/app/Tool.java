package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.ProjectContext;
import com.zarbosoft.pyxyzygy.app.Window;

public abstract class Tool {

	public abstract void markStart(ProjectContext context, Window window, DoubleVector start);

	public abstract void mark(
			ProjectContext context, Window window, DoubleVector start, DoubleVector end
	);

	public abstract void remove(ProjectContext context);
}
