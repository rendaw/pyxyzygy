package com.zarbosoft.pyxyzygy.app.config;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pyxyzygy.app.ConfigBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class RootProjectConfig extends ConfigBase {
	@Configuration
	public final SimpleObjectProperty<TrueColor> backgroundColor =
			new SimpleObjectProperty<>(TrueColor.fromJfx(Color.WHITE));

	@Configuration(optional = true)
	public final SimpleObjectProperty<TrueColor> onionSkinColor =
			new SimpleObjectProperty<>(TrueColor.fromJfx(Color.BLUE));

	@Configuration
	public final SimpleObjectProperty<TrueColor> trueColor = new SimpleObjectProperty<>(TrueColor.fromJfx(Color.BLACK));

	@Configuration
	public final ObservableMap<Long, NodeConfig> nodes = FXCollections.observableHashMap();

	@Configuration(optional = true)
	public double tabsSplit = 0.3;

	@Configuration(optional = true)
	public double timelineSplit = 0.7;

	@Configuration(optional = true)
	public int defaultZoom = 0;

	@Configuration(optional = true)
	public SimpleBooleanProperty maxCanvas = new SimpleBooleanProperty(false);

	@Configuration(optional = true)
	public List<Integer> viewPath = Arrays.asList(0);

	@Configuration(optional = true)
	public List<Integer> editPath = Arrays.asList(0);
}
