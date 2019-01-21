package com.zarbosoft.shoedemo.config;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.shoedemo.ConfigBase;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import static com.zarbosoft.shoedemo.Main.appDirs;

@Configuration
public class GlobalConfig extends ConfigBase {
	@Configuration
	public final SimpleIntegerProperty trueColorBrush = new SimpleIntegerProperty();

	@Configuration
	public final ObservableList<TrueColorBrush> trueColorBrushes = FXCollections.observableArrayList();

	@Configuration
	public String lastDir = appDirs.user_dir().toString();

	@Configuration
	public int maxUndo = 1000;

	@Configuration
	public int tileSize = 32;
}
