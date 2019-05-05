package com.zarbosoft.pyxyzygy.app.config;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

@Configuration(name = "group")
public class GroupNodeConfig extends NodeConfig {
	public static String TOOL_STAMP = "stamp";

	@Configuration
	public final SimpleLongProperty stampSource = new SimpleLongProperty(-1);

	public GroupNodeConfig() {
		super();
	}

	public GroupNodeConfig(ProjectContext context) {
		super(context);
	}
}
