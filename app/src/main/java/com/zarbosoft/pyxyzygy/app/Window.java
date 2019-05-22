package com.zarbosoft.pyxyzygy.app;

import com.google.common.base.Throwables;
import com.zarbosoft.pyxyzygy.app.config.NodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.parts.editor.Editor;
import com.zarbosoft.pyxyzygy.app.parts.structure.Structure;
import com.zarbosoft.pyxyzygy.app.parts.timeline.Timeline;
import com.zarbosoft.pyxyzygy.app.widgets.TitledPane;
import com.zarbosoft.pyxyzygy.app.widgets.*;
import com.zarbosoft.pyxyzygy.app.widgets.binding.*;
import com.zarbosoft.pyxyzygy.app.wrappers.camera.CameraWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupChildWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.paletteimage.PaletteImageNodeWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.truecolorimage.TrueColorImageNodeWrapper;
import com.zarbosoft.pyxyzygy.core.model.v0.*;
import com.zarbosoft.pyxyzygy.seed.model.v0.TrueColor;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zarbosoft.pyxyzygy.app.Global.logger;
import static com.zarbosoft.pyxyzygy.app.Global.nameHuman;
import static com.zarbosoft.pyxyzygy.app.Misc.opt;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.icon;

public class Window {
	public List<FrameMapEntry> timeMap;
	public ManualHalfBinder<Wrapper> selectedForEditWrapperEnabledBinder =
			new ManualHalfBinder<>();
	public ManualHalfBinder<EditHandle> selectedForEditOriginBinder =
			new ManualHalfBinder<>();
	public ManualHalfBinder<EditHandle> selectedForEditOpacityBinder =
			new ManualHalfBinder<>();
	public ManualHalfBinder<EditHandle> selectedForEditFramerateBinder =
			new ManualHalfBinder<>();
	public ManualHalfBinder<EditHandle> selectedForEditOnionBinder =
			new ManualHalfBinder<>();
	public ManualHalfBinder<EditHandle> selectedForEditPlayingBinder =
			new ManualHalfBinder<>();
	public ManualHalfBinder<EditHandle> selectedForEditTreeIconBinder =
			new ManualHalfBinder<>();
	private EditHandle selectedForEdit = null;
	public ManualHalfBinder<CanvasHandle> selectedForViewZoomControlBinder =
			new ManualHalfBinder<>();
	public ManualHalfBinder<CanvasHandle> selectedForViewMaxFrameBinder =
			new ManualHalfBinder<>();
	public ManualHalfBinder<CanvasHandle> selectedForViewPlayingBinder =
			new ManualHalfBinder<>();
	public ManualHalfBinder<CanvasHandle> selectedForViewFrameBinder =
			new ManualHalfBinder<>();
	public ManualHalfBinder<CanvasHandle> selectedForViewTreeIconBinder =
			new ManualHalfBinder<>();
	private CanvasHandle selectedForView = null;
	public Set<KeyCode> pressed = new HashSet<>();
	public Editor editor;
	private Tab layerTab;
	public final ContentReplacer<Node> layerTabContent = new ContentReplacer<Node>() {

		@Override
		protected void innerSet(String title, Node content) {
			layerTab.setContent(content);
		}

		@Override
		protected void innerClear() {
			layerTab.setContent(null);
		}
	};
	public final ContentReplacer<Cursor> editorCursor = new ContentReplacer<Cursor>() {
		@Override
		protected void innerSet(String title, Cursor content) {
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
	public Timeline timeline;
	private StackPane stack;
	public Structure structure;
	private BinderRoot rootTabWidth; // GC root
	private ChangeListener<? super Boolean> maxListener;

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

	public EditHandle getSelectedForEdit() {
		return selectedForEdit;
	}

	public CanvasHandle getSelectedForView() {
		return selectedForView;
	}

	public void selectForView(ProjectContext context, Wrapper wrapper) {
		selectForView(context, wrapper, false);
	}

	public void selectForView(ProjectContext context, Wrapper wrapper, boolean fromEdit) {
		CanvasHandle oldValue = selectedForView;
		selectedForView = null;
		if (wrapper == null) {
			return;
		}
		selectedForView = wrapper.buildCanvas(context, this, null);

		selectedForViewZoomControlBinder.clear();
		selectedForViewMaxFrameBinder.clear();
		selectedForViewPlayingBinder.clear();
		selectedForViewFrameBinder.clear();
		selectedForViewTreeIconBinder.clear();

		editor.selectedForViewChanged(context, selectedForView);

		if (!fromEdit)
			timeline.setNodes(selectedForView, selectedForEdit);

		structure.selectedForView(context, selectedForView);

		selectedForViewZoomControlBinder.set(selectedForView);
		selectedForViewMaxFrameBinder.set(selectedForView);
		selectedForViewPlayingBinder.set(selectedForView);
		selectedForViewFrameBinder.set(selectedForView);
		selectedForViewTreeIconBinder.set(selectedForView);
	}

	public void selectForEdit(ProjectContext context, Wrapper wrapper) {
		selectedForEditWrapperEnabledBinder.set(wrapper);

		/*
		Canvas is always created first if necessary
		Then edit handle
		Then various callbacks called
		 */
		EditHandle oldValue = selectedForEdit;
		selectedForEdit = null;
		if (wrapper == null) {
			return;
		}

		// Clear binder states while transitioning
		selectedForEditOriginBinder.clear();
		selectedForEditOpacityBinder.clear();
		selectedForEditFramerateBinder.clear();
		selectedForEditOnionBinder.clear();
		selectedForEditPlayingBinder.clear();
		selectedForEditTreeIconBinder.clear();

		// Transition
		Wrapper preParent = wrapper;
		Wrapper parent = wrapper.getParent();
		boolean found = false;
		while (parent != null) {
			if (selectedForView.getWrapper() == parent) {
				found = true;
				break;
			}
			preParent = parent;
			parent = parent.getParent();
		}
		if (found) {
			selectedForEdit = wrapper.buildEditControls(context, this);
		} else {
			selectForView(context, preParent, true);
			selectedForEdit = wrapper.buildEditControls(context, this);
		}

		// React to change
		structure.selectedForEdit(context, selectedForEdit);

		timeline.setNodes(selectedForView, selectedForEdit);

		editor.selectedForEditChanged(context, selectedForEdit);

		selectedForEditOriginBinder.set(selectedForEdit);
		selectedForEditOpacityBinder.set(selectedForEdit);
		selectedForEditFramerateBinder.set(selectedForEdit);
		selectedForEditOnionBinder.set(selectedForEdit);
		selectedForEditPlayingBinder.set(selectedForEdit);
		selectedForEditTreeIconBinder.set(selectedForEdit);

		// Mark selected items in structure
		NodeConfig oldConfig;
		if (oldValue != null) {
			oldValue.remove(context, this);
			oldConfig = oldValue.getWrapper().getConfig();
		} else {
			oldConfig = null;
		}
		NodeConfig newConfig;
		if (selectedForEdit != null) {
			newConfig = selectedForEdit.getWrapper().getConfig();
		} else {
			newConfig = null;
		}
		if (newConfig != oldConfig) {
			if (oldConfig != null)
				oldConfig.selectedSomewhere.set(false);
			if (newConfig != null)
				newConfig.selectedSomewhere.set(true);
		}
	}

	public void start(ProjectContext context, Stage primaryStage, boolean main) {
		this.stage = primaryStage;
		Thread.currentThread().setUncaughtExceptionHandler((thread, e) -> {
			logger.writeException(e, "Uncaught error");
			TextArea textArea = new TextArea(Throwables.getStackTraceAsString(e));
			textArea.setEditable(false);
			textArea.setWrapText(true);
			textArea.setMaxWidth(Double.MAX_VALUE);
			textArea.setMaxHeight(Double.MAX_VALUE);
			dialog("An unexpected error occurred")
					.addContent(new TitledPane("Trace", textArea))
					.addAction(ButtonType.OK, true, () -> true)
					.go();
		});

		Stream
				.of(new Hotkeys.Action(Hotkeys.Scope.GLOBAL,
							"undo",
							"Undo",
							Hotkeys.Hotkey.create(KeyCode.Z, true, false, false)
					) {
						@Override
						public void run(ProjectContext context, Window window) {
							context.undo();
						}
					},
						new Hotkeys.Action(Hotkeys.Scope.GLOBAL,
								"redo",
								"Redo",
								Hotkeys.Hotkey.create(KeyCode.Y, true, false, false)
						) {

							@Override
							public void run(ProjectContext context, Window window) {
								context.redo();
							}
						}
				)
				.forEach(context.hotkeys::register);

		TabPane leftTabs = new TabPane();
		leftTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		final Tab structureTab = new Tab("Project");
		final Tab configTab = new Tab("Settings");
		layerTab = new Tab("Layer");
		layerTab.disableProperty().bind(layerTab.contentProperty().isNull());
		leftTabs.getTabs().addAll(structureTab, layerTab, configTab);
		this.rootTabWidth = CustomBinding.bind(leftTabs.minWidthProperty(),
				new IndirectHalfBinder<>(
						new ListPropertyHalfBinder<>(leftTabs.getTabs()).<List<HalfBinder<Node>>>map(
								l -> opt(l.stream()
										.map(t -> new PropertyHalfBinder<>(t.contentProperty()))
										.collect(Collectors.toList()))
						),
						l -> opt(new ListElementsHalfBinder<Double>(l, s -> {
							double out = s
									.filter(t -> t != null)
									.mapToDouble(t -> t.minWidth(-1))
									.max()
									.orElse(0);
							return opt(out);
						}))
				)
		);

		structure = new Structure(context, this, main);
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
			CustomBinding.bindBidirectional(new IndirectBinder<>(selectedForViewZoomControlBinder,
					v -> opt(v == null ? null : v.getWrapper().getConfig().zoom)
			), new PropertyBinder<Integer>(spinner.getValueFactory().valueProperty()));
			final ImageView imageView = new ImageView(icon("zoom.png"));
			zoomBox.getChildren().addAll(imageView, spinner);
		}

		MenuButton menuButton = new MenuButton(null, new ImageView(icon("menu.png")));
		menuButton.disableProperty().bind(Bindings.isEmpty(menuButton.getItems()));
		menuChildren = new ChildrenReplacer<MenuItem>() {
			@Override
			protected void innerSet(String title, List<MenuItem> content) {
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
			protected void innerSet(String title, List<Node> content) {
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
		timeline = new Timeline(context, this);

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
									.addListener((observable, oldValue, newValue) -> GUILaunch.profileConfig.maxUndo =
											newValue);
						}).button(button -> {
							button.setText("Clear undo/redo");
							button.setOnAction(e -> {
								dialog("Are you sure you wish to clear undo/redo?").addAction(ButtonType.OK, false, () -> {
									context.clearHistory();
									return true;
								}).addAction(ButtonType.CANCEL, true, () -> true).go();
							});
						}).check("Show origin", checkBox -> {
							checkBox.selectedProperty().bindBidirectional(GUILaunch.profileConfig.showOrigin);
						}).check("Show timeline", checkBox -> {
							checkBox.selectedProperty().bindBidirectional(GUILaunch.profileConfig.showTimeline);
						}).build()),
						new TitledPane("Global",
								new WidgetFormBuilder().intSpinner("Tile cache (Mb)", 0, 1024 * 16, spinner -> {
									spinner.getValueFactory().setValue(GUILaunch.globalConfig.cacheSize.get());
									spinner
											.getValueFactory()
											.valueProperty()
											.addListener((observable, oldValue, newValue) -> GUILaunch.globalConfig.cacheSize
													.set(newValue));
								}).build()
						),
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
						).get())
				);
		configTab.setContent(configLayout);

		SplitPane generalLayout = new SplitPane();
		generalLayout.setOrientation(Orientation.HORIZONTAL);
		generalLayout.getItems().addAll(specificLayout);
		SplitPane.setResizableWithParent(leftTabs, false);

		stack = new StackPane(generalLayout);

		ClosableScene scene = new ClosableScene(stack, 1200, 800) {
			@Override
			public void close() {
				primaryStage.maximizedProperty().removeListener(maxListener);
			}
		};

		new DoubleHalfBinder<Boolean, Boolean>(new PropertyHalfBinder<>(context.config.maxCanvas),
				new PropertyHalfBinder<>(GUILaunch.profileConfig.showTimeline)
		).map((max, show) -> {
			return opt(!max && show);
		}).addListener(show -> {
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

		structure.populate();

		HelperJFX.switchStage(primaryStage,
				String.format("%s - %s", context.path.getFileName().toString(), nameHuman),
				scene,
				GUILaunch.profileConfig.maximize,
				main ? (
						Global.fixedProject ? e -> Global.shutdown() : e -> {
							e.consume();
							GUILaunch.selectProject(primaryStage);
						}
				) : e -> {
				}
		);
		primaryStage.maximizedProperty().addListener(maxListener = (observable, oldValue, newValue) -> {
			GUILaunch.profileConfig.maximize = newValue.booleanValue();
		});
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
		} else if (node instanceof GroupLayer) {
			return new GroupNodeWrapper(context, parent, parentIndex, (GroupLayer) node);
		} else if (node instanceof GroupChild) {
			return new GroupChildWrapper(context, parent, parentIndex, (GroupChild) node);
		} else if (node instanceof TrueColorImageLayer) {
			return new TrueColorImageNodeWrapper(context, parent, parentIndex, (TrueColorImageLayer) node);
		} else if (node instanceof PaletteImageLayer) {
			return new PaletteImageNodeWrapper(context, parent, parentIndex, (PaletteImageLayer) node);
		} else
			throw new Assertion();
	}

	public class DialogBuilder {
		private final String title;
		VBox layout = new VBox();
		HBox buttons = new HBox();

		private Node defaultNode;
		private Supplier<Boolean> defaultAction;

		private Pane grey;
		private DialogPane content;

		public DialogBuilder(String title) {
			this.title = title;
			layout.setSpacing(3);
			content = new DialogPane();
		}

		public DialogBuilder setDefault(Node node) {
			this.defaultNode = node;
			return this;
		}

		public DialogBuilder addContent(Node node) {
			this.layout.getChildren().add(node);
			return this;
		}

		public DialogBuilder addAction(ButtonType type, boolean isDefault, Supplier<Boolean> callback) {
			content.getButtonTypes().add(type);
			Button button = (Button) content.lookupButton(type);
			if (isDefault) {
				defaultNode = button;
				defaultAction = callback;
			}
			button.addEventHandler(ActionEvent.ACTION, ae -> {
				if (callback.get()) {
					close();
				}
			});
			return this;
		}

		public void close() {
			stack.getChildren().removeAll(grey);
		}

		public void go() {
			buttons.setSpacing(4);
			buttons.setAlignment(Pos.CENTER_RIGHT);

			layout.getChildren().add(buttons);

			content.setHeaderText(title);
			content.setBorder(new Border(new BorderStroke(Color.DARKGRAY,
					BorderStrokeStyle.SOLID,
					CornerRadii.EMPTY,
					new BorderWidths(2)
			)));
			content.setPadding(new Insets(5));
			content
					.layoutXProperty()
					.bind(stage.widthProperty().divide(2.0).subtract(content.widthProperty().divide(2.0)));
			content
					.layoutYProperty()
					.bind(stage.heightProperty().divide(2.0).subtract(content.heightProperty().divide(2.0)));
			content.setContent(layout);

			grey = new Pane();
			grey.setBackground(new Background(new BackgroundFill(new Color(0, 0, 0, 0.4),
					CornerRadii.EMPTY,
					Insets.EMPTY
			)));
			grey.minWidthProperty().bind(stage.widthProperty());
			grey.minHeightProperty().bind(stage.heightProperty());
			grey.getChildren().add(content);

			stack.getChildren().addAll(grey);
			EventHandler<KeyEvent> keyEventEventHandler = e -> {
				if (e.getCode() == KeyCode.ESCAPE) {
					close();
				} else if (e.getCode() == KeyCode.ENTER) {
					if (defaultAction.get()) {
						close();
					}
				} else {
					return;
				}
				e.consume();
			};
			grey.addEventFilter(KeyEvent.KEY_PRESSED, keyEventEventHandler);
			content.addEventFilter(KeyEvent.KEY_PRESSED, keyEventEventHandler);
			if (defaultNode != null)
				defaultNode.requestFocus();
		}
	}

	public DialogBuilder dialog(String title) {
		return new DialogBuilder(title);
	}
}
