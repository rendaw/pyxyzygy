package com.zarbosoft.shoedemo.config;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.shoedemo.ConfigBase;
import com.zarbosoft.shoedemo.Hotkeys;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.HashMap;
import java.util.Map;

import static com.zarbosoft.shoedemo.Main.appDirs;

@Configuration
public class GlobalConfig extends ConfigBase {
	@Configuration
	public final ObservableList<TrueColorBrush> trueColorBrushes = FXCollections.observableArrayList();

	@Configuration
	public String lastDir = appDirs.user_dir().toString();

	@Configuration
	public int maxUndo = 1000;

	@Configuration
	public int tileSize = 32;

	@Configuration
	public Map<String, Hotkeys.Hotkey> hotkeys = new HashMap<>();
}
