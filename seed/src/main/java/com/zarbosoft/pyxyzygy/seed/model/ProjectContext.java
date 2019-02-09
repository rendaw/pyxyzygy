package com.zarbosoft.pyxyzygy.seed.model;

public abstract class ProjectContext {
	/**
	 * @return a ProjectContext of the newest model version.  No-op for the latest model version ProjectObject.
	 */
	public abstract Object migrate();
}
