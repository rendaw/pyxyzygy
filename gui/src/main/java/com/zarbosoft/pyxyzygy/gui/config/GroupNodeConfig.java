package com.zarbosoft.pyxyzygy.gui.config;

import com.zarbosoft.interface1.Configuration;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;

@Configuration
public class GroupNodeConfig extends NodeConfig{
	@Configuration
	public static enum Tool {
		@Configuration(name = "move")
		MOVE,
		@Configuration(name = "stamp")
		STAMP
	}
	@Configuration
	public final SimpleObjectProperty<Tool> tool = new SimpleObjectProperty<>(Tool.MOVE);

	@Configuration
	public final SimpleLongProperty stampSource = new SimpleLongProperty(-1);
}
