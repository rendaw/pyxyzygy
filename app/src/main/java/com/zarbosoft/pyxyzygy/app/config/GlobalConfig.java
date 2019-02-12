package com.zarbosoft.pyxyzygy.app.config;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pyxyzygy.app.ConfigBase;
import com.zarbosoft.pyxyzygy.app.Hotkeys;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.HashMap;
import java.util.Map;

import static com.zarbosoft.pyxyzygy.app.Global.appDirs;

@Configuration
public class GlobalConfig extends ConfigBase {
	@Configuration
	public final ObservableList<TrueColorBrush> trueColorBrushes = FXCollections.observableArrayList();

	@Configuration
	public String lastDir = appDirs.user_dir().toString();

	@Configuration(optional = true)
	public String importDir = appDirs.user_dir().toString();

	@Configuration
	public int maxUndo = 1000;

	@Configuration
	public Map<String, Hotkeys.Hotkey> hotkeys = new HashMap<>();

	@Configuration(optional = true)
	public CreateMode newProjectNormalMode = CreateMode.normal;

	@Configuration(optional = true)
	public final SimpleBooleanProperty showOrigin = new SimpleBooleanProperty(false);

	@Configuration(optional = true)
	public boolean maximize = true;
}
