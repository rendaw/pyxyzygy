package com.zarbosoft.pyxyzygy.generate.model.v0.premodel;

import com.zarbosoft.interface1.Configuration;

@Configuration
public class Camera extends GroupNode {
	@Configuration
	public int width;

	@Configuration
	public int height;

	@Configuration
	public int frameStart;

	@Configuration
	public int frameLength;

	@Configuration
	public int frameRate;
}
