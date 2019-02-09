package com.zarbosoft.pyxyzygy.app;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.appdirsj.AppDirs;
import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.core.model.v0.Project;
import com.zarbosoft.pyxyzygy.seed.deserialize.ModelDeserializationContext;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.zarbosoft.rendaw.common.Common.uncheck;

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

	public static ProjectContext create(Path path, int tileSize) {
		ProjectContext out = new ProjectContext(path);
		out.tileSize = tileSize;
		out.project = Project.create(out);
		out.history = new History(out, ImmutableList.of(), ImmutableList.of(), null);
		out.hotkeys = new Hotkeys();
		out.initConfig();
		return out;
	}

	public static ProjectContext deserialize(Path path) {
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
						data.add((ProjectContext) ((com.zarbosoft.pyxyzygy.seed.model.ProjectContext) value).migrate());
					}

					@Override
					public StackReader.State record() {
						if (type == null) throw new IllegalStateException("Project has no version");
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
			context.objectMap
					.values()
					.stream()
					.filter(o -> o instanceof Project)
					.findFirst()
					.ifPresent(p -> out.project = (Project) p);
			out.history = new History(out, context.undoHistory, context.redoHistory, context.activeChange);
			out.hotkeys = new Hotkeys();
			out.initConfig();
			out.debugCheckRefCounts();
			return out;
		});
	}

	public static Path projectPath(Path base) {
		return base.resolve("project.luxem");
	}
}
