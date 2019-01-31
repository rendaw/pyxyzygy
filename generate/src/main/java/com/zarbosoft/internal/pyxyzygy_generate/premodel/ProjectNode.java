package com.zarbosoft.internal.pyxyzygy_generate.premodel;

import com.zarbosoft.interface1.Configuration;
import javafx.beans.property.SimpleIntegerProperty;

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
}
