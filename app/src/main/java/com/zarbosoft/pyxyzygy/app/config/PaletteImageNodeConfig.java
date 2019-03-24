package com.zarbosoft.pyxyzygy.app.config;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;

@Configuration(name = "paletteimage")
public class PaletteImageNodeConfig extends NodeConfig {
	public PaletteImageNodeConfig(ProjectContext context) {
		super(context);
	}

	public PaletteImageNodeConfig() {

	}

	@Configuration
	public static enum Tool {
		@Configuration(name = "select") SELECT,
		@Configuration(name = "brush") BRUSH
	}

	@Configuration
	public final SimpleObjectProperty<Tool> tool = new SimpleObjectProperty<>(Tool.BRUSH);

	@Configuration
	public final SimpleObjectProperty<Integer> brush = new SimpleObjectProperty<>(1);

	@Configuration
	public int lastBrush = 0;

	@Configuration
	public SimpleObjectProperty<Integer> paletteOffset = new SimpleObjectProperty<>(1);
}
