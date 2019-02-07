package com.zarbosoft.pyxyzygy.generate.premodel;

import com.zarbosoft.interface1.Configuration;

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
