package com.zarbosoft.pyxyzygy.app.config;

import com.zarbosoft.interface1.Configuration;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

@Configuration(name = "palette-brush")
public class PaletteBrush extends BaseBrush {
	@Configuration
	public SimpleBooleanProperty useColor = new SimpleBooleanProperty();

	@Configuration
	public SimpleIntegerProperty index = new SimpleIntegerProperty();
}
