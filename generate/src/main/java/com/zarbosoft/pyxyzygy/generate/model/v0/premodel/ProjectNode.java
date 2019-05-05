package com.zarbosoft.pyxyzygy.generate.model.v0.premodel;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;

import java.util.Map;

/**
 * Shared, mutable
 */
@Configuration
public abstract class ProjectNode extends ProjectObject {

	@Configuration
	String name;

	@Configuration
	Map<String, String> metadata;

	/**
	 * 0-100000
	 */
	@Configuration
	int opacity;

	@Configuration
	public Vector offset;
}
