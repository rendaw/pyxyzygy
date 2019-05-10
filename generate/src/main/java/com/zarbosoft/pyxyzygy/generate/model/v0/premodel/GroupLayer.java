package com.zarbosoft.pyxyzygy.generate.model.v0.premodel;

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

	/**
	 * 0-100000
	 */
	@Configuration
	public int opacity;

	@Configuration
	public boolean enabled;
}
