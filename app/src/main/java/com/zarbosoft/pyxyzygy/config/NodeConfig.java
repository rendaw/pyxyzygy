package com.zarbosoft.pyxyzygy.config;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pyxyzygy.DoubleVector;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;

@Configuration
public class NodeConfig {
	@Configuration
	public final SimpleIntegerProperty frame = new SimpleIntegerProperty(0);

	@Configuration
	public final SimpleBooleanProperty flipHorizontal = new SimpleBooleanProperty(false);

	@Configuration
	public final SimpleBooleanProperty flipVertical = new SimpleBooleanProperty(false);

	@Configuration
	public final SimpleIntegerProperty zoom = new SimpleIntegerProperty(0);

	@Configuration
	public final SimpleObjectProperty<DoubleVector> scroll = new SimpleObjectProperty<>(new DoubleVector(0,0 ));
}
