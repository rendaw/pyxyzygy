package com.zarbosoft.internal.shoedemo_generate.premodel;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.shoedemo.model.ProjectNode;
import com.zarbosoft.shoedemo.model.ProjectObject;

import java.util.List;

@Configuration
public class GroupLayer extends ProjectObject {
	@Configuration
	public List<GroupTimeFrame> timeFrames;
	@Configuration
	public List<GroupPositionFrame> positionFrames;
	@Configuration
	public ProjectNode inner;
}
