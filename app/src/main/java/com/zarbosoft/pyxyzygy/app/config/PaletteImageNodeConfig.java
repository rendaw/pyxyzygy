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
		tool.set(TOOL_BRUSH);
	}

	public PaletteImageNodeConfig() {
		tool.set(TOOL_BRUSH);
	}

	public static final String TOOL_FRAME_MOVE = "frame_move";
	public static final String TOOL_SELECT = "select";
	public static final String TOOL_BRUSH = "brush";

	@Configuration
	public final SimpleObjectProperty<Integer> brush = new SimpleObjectProperty<>(1);

	@Configuration
	public int lastBrush = 0;

	@Configuration
	public SimpleObjectProperty<Integer> paletteOffset = new SimpleObjectProperty<>(1);
}
