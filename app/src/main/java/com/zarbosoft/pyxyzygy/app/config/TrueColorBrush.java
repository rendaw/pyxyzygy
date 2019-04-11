package com.zarbosoft.pyxyzygy.app.config;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pyxyzygy.seed.model.v0.TrueColor;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

@Configuration(name = "true-color-brush")
public class TrueColorBrush extends BaseBrush{
	@Configuration
	public SimpleObjectProperty<TrueColor> color = new SimpleObjectProperty<>();

	@Configuration
	public SimpleBooleanProperty useColor = new SimpleBooleanProperty();

	@Configuration
	public SimpleIntegerProperty blend = new SimpleIntegerProperty();

	@Configuration(optional = true)
	public SimpleBooleanProperty hard = new SimpleBooleanProperty(false);

	@Configuration(optional = true)
	public SimpleBooleanProperty aligned = new SimpleBooleanProperty(false);
}
