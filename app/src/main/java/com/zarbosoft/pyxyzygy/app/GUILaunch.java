package com.zarbosoft.pyxyzygy.app;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Weigher;
import com.google.common.collect.ImmutableList;
import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.pyxyzygy.app.config.*;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.core.model.v0.*;
import com.zarbosoft.pyxyzygy.seed.model.v0.TrueColor;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.ChainComparator;
import com.zarbosoft.rendaw.common.Common;
import com.zarbosoft.rendaw.common.DeadCode;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zarbosoft.pyxyzygy.app.Global.*;
import static com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext.uniqueName;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.icon;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public class GUILaunch extends Application {
	public static RootProfileConfig profileConfig;
	public static RootGlobalConfig globalConfig;
	public static final List<Image> appIcons =
			Stream.of("appicon16.png", "appicon32.png", "appicon64.png").map(s -> icon(s)).collect(Collectors.toList());

	public final static int CACHE_OBJECT;
	public static Cache<Integer, Image> imageCache;

	static {
		int index = 0;
		CACHE_OBJECT = index++;
	}

	public GUILaunch() {
		try {
			// Hack because JavaFX was designed by sea sponges
			Class glassAppClass = getClass().getClassLoader().loadClass("com.sun.glass.ui.Application");
			Object glassApp = glassAppClass.getDeclaredMethod("GetApplication").invoke(null);
			glassApp.getClass().getMethod("setName", String.class).invoke(glassApp, nameHuman);
		} catch (Exception e) {
			logger.writeException(e, "Failed to set application name - ignore if not on Linux.");
		}
	}

	public static void main(String[] args) {
		launch(args);
	}

	public static class ProfileDialog extends Stage {
		Long profile;
		private final ListView<RootGlobalConfig.Profile> list;

		ProfileDialog(RootGlobalConfig profileConfig) {
			setTitle(String.format("%s - Choose profile", Global.nameHuman));
			getIcons().addAll(appIcons);

			list = new ListView<>();
			HBox.setHgrow(list, Priority.ALWAYS);
			list.setItems(FXCollections.observableList(profileConfig.profiles));
			list.setCellFactory(new Callback<ListView<RootGlobalConfig.Profile>, ListCell<RootGlobalConfig.Profile>>() {
				@Override
				public ListCell<RootGlobalConfig.Profile> call(ListView<RootGlobalConfig.Profile> param) {
					return new ListCell<>() {
						{
							setFocusTraversable(false);
						}

						@Override
						protected void updateItem(RootGlobalConfig.Profile item, boolean empty) {
							rebind(item);
							super.updateItem(item, empty);
						}

						private void rebind(RootGlobalConfig.Profile profile1) {
							if (profile1 == null) {
								textProperty().unbind();
								setText(null);
								setGraphic(null);
							} else {
								textProperty().bind(profile1.name);
								setGraphic(null);
							}
						}
					};
				}
			});
			profileConfig.profiles
					.stream()
					.filter(p -> p.id == profileConfig.lastId)
					.findFirst()
					.ifPresentOrElse(p -> list.getSelectionModel().select(p), () -> list.getSelectionModel().select(0));
			list.setFocusTraversable(false);

			Button newProfile = HelperJFX.button("plus.png", "New profile");
			newProfile.setFocusTraversable(false);
			newProfile.setOnAction(e -> {
				RootGlobalConfig.Profile profile1 = new RootGlobalConfig.Profile();
				profile1.name.set(uniqueName("New Profile"));
				profile1.id = profileConfig.nextId++;
				list.getItems().add(profile1);
				list.getSelectionModel().clearSelection();
				list.getSelectionModel().select(profile1);
			});
			Button deleteProfile = HelperJFX.button("minus.png", "Delete profile");
			deleteProfile.setFocusTraversable(false);
			deleteProfile.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
			deleteProfile.setOnAction(e -> {
				RootGlobalConfig.Profile profile1 = list.getSelectionModel().getSelectedItem();
				Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
				confirm.setTitle("Delete " + profile1.name);
				confirm.setHeaderText("Are you sure you want to delete profile " + profile1.name + "?");
				confirm.setContentText("Deleting the profile will also delete all its brushes.");
				Optional<ButtonType> result = confirm.showAndWait();
				if (!result.isPresent() || result.get() != ButtonType.OK) {
					return;
				}
				list.getItems().remove(profile1);
			});
			Button renameProfile = HelperJFX.button("textbox.png", "Rename profile");
			renameProfile.setFocusTraversable(false);
			renameProfile.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
			renameProfile.setOnAction(e -> {
				RootGlobalConfig.Profile profile1 = list.getSelectionModel().getSelectedItem();
				TextInputDialog dialog = new TextInputDialog(profile1.name.get());
				dialog.setTitle("Rename profile");
				dialog.setHeaderText(String.format("Rename profile %s", profile1.name));
				dialog.setContentText("Profile name:");
				dialog.showAndWait().ifPresent(name -> profile1.name.setValue(name));
			});
			VBox listButtons = new VBox();
			listButtons.setSpacing(3);
			listButtons.getChildren().addAll(newProfile, deleteProfile, renameProfile);

			HBox listLayout = new HBox();
			listLayout.setSpacing(3);
			listLayout.getChildren().addAll(list, listButtons);

			Button open = new Button("Select", new ImageView(icon("folder-outline.png")));
			open.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
			open.setOnAction(e -> {
				select();
			});
			open.setDefaultButton(true);
			Button cancel = new Button("Cancel");
			cancel.setOnAction(e -> {
				hide();
			});
			HBox buttonLayout = new HBox();
			buttonLayout.setSpacing(6);
			buttonLayout.setPadding(new Insets(6));
			buttonLayout.setAlignment(Pos.CENTER_RIGHT);
			buttonLayout.getChildren().addAll(open, cancel);

			VBox topLayout = new VBox();
			topLayout.setSpacing(3);
			topLayout.setPadding(new Insets(3));
			topLayout.getChildren().addAll(listLayout, buttonLayout);

			setScene(new Scene(topLayout));
		}

		private void select() {
			profile = list.getSelectionModel().getSelectedItem().id;
			hide();
		}
	}

	public static class ProjectDialog extends Stage {
		public enum Result {
			NONE,
			CREATE,
			OPEN
		}

		public Result result = Result.NONE;
		public Path resultPath;
		CreateMode resultCreateMode;

		public Image directoryImage = icon("folder.png");
		public Image projectImage = icon("folder-outline.png");
		final SimpleObjectProperty<Path> cwd = new SimpleObjectProperty<>();
		final Map<Path, ChooserEntry> entries = new HashMap<>();
		private final ListView<ChooserEntry> list;

		class ChooserEntry {
			final Path path;
			final boolean isDirectory;
			final boolean isProject;
			final Instant modified;

			ChooserEntry(Path path) {
				this.path = path;
				isDirectory = Files.isDirectory(path);
				isProject = Files.exists(path.resolve("project.luxem"));
				modified = uncheck(() -> Files.getLastModifiedTime(path).toInstant());
			}
		}

		ProjectDialog() {
			setTitle(String.format("%s - Choose project", Global.nameHuman));
			getIcons().addAll(appIcons);

			VBox top = new VBox();

			cwd.addListener((observableValue, path, t1) -> {
				entries.clear();
				refresh();
			});
			SimpleObjectProperty<Path> resolvedPath = new SimpleObjectProperty<>();

			Label explanation = new Label("Select an existing project directory or create a new one.");
			explanation.setPadding(new Insets(4));
			explanation.setWrapText(true);
			explanation.setMinWidth(50);

			Label here = new Label();
			here.setTextFill(Color.GRAY);
			here.setTextOverrun(OverrunStyle.LEADING_WORD_ELLIPSIS);
			TextField text = new TextField();
			text.setOnAction(e -> {
				if (!Files.isDirectory(resolvedPath.get()))
					return;
				cwd.set(resolvedPath.get());
			});
			HBox.setHgrow(text, Priority.ALWAYS);
			HBox hereLayout = new HBox();
			hereLayout.setAlignment(Pos.CENTER_RIGHT);
			hereLayout.setSpacing(4);
			hereLayout.setPadding(new Insets(4));
			hereLayout.getChildren().addAll(here, text);

			Button up = HelperJFX.button("arrow-up.png", "Leave directory");
			up.setOnAction(e -> {
				Path parent = cwd.get().getParent();
				if (parent == null)
					return;
				cwd.set(parent);
			});
			Button refresh = HelperJFX.button("refresh.png", "Refresh");
			refresh.setOnAction(e -> {
				refresh();
			});
			Region space = new Region();
			space.setMaxWidth(Double.MAX_VALUE);
			HBox.setHgrow(space, Priority.ALWAYS);
			Button createDirectory = HelperJFX.button("folder-plus.png", "Create directory");
			createDirectory.setOnAction(e -> {
				TextInputDialog dialog = new TextInputDialog("New Folder");
				dialog.setTitle("Directory name");
				dialog.setContentText("Enter a name for the directory");
				dialog.showAndWait().ifPresent(t -> uncheck(() -> {
					Files.createDirectory(cwd.get().resolve(t));
					refresh();
				}));
			});
			ToolBar toolbar = new ToolBar();
			toolbar.getItems().addAll(up, refresh, space, createDirectory);
			list = new ListView<>();
			list.setCellFactory(new Callback<ListView<ChooserEntry>, ListCell<ChooserEntry>>() {
				@Override
				public ListCell<ChooserEntry> call(ListView<ChooserEntry> entryListView) {
					return new ListCell<>() {
						ImageView icon = new ImageView();

						{
							setGraphic(icon);
							addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
								if (e.getClickCount() == 2) {
									cwd.set(getItem().path);
								}
							});
						}

						@Override
						protected void updateItem(ChooserEntry entry, boolean b) {
							if (entry == null) {
								setText("");
								icon.setImage(null);
								setDisable(false);
							} else {
								setText(entry.path.getFileName().toString());
								if (entry.isDirectory) {
									if (entry.isProject)
										icon.setImage(projectImage);
									else
										icon.setImage(directoryImage);
									setDisable(false);
								} else {
									icon.setImage(null);
									setDisable(true);
								}
							}
							super.updateItem(entry, b);
						}
					};
				}
			});
			VBox listLayout = new VBox();
			listLayout.getChildren().addAll(toolbar, list);

			RadioButton newNormal = new RadioButton("Normal");
			newNormal.setUserData(CreateMode.normal);
			RadioButton newPixel = new RadioButton("Pixel");
			newPixel.setUserData(CreateMode.pixel);
			ToggleGroup modeGroup = new ToggleGroup();
			modeGroup
					.selectedToggleProperty()
					.addListener((observable, oldValue, newValue) -> profileConfig.newProjectNormalMode =
							(CreateMode) newValue.getUserData());
			modeGroup.getToggles().addAll(newNormal, newPixel);
			switch (profileConfig.newProjectNormalMode) {
				case normal:
					modeGroup.selectToggle(newNormal);
					break;
				case pixel:
					modeGroup.selectToggle(newPixel);
					break;
				default:
					throw new Assertion();
			}
			Button create = new Button("New", new ImageView(icon("folder-plus-outline.png")));
			create.setOnAction(e -> {
				result = Result.CREATE;
				resultPath = resolvedPath.get();
				resultCreateMode = (CreateMode) modeGroup.getSelectedToggle().getUserData();
				hide();
			});
			Button open = new Button("Open", new ImageView(projectImage));
			open.setOnAction(e -> {
				result = Result.OPEN;
				resultPath = resolvedPath.get();
				hide();
			});
			Button cancel = new Button("Cancel");
			cancel.setOnAction(e -> {
				result = Result.NONE;
				hide();
			});
			HBox buttonLayout = new HBox();
			buttonLayout.setSpacing(3);
			buttonLayout.setAlignment(Pos.CENTER_RIGHT);
			buttonLayout.getChildren().addAll(newNormal, newPixel, create, open, cancel);

			top.setFillWidth(true);
			top.setSpacing(6);
			top.setPadding(new Insets(3));
			top.getChildren().addAll(explanation, hereLayout, listLayout, buttonLayout);

			addEventFilter(KeyEvent.KEY_PRESSED, e -> {
				if (e.getCode() == KeyCode.ENTER) {
					if (!create.isDisable())
						create.fire();
					if (!open.isDisable())
						open.fire();
					return;
				}
				if (e.getCode() == KeyCode.ESCAPE) {
					cancel.fire();
				}
			});

			cwd.addListener((observableValue, path, t1) -> text.setText(""));
			cwd.set(Paths.get(GUILaunch.profileConfig.lastDir));
			SimpleObjectProperty<ChooserEntry> listProxy = new SimpleObjectProperty<>();
			{
				Common.Mutable<Boolean> suppress = new Common.Mutable<>(false);
				listProxy.addListener((observableValue, chooserEntry, t1) -> {
					if (suppress.value)
						return;
					suppress.value = true;
					try {
						list.getSelectionModel().select(t1);
					} finally {
						suppress.value = false;
					}
				});
				list.getSelectionModel().selectedItemProperty().addListener((observableValue, chooserEntry, t1) -> {
					if (suppress.value)
						return;
					suppress.value = true;
					try {
						listProxy.set(t1);
					} finally {
						suppress.value = false;
					}
				});
			}
			CustomBinding.<Path>bindBidirectional(new CustomBinding.PropertyBinder<String>(text.textProperty()).<Path>bimap(t -> Optional.of(cwd.get().resolve(t)),
					(Path v) -> v.getFileName().toString()
					),
					new CustomBinding.PropertyBinder<ChooserEntry>(listProxy).<Path>bimap(e -> Optional
							.ofNullable(e)
							.map(v -> v.path), (Path v) -> entries.get(v))
			);
			resolvedPath.bind(Bindings.createObjectBinding(() -> cwd.get().resolve(text.getText()),
					cwd,
					text.textProperty()
			));
			here.textProperty().bind(cwd.asString().concat("/"));
			BooleanBinding createBinding =
					Bindings.createBooleanBinding(() -> Files.exists(resolvedPath.get()), resolvedPath);
			newNormal.disableProperty().bind(createBinding);
			newPixel.disableProperty().bind(createBinding);
			create.disableProperty().bind(createBinding);
			open.disableProperty().bind(Bindings.createBooleanBinding(() -> {
				Path check = resolvedPath.get().resolve("project.luxem");
				return !Files.exists(check);
			}, resolvedPath));
			list.getSelectionModel().clearSelection();
			list.getSelectionModel().select(0);

			setScene(new Scene(top));
		}

		public void refresh() {
			list.getItems().setAll(uncheck(() -> Files.list(cwd.get()))
					.map(p -> entries.computeIfAbsent(p, p1 -> {
						try {
							return new ChooserEntry(p1);
						} catch (Common.UncheckedNoSuchFileException e) {
							return null;
						}
					}))
					.filter(e -> e != null)
					.sorted(new ChainComparator<ChooserEntry>()
							.greaterFirst(e -> e.isDirectory)
							.greaterFirst(e -> e.modified)
							.build())
					.collect(Collectors.toList()));
		}
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		logger = new Logger.TerminalPlusFile(appDirs);
		Thread.currentThread().setUncaughtExceptionHandler((thread, e) -> {
			logger.writeException(e, "Uncaught error");
			HelperJFX.exceptionPopup(primaryStage, e, "Unexpected error", "An unexpected error occurred.");
		});
		new com.zarbosoft.pyxyzygy.core.mynativeJNI();

		globalConfig = ConfigBase.deserialize(new TypeInfo(RootGlobalConfig.class),
				configDir.resolve("profiles.luxem"),
				() -> {
					RootGlobalConfig config = new RootGlobalConfig();
					RootGlobalConfig.Profile profile = new RootGlobalConfig.Profile();
					profile.id = config.nextId++;
					profile.name.set("Default");
					config.profiles.add(profile);
					return config;
				}
		);
		Long profileId = Optional
				.ofNullable(getParameters().getNamed().get("profile-id"))
				.map(s -> Long.parseLong(s))
				.orElseGet(() -> {
					ProfileDialog dialog = new ProfileDialog(globalConfig);
					dialog.showAndWait();
					if (dialog.profile == null)
						return null;
					globalConfig.lastId = dialog.profile;
					return dialog.profile;
				});
		if (profileId == null) {
			Global.shutdown();
			return;
		}

		globalConfig.cacheSize.addListener(new ChangeListener<Number>() {
			{
				changed(null, null, globalConfig.cacheSize.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends Number> observable, Number oldValue, Number newValue
			) {
				imageCache = CacheBuilder
						.newBuilder()
						.recordStats()
						.maximumWeight(newValue.intValue())
						.weigher((Weigher<Integer, Image>) (key, value) -> (int) (
								value.getWidth() *
										value.getHeight() *
										4 / 1024 / 1024
						))
						.build();
			}
		});
		Timer cacheStatsTimer = new Timer();
		TimerTask cacheStatsTask = new TimerTask() {
			@Override
			public void run() {
				logger.write("Cache stats:\n%s", imageCache.stats());
			}
		};
		cacheStatsTimer.scheduleAtFixedRate(cacheStatsTask, 0, 1000 * 60 * 5);
		shutdown.add(() -> {
			cacheStatsTimer.cancel();
			cacheStatsTask.run();
		});

		profileConfig = ConfigBase.deserialize(new TypeInfo(RootProfileConfig.class),
				Global.configDir.resolve(String.format("profile_%s.luxem", Long.toString(profileId))),
				() -> {
					RootProfileConfig config = new RootProfileConfig();
					{
						TrueColorBrush transparentBrush = new TrueColorBrush();
						transparentBrush.name.set("Transparent");
						transparentBrush.size.set(50);
						transparentBrush.blend.set(Global.blendMax);
						transparentBrush.color.set(TrueColor.fromJfx(Color.TRANSPARENT));
						transparentBrush.useColor.set(true);
						TrueColorBrush defaultBrush = new TrueColorBrush();
						defaultBrush.name.set("Default");
						defaultBrush.size.set(10);
						defaultBrush.blend.set(Global.blendMax);
						defaultBrush.color.set(TrueColor.fromJfx(Color.BLACK));
						defaultBrush.useColor.set(true);
						config.trueColorBrushes.addAll(transparentBrush, defaultBrush);
					}
					{
						PaletteBrush transparentBrush = new PaletteBrush();
						transparentBrush.name.set("Transparent");
						transparentBrush.size.set(50);
						transparentBrush.paletteOffset.set(0);
						transparentBrush.useColor.set(true);
						PaletteBrush defaultBrush = new PaletteBrush();
						defaultBrush.name.set("Default");
						defaultBrush.size.set(10);
						defaultBrush.useColor.set(false);
						config.paletteBrushes.addAll(transparentBrush, defaultBrush);
					}
					return config;
				}
		);

		if (getParameters().getUnnamed().isEmpty()) {
			while (true) {
				ProjectDialog dialog = new ProjectDialog();
				dialog.showAndWait();
				switch (dialog.result) {
					case NONE: {
						shutdown();
						Platform.exit();
						break;
					}
					case CREATE: {
						try {
							newProject(primaryStage, dialog.resultPath, dialog.resultCreateMode);
						} catch (Exception e) {
							logger.writeException(e, "Error creating new project");
							HelperJFX.exceptionPopup(primaryStage,
									e,
									"Error creating new project",
									"An error occurred while trying to create the project.\n" +
											"\n" +
											"Make sure you have permission to write to the project directory."
							);
							continue;
						}
						break;
					}
					case OPEN: {
						try {
							openProject(primaryStage, dialog.resultPath);
						} catch (Exception e) {
							logger.writeException(e, "Error opening project");
							HelperJFX.exceptionPopup(primaryStage,
									e,
									"Error opening project",
									"An error occurred while trying to open the project.\n" +
											"\n" +
											"Make sure you have permission to read and write to the project directory and all the files within."
							);
							continue;
						}
						break;
					}
					default:
						throw new DeadCode();
				}
				break;
			}
		} else {
			Path path = Paths.get(this.getParameters().getUnnamed().get(0));
			if (Files.exists(path)) {
				if (Files.exists(path.resolve("project.luxem"))) {
					openProject(primaryStage, path);
				} else {
					throw new IllegalArgumentException(String.format("Directory is not a project", path));
				}
			} else {
				CreateMode createMode = CreateMode.valueOf(this.getParameters().getUnnamed().get(1));
				newProject(primaryStage, path, createMode);
			}
		}
	}

	public void newProject(Stage primaryStage, Path path, CreateMode createMode) {
		profileConfig.lastDir = path.getParent().toString();
		ProjectContext context = Global.create(path, createMode.tileSize());
		context.config.defaultZoom = createMode.defaultZoom();

		GroupLayer groupLayer = GroupLayer.create(context);

		GroupPositionFrame groupPositionFrame = GroupPositionFrame.create(context);
		groupPositionFrame.initialLengthSet(context, NO_LENGTH);
		groupPositionFrame.initialOffsetSet(context, Vector.ZERO);
		groupLayer.initialPositionFramesAdd(context, ImmutableList.of(groupPositionFrame));

		GroupTimeFrame groupTimeFrame = GroupTimeFrame.create(context);
		groupTimeFrame.initialLengthSet(context, NO_LENGTH);
		groupTimeFrame.initialInnerOffsetSet(context, 0);
		groupTimeFrame.initialInnerLoopSet(context, NO_LOOP);
		groupLayer.initialTimeFramesAdd(context, ImmutableList.of(groupTimeFrame));

		switch (createMode) {
			case normal: {
				TrueColorImageNode trueColorImageNode = TrueColorImageNode.create(context);
				trueColorImageNode.initialNameSet(context, uniqueName(Global.trueColorLayerName));
				trueColorImageNode.initialOpacitySet(context, Global.opacityMax);
				TrueColorImageFrame trueColorImageFrame = TrueColorImageFrame.create(context);
				trueColorImageFrame.initialLengthSet(context, -1);
				trueColorImageFrame.initialOffsetSet(context, new Vector(0, 0));
				trueColorImageNode.initialFramesAdd(context, ImmutableList.of(trueColorImageFrame));
				groupLayer.initialInnerSet(context, trueColorImageNode);
				break;
			}
			case pixel: {
				Palette palette = Palette.create(context);
				palette.initialNameSet(context, uniqueName(Global.paletteName));
				palette.initialNextIdSet(context, 2);
				PaletteColor transparent = PaletteColor.create(context);
				transparent.initialIndexSet(context, 0);
				transparent.initialColorSet(context, TrueColor.fromJfx(Color.TRANSPARENT));
				PaletteColor black = PaletteColor.create(context);
				black.initialIndexSet(context, 1);
				black.initialColorSet(context, TrueColor.fromJfx(Color.BLACK));
				palette.initialEntriesAdd(context, ImmutableList.of(transparent, black));
				context.project.initialPalettesAdd(context, ImmutableList.of(palette));

				PaletteImageNode paletteImageNode = PaletteImageNode.create(context);
				paletteImageNode.initialPaletteSet(context, palette);
				paletteImageNode.initialNameSet(context, uniqueName(Global.paletteLayerName));
				paletteImageNode.initialOpacitySet(context, Global.opacityMax);
				PaletteImageFrame paletteImageFrame = PaletteImageFrame.create(context);
				paletteImageFrame.initialLengthSet(context, -1);
				paletteImageFrame.initialOffsetSet(context, new Vector(0, 0));
				paletteImageNode.initialFramesAdd(context, ImmutableList.of(paletteImageFrame));
				groupLayer.initialInnerSet(context, paletteImageNode);
				break;
			}
			default:
				throw new Assertion();
		}

		GroupNode groupNode = GroupNode.create(context);
		groupNode.initialNameSet(context, uniqueName(groupLayerName));
		groupNode.initialOpacitySet(context, opacityMax);
		groupNode.initialLayersAdd(context, ImmutableList.of(groupLayer));

		context.change(null, c -> c.project(context.project).topAdd(groupNode));

		context.config.viewPath = ImmutableList.of(0);
		context.config.editPath = ImmutableList.of(0, 0);

		new Window().start(context, primaryStage, true);
	}

	public void openProject(Stage primaryStage, Path path) {
		profileConfig.lastDir = path.getParent().toString();
		com.zarbosoft.pyxyzygy.seed.model.ProjectContext rawContext = Global.deserialize(path);
		ProjectContext context;
		if (rawContext.needsMigrate()) {
			Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
			confirm.initOwner(primaryStage);
			confirm.setTitle("Clear Project History");
			confirm.setHeaderText("Opening this project will clear it's history (undo/redo) data.");
			confirm.setContentText("Back up your project before proceeding.");
			Optional<ButtonType> result = confirm.showAndWait();
			if (!result.isPresent() || result.get() != ButtonType.OK) {
				shutdown();
				Platform.exit();
				return;
			}
			rawContext.clearHistory();
			context = (ProjectContext) rawContext.migrate();
		} else {
			context = (ProjectContext) rawContext;
		}
		new Window().start(context, primaryStage, true);
	}
}
