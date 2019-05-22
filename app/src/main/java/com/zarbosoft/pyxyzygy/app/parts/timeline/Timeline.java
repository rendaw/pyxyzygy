package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.config.NodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.ChildrenReplacer;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.app.widgets.binding.*;
import com.zarbosoft.pyxyzygy.app.wrappers.camera.CameraWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.paletteimage.PaletteImageNodeWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.truecolorimage.TrueColorImageNodeWrapper;
import com.zarbosoft.pyxyzygy.core.model.v0.*;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zarbosoft.pyxyzygy.app.Global.logger;
import static com.zarbosoft.pyxyzygy.app.Misc.opt;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.icon;
import static com.zarbosoft.rendaw.common.Common.sublist;

public class Timeline {
	private final ProjectContext context;
	private final Window window;
	public static final int extraFrames = 500;
	public static final double baseSize = 16;
	private final HBox toolBox;
	public final ChildrenReplacer<Node> toolBoxContents = new ChildrenReplacer<Node>() {
		@Override
		protected void innerSet(String title, List<Node> content) {
			toolBox.getChildren().addAll(content);
		}

		@Override
		protected void innerClear() {
			toolBox.getChildren().clear();
		}
	};
	private final BinderRoot rootFrame; // GC root
	private final BinderRoot rootOnionToggle; // GC root
	private final BinderRoot rootFramerate; // GC root
	private final BinderRoot rootPlaying; // GC root
	public double zoom = 16;

	VBox foreground = new VBox();
	ToolBar toolBar = new ToolBar();

	Pane scrub = new Pane();
	Group scrubElements = new Group();
	Rectangle frameMarker = new Rectangle(zoom, 0);
	public final static Color frameMarkerColor = HelperJFX.c(new java.awt.Color(195, 195, 195));
	List<Label> scrubOuterNumbers = new ArrayList<>();
	List<Label> scrubInnerNumbers = new ArrayList<>();
	List<Rectangle> scrubRegionMarkers = new ArrayList<>();
	List<Canvas> scrubRegions = new ArrayList<>();
	SimpleObjectProperty<FrameWidget> selectedFrame = new SimpleObjectProperty<>();
	private List<Runnable> editCleanup = new ArrayList<>();
	private final Button add;
	private final Button duplicate;
	private final Button remove;
	private final Button clear;
	private final Button left;
	private final Button right;
	private final SimpleIntegerProperty requestedMaxFrame = new SimpleIntegerProperty();
	private final SimpleIntegerProperty calculatedMaxFrame = new SimpleIntegerProperty();
	private final SimpleIntegerProperty useMaxFrame = new SimpleIntegerProperty();
	final SimpleIntegerProperty frame = new SimpleIntegerProperty();
	public final SimpleBooleanProperty playingProperty = new SimpleBooleanProperty(false);
	public PlayThread playThread;
	public BiMap<TreeItem<RowAdapter>, GroupChild> groupTreeItemLookup = HashBiMap.create();

	public Node getWidget() {
		return foreground;
	}

	public static SimpleObjectProperty<Image> emptyStateImage = new SimpleObjectProperty<>(null);

	TreeTableView<RowAdapter> tree = new TreeTableView<>();
	TreeTableColumn<RowAdapter, RowAdapter> nameColumn = new TreeTableColumn();
	Pane controlAlignment = new Pane();
	TreeTableColumn<RowAdapter, RowAdapter> framesColumn = new TreeTableColumn();
	ScrollBar timeScroll = new ScrollBar();
	private TimeMapper outerTimeHandle;

	public double getTimelineX(MouseEvent e) {
		Point2D corner = scrubElements.getLocalToSceneTransform().transform(0, 0);
		return e.getSceneX() - corner.getX();
	}

	public Timeline(ProjectContext context, Window window) {
		this.context = context;
		this.window = window;

		Stream.of(new Hotkeys.Action(Hotkeys.Scope.TIMELINE,
				"previous-frame",
				"Previous frame",
				Hotkeys.Hotkey.create(KeyCode.LEFT, false, false, false)
		) {
			@Override
			public void run(ProjectContext context, Window window) {
				frame.set(Math.max(0, frame.get() - 1));
			}
		}, new Hotkeys.Action(Hotkeys.Scope.TIMELINE,
				"next-frame",
				"Next frame",
				Hotkeys.Hotkey.create(KeyCode.RIGHT, false, false, false)
		) {
			@Override
			public void run(ProjectContext context, Window window) {
				frame.set(frame.get() + 1);
			}
		}, new Hotkeys.Action(Hotkeys.Scope.CANVAS,
				"play-toggle",
				"Play/pause",
				Hotkeys.Hotkey.create(KeyCode.P, false, false, false)
		) {
			@Override
			public void run(ProjectContext context, Window window) {
				playingProperty.set(!playingProperty.get());
			}
		}).forEach(context.hotkeys::register);

		window.selectedForViewMaxFrameBinder.addListener(newValue -> {
			Observable[] deps;
			if (newValue == null)
				deps = new Observable[] {requestedMaxFrame, calculatedMaxFrame};
			else
				deps = new Observable[] {
						requestedMaxFrame, calculatedMaxFrame, frame
				};
			useMaxFrame.bind(Bindings.createIntegerBinding(() -> {
				int out = 0;
				out = Math.max(out, requestedMaxFrame.get());
				out = Math.max(out, calculatedMaxFrame.get());
				if (newValue != null)
					out = Math.max(out, frame.get());
				return out + extraFrames;
			}, deps));
		});
		timeScroll.maxProperty().bind(useMaxFrame.multiply(zoom));
		tree.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {
			tree.requestFocus();
			e.consume();
		});
		tree.setRoot(new TreeItem<>());
		tree.setShowRoot(false);
		tree.getRoot().getChildren().addListener((ListChangeListener<TreeItem<RowAdapter>>) c -> {
			if (tree.getSelectionModel().getSelectedItem() == null && !tree.getRoot().getChildren().isEmpty())
				tree.getSelectionModel().select(tree.getRoot().getChildren().get(0));
		});
		scrub.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {
			tree.requestFocus();
			e.consume();
		});
		scrub.setBackground(Background.EMPTY);
		scrub.setMinHeight(30);
		scrub.getChildren().addAll(scrubElements);
		EventHandler<MouseEvent> mouseEventEventHandler = e -> {
			if (window.getSelectedForView() == null)
				return;
			frame.set(Math.max(0, (int) (getTimelineX(e) / zoom)));
			updateFrameMarker();
		};
		scrub.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEventEventHandler);
		scrub.addEventFilter(MouseEvent.MOUSE_DRAGGED, mouseEventEventHandler);
		add = HelperJFX.button("plus.png", "Add");
		BooleanBinding hasNoFrames = Bindings.createBooleanBinding(() -> {
			TreeItem<RowAdapter> selected = tree.getSelectionModel().getSelectedItem();
			if (selected == null)
				return true;
			if (selected.getValue() == null)
				return true;
			if (!selected.getValue().hasNormalFrames())
				return true;
			return false;
		}, tree.getSelectionModel().selectedItemProperty());
		add.disableProperty().bind(hasNoFrames);
		add.setOnAction(e -> {
			if (window.getSelectedForView() == null)
				return;
			context.change(null, change -> {
				tree
						.getSelectionModel()
						.getSelectedCells()
						.stream()
						.filter(c -> c.getTreeItem() != null &&
								c.getTreeItem().getValue() != null &&
								c.getTreeItem().getValue().createFrame(context, window, change, frame.get()))
						.findFirst();
			});
		});
		duplicate = HelperJFX.button("content-copy.png", "Duplicate");
		duplicate.disableProperty().bind(hasNoFrames);
		duplicate.setOnAction(e -> {
			if (window.getSelectedForView() == null)
				return;
			context.change(null, change -> {
				tree
						.getSelectionModel()
						.getSelectedCells()
						.stream()
						.filter(c -> c.getTreeItem() != null &&
								c.getTreeItem().getValue() != null &&
								c.getTreeItem().getValue().duplicateFrame(context, window, change, frame.get()))
						.findFirst();
			});
		});
		remove = HelperJFX.button("minus.png", "Remove");
		remove
				.disableProperty()
				.bind(Bindings.createBooleanBinding(() -> selectedFrame.get() == null ||
						selectedFrame.get().row.frames.size() == 1, selectedFrame));
		remove.setOnAction(e -> {
			context.change(null, change -> {
				selectedFrame.get().frame.remove(context, change);
			});
		});
		clear = HelperJFX.button("eraser-variant.png", "Clear");
		clear.disableProperty().bind(selectedFrame.isNull());
		clear.setOnAction(e -> {
			if (selectedFrame.get() == null)
				return;
			context.change(null, change -> {
				selectedFrame.get().frame.clear(context, change);
			});
		});
		left = HelperJFX.button("arrow-left.png", "Left");
		left
				.disableProperty()
				.bind(Bindings.createBooleanBinding(() -> selectedFrame.get() == null || selectedFrame.get().at == 0,
						selectedFrame
				));
		left.setOnAction(e -> {
			context.change(new ProjectContext.Tuple(selectedFrame.get().row.adapter.getData(), "move"), change -> {
				selectedFrame.get().frame.moveLeft(context, change);
			});
		});
		right = HelperJFX.button("arrow-right.png", "Right");
		right
				.disableProperty()
				.bind(Bindings.createBooleanBinding(() -> selectedFrame.get() == null ||
						selectedFrame.get().index == selectedFrame.get().row.frames.size() - 1, selectedFrame));
		right.setOnAction(e -> {
			context.change(new ProjectContext.Tuple(selectedFrame.get().row.adapter.getData(), "move"), change -> {
				selectedFrame.get().frame.moveRight(context, change);
			});
		});
		ToggleButton onion = new ToggleButton(null, new ImageView(icon("onion.png")));
		onion.setTooltip(new Tooltip("Toggle onion skin"));
		rootOnionToggle = CustomBinding.bindBidirectional(new IndirectBinder<Boolean>(window.selectedForEditOnionBinder,
				e -> Optional.ofNullable(e).map(e1 -> e1.getWrapper().getConfig().onionSkin)
		), new PropertyBinder<Boolean>(onion.selectedProperty()));
		toolBox = new HBox();

		Region space = new Region();
		space.setMinWidth(1);
		HBox.setHgrow(space, Priority.ALWAYS);

		HBox previewRate = new HBox();
		{
			previewRate.setAlignment(Pos.CENTER_LEFT);
			previewRate.setFillHeight(true);
			previewRate.setSpacing(3);
			Spinner<Integer> spinner = new Spinner<Integer>();
			spinner.setMaxWidth(Double.MAX_VALUE);
			spinner.setPrefWidth(60);
			spinner.setEditable(true);
			spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100));
			rootFramerate =
					CustomBinding.bindBidirectional(new IndirectBinder<>(window.selectedForEditFramerateBinder, v -> {
						if (v == null)
							return opt(null);
						Wrapper wrapper = v.getWrapper();
						if (wrapper instanceof CameraWrapper) {
							Camera camera = ((CameraWrapper) wrapper).node;
							return opt(new ScalarBinder<Integer>(camera,
									"frameRate",
									r -> context.change(new ProjectContext.Tuple(camera, "framerate"),
											c -> c.camera(camera).frameRateSet(r)
									)
							));
						} else {
							return opt(wrapper.getConfig().previewRate);
						}
					}), new PropertyBinder<Integer>(spinner.getValueFactory().valueProperty()));
			final ImageView imageView = new ImageView(icon("play-speed.png"));
			previewRate.getChildren().addAll(imageView, spinner);
		}

		Button previewPlay = new Button();
		previewPlay.setGraphic(new ImageView());
		Tooltip.install(previewPlay, new Tooltip("Play/stop"));
		previewPlay.setOnAction(e -> {
			playingProperty.set(!playingProperty.get());
		});

		toolBar
				.getItems()
				.addAll(add, duplicate, left, right, remove, clear, onion, toolBox, space, previewRate, previewPlay);
		nameColumn.setCellValueFactory(p -> new SimpleObjectProperty<>(p.getValue().getValue()));
		nameColumn.setCellFactory(param -> new TreeTableCell<RowAdapter, RowAdapter>() {
			final ImageView showViewing = new ImageView();

			{
				showViewing.setFitWidth(16);
				showViewing.setFitHeight(16);
				showViewing.setPreserveRatio(true);
				setGraphic(showViewing);
			}

			@Override
			protected void updateItem(RowAdapter item, boolean empty) {
				if (item == null) {
					showViewing.imageProperty().unbind();
					textProperty().unbind();
					setText("");
					return;
				}
				textProperty().bind(item.getName());
				showViewing.imageProperty().bind(item.getStateImage());
				super.updateItem(item, empty);
			}
		});
		nameColumn.setPrefWidth(200);
		framesColumn.setGraphic(controlAlignment);
		framesColumn.setCellValueFactory(p -> new SimpleObjectProperty<>(p.getValue().getValue()));
		framesColumn.setCellFactory(new Callback<TreeTableColumn<RowAdapter, RowAdapter>, TreeTableCell<RowAdapter, RowAdapter>>() {
			@Override
			public TreeTableCell<RowAdapter, RowAdapter> call(TreeTableColumn<RowAdapter, RowAdapter> param) {
				return new TreeTableCell<RowAdapter, RowAdapter>() {
					WidgetHandle rowControlsHandle;

					{
						setBackground(Background.EMPTY);
						setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
					}

					@Override
					protected void updateItem(RowAdapter item, boolean empty) {
						if (rowControlsHandle != null) {
							setGraphic(null);
							rowControlsHandle.remove();
							rowControlsHandle = null;
						}
						if (item == null) {
							return;
						}
						if (item.hasFrames()) {
							rowControlsHandle = item.createRowWidget(context, window);
							setGraphic(rowControlsHandle.getWidget());
							updateTimeMap();
						}
						super.updateItem(item, empty);
					}
				};
			}
		});
		tree.getColumns().addAll(nameColumn, framesColumn);
		CustomBinding.bindBidirectional(new IndirectBinder<>(window.selectedForEditPlayingBinder,
				e -> e.getWrapper() instanceof GroupNodeWrapper ?
						opt(((GroupNodeWrapper) e.getWrapper()).specificChild) :
						Optional.empty()
		), new SelectionModelBinder<>(tree.getSelectionModel()).<GroupChild>bimap(t -> {
			if (t == null) return Optional.empty();
			if (groupTreeItemLookup.containsKey(t)) {
				return opt(groupTreeItemLookup.get(t));
			} else if (groupTreeItemLookup.containsKey(t.getParent())) {
				return opt(groupTreeItemLookup.get(t.getParent()));
			}
			return Optional.<GroupChild>empty();
		}, c -> {
			if (groupTreeItemLookup.inverse().containsKey(c)) {
				return opt(groupTreeItemLookup.inverse().get(c));
			}
			return Optional.<TreeItem<RowAdapter>>empty();
		}));
		framesColumn
				.prefWidthProperty()
				.bind(tree
						.widthProperty()
						.subtract(nameColumn.widthProperty())
						.subtract(25 /* No way to get actual paint width? Also leave room for scrollbar :& */));
		foreground.getChildren().addAll(toolBar, tree, scrub, timeScroll);

		foreground.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (context.hotkeys.event(context, window, Hotkeys.Scope.TIMELINE, e))
				e.consume();
		});
		foreground.getStyleClass().addAll("part-timeline");

		timeScroll.setMin(0);
		scrubElements.layoutXProperty().bind(Bindings.createDoubleBinding(() -> {
			double corner = controlAlignment.localToScene(0, 0).getX();
			return corner - scrub.localToScene(0, 0).getX() - timeScroll.getValue() + Timeline.baseSize * 2;
		}, controlAlignment.localToSceneTransformProperty(), timeScroll.valueProperty()));
		frameMarker.heightProperty().bind(scrub.heightProperty());
		frameMarker.setFill(frameMarkerColor);
		frameMarker.setBlendMode(BlendMode.MULTIPLY);
		scrubElements.getChildren().add(frameMarker);
		timeScroll.visibleAmountProperty().bind(scrub.widthProperty().subtract(nameColumn.widthProperty()));

		rootFrame = CustomBinding.bind(frame, new IndirectHalfBinder<Number>(window.selectedForViewFrameBinder,
				e -> e == null ? Optional.empty() : opt(new PropertyHalfBinder<>(e.getWrapper().getConfig().frame))
		));
		final ChangeListener<Number> frameListener = new ChangeListener<>() {
			{
				changed(null, null, frame.get());
			}

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				updateFrameMarker();
			}
		};
		frame.addListener(frameListener);

		rootPlaying =
				new DoubleHalfBinder<Boolean, Pair<CanvasHandle, EditHandle>>(playingProperty, new DoubleHalfBinder<>(
						window.selectedForViewPlayingBinder,
						window.selectedForEditPlayingBinder
				)).addListener((Boolean playing, Pair<CanvasHandle, EditHandle> sel) -> {
					((ImageView) previewPlay.getGraphic()).setImage(playing ? icon("stop.png") : icon("play.png"));
					if (playThread != null)
						playThread.playing.set(false);
					playThread = null;
					if (playing) {
						Wrapper view = sel.first.getWrapper();
						Wrapper edit = sel.second.getWrapper();
						if (edit instanceof CameraWrapper) {
							playThread = new PlayThread(view) {
								Camera node = ((CameraWrapper) edit).node;

								@Override
								public PlayState updateState() {
									return new PlayState(1000 / node.frameRate(),
											node.frameStart(),
											node.frameLength()
									);
								}
							};
						} else {
							NodeConfig config = edit.getConfig();
							playThread = new PlayThread(view) {
								@Override
								public PlayState updateState() {
									return new PlayState(1000 / config.previewRate.get(),
											config.previewStart.get(),
											config.previewLength.get()
									);
								}
							};
						}
					}
				});
	}

	public static abstract class PlayThread extends Thread {
		private final NodeConfig config;
		AtomicBoolean playing = new AtomicBoolean(true);
		AtomicBoolean enqueued = new AtomicBoolean(false);

		public PlayThread(Wrapper view) {
			this.config = view.getConfig();
		}

		public class PlayState {
			public final long frameMs;
			public final int start;
			public final int length;

			public PlayState(long frameMs, int start, int length) {
				this.frameMs = frameMs;
				this.start = start;
				this.length = length;
			}
		}

		AtomicReference<PlayState> playState = new AtomicReference<>();

		{
			playState.set(updateState());
			setDaemon(true);
			start();
		}

		public abstract PlayState updateState();

		@Override
		public void run() {
			try {
				while (playing.get()) {
					if (!enqueued.getAndSet(true))
						Platform.runLater(() -> {
							try {
								PlayState playState = updateState();
								this.playState.set(playState);
								config.frame.set(Math.max(playState.start,
										(config.frame.get() - playState.start + 1) % playState.length + playState.start
								));
							} catch (Exception e) {
								logger.writeException(e, "Error in JavaFX play thread");
							} finally {
								enqueued.set(false);
							}
						});
					PlayState state = playState.get();
					long now = System.currentTimeMillis();
					Thread.sleep((now / state.frameMs + 1) * state.frameMs - now);
				}
			} catch (Exception e) {
				logger.writeException(e, "Error in system play thread");
			}
		}
	}

	public void cleanItemSubtree(TreeItem<RowAdapter> item) {
		item.getChildren().forEach(child -> cleanItemSubtree(child));
		groupTreeItemLookup.remove(item);
		if (item.getValue() != null)
			item.getValue().remove(context);
	}

	public void setNodes(CanvasHandle root1, EditHandle edit1) {
		updateFrameMarker();

		// Clean up everything
		select(null);
		cleanItemSubtree(tree.getRoot());
		editCleanup.forEach(Runnable::run);
		editCleanup.clear();
		tree.getRoot().getChildren().clear();
		if (outerTimeHandle != null) {
			outerTimeHandle.remove();
			outerTimeHandle = null;
		}
		groupTreeItemLookup.clear();

		if (root1 == null || edit1 == null)
			return;

		Wrapper root = root1.getWrapper();
		Wrapper edit = edit1.getWrapper();

		// Prepare time translation
		outerTimeHandle = createTimeMapper(root.getValue());
		outerTimeHandle.updateTime(ImmutableList.of(new FrameMapEntry(Global.NO_LENGTH, 0)));
		if (window.timeMap == null)
			throw new Assertion(); // DEBUG

		// Prepare rows
		if (edit instanceof GroupNodeWrapper) {
			TreeItem childrenRoot = new RowAdapter() {

				@Override
				public ObservableValue<String> getName() {
					return new SimpleStringProperty("Layers");
				}

				@Override
				public boolean hasFrames() {
					return false;
				}

				@Override
				public boolean hasNormalFrames() {
					return false;
				}

				@Override
				public boolean createFrame(
						ProjectContext context, Window window, ChangeStepBuilder change, int outer
				) {
					return false;
				}

				@Override
				public ObservableObjectValue<Image> getStateImage() {
					return emptyStateImage;
				}

				@Override
				public boolean duplicateFrame(
						ProjectContext context, Window window, ChangeStepBuilder change, int outer
				) {
					return false;
				}

				@Override
				public WidgetHandle createRowWidget(ProjectContext context, Window window) {
					return null;
				}

				@Override
				public int updateTime(ProjectContext context, Window window) {
					return 0;
				}

				@Override
				public void updateFrameMarker(ProjectContext context, Window window) {

				}

				@Override
				public void remove(ProjectContext context) {

				}

				@Override
				public boolean frameAt(Window window, int outer) {
					return false;
				}

				@Override
				public Object getData() {
					return null;
				}
			};
			editCleanup.add(((GroupLayer) edit.getValue()).mirrorChildren(childrenRoot.getChildren(), child -> {
				RowAdapterGroupChild childRowAdapter = new RowAdapterGroupChild((GroupNodeWrapper) edit, child);
				childRowAdapter.setExpanded(true);
				childRowAdapter.getChildren().addAll(new RowAdapterGroupChildTime(this, child, childRowAdapter),
						new RowAdapterGroupChildPosition(this, child, childRowAdapter)
				);
				groupTreeItemLookup.put(childRowAdapter, child);
				return childRowAdapter;
			}, this::cleanItemSubtree, Misc.noopConsumer()));
			RowAdapter loop;
			if (edit instanceof CameraWrapper) {
				loop = new RowAdapterCameraLoop(this, ((CameraWrapper) edit).node);
			} else if (edit.getClass() == GroupNodeWrapper.class) {
				loop = new RowAdapterPreview(this, edit);
			} else
				throw new Assertion();
			tree.getRoot().getChildren().addAll(loop, childrenRoot);
		} else if (edit instanceof TrueColorImageNodeWrapper) {
			tree.getRoot().getChildren().addAll(new RowAdapterPreview(this, edit),
					new RowAdapterTrueColorImageNode(this, (TrueColorImageLayer) edit.getValue())
			);
		} else if (edit instanceof PaletteImageNodeWrapper) {
			tree.getRoot().getChildren().addAll(new RowAdapterPreview(this, edit),
					new RowAdapterPaletteImageNode(this, (PaletteImageLayer) edit.getValue())
			);
		}

		tree.getSelectionModel().clearSelection();
		tree
				.getSelectionModel()
				.select(tree.getRoot().getChildren().size() > 1 ?
						tree.getRoot().getChildren().get(1) :
						tree.getRoot().getChildren().get(0));
	}

	private void updateFrameMarker() {
		CanvasHandle root = window.getSelectedForView();
		if (root == null)
			return;
		root.setFrame(context, frame.get());
		frameMarker.setLayoutX(frame.get() * zoom);
		new Consumer<TreeItem<RowAdapter>>() {
			@Override
			public void accept(TreeItem<RowAdapter> item) {
				item.getChildren().forEach(child -> accept(child));
				if (item.getValue() != null)
					item.getValue().updateFrameMarker(context, window);
			}
		}.accept(tree.getRoot());
	}

	public void updateTimeMap() {
		// Update rows
		this.calculatedMaxFrame.set(new Function<TreeItem<RowAdapter>, Integer>() {
			@Override
			public Integer apply(TreeItem<RowAdapter> item) {
				int out = 0;
				out = Math.max(out, item.getChildren().stream().mapToInt(child -> apply(child)).max().orElse(0));
				if (item.getValue() != null)
					out = Math.max(out, item.getValue().updateTime(context, window));
				return out;
			}
		}.apply(tree.getRoot()));

		// Update scrub bar
		final int step = 4;
		for (int i = 0; i < useMaxFrame.get() / step; ++i) {
			Label label;
			if (i >= scrubOuterNumbers.size()) {
				scrubOuterNumbers.add(label = new Label(Integer.toString(i * step)));
				scrubElements.getChildren().addAll(label);
				label.setPadding(new Insets(0, 0, 0, 2));
			} else {
				label = scrubOuterNumbers.get(i);
			}
			label.setLayoutX(i * step * zoom);
			label.setLayoutY(0);
			scrub
					.heightProperty()
					.addListener((observable, oldValue, newValue) -> label.setMinHeight(newValue.doubleValue()));
			label.setAlignment(Pos.TOP_LEFT);
		}

		int at = 0;
		int innerIndex = 0;
		int innerMarkIndex = 0;
		for (FrameMapEntry frame : window.timeMap) {
			if (at != 0) {
				// Draw time region markers
				Rectangle mark;
				if (innerMarkIndex >= scrubRegionMarkers.size()) {
					scrubRegionMarkers.add(mark = new Rectangle());
					scrubElements.getChildren().add(0, mark);
				} else {
					mark = scrubRegionMarkers.get(innerMarkIndex);
				}
				innerMarkIndex += 1;
				mark.setWidth(1);
				mark.heightProperty().bind(scrub.heightProperty());
				mark.fillProperty().setValue(HelperJFX.c(new java.awt.Color(35, 37, 112)));
				mark.setLayoutX(at * zoom);
			}

			// Draw times in region
			for (int i = 0; i <
					Math.max(1,
							(frame.length == Global.NO_LENGTH ? extraFrames : (frame.length - 2)) / step + 1
					); ++i) {
				Label label;
				if (innerIndex >= scrubInnerNumbers.size()) {
					scrubInnerNumbers.add(label = new Label());
					scrubElements.getChildren().add(label);
					label.setPadding(new Insets(0, 0, 0, 2));
				} else {
					label = scrubInnerNumbers.get(innerIndex);
				}
				innerIndex += 1;
				label.setText(Integer.toString((frame.innerOffset + i) * step));
				label.setLayoutX((at + i * step) * zoom);
				label.setLayoutY(0);
				label.minHeightProperty().bind(scrub.heightProperty());
				label.setAlignment(Pos.BOTTOM_LEFT);
			}
			at += frame.length;
		}

		// Cleanup unused markers, text
		if (innerIndex < scrubInnerNumbers.size()) {
			List<Label> temp = sublist(scrubInnerNumbers, innerIndex);
			scrubElements.getChildren().removeAll(temp);
			temp.clear();
		}
		if (innerMarkIndex < scrubRegionMarkers.size()) {
			List<Rectangle> temp = sublist(scrubRegionMarkers, innerMarkIndex);
			scrubElements.getChildren().removeAll(temp);
			temp.clear();
		}
	}

	public static List<FrameMapEntry> computeSubMap(List<FrameMapEntry> outerFrames, List<GroupTimeFrame> innerFrames) {
		return outerFrames.stream().flatMap(outer -> {
			if (outer.innerOffset == Global.NO_INNER)
				return Stream.of(outer);
			List<FrameMapEntry> subMap = new ArrayList<>();
			int at = 0;
			int outerRemaining = outer.length;
			for (GroupTimeFrame inner : innerFrames) {
				if (inner.length() != Global.NO_LENGTH && at + inner.length() <= outer.innerOffset) {
					at += inner.length();
					continue;
				}

				int innerOffset = inner.innerOffset();
				if (innerOffset != Global.NO_INNER && at < outer.innerOffset)
					innerOffset += outer.innerOffset - at;

				// Find out how much of the outer frame to fill up with this frame's overlap
				int maxLength = outerRemaining;
				if (inner.length() != Global.NO_LENGTH) {
					int effectiveFrameLength = inner.length();
					if (at < outer.innerOffset)
						effectiveFrameLength -= outer.innerOffset - at;
					if (maxLength == Global.NO_LENGTH || effectiveFrameLength < maxLength)
						maxLength = effectiveFrameLength;
				}

				// Find out how much a single frame can use of that space (or if multiple frames will be
				// needed)
				int useLength = maxLength;
				if (inner.innerLoop() != Global.NO_LOOP) {
					innerOffset = innerOffset % inner.innerLoop();
					int effectiveLoop = inner.innerLoop();
					if (at < outer.innerOffset) {
						effectiveLoop -= outer.innerOffset - at;
						effectiveLoop = Math.floorMod((effectiveLoop - 1), inner.innerLoop()) + 1;
					}
					if (useLength == Global.NO_LENGTH || effectiveLoop < useLength) {
						useLength = effectiveLoop;
					}
				}

				// Create the paint frames to fill up the designated range
				if (useLength == Global.NO_LENGTH) {
					subMap.add(new FrameMapEntry(Global.NO_LENGTH, innerOffset));
					break;
				} else {
					// Find out how much of the outer frame to practically fill up - maxLength is actually the
					// ideal fill
					int endAt = Math.max(outer.innerOffset, at);
					if (maxLength == Global.NO_LENGTH) {
						endAt += useLength + inner.innerLoop() * 4;
					} else {
						endAt += maxLength;
					}

					// Make paint frames for each loop + a cap if unbounded
					while (at < endAt) {
						subMap.add(new FrameMapEntry(useLength, innerOffset));
						if (outerRemaining != Global.NO_LENGTH)
							outerRemaining -= useLength;
						if (at < outer.innerOffset)
							at = outer.innerOffset;
						at += useLength;
						useLength = Math.min(inner.innerLoop(), endAt - at);
						innerOffset = 0;
					}
					if (maxLength == Global.NO_LENGTH)
						subMap.add(new FrameMapEntry(Global.NO_LENGTH, innerOffset));
				}

				if (outerRemaining == 0)
					break;
			}
			return subMap.stream();
		}).collect(Collectors.toList());
	}

	public TimeMapper createTimeMapper(ProjectObject object) {
		if (object == window.getSelectedForEdit().getWrapper().getValue())
			return createEndTimeHandle();
		if (false) {
			throw new Assertion();
		} else if (object instanceof GroupLayer) {
			return new TimeMapper() {
				private final Listener.Clear<GroupLayer> childrenClearListener;
				private final Listener.ListMoveTo<GroupLayer> childrenMoveToListener;
				private final Listener.ListRemove<GroupLayer> childrenRemoveListener;
				private final Listener.ListAdd<GroupLayer, GroupChild> childrenAddListener;
				TimeMapper child;

				{
					childrenAddListener =
							((GroupLayer) object).addChildrenAddListeners((target, at, value) -> relocate());
					childrenRemoveListener =
							((GroupLayer) object).addChildrenRemoveListeners((target, at, count) -> relocate());
					childrenMoveToListener =
							((GroupLayer) object).addChildrenMoveToListeners((target, source, count, dest) -> relocate());
					childrenClearListener = ((GroupLayer) object).addChildrenClearListeners(target -> relocate());
				}

				public void relocate() {
					if (child != null) {
						child.remove();
						child = null;
					}

					// Find the layer that leads to the edited node
					// First find the layer whose parent is this node
					Wrapper beforeAt = null;
					Wrapper at = window.getSelectedForEdit().getWrapper();
					while (at != null && at.getValue() != object) {
						beforeAt = at;
						at = at.getParent();
					}
					if (beforeAt == null)
						child = createEndTimeHandle();
					else
						child = createTimeMapper(beforeAt.getValue());
				}

				@Override
				public void remove() {
					if (child != null) {
						child.remove();
					}
					((GroupLayer) object).removeChildrenAddListeners(childrenAddListener);
					((GroupLayer) object).removeChildrenRemoveListeners(childrenRemoveListener);
					((GroupLayer) object).removeChildrenMoveToListeners(childrenMoveToListener);
					((GroupLayer) object).removeChildrenClearListeners(childrenClearListener);
				}

				@Override
				public void updateTime(List<FrameMapEntry> timeMap) {
					super.updateTime(timeMap);
					child.updateTime(timeMap);
				}
			};
		} else if (object instanceof GroupChild) {
			return new TimeMapper() {
				private final Listener.ScalarSet<GroupChild, ProjectLayer> innerSetListener;
				Runnable framesCleanup;
				private List<Runnable> frameCleanup = new ArrayList<>();
				private TimeMapper child;
				int suppressRecalc = 0;

				{
					child = createTimeMapper(((GroupChild) object).inner());

					innerSetListener =
							((GroupChild) object).addInnerSetListeners((GroupChild target, ProjectLayer value) -> {
								if (child != null)
									child.remove();
								if (value ==
										Optional
												.ofNullable(window.getSelectedForEdit())
												.map(e -> e.getWrapper().getValue())
												.orElse(null))
									child = createTimeMapper(value);
								else
									child = createEndTimeHandle();
								recalcTimes();
							});

					suppressRecalc += 1;
					framesCleanup = ((GroupChild) object).mirrorTimeFrames(frameCleanup, frame -> {
						suppressRecalc += 1;
						Listener.ScalarSet<GroupTimeFrame, Integer> innerOffsetSetListener =
								frame.addInnerOffsetSetListeners((target, value) -> recalcTimes());
						Listener.ScalarSet<GroupTimeFrame, Integer> lengthSetListener =
								frame.addLengthSetListeners((target, value) -> recalcTimes());
						Listener.ScalarSet<GroupTimeFrame, Integer> loopListener =
								frame.addInnerLoopSetListeners((target, value) -> recalcTimes());
						suppressRecalc -= 1;
						return () -> {
							frame.removeInnerOffsetSetListeners(innerOffsetSetListener);
							frame.removeLengthSetListeners(lengthSetListener);
							frame.removeInnerLoopSetListeners(loopListener);
						};
					}, c -> c.run(), at -> {
						recalcTimes();
					});
					suppressRecalc -= 1;
				}

				public void recalcTimes() {
					if (timeMap == null)
						return;
					if (suppressRecalc != 0)
						return;
					child.updateTime(computeSubMap(timeMap, ((GroupChild) object).timeFrames()));
				}

				@Override
				public void remove() {
					if (child != null)
						child.remove();
					framesCleanup.run();
					frameCleanup.forEach(c -> c.run());
					((GroupChild) object).removeInnerSetListeners(innerSetListener);
				}

				@Override
				public void updateTime(List<FrameMapEntry> timeMap) {
					super.updateTime(timeMap);
					recalcTimes();
				}
			};
		} else if (object instanceof TrueColorImageLayer) {
			// Should be == edit node, or one of it's parents should == edit node
			// and this should never be reached
			throw new Assertion();
		} else if (object instanceof TrueColorImageFrame) {
			throw new Assertion();
		} else if (object instanceof PaletteImageLayer) {
			// Should be == edit node, or one of it's parents should == edit node
			// and this should never be reached
			throw new Assertion();
		} else if (object instanceof PaletteImageFrame) {
			throw new Assertion();
		} else
			throw new Assertion();
	}

	private TimeMapper createEndTimeHandle() {
		return new TimeMapper() {
			@Override
			public void updateTime(List<FrameMapEntry> timeMap) {
				window.timeMap = timeMap;
			}

			@Override
			public void remove() {
				window.timeMap = new ArrayList<>();
				window.timeMap.add(new FrameMapEntry(Global.NO_LENGTH, 0));
			}
		};
	}

	public void select(FrameWidget frame) {
		if (selectedFrame.get() != null && (frame == null || selectedFrame.get() != frame)) {
			selectedFrame.get().deselect();
		}
		selectedFrame.set(frame);
		if (frame != null) {
			frame.select();
			this.frame.set(frame.at);
		}
	}
}
