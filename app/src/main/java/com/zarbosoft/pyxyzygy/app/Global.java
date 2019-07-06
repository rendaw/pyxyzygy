package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.appdirsj.AppDirs;
import com.zarbosoft.automodel.lib.Logger;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static com.zarbosoft.automodel.lib.Logger.logger;

public class Global {
  public static final int opacityMax = 1000;
  public static final int blendMax = 1000;
  public static final int NO_LOOP = 0;
  public static final int NO_LENGTH = -1;
  public static final int NO_INNER = -1;
  public static final Hotkeys.Hotkey cutHotkey =
      Hotkeys.Hotkey.create(KeyCode.X, true, false, false);
  public static final Hotkeys.Hotkey pasteHotkey =
      Hotkeys.Hotkey.create(KeyCode.V, true, false, false);
  public static final Hotkeys.Hotkey copyHotkey =
      Hotkeys.Hotkey.create(KeyCode.C, true, false, false);
  public static final String nameSymbol = "pyxyzygy";
  public static final AppDirs appDirs =
      new AppDirs().set_appname(nameSymbol).set_appauthor("zarbosoft");
  public static final Path configDir = appDirs.user_config_dir();
  public static final Path configPath = configDir.resolve("config.luxem");
  static final String nameHuman = "pyxyzygy";

  public static List<Runnable> shutdown = new ArrayList<>();
  public static boolean fixedProfile = false;
  public static boolean fixedProject = false;
  public static ResourceBundle localization;

  static {
    // Load localization
    System.setProperty("java.util.PropertyResourceBundle.encoding", "UTF-8"); // Just in case?
    {
      Locale locale1 = Locale.getDefault();
      String locale0 = String.format("%s-%s", locale1.getLanguage(), locale1.getCountry());
      String[] localeParts = locale0.split("-");
      Locale locale = new Locale(localeParts[0], localeParts[1]);
      try {
        localization = ResourceBundle.getBundle("com.zarbosoft.pyxyzygy.app.i18n.messages", locale);
      } catch (MissingResourceException e) {
        localization =
            ResourceBundle.getBundle(
                "com.zarbosoft.pyxyzygy.app.i18n.messages", new Locale("en", "US"));
      }
    }
  }

  public static void shutdown() {
    for (Runnable s : shutdown) s.run();
    shutdown.clear();
    logger.flush();
    Platform.exit();
  }

  public static String getNameHuman() {
    return nameHuman;
  }

  public static String getTrueColorLayerName() {
    return localization.getString("true.color.layer");
  }

  public static String getPaletteName() {
    return localization.getString("palette");
  }

  public static String getPaletteLayerName() {
    return localization.getString("palette.layer");
  }

  public static String getGroupLayerName() {
    return localization.getString("group");
  }
}
