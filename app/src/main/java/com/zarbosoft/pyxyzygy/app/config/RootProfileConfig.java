package com.zarbosoft.pyxyzygy.app.config;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pyxyzygy.app.ConfigBase;
import com.zarbosoft.pyxyzygy.app.Hotkeys;
import com.zarbosoft.pyxyzygy.seed.model.v0.TrueColor;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

import java.util.HashMap;
import java.util.Map;

import static com.zarbosoft.pyxyzygy.app.Global.appDirs;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.c;

@Configuration
public class RootProfileConfig extends ConfigBase {
	@Configuration
	public final ObservableList<TrueColorBrush> trueColorBrushes = FXCollections.observableArrayList();

	@Configuration
	public final ObservableList<PaletteBrush> paletteBrushes = FXCollections.observableArrayList();

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

	@Configuration(optional = true)
	public final SimpleBooleanProperty showTimeline = new SimpleBooleanProperty(true);

	@Configuration(optional = true)
	public final SimpleObjectProperty<TrueColor> backgroundColor =
			new SimpleObjectProperty<>(TrueColor.fromJfx(Color.WHITE));

	@Configuration(optional = true)
	public final SimpleObjectProperty<TrueColor> onionSkinColor =
			new SimpleObjectProperty<>(TrueColor.fromJfx(c(new java.awt.Color(0f, 0f, 1f, 0.5f))));

}
