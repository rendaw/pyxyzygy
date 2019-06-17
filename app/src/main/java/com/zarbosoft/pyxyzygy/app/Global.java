package com.zarbosoft.pyxyzygy.app;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.appdirsj.AppDirs;
import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.core.model.v0.Palette;
import com.zarbosoft.pyxyzygy.core.model.v0.PaletteImageLayer;
import com.zarbosoft.pyxyzygy.core.model.v0.Project;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectLayer;
import com.zarbosoft.pyxyzygy.seed.deserialize.ModelDeserializationContext;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static com.zarbosoft.rendaw.common.Common.uncheck;

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

  public static Logger logger;

  public static List<Runnable> shutdown = new ArrayList<>();
  public static boolean fixedProfile = false;
  public static boolean fixedProject = false;
  public static ResourceBundle localization;

  static {
    // Load localization
    System.setProperty("java.util.PropertyResourceBundle.encoding","UTF-8"); // Just in case?
    {
      Locale locale1 = Locale.getDefault();
      String locale0 = String.format("%s-%s", locale1.getLanguage(), locale1.getCountry());
      String[] localeParts = locale0.split("-");
      Locale locale = new Locale(localeParts[0], localeParts[1]);
      try {
        localization =
          ResourceBundle.getBundle(
            "com.zarbosoft.pyxyzygy.app.i18n.messages", locale);
      } catch (MissingResourceException e) {
        localization =
          ResourceBundle.getBundle(
            "com.zarbosoft.pyxyzygy.app.i18n.messages",
            new Locale("en", "US"));
      }
    }
  }

  public static void shutdown() {
    for (Runnable s : shutdown) s.run();
    shutdown.clear();
    logger.flush();
    Platform.exit();
  }

  public static ProjectContext create(
      Path path, int tileSize, Consumer<ProjectContext> initialize) {
    ProjectContext out = new ProjectContext(path);
    out.history = new History(out, ImmutableList.of(), ImmutableList.of(), null);
    out.hotkeys = new Hotkeys();
    out.initConfig();
    out.tileSize = tileSize;
    out.project = Project.create(out);
    initialize.accept(out);
    out.project.incRef(out); // Never delete project
    out.setDirty(out.history.changeStep);
    out.setDirty(out);
    return out;
  }

  public static com.zarbosoft.pyxyzygy.seed.model.ProjectContext deserialize(Path path) {
    return uncheck(
        () -> {
          ModelDeserializationContext context = new ModelDeserializationContext();
          ProjectContext out;
          try (InputStream source = Files.newInputStream(projectPath(path))) {
            out =
                (ProjectContext)
                    new StackReader()
                        .read(
                            source,
                            new StackReader.ArrayState() {
                              @Override
                              public StackReader.State array() {
                                throw new IllegalStateException("Project data should be a record.");
                              }

                              @Override
                              public void value(Object value) {
                                data.add(value);
                              }

                              @Override
                              public StackReader.State record() {
                                if (type == null)
                                  throw new IllegalStateException("Project has no version");
                                switch (type) {
                                  case ProjectContext.version:
                                    return new ProjectContext.Deserializer(context, path);
                                  default:
                                    throw new IllegalStateException(
                                        String.format("Unknown project version [%s]", type));
                                }
                              }
                            })
                        .get(0);
          }
          context.finishers.forEach(finisher -> finisher.finish(context));
          out.project =
              (Project)
                  context.objectMap.values().stream()
                      .filter(o -> o instanceof Project)
                      .findFirst()
                      .get();
          context.objectMap.values().stream()
              .filter(o -> o instanceof ProjectLayer)
              .forEach(o -> out.namer.countUniqueName(((ProjectLayer) o).name()));
          context.objectMap.values().stream()
              .filter(o -> o instanceof Palette)
              .forEach(o -> out.namer.countUniqueName(((Palette) o).name()));
          for (Object o : context.objectMap.values()) {
            // Default values in new fields
            if (o instanceof ProjectLayer && ((ProjectLayer) o).offset() == null) {
              ((ProjectLayer) o).forceInitialOffsetSet(Vector.ZERO);
            }
          }
          out.history =
              new History(out, context.undoHistory, context.redoHistory, context.activeChange);
          out.hotkeys = new Hotkeys();
          out.initConfig();
          boolean fixed = out.debugCheckRefsFix();
          if (fixed) {
            Alert alert =
                new Alert(
                    Alert.AlertType.ERROR,
                    String.format(
                      localization.getString(
                        "there.were.one.or.more.errors.while.opening.the.project.n.n.s.attempted.to.fix.them.but.in.case.it.did.so.incorrectly.we.recommend.you.back.up.the.project.and.re.open.it"),
                      getNameHuman()
                    ),
                    ButtonType.OK);
            alert.setTitle(String.format(localization.getString("s.error"), Global.getNameHuman()));
            alert.setHeaderText(localization.getString("error.opening.project"));
            alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
            alert.showAndWait();
          }
          out.debugCheckRefs();
          context
              .objectMap
              .values()
              .forEach(
                  n -> {
                    if (n instanceof PaletteImageLayer) {
                      out.addPaletteUser((PaletteImageLayer) n);
                    }
                  });
          return out;
        });
  }

  public static Path projectPath(Path base) {
    return base.resolve("project.luxem");
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
