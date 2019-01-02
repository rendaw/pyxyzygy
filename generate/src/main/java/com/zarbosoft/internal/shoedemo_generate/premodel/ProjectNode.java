package com.zarbosoft.internal.shoedemo_generate.premodel;

import com.zarbosoft.interface1.Configuration;

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
}
