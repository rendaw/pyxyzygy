package com.zarbosoft.shoedemo;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.appdirsj.AppDirs;
import com.zarbosoft.luxem.extra.SimpleKVStore;
import com.zarbosoft.shoedemo.model.ImageFrame;
import com.zarbosoft.shoedemo.model.ImageNode;
import com.zarbosoft.shoedemo.model.ProjectNode;
import com.zarbosoft.shoedemo.model.Vector;
import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import static com.zarbosoft.shoedemo.Window.uniqueName;

public class Main extends Application {
	public static AppDirs appDirs = new AppDirs().set_appname("shoedemo2").set_appauthor("zarbosoft");
	public static SimpleKVStore settings = new SimpleKVStore(appDirs.user_config_dir().resolve("settings.luxem"));
	public final static String SETTING_LAST_DIR = "lastdir";
	public final static String SETTING_MAX_UNDO = "maxundo";
	public final static int opacityMax = 1000;

	public static void main(String[] args) {
		Main.launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Shoe Demo 2");
		Path path;
		if (getParameters().getUnnamed().isEmpty()) {
			DirectoryChooser chooser = new DirectoryChooser();
			chooser.setInitialDirectory(new File(settings.get(SETTING_LAST_DIR).orElse(appDirs.user_dir().toString())));
			chooser.setTitle("Select a new or existing project!");
			File result = chooser.showDialog(null);
			if (result == null)
				return;
			path = result.toPath();
			settings.set(SETTING_LAST_DIR, path.getParent().toAbsolutePath().toString());
		} else {
			path = Paths.get(this.getParameters().getUnnamed().get(0));
		}
		ProjectContext context;
		if (Files.list(path).findAny().isPresent()) {
			context = ProjectContext.deserialize(path);
		} else {
			context = ProjectContext.create(path);
			ImageNode imageNode = ImageNode.create(context);
			imageNode.initialNameSet(context, uniqueName("Image"));
			imageNode.initialOpacitySet(context, opacityMax);
			ImageFrame imageFrame = ImageFrame.create(context);
			imageFrame.initialLengthSet(context, -1);
			imageFrame.initialOffsetSet(context, new Vector(0, 0));
			imageNode.initialFramesAdd(context, ImmutableList.of(imageFrame));
			context.history.change(c -> c.project(context.project).topAdd(imageNode));
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
				builder.slider("Opacity", 0, 100000, slider -> {
					slider.setValue(node.opacity());
					Main.<DoubleProperty, Number>bind(
							slider.valueProperty(),
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
}
