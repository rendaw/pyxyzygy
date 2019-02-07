package com.zarbosoft.pyxyzygy.gui.wrappers.group;

import com.zarbosoft.pyxyzygy.DoubleVector;
import com.zarbosoft.pyxyzygy.ProjectContext;
import com.zarbosoft.pyxyzygy.Window;
import javafx.scene.Node;

public abstract class Tool {

	public abstract void markStart(ProjectContext context, Window window, DoubleVector start);

	public abstract void mark(
			ProjectContext context, Window window, DoubleVector start, DoubleVector end
	);

	public abstract void remove(ProjectContext context);
}
