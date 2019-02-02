package com.zarbosoft.pyxyzygy.config;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pyxyzygy.ConfigBase;
import com.zarbosoft.pyxyzygy.Hotkeys;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.HashMap;
import java.util.Map;

import static com.zarbosoft.pyxyzygy.Main.appDirs;

@Configuration
public class GlobalConfig extends ConfigBase {
	@Configuration
	public final ObservableList<TrueColorBrush> trueColorBrushes = FXCollections.observableArrayList();

	@Configuration
	public String lastDir = appDirs.user_dir().toString();

	@Configuration
	public int maxUndo = 1000;

	@Configuration
	public Map<String, Hotkeys.Hotkey> hotkeys = new HashMap<>();

	@Configuration(optional = true)
	public CreateMode newProjectNormalMode = CreateMode.normal;

	@Configuration(optional = true)
	public final SimpleBooleanProperty showOrigin = new SimpleBooleanProperty(false);
}
