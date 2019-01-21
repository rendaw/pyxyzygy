package com.zarbosoft.shoedemo;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.appdirsj.AppDirs;
import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.shoedemo.config.GlobalConfig;
import com.zarbosoft.shoedemo.config.TrueColor;
import com.zarbosoft.shoedemo.config.TrueColorBrush;
import com.zarbosoft.shoedemo.model.ProjectNode;
import com.zarbosoft.shoedemo.model.TrueColorImageFrame;
import com.zarbosoft.shoedemo.model.TrueColorImageNode;
import com.zarbosoft.shoedemo.model.Vector;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zarbosoft.rendaw.common.Common.uncheck;
import static com.zarbosoft.shoedemo.ProjectContext.uniqueName;

public class Main extends Application {
	public static AppDirs appDirs = new AppDirs().set_appname("shoedemo2").set_appauthor("zarbosoft");
	public static final Path configDir = appDirs.user_config_dir();
	public static final Path configPath = configDir.resolve("config.luxem");
	public final static int opacityMax = 1000;
	public final static int blendMax = 1000;
	public static final GlobalConfig config = ConfigBase.deserialize(new TypeInfo(GlobalConfig.class),configDir ,() -> {
		GlobalConfig config = new GlobalConfig();
		TrueColorBrush transparentBrush = new TrueColorBrush();
		transparentBrush.name.set("Transparent");
		transparentBrush.size.set(50);
		transparentBrush.blend.set(blendMax);
		transparentBrush.color.set(TrueColor.fromJfx(Color.TRANSPARENT));
		transparentBrush.useColor.set(true);
		TrueColorBrush defaultBrush = new TrueColorBrush();
		defaultBrush.name.set("Default");
		defaultBrush.size.set(10);
		defaultBrush.blend.set(blendMax);
		defaultBrush.color.set(TrueColor.fromJfx(Color.BLACK));
		defaultBrush.useColor.set(true);
		config.trueColorBrushes.addAll(transparentBrush, defaultBrush);
		return config;
	});

	public final static int NO_LOOP = 0;
	public final static int NO_LENGTH = -1;
	public final static int NO_INNER = -1;


	public static void main(String[] args) {
		Main.launch(args);
	}

	public static void moveTo(List list, int source, int count, int dest) {
		if (list.get(0) instanceof Wrapper)
			throw new Assertion(); // DEBUG
		List temp0 = list.subList(source, source + count);
		List temp1 = new ArrayList(temp0);
		temp0.clear();
		list.addAll(dest, temp1);
	}

	public static void moveWrapperTo(List<Wrapper> list, int source, int count, int dest) {
		List temp0 = list.subList(source, source + count);
		List temp1 = new ArrayList(temp0);
		temp0.clear();
		list.addAll(dest, temp1);
		for (int i = Math.min(source, dest); i < list.size(); ++i) {
			list.get(i).parentIndex = i;
		}
	}

	public static void shutdown() {
		config.shutdown();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		new mynativeJNI();
		primaryStage.setTitle("Shoe Demo 2");
		Path path;
		if (getParameters().getUnnamed().isEmpty()) {
			DirectoryChooser chooser = new DirectoryChooser();
			chooser.setInitialDirectory(new File(config.lastDir));
			chooser.setTitle("Shoe Demo 2 - Select new or existing project");
			File result = chooser.showDialog(null);
			if (result == null) {
				shutdown();
				Platform.exit();
				return;
			}
			path = result.toPath();
			config.lastDir = path.getParent().toAbsolutePath().toString();
		} else {
			path = Paths.get(this.getParameters().getUnnamed().get(0));
		}
		ProjectContext context;
		if (Files.list(path).findAny().isPresent()) {
			context = ProjectContext.deserialize(path);
		} else {
			context = ProjectContext.create(path);
			TrueColorImageNode trueColorImageNode = TrueColorImageNode.create(context);
			trueColorImageNode.initialNameSet(context, uniqueName("Image"));
			trueColorImageNode.initialOpacitySet(context, opacityMax);
			TrueColorImageFrame trueColorImageFrame = TrueColorImageFrame.create(context);
			trueColorImageFrame.initialLengthSet(context, -1);
			trueColorImageFrame.initialOffsetSet(context, new Vector(0, 0));
			trueColorImageNode.initialFramesAdd(context, ImmutableList.of(trueColorImageFrame));
			context.history.change(c -> c.project(context.project).topAdd(trueColorImageNode));
			context.history.finishChange();
		}
		new Window().start(context, primaryStage);
	}

	public static Runnable nodeFormFields(ProjectContext context, WidgetFormBuilder builder, ProjectNode node) {
		return new Runnable() {
			private ProjectNode.NameSetListener nameSetListener;
			private ProjectNode.OpacitySetListener opacitySetListener;

			{
				builder.text("Name", t -> {
					t.setText(node.name());
					Main.<StringProperty, String>bind(t.textProperty(),
							v -> context.history.change(c -> c.projectNode(node).nameSet(v)),
							setter -> nameSetListener = node.addNameSetListeners((target, value) -> {
								setter.accept(value);
							})
					);
				});
				builder.slider("Opacity", 0, opacityMax, slider -> {
					slider.setValue(node.opacity());
					Main.<DoubleProperty, Number>bind(slider.valueProperty(),
							v -> context.history.change(c -> c.projectNode(node).opacitySet(v.intValue())),
							setter -> opacitySetListener = node.addOpacitySetListeners((target, value) -> {
								setter.accept(value);
							})
					);
				});
			}

			@Override
			public void run() {
				node.removeNameSetListeners(nameSetListener);
				node.removeOpacitySetListeners(opacitySetListener);
			}
		};
	}

	public static <T extends Property, V> void bind(T prop, Consumer<V> setter, Consumer<Consumer<V>> listen) {
		new Object() {
			boolean blockValueUpdate = false;
			boolean blockWidgetUpdate = false;

			{
				prop.addListener((observable, oldValue, newValue) -> {
					if (blockWidgetUpdate)
						return;
					blockValueUpdate = true;
					try {
						setter.accept((V) newValue);
					} finally {
						blockValueUpdate = false;
					}
				});
				listen.accept(v -> {
					if (blockValueUpdate)
						return;
					blockWidgetUpdate = true;
					try {
						prop.setValue(v);
					} finally {
						blockWidgetUpdate = false;
					}
				});
			}
		};
	}

	public static <T, R> Runnable mirror(
			ObservableList<T> source, List<R> target, Function<T, R> add, Consumer<R> remove, Consumer<Integer> update
	) {
		return new Runnable() {
			private ListChangeListener<T> listener;
			private boolean dead = false;

			{
				listener = c -> {
					if (dead)
						return;
					while (c.next()) {
						if (c.wasAdded()) {
							target.addAll(c.getFrom(),
									c.getAddedSubList().stream().map(add).collect(Collectors.toList())
							);
						} else if (c.wasRemoved()) {
							List<R> removing = target.subList(c.getFrom(), c.getFrom() + c.getRemovedSize());
							removing.forEach(remove);
							removing.clear();
						} else if (c.wasPermutated()) {
							throw new Assertion();
						} else if (c.wasUpdated()) {
							throw new Assertion();
						}
						update.accept(c.getFrom());
					}
				};
				source.addListener(listener);
				target.addAll(source.stream().map(add).collect(Collectors.toList()));
				update.accept(0);
			}

			@Override
			public void run() {
				dead = true;
				source.removeListener(listener);
			}
		};
	}

	public static <T> Consumer<T> noopConsumer() {
		return t -> {
		};
	}
}
