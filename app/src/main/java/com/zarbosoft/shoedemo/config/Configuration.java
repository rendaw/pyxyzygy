package com.zarbosoft.shoedemo.config;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

import static com.zarbosoft.shoedemo.Main.appDirs;

@com.zarbosoft.interface1.Configuration
public class Configuration {
	@com.zarbosoft.interface1.Configuration
	public final SimpleIntegerProperty trueColorBrush = new SimpleIntegerProperty();

	@com.zarbosoft.interface1.Configuration
	public final SimpleObjectProperty<TrueColor> trueColor = new SimpleObjectProperty<>();

	@com.zarbosoft.interface1.Configuration
	public final ObservableList<TrueColorBrush> trueColorBrushes = FXCollections.observableArrayList();

	@com.zarbosoft.interface1.Configuration
	public String lastDir = appDirs.user_dir().toString();

	@com.zarbosoft.interface1.Configuration
	public int maxUndo = 1000;
}
