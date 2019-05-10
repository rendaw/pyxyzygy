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

	public static final String TOOL_FRAME_MOVE = "frame_move";
	public static final String TOOL_SELECT = "select";
	public static final String TOOL_BRUSH = "brush";

	@Configuration
	public final SimpleIntegerProperty brush = new SimpleIntegerProperty(1);

	@Configuration
	public int lastBrush = 0;
}
