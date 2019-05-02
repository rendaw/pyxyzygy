package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.pyxyzygy.app.config.NodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.parts.editor.Editor;
import com.zarbosoft.pyxyzygy.app.parts.structure.Structure;
import com.zarbosoft.pyxyzygy.app.parts.timeline.Timeline;
import com.zarbosoft.pyxyzygy.app.widgets.TitledPane;
import com.zarbosoft.pyxyzygy.app.widgets.*;
import com.zarbosoft.pyxyzygy.app.wrappers.camera.CameraWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupLayerWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.paletteimage.PaletteImageNodeWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.truecolorimage.TrueColorImageNodeWrapper;
import com.zarbosoft.pyxyzygy.core.model.v0.*;
import com.zarbosoft.pyxyzygy.seed.model.v0.TrueColor;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zarbosoft.pyxyzygy.app.Global.nameHuman;
import static com.zarbosoft.pyxyzygy.app.Misc.opt;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.icon;

public class Window {
	public List<FrameMapEntry> timeMap;
	public SimpleObjectProperty<EditHandle> selectedForEdit = new SimpleObjectProperty<>();
	public SimpleObjectProperty<CanvasHandle> selectedForView = new SimpleObjectProperty<>();
	public Set<KeyCode> pressed = new HashSet<>();
	public Editor editor;
	private Tab layerTab;
	public final ContentReplacer<Node> layerTabContent = new ContentReplacer<Node>() {

		@Override
		protected void innerSet(Node content) {
			layerTab.setContent(content);
		}

		@Override
		protected void innerClear() {
			layerTab.setContent(null);
		}
	};
	public final ContentReplacer<Cursor> editorCursor = new ContentReplacer<Cursor>() {
		@Override
		protected void innerSet(Cursor content) {
			editor.outerCanvas.setCursor(content);
		}

		@Override
		protected void innerClear() {
			editor.outerCanvas.setCursor(null);
		}
	};
	private ToolBar toolBar;
	public ChildrenReplacer<Node> toolBarChildren;
	public ChildrenReplacer<MenuItem> menuChildren;
	public Stage stage;

	public static class Tab extends javafx.scene.control.Tab {
		ScrollPane scrollPane = new ScrollPane();

		{
			setContent(scrollPane);
		}

		public Tab(String title) {
			super(title);
		}

		public void setContent2(Node node) {
			scrollPane.setContent(node);
		}
	}

	public void start(ProjectContext context, Stage primaryStage, boolean main) {
		this.stage = primaryStage;
		primaryStage.getIcons().addAll(GUILaunch.appIcons);
		primaryStage.setOnCloseRequest(e -> {
			context.shutdown();
		});

		Stream
				.of(new Hotkeys.Action(Hotkeys.Scope.GLOBAL,
							"undo",
							"Undo",
							Hotkeys.Hotkey.create(KeyCode.Z, true, false, false)
					) {
						@Override
						public void run(ProjectContext context, Window window) {
							context.history.undo();
						}
					},
						new Hotkeys.Action(Hotkeys.Scope.GLOBAL,
								"redo",
								"Redo",
								Hotkeys.Hotkey.create(KeyCode.Y, true, false, false)
						) {

							@Override
							public void run(ProjectContext context, Window window) {
								context.history.redo();
							}
						}
				)
				.forEach(context.hotkeys::register);

		selectedForEdit.addListener((observable, oldValue, newValue) -> {
			NodeConfig oldConfig;
			if (oldValue != null) {
				oldValue.remove(context, this);
				oldConfig = oldValue.getWrapper().getConfig();
			} else {
				oldConfig = null;
			}
			NodeConfig newConfig;
			if (newValue != null) {
				newConfig = newValue.getWrapper().getConfig();
			} else {
				newConfig = null;
			}
			if (newConfig != oldConfig) {
				if (oldConfig != null)
					oldConfig.selectedSomewhere.set(false);
				if (newConfig != null)
					newConfig.selectedSomewhere.set(true);
			}
		});

		TabPane leftTabs = new TabPane();
		leftTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		final Tab structureTab = new Tab("Project");
		final Tab configTab = new Tab("Settings");
		layerTab = new Tab("Layer");
		layerTab.disableProperty().bind(layerTab.contentProperty().isNull());
		leftTabs.getTabs().addAll(structureTab, layerTab, configTab);
		{
			CustomBinding.ListPropertyHalfBinder<ObservableList<javafx.scene.control.Tab>> tabsProp =
					new CustomBinding.ListPropertyHalfBinder<>(leftTabs.getTabs());
			CustomBinding.bind(leftTabs.minWidthProperty(),
					new CustomBinding.IndirectHalfBinder<>(tabsProp.<List<CustomBinding.HalfBinder<Double>>>map(l -> opt(
							l
									.stream()
									.map(t -> new CustomBinding.IndirectHalfBinder<Double>(t.contentProperty(),
											c -> opt(c == null ? null : ((Region) c).widthProperty().asObject())
									))
									.collect(Collectors.toList()))),
							l -> opt(new CustomBinding.ListElementsHalfBinder<Double>(l, s -> {
								double out = leftTabs
										.getTabs()
										.stream()
										.mapToDouble(t -> t.getContent().minWidth(-1))
										.max()
										.orElse(0);
								return opt(out);
							}))
					)
			);
		}

		Structure structure = new Structure(context, this, main);
		structureTab.setContent(structure.getWidget());

		Region menuSpring = new Region();
		menuSpring.setMinWidth(1);
		HBox.setHgrow(menuSpring, Priority.ALWAYS);

		Button resetScroll = HelperJFX.button("image-filter-center-focus-weak.png", "Recenter view");
		resetScroll.setOnAction(e -> {
			editor.updateScroll(context, DoubleVector.zero);
		});

		HBox zoomBox = new HBox();
		{
			zoomBox.setAlignment(Pos.CENTER_LEFT);
			zoomBox.setFillHeight(true);
			Spinner<Integer> spinner = new Spinner<Integer>();
			spinner.setMaxWidth(Double.MAX_VALUE);
			spinner.setPrefWidth(60);
			spinner.setEditable(true);
			spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-10, 50));
			CustomBinding.bindBidirectional(new CustomBinding.IndirectBinder<>(selectedForView,
							v -> opt(v == null ? null : v.getWrapper().getConfig().zoom)
					),
					new CustomBinding.PropertyBinder<Integer>(spinner.getValueFactory().valueProperty())
			);
			final ImageView imageView = new ImageView(icon("zoom.png"));
			zoomBox.getChildren().addAll(imageView, spinner);
		}

		MenuButton menuButton = new MenuButton(null, new ImageView(icon("menu.png")));
		menuButton.disableProperty().bind(Bindings.isEmpty(menuButton.getItems()));
		menuChildren = new ChildrenReplacer<MenuItem>() {
			@Override
			protected void innerSet(List<MenuItem> content) {
				menuButton.getItems().addAll(content);
			}

			@Override
			protected void innerClear() {
				menuButton.getItems().clear();
			}
		};

		HBox toolbarExtra = new HBox();
		toolbarExtra.setSpacing(3);
		toolbarExtra.setFillHeight(true);
		toolbarExtra.setAlignment(Pos.CENTER_LEFT);
		toolBarChildren = new ChildrenReplacer<Node>() {
			@Override
			protected void innerSet(List<Node> content) {
				toolbarExtra.getChildren().addAll(content);
			}

			@Override
			protected void innerClear() {
				toolbarExtra.getChildren().clear();
			}
		};

		toolBar = new ToolBar();
		toolBar.getItems().addAll(toolbarExtra, menuSpring, zoomBox, resetScroll, menuButton);

		editor = new Editor(context, this);
		Timeline timeline = new Timeline(context, this);

		VBox editorBox = new VBox();
		editorBox.setFillWidth(true);
		VBox.setVgrow(editor.getWidget(), Priority.ALWAYS);
		editorBox.getChildren().addAll(toolBar, editor.getWidget());

		SplitPane specificLayout = new SplitPane();
		specificLayout.setOrientation(Orientation.VERTICAL);
		specificLayout.getItems().addAll(editorBox);
		SplitPane.setResizableWithParent(timeline.getWidget(), false);

		VBox configLayout = new VBox();
		configLayout.setSpacing(3);
		configLayout
				.getChildren()
				.addAll(new TitledPane("Profile", new WidgetFormBuilder().twoLine("Background color", () -> {
					TrueColorPicker w = new TrueColorPicker();
					w.colorProxyProperty.set(GUILaunch.profileConfig.backgroundColor.get().toJfx());
					w.colorProxyProperty.addListener((observable, oldValue, newValue) -> GUILaunch.profileConfig.backgroundColor
							.set(TrueColor.fromJfx(newValue)));
					return w;
				}).twoLine("Onion skin color", () -> {
					TrueColorPicker w = new TrueColorPicker();
					w.colorProxyProperty.set(GUILaunch.profileConfig.onionSkinColor.get().toJfx());
					w.colorProxyProperty.addListener((observable, oldValue, newValue) -> GUILaunch.profileConfig.onionSkinColor
							.set(TrueColor.fromJfx(newValue)));
					return w;
				}).intSpinner("Max undo", 1, 100000, spinner -> {
					spinner.getValueFactory().setValue(GUILaunch.profileConfig.maxUndo);
					spinner
							.getValueFactory()
							.valueProperty()
							.addListener((observable, oldValue, newValue) -> GUILaunch.profileConfig.maxUndo = newValue);
				}).button(button -> {
					button.setText("Clear undo/redo");
					button.setOnAction(e -> {
						Alert confirm =
								new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you wish to clear undo/redo?");
						confirm.initOwner(stage);
						confirm.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(x -> {
							context.history.clearHistory();
						});
					});
				}).check("Show origin", checkBox -> {
					checkBox.selectedProperty().bindBidirectional(GUILaunch.profileConfig.showOrigin);
				}).check("Show timeline", checkBox -> {
					checkBox.selectedProperty().bindBidirectional(GUILaunch.profileConfig.showTimeline);
				}).build()), new TitledPane("Global", new WidgetFormBuilder().intSpinner("Tile cache (Mb)",0,1024 * 16,spinner -> {
							spinner.getValueFactory().setValue(GUILaunch.profileConfig.maxUndo);
							spinner
									.getValueFactory()
									.valueProperty()
									.addListener((observable, oldValue, newValue) -> GUILaunch.profileConfig.maxUndo = newValue);
						}).intSpinner("Onion skin cache (Mb)",0,1024 * 16,s -> {

						}).build()),
						new TitledPane("Hotkeys", (
						(Supplier<Node>) () -> {
							TableColumn<Hotkeys.Action, String> scope = new TableColumn("Scope");
							scope.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().scope.name()));
							TableColumn<Hotkeys.Action, String> description = new TableColumn("Description");
							description.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().description));
							TableColumn<Hotkeys.Action, String> key = new TableColumn("Key");
							key.setCellValueFactory(param -> param.getValue().key.asString());
							TableView<Hotkeys.Action> table = new TableView<>();
							table.getColumns().addAll(scope, description, key);
							table.setItems(context.hotkeys.actions);
							return table;
						}
				).get()));
		configTab.setContent(configLayout);

		SplitPane generalLayout = new SplitPane();
		generalLayout.setOrientation(Orientation.HORIZONTAL);
		generalLayout.getItems().addAll(specificLayout);
		SplitPane.setResizableWithParent(leftTabs, false);

		Scene scene = new Scene(generalLayout, 1200, 800);

		new CustomBinding.DoubleHalfBinder<Boolean, Boolean, Boolean>(new CustomBinding.PropertyHalfBinder<>(context.config.maxCanvas),
				new CustomBinding.PropertyHalfBinder<>(GUILaunch.profileConfig.showTimeline),
				(max, show) -> {
					return opt(!max && show);
				}
		).addListener(show -> {
			if (show) {
				specificLayout.getItems().add(1, timeline.getWidget());
				specificLayout.setDividerPositions(context.config.timelineSplit);
				specificLayout
						.getDividers()
						.get(0)
						.positionProperty()
						.addListener((observable, oldValue, newValue) -> context.config.timelineSplit =
								newValue.doubleValue());
			} else {
				specificLayout.getItems().remove(timeline.getWidget());
			}
		});

		context.config.maxCanvas.addListener(new ChangeListener<Boolean>() {
			{
				changed(null, null, false);
			}

			@Override
			public void changed(
					ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue
			) {
				if (newValue) {
					generalLayout.getItems().remove(leftTabs);
				} else {
					generalLayout.getItems().add(0, leftTabs);
					generalLayout.setDividerPositions(context.config.tabsSplit);
				}
			}
		});

		generalLayout
				.getDividers()
				.get(0)
				.positionProperty()
				.addListener((observable, oldValue, newValue) -> context.config.tabsSplit = newValue.doubleValue());

		scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			pressed.add(e.getCode());
			if (context.hotkeys.event(context, this, Hotkeys.Scope.GLOBAL, e))
				e.consume();
		});
		scene.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
			pressed.remove(e.getCode());
		});
		scene.getStylesheets().addAll(getClass().getResource("widgets/style.css").toExternalForm(),
				getClass().getResource("widgets/colorpicker/style.css").toExternalForm(),
				getClass().getResource("widgets/brushbutton/style.css").toExternalForm()
		);

		primaryStage.setMaximized(GUILaunch.profileConfig.maximize);
		primaryStage.maximizedProperty().addListener((observable, oldValue, newValue) -> {
			GUILaunch.profileConfig.maximize = newValue.booleanValue();
		});

		structure.populate();

		primaryStage.setTitle(String.format("%s - %s", context.path.getFileName().toString(), nameHuman));
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	public Pair<Integer, FrameMapEntry> findTimeMapEntry(int outer) {
		int outerAt = 0;
		for (FrameMapEntry outerFrame : timeMap) {
			if (outer >= outerAt && (outerFrame.length == -1 || outer < outerAt + outerFrame.length)) {
				return new Pair<>(outerAt, outerFrame);
			}
			outerAt += outerFrame.length;
		}
		throw new Assertion();
	}

	public int timeToInner(int outer) {
		Pair<Integer, FrameMapEntry> entry = findTimeMapEntry(outer);
		if (entry.second.innerOffset == -1)
			return -1;
		return entry.second.innerOffset + outer - entry.first;
	}

	public static List<CanvasHandle> getAncestorsOutward(CanvasHandle start, CanvasHandle target) {
		List<CanvasHandle> ancestors = new ArrayList<>();
		CanvasHandle at = target.getParent();
		while (at != start) {
			ancestors.add(at);
			at = at.getParent();
		}
		return ancestors;
	}

	public static List<CanvasHandle> getAncestorsInward(CanvasHandle start, CanvasHandle target) {
		List<CanvasHandle> ancestors = getAncestorsOutward(start, target);
		Collections.reverse(ancestors);
		return ancestors;
	}

	public static DoubleVector toLocal(CanvasHandle wrapper, DoubleVector v) {
		for (CanvasHandle parent : getAncestorsInward(null, wrapper)) {
			v = parent.toInner(v);
		}
		return v;
	}

	/**
	 * Not really global but canvas-space
	 *
	 * @param wrapper
	 * @param v
	 * @return
	 */
	public static DoubleVector toGlobal(CanvasHandle wrapper, DoubleVector v) {
		DoubleVector zero = new DoubleVector(0, 0);
		for (CanvasHandle parent : getAncestorsOutward(null, wrapper)) {
			v = v.minus(parent.toInner(zero));
		}
		return v;
	}

	public static Wrapper createNode(ProjectContext context, Wrapper parent, int parentIndex, ProjectObject node) {
		if (false) {
			throw new Assertion();
		} else if (node instanceof Camera) {
			return new CameraWrapper(context, parent, parentIndex, (Camera) node);
		} else if (node instanceof GroupNode) {
			return new GroupNodeWrapper(context, parent, parentIndex, (GroupNode) node);
		} else if (node instanceof GroupLayer) {
			return new GroupLayerWrapper(context, parent, parentIndex, (GroupLayer) node);
		} else if (node instanceof TrueColorImageNode) {
			return new TrueColorImageNodeWrapper(context, parent, parentIndex, (TrueColorImageNode) node);
		} else if (node instanceof PaletteImageNode) {
			return new PaletteImageNodeWrapper(context, parent, parentIndex, (PaletteImageNode) node);
		} else
			throw new Assertion();
	}
}
