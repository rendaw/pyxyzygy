package com.zarbosoft.shoedemo;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.appdirsj.AppDirs;
import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.luxem.Luxem;
import com.zarbosoft.luxem.read.ReadTypeGrammar;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.shoedemo.config.Configuration;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zarbosoft.rendaw.common.Common.atomicWrite;
import static com.zarbosoft.rendaw.common.Common.uncheck;
import static com.zarbosoft.shoedemo.ProjectContext.uniqueName;

public class Main extends Application {
	public static AppDirs appDirs = new AppDirs().set_appname("shoedemo2").set_appauthor("zarbosoft");
	public static final Path configDir = appDirs.user_config_dir();
	public static final Path configPath = configDir.resolve("config.luxem");
	public final static String SETTING_LAST_DIR = "lastdir";
	public final static String SETTING_MAX_UNDO = "maxundo";
	public final static int opacityMax = 1000;
	public final static int blendMax = 1000;

	public final static int NO_LOOP = 0;
	public final static int NO_LENGTH = -1;
	public final static int NO_INNER = -1;

	public final static Timer configFlushTimer = new Timer("config-flush", true);
	public static Configuration config;
	public static ReadTypeGrammar.TypeMap configTypeMap = new ReadTypeGrammar.TypeMap()
			.add(new TypeInfo(SimpleIntegerProperty.class), new ReadTypeGrammar.TypeMapEntry<SimpleIntegerProperty>() {
				@Override
				public void setIn(Object object, TypeInfo field, Object value) {
					uncheck(() -> ((SimpleIntegerProperty) field.field.get(object)).set((Integer) value));
				}

				@Override
				public Object convertOut(SimpleIntegerProperty source) {
					return source.get();
				}

				@Override
				public TypeInfo serializedType(TypeInfo info) {
					return new TypeInfo(info.field, int.class);
				}
			})
			.add(new TypeInfo(SimpleStringProperty.class), new ReadTypeGrammar.TypeMapEntry<SimpleStringProperty>() {
				@Override
				public void setIn(Object object, TypeInfo field, Object value) {
					uncheck(() -> ((SimpleStringProperty) field.field.get(object)).set((String) value));
				}

				@Override
				public Object convertOut(SimpleStringProperty source) {
					return source.get();
				}

				@Override
				public TypeInfo serializedType(TypeInfo info) {
					return new TypeInfo(info.field, String.class);
				}
			})
			.add(new TypeInfo(SimpleBooleanProperty.class), new ReadTypeGrammar.TypeMapEntry<SimpleBooleanProperty>() {
				@Override
				public void setIn(Object object, TypeInfo field, Object value) {
					uncheck(() -> ((SimpleBooleanProperty) field.field.get(object)).set((Boolean) value));
				}

				@Override
				public Object convertOut(SimpleBooleanProperty source) {
					return source.get();
				}

				@Override
				public TypeInfo serializedType(TypeInfo info) {
					return new TypeInfo(info.field, Boolean.class);
				}
			})
			.add(new TypeInfo(SimpleObjectProperty.class), new ReadTypeGrammar.TypeMapEntry<SimpleObjectProperty>() {
				@Override
				public void setIn(Object object, TypeInfo field, Object value) {
					uncheck(() -> ((SimpleObjectProperty) field.field.get(object)).set(value));
				}

				@Override
				public Object convertOut(SimpleObjectProperty source) {
					return source.get();
				}

				@Override
				public TypeInfo serializedType(TypeInfo info) {
					return new TypeInfo(info.field, info.parameters[0].type, info.parameters[0].parameters);
				}
			}).add(new TypeInfo(ObservableList.class), new ReadTypeGrammar.TypeMapEntry() {
				@Override
				public void setIn(Object object, TypeInfo field, Object value) {
					uncheck(() -> ((ObservableList) field.field.get(object)).addAll((Collection)value));
				}

				@Override
				public TypeInfo serializedType(TypeInfo info) {
					return new TypeInfo(info.field, ArrayList.class, info.parameters);
				}
			});

	static {
		uncheck(() -> {
			Files.createDirectories(configDir);
			try (InputStream source = Files.newInputStream(configPath)) {
				config = Luxem
						.parse(null, new TypeInfo(Configuration.class))
						.map(configTypeMap)
						.from(source)
						.map(o -> (Configuration) o)
						.findFirst()
						.get();
			} catch (NoSuchFileException e) {
				config = new Configuration();
				config.trueColor.set(TrueColor.fromJfx(Color.BLACK));
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
				config.trueColorBrush.set(1);
			}
		});
		configFlushTimer.scheduleAtFixedRate(new TimerTask() {
			Logger logger = LoggerFactory.getLogger("config-flush");
			@Override
			public void run() {
				Platform.runLater(() -> {
					try {
						flushConfig();
					} catch (Exception e) {
						logger.error("Error flushing config",e );
					}
				});
			}
		}, Duration.ofMinutes(5).toMillis(), Duration.ofMinutes(5).toMillis());
	}

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

	@Override
	public void start(Stage primaryStage) throws Exception {
		new mynativeJNI();
		primaryStage.setTitle("Shoe Demo 2");
		Path path;
		if (getParameters().getUnnamed().isEmpty()) {
			DirectoryChooser chooser = new DirectoryChooser();
			chooser.setInitialDirectory(new File(config.lastDir));
			chooser.setTitle("Select a new or existing project!");
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
							List<R> removing = target.subList(c.getFrom(), c.getTo());
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

	public static void shutdown() {
		configFlushTimer.cancel();
	}

	public static void flushConfig() {
		System.out.format("flushing config\n");
		atomicWrite(configPath, stream -> {
			Luxem.write(config).map(configTypeMap).pretty().toStream(stream);
		});
		System.out.format("flushing config DONE\n");
	}
}
