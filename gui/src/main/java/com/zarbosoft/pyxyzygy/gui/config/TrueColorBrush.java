package com.zarbosoft.pyxyzygy.gui.config;

import com.zarbosoft.interface1.Configuration;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

@Configuration(name = "true-color-brush")
public class TrueColorBrush {
	@Configuration
	public SimpleIntegerProperty size = new SimpleIntegerProperty();

	public double sizeInPixels() {
		return size.get() / 10.0;
	}

	@Configuration
	public SimpleStringProperty name = new SimpleStringProperty();

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
