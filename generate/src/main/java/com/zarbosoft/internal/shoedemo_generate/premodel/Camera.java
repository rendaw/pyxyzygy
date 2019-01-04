package com.zarbosoft.internal.shoedemo_generate.premodel;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.shoedemo.model.Vector;

@Configuration
public class Camera extends ProjectNode {
	@Configuration
	public ProjectNode inner;

	@Configuration
	public int width;

	@Configuration
	public int height;

	@Configuration
	public int end;

	@Configuration
	public int frameRate;

	@Configuration
	public Vector offset;
}
