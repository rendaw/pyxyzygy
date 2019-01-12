package com.zarbosoft.shoedemo.parts.timeline;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.DeadCode;
import com.zarbosoft.shoedemo.*;
import com.zarbosoft.shoedemo.model.*;
import com.zarbosoft.shoedemo.structuretree.CameraWrapper;
import com.zarbosoft.shoedemo.structuretree.GroupNodeWrapper;
import com.zarbosoft.shoedemo.structuretree.ImageNodeWrapper;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
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
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zarbosoft.rendaw.common.Common.sublist;
import static com.zarbosoft.shoedemo.Main.*;
import static javafx.scene.paint.Color.BLUE;

public class Timeline {
	private final ProjectContext context;
	private final Window window;
	public static final int extraFrames = 500;
	public static final double baseSize = 16;
	public double zoom = 16;

	VBox foreground = new VBox();
	ToolBar toolBar = new ToolBar();

	Pane scrub = new Pane();
	Group scrubElements = new Group();
	Rectangle frameMarker = new Rectangle(zoom, 0);
	public final static Color frameMarkerColor = Main.c(new java.awt.Color(195, 195, 195));
	List<Label> scrubOuterNumbers = new ArrayList<>();
	List<Label> scrubInnerNumbers = new ArrayList<>();
	List<Rectangle> scrubRegionMarkers = new ArrayList<>();
	List<Canvas> scrubRegions = new ArrayList<>();
	SimpleObjectProperty<FrameWidget> selectedFrame = new SimpleObjectProperty<>();
	private Runnable editHandle = null;
	private final Button add;
	private final Button duplicate;
	private final Button remove;
	private final Button clear;
	private final Button left;
	private final Button right;
	private final SimpleIntegerProperty requestedMaxFrame = new SimpleIntegerProperty();
	private final SimpleIntegerProperty calculatedMaxFrame = new SimpleIntegerProperty();
	private final SimpleIntegerProperty useMaxFrame = new SimpleIntegerProperty();

	public Node getWidget() {
		return foreground;
	}

	public static SimpleObjectProperty<Image> emptyStateImage = new SimpleObjectProperty<>(null);

	TreeTableView<RowAdapter> tree = new TreeTableView<>();
	TreeTableColumn<RowAdapter, RowAdapter> nameColumn = new TreeTableColumn();
	Pane controlAlignment = new Pane();
	TreeTableColumn<RowAdapter, RowAdapter> framesColumn = new TreeTableColumn();
	ScrollBar timeScroll = new ScrollBar();
	private TimeAdapterNode outerTimeHandle;

	public Timeline(ProjectContext context, Window window) {
		this.context = context;
		this.window = window;
		window.selectedForView.addListener(new ChangeListener<Wrapper>() {
			{
				changed(null,null ,window.selectedForView.get() );
			}

			@Override
			public void changed(
					ObservableValue<? extends Wrapper> observable, Wrapper oldValue, Wrapper newValue
			) {
				Observable[] deps;
				if (newValue == null)
					deps = new Observable[] {requestedMaxFrame, calculatedMaxFrame};
				else
					deps = new Observable[] {
							requestedMaxFrame,
							calculatedMaxFrame,
							window.selectedForView.get().frame
					};
				useMaxFrame.bind(Bindings.createIntegerBinding(() -> {
					int out = 0;
					out = Math.max(out, requestedMaxFrame.get());
					out = Math.max(out, calculatedMaxFrame.get());
					if (newValue != null)
						out = Math.max(out, window.selectedForView.get().frame.get());
					return out + extraFrames;
				}, deps));
			}
		});
		timeScroll.maxProperty().bind(useMaxFrame.multiply(zoom));
		tree.setRoot(new TreeItem<>());
		tree.setShowRoot(false);
		scrub.setBackground(Background.EMPTY);
		scrub.setMinHeight(30);
		scrub.getChildren().addAll(scrubElements);
		EventHandler<MouseEvent> mouseEventEventHandler = e -> {
			if (window.selectedForView.get() == null)
				return;
			Point2D corner = scrubElements.getLocalToSceneTransform().transform(0, 0);
			window.selectedForView.get().frame.set(Math.max(0, (int) ((e.getSceneX() - corner.getX()) / zoom)));
			updateFrameMarker();
		};
		scrub.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEventEventHandler);
		scrub.addEventFilter(MouseEvent.MOUSE_DRAGGED, mouseEventEventHandler);
		scrub.addEventFilter(KeyEvent.KEY_TYPED, e -> {
			switch (e.getCode()) {
				case LEFT:
					window.selectedForView.get().frame.set(Math.max(0, window.selectedForView.get().frame.get() - 1));
					break;
				case RIGHT:
					window.selectedForView.get().frame.set(window.selectedForView.get().frame.get() + 1);
					break;
			}
		});
		add = com.zarbosoft.shoedemo.Window.button("plus.svg", "Add");
		add.setOnAction(e -> {
			if (window.selectedForView.get() == null)
				return;
			tree
					.getSelectionModel()
					.getSelectedCells()
					.stream()
					.filter(c -> c.getTreeItem() != null &&
							c.getTreeItem().getValue() != null &&
							c.getTreeItem().getValue().createFrame(context, window, window.selectedForView.get().frame.get()))
					.findFirst();
		});
		duplicate = com.zarbosoft.shoedemo.Window.button("content-copy.svg", "Duplicate");
		duplicate.setOnAction(e -> {
			if (window.selectedForView.get() == null)
				return;
			tree
					.getSelectionModel()
					.getSelectedCells()
					.stream()
					.filter(c -> c.getTreeItem() != null &&
							c.getTreeItem().getValue() != null &&
							c
									.getTreeItem()
									.getValue()
									.duplicateFrame(context, window,window.selectedForView.get().frame.get()))
					.findFirst();
		});
		remove = com.zarbosoft.shoedemo.Window.button("minus.svg", "Remove");
		remove.setOnAction(e -> {
			if (selectedFrame.get() == null)
				return;
			selectedFrame.get().frame.remove(context);
			selectedFrame.set(null);
		});
		clear = com.zarbosoft.shoedemo.Window.button("eraser-variant.svg", "Clear");
		clear.setOnAction(e -> {
			if (selectedFrame.get() == null)
				return;
			selectedFrame.get().frame.clear(context);
		});
		left = com.zarbosoft.shoedemo.Window.button("arrow-left.svg", "Left");
		left.setOnAction(e -> {
			if (selectedFrame.get() == null)
				return;
			selectedFrame.get().frame.moveLeft(context);
		});
		right = Window.button("arrow-right.svg", "Right");
		right.setOnAction(e -> {
			if (selectedFrame.get() == null)
				return;
			selectedFrame.get().frame.moveRight(context);
		});
		updateButtons();
		toolBar.getItems().addAll(add, duplicate, left, right, remove, clear);
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
							rowControlsHandle = item.createRowWidget(context,window);
							setGraphic(rowControlsHandle.getWidget());
							updateTime();
						}
						super.updateItem(item, empty);
					}
				};
			}
		});
		tree.getColumns().addAll(nameColumn, framesColumn);
		tree.getSelectionModel().getSelectedItems().addListener((ListChangeListener<TreeItem<RowAdapter>>) c -> {
			while (c.next()) {
				for (TreeItem<RowAdapter> removed : c.getRemoved()) {
					if (removed == null)
						continue; // ??
					if (removed.getValue() == null)
						continue;
					removed.getValue().deselected();
				}
				for (TreeItem<RowAdapter> added : c.getAddedSubList()) {
					if (added == null)
						continue; // ??
					if (added.getValue() == null)
						continue;
					added.getValue().selected();
				}
			}
			updateButtons();
		});
		framesColumn
				.prefWidthProperty()
				.bind(tree
						.widthProperty()
						.subtract(nameColumn.widthProperty())
						.subtract(25 /* No way to get actual inner width? Also leave room for scrollbar :& */));
		foreground.getChildren().addAll(toolBar, tree, scrub, timeScroll);

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

		window.selectedForView.addListener((observable, oldValue, newValue) -> setNodes(newValue,
				window.selectedForEdit.get()
		));
		window.selectedForEdit.addListener((observable, oldValue, newValue) -> setNodes(window.selectedForView.get(),
				newValue
		));
		if (window.selectedForEdit.get() != null && window.selectedForView.get() != null)
			setNodes(window.selectedForView.get(), window.selectedForEdit.get());
	}

	public void cleanItemSubtree(TreeItem<RowAdapter> item) {
		item.getChildren().forEach(child -> cleanItemSubtree(child));
		if (item.getValue() != null)
			item.getValue().remove(context);
	}

	public void setNodes(Wrapper root, Wrapper edit) {
		updateFrameMarker();

		// Clean up everything
		cleanItemSubtree(tree.getRoot());
		if (editHandle != null) {
			editHandle.run();
			editHandle = null;
		}
		tree.getRoot().getChildren().clear();
		if (outerTimeHandle != null) {
			outerTimeHandle.remove();
			outerTimeHandle = null;
		}

		if (root == null || edit == null)
			return;

		// Prepare time translation
		outerTimeHandle = createTimeHandle(root.getValue());
		outerTimeHandle.updateTime(ImmutableList.of(new FrameMapEntry(NO_LENGTH, 0)));
		if (window.timeMap == null)
			throw new Assertion(); // DEBUG

		// Prepare rows
		if (false) {
			throw new DeadCode();
		} else if (edit instanceof CameraWrapper) {
			// nop
		} else if (edit instanceof GroupNodeWrapper) {
			editHandle = ((GroupNode) edit.getValue()).mirrorLayers(tree.getRoot().getChildren(), layer -> {
				RowAdapterGroupLayer layerRowAdapter = new RowAdapterGroupLayer((GroupNodeWrapper) edit, layer);
				TreeItem<RowAdapter> layerItem = new TreeItem(layerRowAdapter);
				layerItem.setExpanded(true);
				layerItem.getChildren().addAll(new TreeItem(new RowAdapterGroupLayerTime(this, layer, layerRowAdapter)),
						new TreeItem(new RowAdapterGroupLayerPosition(this, layer, layerRowAdapter))
				);
				return layerItem;
			}, this::cleanItemSubtree, noopConsumer());
		} else if (edit instanceof ImageNodeWrapper) {
			tree.getRoot().getChildren().add(new TreeItem(new RowAdapterImageNode(this, (ImageNode) edit.getValue())));
		}

		updateTime();
	}

	private void updateButtons() {
		Wrapper root = window.selectedForView.get();
		TreeItem<RowAdapter> nowSelected = tree.getSelectionModel().getSelectedItem();
		boolean noSelection = root == null ||
				nowSelected == null ||
				nowSelected.getValue() == null ||
				!nowSelected.getValue().hasFrames();
		add.setDisable(noSelection);
		duplicate.setDisable(noSelection);
		left.setDisable(noSelection);
		right.setDisable(noSelection);
		clear.setDisable(noSelection);
		remove.setDisable(noSelection);
	}

	private void updateFrameMarker() {
		Wrapper root = window.selectedForView.get();
		if (root == null)
			return;
		root.setFrame(context, root.frame.get());
		frameMarker.setLayoutX(root.frame.getValue() * zoom);
		new Consumer<TreeItem<RowAdapter>>() {
			@Override
			public void accept(TreeItem<RowAdapter> item) {
				item.getChildren().forEach(child -> accept(child));
				if (item.getValue() != null)
					item.getValue().updateFrameMarker(context, window);
			}
		}.accept(tree.getRoot());
	}

	public void updateTime() {
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
				mark.fillProperty().setValue(c(new java.awt.Color(35, 37, 112)));
				mark.setLayoutX(at * zoom);
			}

			// Draw times in region
			for (int i = 0; i < Math.max(1, (frame.length == NO_LENGTH ? extraFrames : (frame.length - 2)) / step + 1); ++i) {
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
			if (outer.innerOffset == NO_INNER)
				return Stream.of(outer);
			List<FrameMapEntry> subMap = new ArrayList<>();
			int at = 0;
			int outerRemaining = outer.length;
			for (GroupTimeFrame inner : innerFrames) {
				if (inner.length() != NO_LENGTH && at + inner.length() <= outer.innerOffset) {
					at += inner.length();
					continue;
				}

				int innerOffset = inner.innerOffset();
				if (innerOffset != NO_INNER && at < outer.innerOffset)
					innerOffset += outer.innerOffset - at;

				// Find out how much of the outer frame to fill up with this frame's overlap
				int maxLength = outerRemaining;
				if (inner.length() != NO_LENGTH) {
					int effectiveFrameLength = inner.length();
					if (at < outer.innerOffset)
						effectiveFrameLength -= outer.innerOffset - at;
					if (maxLength == NO_LENGTH || effectiveFrameLength < maxLength)
						maxLength = effectiveFrameLength;
				}

				// Find out how much a single frame can use of that space (or if multiple frames will be
				// needed)
				int useLength = maxLength;
				if (inner.innerLoop() != NO_LOOP) {
					innerOffset = innerOffset % inner.innerLoop();
					int effectiveLoop = inner.innerLoop();
					if (at < outer.innerOffset) {
						effectiveLoop -= outer.innerOffset - at;
						effectiveLoop = Math.floorMod((effectiveLoop - 1), inner.innerLoop()) + 1;
					}
					if (useLength == NO_LENGTH || effectiveLoop < useLength) {
						useLength = effectiveLoop;
					}
				}

				// Create the inner frames to fill up the designated range
				//System.out.format("(fr %s %s %s) use %s, max %s, in off %s; at %s\n", inner.length(), inner.innerOffset(), inner.innerLoop(), useLength, maxLength, innerOffset, at);
				if (useLength == NO_LENGTH) {
					subMap.add(new FrameMapEntry(NO_LENGTH, innerOffset));
					break;
				} else {
					// Find out how much of the outer frame to practically fill up - maxLength is actually the
					// ideal fill
					int endAt = Math.max(outer.innerOffset, at);
					if (maxLength == NO_LENGTH) {
						endAt += useLength + inner.innerLoop() * 4;
					} else {
						endAt += maxLength;
					}
					//System.out.format("\tendAt %s\n",endAt);

					// Make inner frames for each loop + a cap if unbounded
					while (at < endAt) {
						//System.out.format("\tloop %s %s\n", useLength, innerOffset);
						subMap.add(new FrameMapEntry(useLength, innerOffset));
						if (outerRemaining != NO_LENGTH)
							outerRemaining -= useLength;
						if (at < outer.innerOffset)
							at = outer.innerOffset;
						at += useLength;
						useLength = Math.min(inner.innerLoop(), endAt - at);
						innerOffset = 0;
					}
					if (maxLength == NO_LENGTH)
						subMap.add(new FrameMapEntry(NO_LENGTH, innerOffset));
				}

				if (outerRemaining == 0)
					break;
			}
			return subMap.stream();
		}).collect(Collectors.toList());
	}

	public TimeAdapterNode createTimeHandle(ProjectObject object) {
		if (object == window.selectedForEdit.getValue().getValue())
			return createEndTimeHandle();
		if (false) {
			throw new Assertion();
		} else if (object instanceof GroupNode) {
			return new TimeAdapterNode() {
				private GroupNode.LayersClearListener layersClearListener;
				private GroupNode.LayersMoveToListener layersMoveToListener;
				private GroupNode.LayersRemoveListener layersRemoveListener;
				private GroupNode.LayersAddListener layersAddListener;
				TimeAdapterNode child;

				{
					layersAddListener = (target, at, value) -> relocate();
					((GroupNode) object).addLayersAddListeners(layersAddListener);
					layersRemoveListener = (target, at, count) -> relocate();
					((GroupNode) object).addLayersRemoveListeners(layersRemoveListener);
					layersMoveToListener = (target, source, count, dest) -> relocate();
					((GroupNode) object).addLayersMoveToListeners(layersMoveToListener);
					layersClearListener = target -> relocate();
					((GroupNode) object).addLayersClearListeners(layersClearListener);
				}

				public void relocate() {
					if (child != null) {
						child.remove();
						child = null;
					}

					// Find the layer that leads to the edited node
					// First find the layer whose parent is this node
					Wrapper beforeAt = null;
					Wrapper at = window.selectedForEdit.get();
					while (at != null && at.getValue() != object) {
						beforeAt = at;
						at = at.getParent();
					}
					if (beforeAt == null)
						child = createEndTimeHandle();
					else
						child = createTimeHandle(beforeAt.getValue());
				}

				@Override
				public void remove() {
					if (child != null) {
						child.remove();
					}
					((GroupNode) object).removeLayersAddListeners(layersAddListener);
					((GroupNode) object).removeLayersRemoveListeners(layersRemoveListener);
					((GroupNode) object).removeLayersMoveToListeners(layersMoveToListener);
					((GroupNode) object).removeLayersClearListeners(layersClearListener);
				}

				@Override
				public void updateTime(List<FrameMapEntry> timeMap) {
					super.updateTime(timeMap);
					child.updateTime(timeMap);
				}
			};
		} else if (object instanceof GroupLayer) {
			return new TimeAdapterNode() {
				private GroupLayer.InnerSetListener innerSetListener;
				Runnable framesCleanup;
				private List<Runnable> frameCleanup = new ArrayList<>();
				private TimeAdapterNode child;
				int suppressRecalc = 0;

				{
					child = createTimeHandle(((GroupLayer) object).inner());

					innerSetListener = new GroupLayer.InnerSetListener() {
						@Override
						public void accept(GroupLayer target, ProjectNode value) {
							if (child != null)
								child.remove();
							if (value ==
									Optional
											.ofNullable(window.selectedForEdit.get())
											.map(e -> e.getValue())
											.orElse(null))
								child = createTimeHandle(value);
							else
								child = createEndTimeHandle();
							recalcTimes();
						}
					};
					((GroupLayer) object).addInnerSetListeners(innerSetListener);

					suppressRecalc += 1;
					framesCleanup = ((GroupLayer) object).mirrorTimeFrames(frameCleanup, frame -> {
						suppressRecalc += 1;
						GroupTimeFrame.InnerOffsetSetListener innerOffsetSetListener =
								frame.addInnerOffsetSetListeners((target, value) -> recalcTimes());
						GroupTimeFrame.LengthSetListener lengthSetListener =
								frame.addLengthSetListeners((target, value) -> recalcTimes());
						GroupTimeFrame.InnerLoopSetListener loopListener =
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
					child.updateTime(computeSubMap(timeMap, ((GroupLayer) object).timeFrames()));
				}

				@Override
				public void remove() {
					if (child != null)
						child.remove();
					framesCleanup.run();
					frameCleanup.forEach(c -> c.run());
					((GroupLayer) object).removeInnerSetListeners(innerSetListener);
				}

				@Override
				public void updateTime(List<FrameMapEntry> timeMap) {
					super.updateTime(timeMap);
					recalcTimes();
				}
			};
		} else if (object instanceof ImageNode) {
			// Should be == edit node, or one of it's parents should == edit node
			// and this should never be reached
			throw new Assertion();
		} else if (object instanceof ImageFrame) {
			throw new Assertion();
		} else
			throw new Assertion();
	}

	private TimeAdapterNode createEndTimeHandle() {
		return new TimeAdapterNode() {
			@Override
			public void updateTime(List<FrameMapEntry> timeMap) {
				window.timeMap = timeMap;
			}

			@Override
			public void remove() {
				window.timeMap = new ArrayList<>();
				window.timeMap.add(new FrameMapEntry(NO_LENGTH, 0));
			}
		};
	}

	public void select(FrameWidget frame) {
		if (selectedFrame.get() != null && selectedFrame.get().frame != frame.frame) {
			selectedFrame.get().deselect();
		}
		selectedFrame.set(frame);
		if (frame != null) {
			frame.select();
		}
	}
}
