package com.zarbosoft.pyxyzygy.app.config;

import com.zarbosoft.interface1.Configuration;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

@Configuration
public class BaseBrush {
	@Configuration
	public SimpleIntegerProperty size = new SimpleIntegerProperty();

	public double sizeInPixels() {
		return size.get() / 10.0;
	}

	@Configuration
	public SimpleStringProperty name = new SimpleStringProperty();
}
