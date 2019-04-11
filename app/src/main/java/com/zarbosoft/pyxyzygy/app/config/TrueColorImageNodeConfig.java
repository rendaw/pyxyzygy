package com.zarbosoft.pyxyzygy.app.config;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

@Configuration(name = "truecolorimage")
public class TrueColorImageNodeConfig extends NodeConfig {
	public TrueColorImageNodeConfig(ProjectContext context) {
		super(context);
	}

	public TrueColorImageNodeConfig() {

	}

	@Configuration
	public static enum Tool {
		@Configuration(name = "move") MOVE,
		@Configuration(name = "select") SELECT,
		@Configuration(name = "brush") BRUSH
	}

	@Configuration
	public final SimpleObjectProperty<Tool> tool = new SimpleObjectProperty<>(Tool.BRUSH);

	@Configuration
	public final SimpleIntegerProperty brush = new SimpleIntegerProperty(1);

	@Configuration
	public int lastBrush = 0;
}
