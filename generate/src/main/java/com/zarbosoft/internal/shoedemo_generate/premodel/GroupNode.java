package com.zarbosoft.internal.shoedemo_generate.premodel;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.shoedemo.model.ProjectNode;

import java.util.List;

@Configuration
public class GroupNode extends ProjectNode {
	@Configuration
	public List<GroupLayer> layers;
}
