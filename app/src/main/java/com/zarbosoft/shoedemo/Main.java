package com.zarbosoft.shoedemo;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.appdirsj.AppDirs;
import com.zarbosoft.luxem.extra.SimpleKVStore;
import com.zarbosoft.shoedemo.model.Dirtyable;
import com.zarbosoft.shoedemo.model.ImageFrame;
import com.zarbosoft.shoedemo.model.ImageNode;
import com.zarbosoft.shoedemo.model.Vector;
import javafx.application.Application;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.zarbosoft.rendaw.common.Common.uncheck;

public class Main extends Application {
	public static AppDirs appDirs = new AppDirs().set_appname("shoedemo2").set_appauthor("zarbosoft");
	public static SimpleKVStore settings = new SimpleKVStore(appDirs.user_config_dir().resolve("settings.luxem"));
	public final static String SETTING_LAST_DIR = "lastdir";
	public final static String SETTING_MAX_UNDO = "maxundo";

	public static void main(String[] args) {
		Main.launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		primaryStage.setTitle("Shoe Demo 2");
		Path path;
		if (getParameters().getUnnamed().isEmpty()) {
			DirectoryChooser chooser = new DirectoryChooser();
			chooser.setInitialDirectory(new File(settings
					.get(SETTING_LAST_DIR)
					.orElse(appDirs.user_dir().toString())));
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
			imageNode.initialNameSet(context, "Image");
			ImageFrame imageFrame = ImageFrame.create(context);
			imageFrame.initialLengthSet(context, -1);
			imageFrame.initialOffsetSet(context, new Vector(0, 0));
			imageNode.initialFramesAdd(context, ImmutableList.of(imageFrame));
			context.history.change(c -> c.project(context.project).topAdd(imageNode));
			context.history.finishChange();
		}
		new Window().start(context, primaryStage);
	}

}
