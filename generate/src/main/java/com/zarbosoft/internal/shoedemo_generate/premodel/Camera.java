package com.zarbosoft.internal.shoedemo_generate.premodel;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.shoedemo.model.ProjectNode;

@Configuration
public class Camera extends ProjectNode {
	@Configuration
	public ProjectNode inner;

	@Configuration
	public int width;

	@Configuration
	public int height;
}
