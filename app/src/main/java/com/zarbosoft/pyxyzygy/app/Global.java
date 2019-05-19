package com.zarbosoft.pyxyzygy.app;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.appdirsj.AppDirs;
import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.core.model.v0.PaletteImageLayer;
import com.zarbosoft.pyxyzygy.core.model.v0.Project;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectLayer;
import com.zarbosoft.pyxyzygy.seed.deserialize.ModelDeserializationContext;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext.countUniqueName;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public class Global {
	public final static int opacityMax = 1000;
	public final static int blendMax = 1000;
	public final static int NO_LOOP = 0;
	public final static int NO_LENGTH = -1;
	public final static int NO_INNER = -1;
	public static final Hotkeys.Hotkey cutHotkey = Hotkeys.Hotkey.create(KeyCode.X, true, false, false);
	public static final Hotkeys.Hotkey pasteHotkey = Hotkeys.Hotkey.create(KeyCode.V, true, false, false);
	public static final Hotkeys.Hotkey copyHotkey = Hotkeys.Hotkey.create(KeyCode.C, true, false, false);
	public static final String nameSymbol = "pyxyzygy";
	public static final AppDirs appDirs = new AppDirs().set_appname(nameSymbol).set_appauthor("zarbosoft");
	public static final Path configDir = appDirs.user_config_dir();
	public static final Path configPath = configDir.resolve("config.luxem");
	public static final String nameHuman = "pyxyzygy";

	public static Logger logger;
	public final static String trueColorLayerName = "True color layer";
	public final static String paletteName = "Palette";
	public final static String paletteLayerName = "Palette layer";
	public final static String groupLayerName = "Group";

	public static List<Runnable> shutdown = new ArrayList<>();
	public static boolean fixedProfile = false;
	public static boolean fixedProject = false;

	public static void shutdown() {
		for (Runnable s : shutdown) s.run();
		shutdown.clear();
		logger.flush();
		Platform.exit();
	}

	public static ProjectContext create(Path path, int tileSize) {
		ProjectContext out = new ProjectContext(path);
		out.tileSize = tileSize;
		out.project = Project.create(out);
		out.history = new History(out, ImmutableList.of(), ImmutableList.of(), null);
		out.hotkeys = new Hotkeys();
		out.initConfig();
		return out;
	}

	public static com.zarbosoft.pyxyzygy.seed.model.ProjectContext deserialize(Path path) {
		return uncheck(() -> {
			ModelDeserializationContext context = new ModelDeserializationContext();
			ProjectContext out;
			try (InputStream source = Files.newInputStream(projectPath(path))) {
				out = (ProjectContext) new StackReader().read(source, new StackReader.ArrayState() {
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
								throw new IllegalStateException(String.format("Unknown project version [%s]", type));
						}
					}
				}).get(0);
			}
			context.finishers.forEach(finisher -> finisher.finish(context));
			out.project = (Project) context.objectMap
					.values()
					.stream()
					.filter(o -> o instanceof Project)
					.findFirst()
					.get();
			context.objectMap
					.values()
					.stream()
					.filter(o -> o instanceof ProjectLayer)
					.forEach(o -> countUniqueName(((ProjectLayer) o).name()));
			for (Object o : context.objectMap.values()) {
				// Default values in new fields
				if (o instanceof ProjectLayer && ((ProjectLayer) o).offset() == null) {
					((ProjectLayer) o).forceInitialOffsetSet(Vector.ZERO);
				}
			}
			out.history = new History(out, context.undoHistory, context.redoHistory, context.activeChange);
			out.hotkeys = new Hotkeys();
			out.initConfig();
			out.debugCheckRefs();
			context.objectMap.values().forEach(n -> {
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
}
