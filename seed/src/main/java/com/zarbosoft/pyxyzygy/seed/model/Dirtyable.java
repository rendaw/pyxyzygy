package com.zarbosoft.pyxyzygy.seed.model;

import com.zarbosoft.pyxyzygy.seed.model.v0.ProjectContextBase;

public interface Dirtyable {
	void dirtyFlush(ProjectContextBase context);
}
