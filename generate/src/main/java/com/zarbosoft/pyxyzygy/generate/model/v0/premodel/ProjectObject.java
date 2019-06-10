package com.zarbosoft.pyxyzygy.generate.model.v0.premodel;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pyxyzygy.seed.model.v0.ModelRootType;

@Configuration
public abstract class ProjectObject extends ModelRootType {
  @Configuration long refCount;

  @Configuration long id;
}
