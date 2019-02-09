package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.appdirsj.AppDirs;

import java.nio.file.Path;

public class Global {
	public final static int opacityMax = 1000;
	public final static int blendMax = 1000;
	public final static int NO_LOOP = 0;
	public final static int NO_LENGTH = -1;
	public final static int NO_INNER = -1;
	public static String nameSymbol = "pyxyzygy";
	public static AppDirs appDirs = new AppDirs().set_appname(nameSymbol).set_appauthor("zarbosoft");
	public static final Path configDir = appDirs.user_config_dir();
	public static final Path configPath = configDir.resolve("config.luxem");
	public static String nameHuman = "pyxyzygy";
}
