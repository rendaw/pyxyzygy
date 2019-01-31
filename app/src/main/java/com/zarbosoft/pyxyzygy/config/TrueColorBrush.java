package com.zarbosoft.pyxyzygy.config;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pyxyzygy.Brush;
import javafx.beans.property.*;

@Configuration(name = "true-color-brush")
public class TrueColorBrush extends Brush {
	/**
	 * Size in pixels = `size / 10`
	 */
	@Configuration
	public SimpleIntegerProperty size = new SimpleIntegerProperty();

	@Configuration
	public SimpleStringProperty name = new SimpleStringProperty();

	@Configuration
	public SimpleObjectProperty<TrueColor> color = new SimpleObjectProperty<>();

	@Configuration
	public SimpleBooleanProperty useColor = new SimpleBooleanProperty();

	@Configuration
	public SimpleIntegerProperty blend = new SimpleIntegerProperty();
}
