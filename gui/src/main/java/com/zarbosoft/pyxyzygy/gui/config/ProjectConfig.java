package com.zarbosoft.pyxyzygy.gui.config;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pyxyzygy.gui.ConfigBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class ProjectConfig extends ConfigBase {
	@Configuration
	public final SimpleObjectProperty<TrueColor> backgroundColor =
			new SimpleObjectProperty<>(TrueColor.fromJfx(Color.WHITE));

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
	public List<Integer> viewPath = new ArrayList<>();

	@Configuration(optional = true)
	public List<Integer> editPath = new ArrayList<>();
}
