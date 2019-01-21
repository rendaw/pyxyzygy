package com.zarbosoft.shoedemo.config;

import com.zarbosoft.interface1.Configuration;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

public class TrueColorImageNodeConfig extends NodeConfig{
	@Configuration
	public static enum Tool {
		@Configuration(name = "select")
		SELECT,
		@Configuration(name = "brush")
		BRUSH
	}
	@Configuration
	public final SimpleObjectProperty<Tool> tool = new SimpleObjectProperty<>(Tool.BRUSH);

	@Configuration
	public final SimpleIntegerProperty brush = new SimpleIntegerProperty(1);
}
