package com.zarbosoft.pyxyzygy.wrappers.group;

import com.zarbosoft.pyxyzygy.DoubleVector;
import com.zarbosoft.pyxyzygy.ProjectContext;
import javafx.scene.Node;

public abstract class Tool {

	public abstract void markStart(ProjectContext context, DoubleVector start);

	public abstract void mark(ProjectContext context, DoubleVector start, DoubleVector end);

	public abstract Node getProperties();

	public abstract void remove(ProjectContext context);
}
