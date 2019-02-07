package com.zarbosoft.pyxyzygy.app.config;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.ProjectContext;
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
	public final SimpleObjectProperty<DoubleVector> scroll = new SimpleObjectProperty<>(new DoubleVector(0, 0));

	@Configuration
	public final SimpleBooleanProperty onionSkin = new SimpleBooleanProperty(false);

	public NodeConfig(ProjectContext context) {
		zoom.set(context.config.defaultZoom);
	}

	public NodeConfig() {

	}
}
