package com.zarbosoft.internal.pyxyzygy_generate.premodel;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.internal.pyxyzygy_seed.model.ModelRootType;

@Configuration
public abstract class ProjectObject extends ModelRootType  {
	@Configuration
	long refCount;

	@Configuration
	long id;
}
