package com.zarbosoft.internal.shoedemo_generate.premodel;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.shoedemo.modelhelp.Model;
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

	/*
	@Model
			@Configuration
	SimpleIntegerProperty viewFrame;

	@Model
	@Configuration
	SimpleIntegerProperty viewZoom;

	@Model
	@Configuration
	SimpleIntegerProperty flipX;

	@Model
	@Configuration
	SimpleIntegerProperty flipY;
	*/
}
