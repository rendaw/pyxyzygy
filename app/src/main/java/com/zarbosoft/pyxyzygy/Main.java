package com.zarbosoft.pyxyzygy;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.appdirsj.AppDirs;
import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.internal.pyxyzygy_seed.model.Vector;
import com.zarbosoft.pyxyzygy.config.CreateMode;
import com.zarbosoft.pyxyzygy.config.GlobalConfig;
import com.zarbosoft.pyxyzygy.config.TrueColor;
import com.zarbosoft.pyxyzygy.config.TrueColorBrush;
import com.zarbosoft.pyxyzygy.model.TrueColorImageFrame;
import com.zarbosoft.pyxyzygy.model.TrueColorImageNode;
import com.zarbosoft.pyxyzygy.widgets.HelperJFX;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.ChainComparator;
import com.zarbosoft.rendaw.common.Common;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleObjectProperty;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zarbosoft.pyxyzygy.ProjectContext.uniqueName;
import static com.zarbosoft.pyxyzygy.widgets.HelperJFX.icon;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public class Main extends Application {
	public static String nameSymbol = "pyxyzygy";
	public static String nameHuman = "pyxyzygy";
	public static AppDirs appDirs = new AppDirs().set_appname(nameSymbol).set_appauthor("zarbosoft");
	public static final Path configDir = appDirs.user_config_dir();
	public static final Path configPath = configDir.resolve("config.luxem");
	public final static int opacityMax = 1000;
	public final static int blendMax = 1000;
	public static final GlobalConfig config;
	static {
		try {
			config = com.zarbosoft.pyxyzygy.ConfigBase.deserialize(new TypeInfo(GlobalConfig.class), configDir, () -> {
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
		} catch (Exception e) {
			Platform.exit();
			throw e;
		}
	}

	public final static int NO_LOOP = 0;
	public final static int NO_LENGTH = -1;
	public final static int NO_INNER = -1;

	public static void main(String[] args) {
		Main.launch(args);
	}

	public static void shutdown() {
		config.shutdown();
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		new com.zarbosoft.pyxyzygy.mynativeJNI();
		if (getParameters().getUnnamed().isEmpty()) {
			primaryStage.setTitle(String.format("%s - Choose project", nameHuman));
			Stream
					.of("appicon16.png", "appicon32.png", "appicon64.png")
					.forEach(s -> primaryStage.getIcons().add(icon(s)));
			primaryStage.setOnCloseRequest(e -> {
				shutdown();
			});
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

			class ProjectChooser extends VBox {
				public Image directoryImage = icon("folder.png");
				public Image projectImage = icon("folder-outline.png");
				final SimpleObjectProperty<Path> cwd = new SimpleObjectProperty<>();
				final Map<Path, ChooserEntry> entries = new HashMap<>();
				private final ListView<ChooserEntry> list;

				public ProjectChooser(Runnable chooseNone) {
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
					toolbar.getItems().addAll(up, space, createDirectory);
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
							.addListener((observable, oldValue, newValue) -> config.newProjectNormalMode =
									(CreateMode) newValue.getUserData());
					modeGroup.getToggles().addAll(newNormal, newPixel);
					switch (config.newProjectNormalMode) {
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
						newProject(primaryStage,
								resolvedPath.get(),
								(CreateMode) modeGroup.getSelectedToggle().getUserData()
						);
					});
					Button open = new Button("Open", new ImageView(projectImage));
					open.setOnAction(e -> {
						openProject(primaryStage, resolvedPath.get());
					});
					Button cancel = new Button("Cancel");
					cancel.setOnAction(e -> {
						chooseNone.run();
					});
					HBox buttonLayout = new HBox();
					buttonLayout.setSpacing(3);
					buttonLayout.setAlignment(Pos.CENTER_RIGHT);
					buttonLayout.getChildren().addAll(newNormal, newPixel, create, open, cancel);

					setFillWidth(true);
					setSpacing(6);
					setPadding(new Insets(3));
					getChildren().addAll(explanation, hereLayout, listLayout, buttonLayout);

					addEventFilter(KeyEvent.KEY_PRESSED,e-> {
						if (e.getCode() == KeyCode.ENTER) {
							if (!create.isDisable()) create.fire();
								return;
						}
						if (e.getCode() == KeyCode.ESCAPE) {
							cancel.fire();
						}
					});

					cwd.addListener((observableValue, path, t1) -> text.setText(""));
					cwd.set(Paths.get(Main.config.lastDir));
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
						list
								.getSelectionModel()
								.selectedItemProperty()
								.addListener((observableValue, chooserEntry, t1) -> {
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
					com.zarbosoft.pyxyzygy.CustomBinding.<Path>bindBidirectionalMultiple(new com.zarbosoft.pyxyzygy.CustomBinding.Binder<>(
							text.textProperty(),
							() -> Optional.of(cwd.get().resolve(text.getText())),
							v -> text.setText(v.getFileName().toString())
					), new com.zarbosoft.pyxyzygy.CustomBinding.Binder<>(listProxy,
							() -> Optional.ofNullable(listProxy.get()).map(v -> v.path),
							v -> listProxy.set(entries.get(v))
					));
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
				}

				public void refresh() {
					list
							.getItems()
							.setAll(uncheck(() -> Files.list(cwd.get()))
									.map(p -> entries.computeIfAbsent(p, p1 -> {
										try {
											return new ChooserEntry(p1);
										} catch (Common.UncheckedNoSuchFileException e) {
											System.out.format("File list dropping %s: %s\n", p1, e);
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
			primaryStage.setScene(new Scene(new ProjectChooser(() -> {
				shutdown();
				Platform.exit();
			})));
			primaryStage.show();
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
		config.lastDir = path.getParent().toString();
		ProjectContext context = ProjectContext.create(path, createMode.tileSize());
		context.config.defaultZoom = createMode.defaultZoom();
		TrueColorImageNode trueColorImageNode = TrueColorImageNode.create(context);
		trueColorImageNode.initialNameSet(context, uniqueName("Image"));
		trueColorImageNode.initialOpacitySet(context, opacityMax);
		TrueColorImageFrame trueColorImageFrame = TrueColorImageFrame.create(context);
		trueColorImageFrame.initialLengthSet(context, -1);
		trueColorImageFrame.initialOffsetSet(context, new Vector(0, 0));
		trueColorImageNode.initialFramesAdd(context, ImmutableList.of(trueColorImageFrame));
		context.history.change(c -> c.project(context.project).topAdd(trueColorImageNode));
		context.history.finishChange();
		new Window().start(context, primaryStage);
	}

	public void openProject(Stage primaryStage, Path path) {
		config.lastDir = path.getParent().toString();
		ProjectContext context = ProjectContext.deserialize(path);
		new Window().start(context, primaryStage);
	}
}
