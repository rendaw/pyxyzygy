package com.zarbosoft.internal.shoedemo_generate.premodel;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.shoedemo.model.ProjectNode;
import com.zarbosoft.shoedemo.model.ProjectObject;

import java.util.List;

@Configuration
public class Project extends ProjectObject {
	@Configuration
	public List<ProjectNode> top;
}
