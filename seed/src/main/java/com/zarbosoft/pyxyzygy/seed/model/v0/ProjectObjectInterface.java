package com.zarbosoft.pyxyzygy.seed.model.v0;

import com.zarbosoft.luxem.write.RawWriter;

public interface ProjectObjectInterface {
	public long id();
	void serialize(RawWriter writer);
	void incRef(ProjectContextBase project);
	void decRef(ProjectContextBase project);
}
