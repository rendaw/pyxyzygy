package com.zarbosoft.shoedemo.parts.timeline;

import com.zarbosoft.shoedemo.ProjectContext;

public abstract class RowAdapterFrame {
	public abstract int length();

	public abstract void setLength(ProjectContext context, int length);

	public abstract void remove(ProjectContext context);

	public abstract void clear(ProjectContext context);

	public abstract void moveLeft(ProjectContext context);

	public abstract void moveRight(ProjectContext context);

	public abstract Object id();
}
