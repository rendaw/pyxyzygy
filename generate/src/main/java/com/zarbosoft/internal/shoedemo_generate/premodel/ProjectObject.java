package com.zarbosoft.internal.shoedemo_generate.premodel;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.shoedemo.model.ModelRootType;
import com.zarbosoft.shoedemo.modelhelp.Model;

@Configuration
public abstract class ProjectObject extends ModelRootType  {
	@Model
	@Configuration
	long refCount;

	@Model
	@Configuration
	long id;
}
