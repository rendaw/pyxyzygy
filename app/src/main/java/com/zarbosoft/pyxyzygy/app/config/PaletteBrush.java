package com.zarbosoft.pyxyzygy.app.config;

import com.zarbosoft.interface1.Configuration;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;

@Configuration(name = "palette-brush")
public class PaletteBrush extends BaseBrush {
	@Configuration
	public SimpleBooleanProperty useColor = new SimpleBooleanProperty();

	@Configuration
	public SimpleObjectProperty<Integer> paletteOffset = new SimpleObjectProperty<>(1);
}
