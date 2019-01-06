package com.zarbosoft.shoedemo;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.DeadCode;
import com.zarbosoft.rendaw.common.Pair;
import com.zarbosoft.shoedemo.model.*;
import com.zarbosoft.shoedemo.structuretree.CameraWrapper;
import com.zarbosoft.shoedemo.structuretree.GroupLayerWrapper;
import com.zarbosoft.shoedemo.structuretree.GroupNodeWrapper;
import com.zarbosoft.shoedemo.structuretree.ImageNodeWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zarbosoft.rendaw.common.Common.last;
import static com.zarbosoft.rendaw.common.Common.sublist;
import static com.zarbosoft.shoedemo.Window.icon;
import static javafx.scene.paint.Color.*;

public class Timeline {
	private final ProjectContext context;
	private double zoom = 16;
	private Runnable editHandle;

	VBox foreground = new VBox();
	ToolBar toolBar = new ToolBar();

	Pane scrub = new Pane();
	Group scrubElements = new Group();
	Rectangle frameMarker = new Rectangle(zoom, 0);
	Color frameMarkerColor = c(new java.awt.Color(159, 123, 130));
	List<Label> scrubOuterNumbers = new ArrayList<>();
	List<Label> scrubInnerNumbers = new ArrayList<>();
	List<Rectangle> scrubRegionMarkers = new ArrayList<>();
	List<Canvas> scrubRegions = new ArrayList<>();
	Frame selectedFrameWidget = null;
	Object selectedId = null;
	private final Button add;
	private final Button duplicate;
	private final Button remove;
	private final Button clear;
	private final Button left;
	private final Button right;

	public Node getWidget() {
		return foreground;
	}

	public abstract static class RowAdapterFrame {
		public abstract int length();

		public abstract void setLength(int length);

		public abstract void remove();

		public abstract void clear();

		public abstract void moveLeft();

		public abstract void moveRight();

		public abstract Object id();
	}

	private static SimpleObjectProperty<Image> emptyStateImage = new SimpleObjectProperty<>(null);

	public abstract static class RowAdapter {
		public Optional<Row> row = Optional.empty();

		public abstract ObservableValue<String> getName();

		public abstract List<RowAdapterFrame> getFrames();

		public abstract boolean hasFrames();

		public abstract boolean createFrame(int outer);

		public abstract ObservableObjectValue<Image> getStateImage();

		public abstract void deselected();

		public abstract void selected();

		public abstract boolean duplciateFrame(int outer);
	}

	TreeTableView<RowAdapter> tree = new TreeTableView<>();
	TreeTableColumn<RowAdapter, RowAdapter> nameColumn = new TreeTableColumn();
	TreeTableColumn<RowAdapter, RowAdapter> framesColumn = new TreeTableColumn();
	ScrollBar timeScroll = new ScrollBar();
	private TimeHandleNode outerTimeHandle;

	List<Row> rows = new ArrayList<>();

	public Pair<Integer, FrameMapEntry> findTimeMapEntry(int outer) {
		int outerAt = 0;
		for (FrameMapEntry outerFrame : context.timeMap) {
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

	public static Color c(java.awt.Color source) {
		return Color.rgb(source.getRed(), source.getGreen(), source.getBlue());
	}

	Timeline(ProjectContext context) {
		this.context = context;
		tree.setRoot(new TreeItem<>());
		tree.setShowRoot(false);
		scrub.setBackground(Background.EMPTY);
		scrub.setMinHeight(30);
		scrub.getChildren().addAll(scrubElements);
		EventHandler<MouseEvent> mouseEventEventHandler = e -> {
			if (context.selectedForView.get() == null)
				return;
			Point2D corner = scrubElements.getLocalToSceneTransform().transform(0, 0);
			context.selectedForView.get().frame.set(Math.max(0, (int) ((e.getSceneX() - corner.getX()) / zoom)));
			updateFrameMarker();
		};
		scrub.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEventEventHandler);
		scrub.addEventFilter(MouseEvent.MOUSE_DRAGGED, mouseEventEventHandler);
		add = Window.button("plus.svg", "Add");
		add.setOnAction(e -> {
			if (context.selectedForView.get() == null)
				return;
			tree
					.getSelectionModel()
					.getSelectedCells()
					.stream()
					.filter(c -> c.getTreeItem() != null &&
							c.getTreeItem().getValue() != null &&
							c.getTreeItem().getValue().createFrame(context.selectedForView.get().frame.get()))
					.findFirst();
		});
		duplicate = Window.button("content-copy.svg", "Duplicate");
		duplicate.setOnAction(e -> {
			if (context.selectedForView.get() == null)
				return;
			tree
					.getSelectionModel()
					.getSelectedCells()
					.stream()
					.filter(c -> c.getTreeItem() != null &&
							c.getTreeItem().getValue() != null &&
							c.getTreeItem().getValue().duplciateFrame(context.selectedForView.get().frame.get()))
					.findFirst();
		});
		remove = Window.button("minus.svg", "Remove");
		remove.setOnAction(e -> {
			if (selectedFrameWidget == null)
				return;
			selectedFrameWidget.frame.remove();
			selectedFrameWidget = null;
		});
		clear = Window.button("eraser-variant.svg", "Clear");
		clear.setOnAction(e -> {
			if (selectedFrameWidget == null)
				return;
			selectedFrameWidget.frame.clear();
		});
		left = Window.button("arrow-left.svg", "Left");
		left.setOnAction(e -> {
			if (selectedFrameWidget == null)
				return;
			selectedFrameWidget.frame.moveLeft();
		});
		right = Window.button("arrow-right.svg", "Right");
		right.setOnAction(e -> {
			if (selectedFrameWidget == null)
				return;
			selectedFrameWidget.frame.moveRight();
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
				super.updateItem(item, empty);
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
		framesColumn.setCellValueFactory(p -> new SimpleObjectProperty<>(p.getValue().getValue()));
		framesColumn.setCellFactory(new Callback<TreeTableColumn<RowAdapter, RowAdapter>, TreeTableCell<RowAdapter, RowAdapter>>() {
			@Override
			public TreeTableCell<RowAdapter, RowAdapter> call(TreeTableColumn<RowAdapter, RowAdapter> param) {
				return new TreeTableCell<RowAdapter, RowAdapter>() {
					Row row = new Row();

					{
						setBackground(Background.EMPTY);
						setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
					}

					@Override
					protected void updateItem(RowAdapter item, boolean empty) {
						if (item == null || empty) {
							if (rows.contains(row)) {
								setGraphic(null);
								row.adapter.row = Optional.empty();
								row.adapter = null;
								rows.remove(row);
							}
							return;
						}
						if (item.hasFrames()) {
							row.adapter = item;
							item.row = Optional.of(row);
							setGraphic(row);
							if (!rows.contains(row))
								rows.add(row);
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
		framesColumn.prefWidthProperty().bind(tree.widthProperty().subtract(nameColumn.widthProperty()));
		foreground.getChildren().addAll(toolBar, tree, scrub, timeScroll);

		timeScroll.setMin(0);
		scrubElements.layoutXProperty().bind(timeScroll.valueProperty().multiply(-1.0).add(nameColumn.widthProperty()));
		scrubElements.getChildren().add(frameMarker);
		frameMarker.heightProperty().bind(scrub.heightProperty());
		frameMarker.setFill(frameMarkerColor);
		timeScroll.visibleAmountProperty().bind(scrub.widthProperty().subtract(nameColumn.widthProperty()));

		context.selectedForView.addListener((observable, oldValue, newValue) -> setNodes(newValue,
				context.selectedForEdit.get()
		));
		context.selectedForEdit.addListener((observable, oldValue, newValue) -> setNodes(context.selectedForView.get(),
				newValue
		));
	}

	private class Frame extends Canvas {
		private final Row row;
		private int index;
		private RowAdapterFrame frame;

		private int absStart;
		private int absEnd;
		private int minLength;

		public Frame(Row row) {
			this.row = row;
			addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
				if (selectedFrameWidget != this) {
					if (selectedFrameWidget != null)
						selectedFrameWidget.deselect();
					selectedFrameWidget = this;
					selectedId = this.frame.id();
					selectedFrameWidget.select();
				}
			});
			addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
				if (index == 0)
					return;
				double x = (e.getSceneX() - row.getLocalToSceneTransform().transform(0, 0).getX());
				int frame = (int) (x / zoom);
				if (absEnd != -1)
					frame = Math.min(frame, absEnd - 1);
				frame = Math.max(frame, absStart);
				int length = minLength + frame - absStart;
				this.row.frames.get(index - 1).frame.setLength(length);
			});
			addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
				context.history.finishChange();
			});
			setWidth(zoom);
			setHeight(zoom);
			deselect();
		}

		public void select() {
			GraphicsContext gc = getGraphicsContext2D();
			gc.clearRect(0, 0, getWidth(), getHeight());
			gc.setFill(PURPLE);
			gc.fillOval(2, 2, zoom - 4, zoom - 4);
		}

		public void deselect() {
			GraphicsContext gc = getGraphicsContext2D();
			gc.clearRect(0, 0, getWidth(), getHeight());
			gc.setFill(BLACK);
			gc.strokeOval(2, 2, zoom - 4, zoom - 4);
		}

		/**
		 * @param index
		 * @param frame
		 * @param absStart  Farthest left frame can be dragged
		 * @param absEnd    Farthest right frame can be dragged or -1
		 * @param minLength When dragged all the way to the left, what length does the frame's preceding frame get
		 * @param offset    Where to draw the frame relative to absStart
		 */
		public void set(int index, RowAdapterFrame frame, int absStart, int absEnd, int minLength, int offset) {
			this.index = index;
			this.frame = frame;
			this.absStart = absStart;
			this.absEnd = absEnd;
			this.minLength = minLength;
			setLayoutX((absStart + offset) * zoom);
		}
	}

	private class Row extends Pane {
		private Group inner = new Group();
		private RowAdapter adapter;
		private Rectangle frameMarker = new Rectangle(zoom, 0);
		private List<Frame> frames = new ArrayList<>();

		private Row() {
			framesColumn
					.widthProperty()
					.addListener((observable, oldValue, newValue) -> Row.this.setWidth(newValue.doubleValue()));
			inner.layoutXProperty().bind(timeScroll.valueProperty().multiply(-1.0));
			setMinHeight(zoom);
			frameMarker.setHeight(zoom);
			frameMarker.setFill(frameMarkerColor);
			inner.getChildren().add(frameMarker);
			getChildren().addAll(inner);
		}

		/**
		 * @return max frame encountered
		 */
		public int update() {
			Frame foundSelectedFrame = null;

			int frameIndex = 0;
			int outerAt = 0;
			for (FrameMapEntry outer : context.timeMap) {
				if (outer.innerOffset != -1) {
					int previousInnerAt = 0;
					int innerAt = 0;
					for (RowAdapterFrame inner : adapter.getFrames()) {
						int innerLeft = Math.max(previousInnerAt + 1, outer.innerOffset);
						int offset = innerAt - outer.innerOffset;
						if (outer.length != -1 && offset >= outer.length)
							break;
						Frame frame;
						int useFrameIndex = frameIndex++;
						if (frames.size() <= useFrameIndex) {
							frames.add(frame = new Frame(this));
							this.inner.getChildren().add(frame);
						} else {
							frame = frames.get(useFrameIndex);
							if (frame.frame.id() == selectedId) {
								foundSelectedFrame = frame;
							}
						}
						System.out.format("fr set il %s; inn off %s; outer at %s; prev inn at %s; inn at %s\n",
								innerLeft,
								outer.innerOffset,
								outerAt,
								previousInnerAt,
								innerAt
						);
						frame.set(useFrameIndex,
								inner,
								outerAt + innerLeft - outer.innerOffset,
								outer.length == -1 ? -1 : outerAt + outer.length,
								innerLeft - previousInnerAt,
								innerAt - innerLeft
						);
						previousInnerAt = innerAt;
						innerAt += inner.length();
					}
				}
				if (outer.length != -1)
					outerAt += outer.length;
			}

			if (selectedFrameWidget != foundSelectedFrame) {
				if (selectedFrameWidget != null)
					selectedFrameWidget.deselect();
				selectedFrameWidget = foundSelectedFrame;
				if (selectedFrameWidget != null)
					selectedFrameWidget.select();
			}

			if (frameIndex < frames.size()) {
				List<Frame> remove = sublist(frames, frameIndex);
				for (Frame frame : remove) {
					if (selectedFrameWidget == frame) {
						selectedFrameWidget = null;
					}
				}
				this.inner.getChildren().removeAll(remove);
				remove.clear();
			}

			return outerAt;
		}

		public void updateFrameMarker() {
			if (context.selectedForView.get() == null)
				return;
			frameMarker.setLayoutX(context.selectedForView.get().frame.getValue() * zoom);
		}
	}

	public void setNodes(Wrapper root, Wrapper edit) {
		updateFrameMarker();

		// Clean up everything
		tree.getRoot().getChildren().clear();
		if (outerTimeHandle != null) {
			outerTimeHandle.remove();
			outerTimeHandle = null;
		}
		if (editHandle != null) {
			editHandle.run();
			editHandle = null;
		}

		if (root == null || edit == null)
			return;

		// Prepare time translation
		outerTimeHandle = createTimeHandle(root.getValue());
		outerTimeHandle.updateTime(ImmutableList.of(new FrameMapEntry(-1, 0)));

		// Prepare rows
		if (false) {
			throw new DeadCode();
		} else if (edit instanceof CameraWrapper) {
			// nop
		} else if (edit instanceof GroupNodeWrapper) {
			class GroupLayerRowAdapter extends RowAdapter {
				private final GroupLayer layer;
				List<Runnable> cleanup = new ArrayList<>();
				SimpleObjectProperty<Image> stateImage = new SimpleObjectProperty<>(null);

				GroupLayerRowAdapter(GroupLayer layer) {
					this.layer = layer;
				}

				@Override
				public ObservableValue<String> getName() {
					SimpleStringProperty out = new SimpleStringProperty(layer.inner().name());
					ProjectNode.NameSetListener listener;
					layer.inner().addNameSetListeners(listener = (target, value) -> out.setValue(value));
					cleanup.add(() -> layer.inner().removeNameSetListeners(listener));
					return out;
				}

				@Override
				public List<RowAdapterFrame> getFrames() {
					return null;
				}

				@Override
				public boolean hasFrames() {
					return false;
				}

				@Override
				public boolean createFrame(int outer) {
					throw new Assertion();
				}

				@Override
				public boolean duplciateFrame(int outer) {
					throw new Assertion();
				}

				@Override
				public ObservableObjectValue<Image> getStateImage() {
					return stateImage;
				}

				@Override
				public void deselected() {
					treeDeselected();
				}

				@Override
				public void selected() {
					treeSelected();
				}

				public void treeDeselected() {
					((GroupNodeWrapper) edit).setSpecificLayer(null);
					stateImage.set(null);
				}

				public void treeSelected() {
					((GroupNodeWrapper) edit).setSpecificLayer(layer);
					stateImage.set(icon("cursor-move.svg"));
				}
			}
			editHandle = new Runnable() {
				List<Runnable> cleanup = new ArrayList<>();

				{
					cleanup.add(((GroupNode) edit.getValue()).mirrorLayers(tree.getRoot().getChildren(), layer -> {
						GroupLayerRowAdapter layerRowAdapter = new GroupLayerRowAdapter(layer);
						TreeItem<RowAdapter> layerItem = new TreeItem(layerRowAdapter);
						TreeItem<RowAdapter> timeFramesItem = new TreeItem(new RowAdapter() {
							@Override
							public ObservableValue<String> getName() {
								return new ObservableValueBase<String>() {
									@Override
									public String getValue() {
										return "Time";
									}
								};
							}

							@Override
							public List<RowAdapterFrame> getFrames() {
								List<RowAdapterFrame> out = new ArrayList<>();
								for (int i0 = 0; i0 < layer.timeFramesLength(); ++i0) {
									final int i = i0;
									GroupTimeFrame f = layer.timeFramesGet(i);
									out.add(new RowAdapterFrame() {
										@Override
										public Object id() {
											return f;
										}

										@Override
										public int length() {
											return f.length();
										}

										@Override
										public void setLength(int length) {
											context.history.change(c -> c.groupTimeFrame(f).lengthSet(length));
										}

										@Override
										public void remove() {
											if (i == 0)
												return;
											context.history.change(c -> c.groupLayer(layer).timeFramesRemove(i, 1));
											if (i == layer.timeFramesLength())
												context.history.change(c -> c
														.groupTimeFrame(last(layer.timeFrames()))
														.lengthSet(-1));
										}

										@Override
										public void clear() {
											context.history.change(c -> c.groupTimeFrame(f).innerOffsetSet(0));
										}

										@Override
										public void moveLeft() {
											if (i == 0)
												return;
											GroupTimeFrame frameBefore = layer.timeFramesGet(i - 1);
											context.history.change(c -> c
													.groupLayer(layer)
													.timeFramesMoveTo(i, 1, i - 1));
											final int lengthThis = f.length();
											if (lengthThis == -1) {
												final int lengthBefore = frameBefore.length();
												context.history.change(c -> c
														.groupTimeFrame(f)
														.lengthSet(lengthBefore));
												context.history.change(c -> c
														.groupTimeFrame(frameBefore)
														.lengthSet(lengthThis));
											}
										}

										@Override
										public void moveRight() {
											if (i == layer.timeFramesLength() - 1)
												return;
											GroupTimeFrame frameAfter = layer.timeFramesGet(i + 1);
											context.history.change(c -> c
													.groupLayer(layer)
													.timeFramesMoveTo(i, 1, i + 1));
											final int lengthAfter = frameAfter.length();
											if (lengthAfter == -1) {
												final int lengthThis = f.length();
												context.history.change(c -> c.groupTimeFrame(f).lengthSet(lengthAfter));
												context.history.change(c -> c
														.groupTimeFrame(frameAfter)
														.lengthSet(lengthThis));
											}
										}
									});
								}
								return out;
							}

							@Override
							public boolean hasFrames() {
								return true;
							}

							@Override
							public boolean createFrame(int outer) {
								return createFrame(outer, previous -> GroupTimeFrame.create(context));
							}

							@Override
							public boolean duplciateFrame(int outer) {
								return createFrame(outer, previous -> {
									GroupTimeFrame created = GroupTimeFrame.create(context);
									created.initialInnerOffsetSet(context, previous.innerOffset());
									return created;
								});
							}

							public boolean createFrame(int outer, Function<GroupTimeFrame, GroupTimeFrame> cb) {
								int inner = timeToInner(outer);
								GroupLayerWrapper.TimeResult previous = GroupLayerWrapper.findTime(layer, inner);
								GroupTimeFrame newFrame = cb.apply(previous.frame);
								int offset = inner - previous.at;
								if (previous.frame.length() == -1) {
									context.history.change(c -> c.groupTimeFrame(previous.frame).lengthSet(offset));
									newFrame.initialLengthSet(context, -1);
								} else {
									newFrame.initialLengthSet(context, previous.frame.length() - offset);
									context.history.change(c -> c.groupTimeFrame(previous.frame).lengthSet(offset));
								}
								newFrame.initialInnerOffsetSet(context, previous.frame.innerOffset() + offset);
								context.history.change(c -> c
										.groupLayer(layer)
										.timeFramesAdd(previous.frameIndex + 1, newFrame));
								return true;
							}

							@Override
							public ObservableObjectValue<Image> getStateImage() {
								return emptyStateImage;
							}

							@Override
							public void deselected() {
								layerRowAdapter.treeDeselected();
								;
							}

							@Override
							public void selected() {
								layerRowAdapter.treeSelected();
							}
						});
						layerItem.getChildren().add(timeFramesItem);
						TreeItem<RowAdapter> positionFramesItem = new TreeItem(new RowAdapter() {
							@Override
							public ObservableValue<String> getName() {
								return new ObservableValueBase<String>() {
									@Override
									public String getValue() {
										return "Position";
									}
								};
							}

							@Override
							public List<RowAdapterFrame> getFrames() {
								List<RowAdapterFrame> out = new ArrayList<>();
								for (int i0 = 0; i0 < layer.positionFramesLength(); ++i0) {
									final int i = i0;
									GroupPositionFrame f = layer.positionFramesGet(i);
									out.add(new RowAdapterFrame() {
										@Override
										public Object id() {
											return f;
										}

										@Override
										public int length() {
											return f.length();
										}

										@Override
										public void setLength(int length) {
											context.history.change(c -> c.groupPositionFrame(f).lengthSet(length));
										}

										@Override
										public void remove() {
											if (i == 0)
												return;
											context.history.change(c -> c.groupLayer(layer).positionFramesRemove(i, 1));
											if (i == layer.positionFramesLength())
												context.history.change(c -> c
														.groupPositionFrame(last(layer.positionFrames()))
														.lengthSet(-1));
										}

										@Override
										public void clear() {
											context.history.change(c -> c
													.groupPositionFrame(f)
													.offsetSet(new Vector(0, 0)));
										}

										@Override
										public void moveLeft() {
											if (i == 0)
												return;
											GroupPositionFrame frameBefore = layer.positionFramesGet(i - 1);
											context.history.change(c -> c
													.groupLayer(layer)
													.positionFramesMoveTo(i, 1, i - 1));
											final int lengthThis = f.length();
											if (lengthThis == -1) {
												final int lengthBefore = frameBefore.length();
												context.history.change(c -> c
														.groupPositionFrame(f)
														.lengthSet(lengthBefore));
												context.history.change(c -> c
														.groupPositionFrame(frameBefore)
														.lengthSet(lengthThis));
											}
										}

										@Override
										public void moveRight() {
											if (i == layer.positionFramesLength() - 1)
												return;
											GroupPositionFrame frameAfter = layer.positionFramesGet(i + 1);
											context.history.change(c -> c
													.groupLayer(layer)
													.positionFramesMoveTo(i, 1, i + 1));
											final int lengthAfter = frameAfter.length();
											if (lengthAfter == -1) {
												final int lengthThis = f.length();
												context.history.change(c -> c
														.groupPositionFrame(f)
														.lengthSet(lengthAfter));
												context.history.change(c -> c
														.groupPositionFrame(frameAfter)
														.lengthSet(lengthThis));
											}
										}
									});
								}
								return out;
							}

							@Override
							public boolean hasFrames() {
								return true;
							}

							@Override
							public boolean createFrame(int outer) {
								return createFrame(outer, previous -> GroupPositionFrame.create(context));
							}

							@Override
							public boolean duplciateFrame(int outer) {
								return createFrame(outer, previous -> {
									GroupPositionFrame created = GroupPositionFrame.create(context);
									created.initialOffsetSet(context, previous.offset());
									return created;
								});
							}

							public boolean createFrame(int outer, Function<GroupPositionFrame, GroupPositionFrame> cb) {
								int inner = timeToInner(outer);
								GroupLayerWrapper.PositionResult previous =
										GroupLayerWrapper.findPosition(layer, inner);
								GroupPositionFrame newFrame = cb.apply(previous.frame);
								int offset = inner - previous.at;
								if (offset <= 0)
									throw new Assertion();
								System.out.format("P create frame\n");
								layer.positionFrames().forEach(f -> System.out.format("  pos fr %s\n", f.length()));
								System.out.format("  offset %s; previous l %s; previous i %s\n",
										offset,
										previous.frame.length(),
										previous.frameIndex
								);
								if (previous.frame.length() == -1) {
									newFrame.initialLengthSet(context, -1);
								} else {
									newFrame.initialLengthSet(context, previous.frame.length() - offset);
								}
								context.history.change(c -> c.groupPositionFrame(previous.frame).lengthSet(offset));
								newFrame.initialOffsetSet(context, previous.frame.offset());
								context.history.change(c -> c
										.groupLayer(layer)
										.positionFramesAdd(previous.frameIndex + 1, newFrame));
								return true;
							}

							@Override
							public ObservableObjectValue<Image> getStateImage() {
								return emptyStateImage;
							}

							@Override
							public void deselected() {
								layerRowAdapter.treeDeselected();
							}

							@Override
							public void selected() {
								layerRowAdapter.treeSelected();
							}
						});
						layerItem.getChildren().add(positionFramesItem);

						GroupLayer.TimeFramesAddListener timeFramesAddListener =
								layer.addTimeFramesAddListeners((target, at, value) -> timeFramesItem.getValue().row.ifPresent(
										r -> r.update()));
						GroupLayer.TimeFramesRemoveListener timeFramesRemoveListener =
								layer.addTimeFramesRemoveListeners((target, at, count) -> timeFramesItem.getValue().row.ifPresent(
										r -> r.update()));
						GroupLayer.TimeFramesMoveToListener timeFramesMoveToListener =
								layer.addTimeFramesMoveToListeners((target, source, count, dest) -> timeFramesItem.getValue().row
										.ifPresent(r -> r.update()));
						GroupLayer.TimeFramesClearListener timeFramesClearListener =
								layer.addTimeFramesClearListeners(target -> timeFramesItem.getValue().row.ifPresent(r -> r
										.update()));

						GroupLayer.PositionFramesAddListener positionFramesAddListener =
								layer.addPositionFramesAddListeners((target, at, value) -> positionFramesItem.getValue().row
										.ifPresent(r -> r.update()));
						GroupLayer.PositionFramesRemoveListener positionFramesRemoveListener =
								layer.addPositionFramesRemoveListeners((target, at, count) -> positionFramesItem.getValue().row
										.ifPresent(r -> r.update()));
						GroupLayer.PositionFramesMoveToListener positionFramesMoveToListener =
								layer.addPositionFramesMoveToListeners((target, source, count, dest) -> positionFramesItem
										.getValue().row.ifPresent(r -> r.update()));
						GroupLayer.PositionFramesClearListener positionFramesClearListener =
								layer.addPositionFramesClearListeners(target -> positionFramesItem.getValue().row.ifPresent(
										r -> r.update()));
						cleanup.add(() -> {
							layer.removeTimeFramesAddListeners(timeFramesAddListener);
							layer.removeTimeFramesRemoveListeners(timeFramesRemoveListener);
							layer.removeTimeFramesMoveToListeners(timeFramesMoveToListener);
							layer.removeTimeFramesClearListeners(timeFramesClearListener);
							layer.removePositionFramesAddListeners(positionFramesAddListener);
							layer.removePositionFramesRemoveListeners(positionFramesRemoveListener);
							layer.removePositionFramesMoveToListeners(positionFramesMoveToListener);
							layer.removePositionFramesClearListeners(positionFramesClearListener);
						});

						return layerItem;
					}, item -> ((GroupLayerRowAdapter) item.getValue()).cleanup.forEach(c -> c.run())));
				}

				@Override
				public void run() {
					cleanup.forEach(c -> c.run());
					tree
							.getRoot()
							.getChildren()
							.stream()
							.map(c -> (GroupLayerRowAdapter) c.getValue())
							.forEach(v -> v.cleanup.forEach(c -> c.run()));
				}
			};
		} else if (edit instanceof ImageNodeWrapper) {
			ImageNode editNode = (ImageNode) edit.getValue();
			List<Runnable> frameCleanup = new ArrayList<>();

			RowAdapter imageAdapter = new RowAdapter() {
				@Override
				public ObservableValue<String> getName() {
					return new SimpleStringProperty("Frames");
				}

				@Override
				public List<RowAdapterFrame> getFrames() {
					List<RowAdapterFrame> out = new ArrayList<>();
					for (int i0 = 0; i0 < editNode.framesLength(); ++i0) {
						final int i = i0;
						ImageFrame f = editNode.framesGet(i);
						out.add(new RowAdapterFrame() {
							@Override
							public Object id() {
								return f;
							}

							@Override
							public int length() {
								return f.length();
							}

							@Override
							public void setLength(int length) {
								context.history.change(c -> c.imageFrame(f).lengthSet(length));
							}

							@Override
							public void remove() {
								if (i == 0)
									return;
								context.history.change(c -> c.imageNode(editNode).framesRemove(i, 1));
								if (i == editNode.framesLength())
									context.history.change(c -> c.imageFrame(last(editNode.frames())).lengthSet(-1));
							}

							@Override
							public void clear() {
								context.history.change(c -> c.imageFrame(f).tilesClear());
							}

							@Override
							public void moveLeft() {
								if (i == 0)
									return;
								ImageFrame frameBefore = editNode.framesGet(i - 1);
								context.history.change(c -> c.imageNode(editNode).framesMoveTo(i, 1, i - 1));
								final int lengthThis = f.length();
								if (lengthThis == -1) {
									final int lengthBefore = frameBefore.length();
									context.history.change(c -> c.imageFrame(f).lengthSet(lengthBefore));
									context.history.change(c -> c.imageFrame(frameBefore).lengthSet(lengthThis));
								}
							}

							@Override
							public void moveRight() {
								if (i == editNode.framesLength() - 1)
									return;
								ImageFrame frameAfter = editNode.framesGet(i + 1);
								context.history.change(c -> c.imageNode(editNode).framesMoveTo(i, 1, i + 1));
								final int lengthAfter = frameAfter.length();
								if (lengthAfter == -1) {
									final int lengthThis = f.length();
									context.history.change(c -> c.imageFrame(f).lengthSet(lengthAfter));
									context.history.change(c -> c.imageFrame(frameAfter).lengthSet(lengthThis));
								}
							}
						});
					}
					return out;
				}

				@Override
				public boolean hasFrames() {
					return true;
				}

				@Override
				public boolean createFrame(int outer) {
					return insertNewFrame(outer, previous -> {
						return ImageFrame.create(context);
					});
				}

				@Override
				public boolean duplciateFrame(int outer) {
					return insertNewFrame(outer, previous -> {
						ImageFrame created = ImageFrame.create(context);
						created.initialOffsetSet(context, previous.offset());
						created.initialTilesPutAll(context, previous.tiles());
						return created;
					});
				}

				private boolean insertNewFrame(int outer, Function<ImageFrame, ImageFrame> cb) {
					final int inner = timeToInner(outer);
					ImageNodeWrapper.FrameResult previous = ImageNodeWrapper.findFrame(editNode, inner);
					ImageFrame newFrame = cb.apply(previous.frame);
					int offset = inner - previous.at;
					if (previous.frame.length() == -1) {
						context.history.change(c -> c.imageFrame(previous.frame).lengthSet(offset));
						newFrame.initialLengthSet(context, -1);
					} else {
						newFrame.initialLengthSet(context, previous.frame.length() - offset);
						context.history.change(c -> c.imageFrame(previous.frame).lengthSet(offset));
					}
					context.history.change(c -> c.imageNode(editNode).framesAdd(previous.frameIndex + 1, newFrame));

					return true;
				}

				@Override
				public ObservableObjectValue<Image> getStateImage() {
					return emptyStateImage;
				}

				@Override
				public void deselected() {

				}

				@Override
				public void selected() {

				}
			};

			ImageNode.FramesAddListener framesAddListener = editNode.addFramesAddListeners((target, at, value) -> {
				frameCleanup.addAll(at, value.stream().map(f -> {
					ImageFrame.LengthSetListener listener =
							f.addLengthSetListeners((target1, value1) -> imageAdapter.row.ifPresent(r -> r.update()));
					return (Runnable) () -> f.removeLengthSetListeners(listener);
				}).collect(Collectors.toList()));
				imageAdapter.row.ifPresent(r -> r.update());
			});
			ImageNode.FramesRemoveListener framesRemoveListener =
					editNode.addFramesRemoveListeners((target, at, count) -> {
						System.out.format("Frames removed %s %s\n", at, count);
						List<Runnable> temp = sublist(frameCleanup, at, at + count);
						temp.forEach(c -> c.run());
						temp.clear();
						imageAdapter.row.ifPresent(r -> r.update());
					});
			ImageNode.FramesMoveToListener framesMoveToListener =
					editNode.addFramesMoveToListeners((target, source, count, dest) -> {
						moveTo(frameCleanup, source, count, dest);
						imageAdapter.row.ifPresent(r -> r.update());
					});
			ImageNode.FramesClearListener framesClearListener = editNode.addFramesClearListeners(target -> {
				frameCleanup.forEach(c -> c.run());
				frameCleanup.clear();
				imageAdapter.row.ifPresent(r -> r.update());
			});
			TreeItem imageItem = new TreeItem(imageAdapter);
			tree.getRoot().getChildren().add(imageItem);
			editHandle = () -> {
				editNode.removeFramesAddListeners(framesAddListener);
				editNode.removeFramesRemoveListeners(framesRemoveListener);
				editNode.removeFramesMoveToListeners(framesMoveToListener);
				editNode.removeFramesClearListeners(framesClearListener);
				frameCleanup.forEach(c -> c.run());
			};
		}
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

	private void updateButtons() {
		Wrapper root = context.selectedForView.get();
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
		Wrapper root = context.selectedForView.get();
		if (root == null)
			return;
		root.setFrame(context, root.frame.get());
		frameMarker.setLayoutX(root.frame.getValue() * zoom);
		rows.forEach(r -> r.updateFrameMarker());
	}

	public abstract static class TimeHandleNode {
		public List<FrameMapEntry> timeMap = null;

		public void remove() {
		}

		public void updateTime(List<FrameMapEntry> timeMap) {
			this.timeMap = timeMap;
		}
	}

	public void updateTime() {
		// Update rows
		int maxFrame = rows.stream().mapToInt(row -> row.update()).max().orElse(0);

		// Update scrub bar
		timeScroll.setMax(maxFrame * 1.1 + 50 * zoom);
		final int step = 4;
		for (int i = 0; i < (maxFrame + 50) / step; ++i) {
			Label label;
			if (i >= scrubOuterNumbers.size()) {
				scrubOuterNumbers.add(label = new Label(Integer.toString(i * step)));
				scrubElements.getChildren().addAll(label);
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
		for (FrameMapEntry frame : context.timeMap) {
			// Draw time region markers
			Rectangle mark;
			if (innerMarkIndex >= scrubRegionMarkers.size()) {
				scrubRegionMarkers.add(mark = new Rectangle());
				scrubElements.getChildren().add(0, mark);
			} else {
				mark = scrubRegionMarkers.get(innerMarkIndex);
			}
			mark.setWidth(1);
			mark.heightProperty().bind(scrub.heightProperty());
			mark.fillProperty().setValue(BLUE);
			mark.setLayoutX(at * zoom);

			// Draw times in region
			for (int i = 0; i < ((frame.length == -1 ? 50 : frame.length) - step + 1) / step; ++i) {
				Label label;
				if (innerIndex >= scrubInnerNumbers.size()) {
					scrubInnerNumbers.add(label = new Label());
					scrubElements.getChildren().addAll(label);
				} else {
					label = scrubInnerNumbers.get(innerIndex);
				}
				innerIndex += 1;
				label.setText(Integer.toString(frame.innerOffset + i * step));
				label.setLayoutX(at + i * step * zoom);
				label.setLayoutY(0);
				scrub
						.heightProperty()
						.addListener((observable, oldValue, newValue) -> label.setMinHeight(newValue.doubleValue()));
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

	public TimeHandleNode createTimeHandle(ProjectObject object) {
		if (object == context.selectedForEdit.getValue().getValue()) {
			return new TimeHandleNode() {
				@Override
				public void updateTime(List<FrameMapEntry> timeMap) {
					context.timeMap = timeMap;
					Timeline.this.updateTime();
					super.updateTime(timeMap);
				}
			};
		}
		if (false) {
			throw new Assertion();
		} else if (object instanceof Camera) {
			return new TimeHandleNode() {
				private TimeHandleNode child;

				{
					((Camera) object).addInnerSetListeners(new Camera.InnerSetListener() {
						@Override
						public void accept(Camera target, ProjectNode value) {
							if (child != null)
								child.remove();
							if (value ==
									Optional
											.ofNullable(context.selectedForEdit.get())
											.map(e -> e.getValue())
											.orElse(null))
								child = createTimeHandle(value);
							else
								child = createEndTimeHandle();
							if (timeMap != null)
								child.updateTime(timeMap);
						}
					});
				}

				@Override
				public void remove() {
					if (child != null)
						child.remove();
					child.remove();
				}

				@Override
				public void updateTime(List<FrameMapEntry> timeMap) {
					child.updateTime(timeMap);
					super.updateTime(timeMap);
				}
			};
		} else if (object instanceof GroupNode) {
			return new TimeHandleNode() {
				private GroupNode.LayersClearListener layersClearListener;
				private GroupNode.LayersMoveToListener layersMoveToListener;
				private GroupNode.LayersRemoveListener layersRemoveListener;
				private GroupNode.LayersAddListener layersAddListener;
				TimeHandleNode child;

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
					Wrapper at = context.selectedForEdit.get();
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
					child.updateTime(timeMap);
					super.updateTime(timeMap);
				}
			};
		} else if (object instanceof GroupLayer) {
			return new TimeHandleNode() {
				private TimeHandleNode child;
				private List<Runnable> cleanup = new ArrayList<>();
				private List<Runnable> frameCleanup = new ArrayList<>();

				{
					GroupLayer.InnerSetListener innerSetListener = new GroupLayer.InnerSetListener() {
						@Override
						public void accept(GroupLayer target, ProjectNode value) {
							if (child != null)
								child.remove();
							if (value ==
									Optional
											.ofNullable(context.selectedForEdit.get())
											.map(e -> e.getValue())
											.orElse(null))
								child = createTimeHandle(value);
							else
								child = createEndTimeHandle();
							recalcTimes();
						}
					};
					((GroupLayer) object).addInnerSetListeners(innerSetListener);
					GroupLayer.TimeFramesAddListener timeFramesAddListener = (target, at, value) -> recalcTimes();
					((GroupLayer) object).addTimeFramesAddListeners(timeFramesAddListener);
					GroupLayer.TimeFramesRemoveListener timeFramesRemoveListener = (target, at, count) -> recalcTimes();
					((GroupLayer) object).addTimeFramesRemoveListeners(timeFramesRemoveListener);
					GroupLayer.TimeFramesClearListener timeFramesClearListener = target -> recalcTimes();
					((GroupLayer) object).addTimeFramesClearListeners(timeFramesClearListener);
					GroupLayer.TimeFramesMoveToListener timeFramesMoveToListener =
							(target, source, count, dest) -> recalcTimes();
					((GroupLayer) object).addTimeFramesMoveToListeners(timeFramesMoveToListener);
					child = createTimeHandle(((GroupLayer) object).inner());
					cleanup.add(() -> {
						((GroupLayer) object).removeInnerSetListeners(innerSetListener);
						((GroupLayer) object).removeTimeFramesAddListeners(timeFramesAddListener);
						((GroupLayer) object).removeTimeFramesRemoveListeners(timeFramesRemoveListener);
						((GroupLayer) object).removeTimeFramesClearListeners(timeFramesClearListener);
						((GroupLayer) object).removeTimeFramesMoveToListeners(timeFramesMoveToListener);
					});
				}

				public void recalcTimes() {
					if (timeMap == null)
						return;
					frameCleanup.forEach(c -> c.run());
					frameCleanup.clear();
					((GroupLayer) object).timeFrames().forEach(frame -> {
						GroupTimeFrame.InnerOffsetSetListener innerOffsetSetListener = (target, value) -> recalcTimes();
						frame.addInnerOffsetSetListeners(innerOffsetSetListener);
						GroupTimeFrame.LengthSetListener lengthSetListener = (target, value) -> recalcTimes();
						frame.addLengthSetListeners(lengthSetListener);
						frameCleanup.add(() -> {
							frame.removeInnerOffsetSetListeners(innerOffsetSetListener);
							frame.removeLengthSetListeners(lengthSetListener);
						});
					});
					child.updateTime(timeMap.stream().flatMap(entry -> {
						List<FrameMapEntry> subMap = new ArrayList<>();
						int at = 0;
						int sumLength = 0;
						for (GroupTimeFrame frame : ((GroupLayer) object).timeFrames()) {
							if (at >= entry.innerOffset + entry.length)
								break;
							int effectiveLength = frame.length();
							if (at < entry.innerOffset) {
								effectiveLength -= entry.innerOffset - at;
								at = entry.innerOffset;
							}
							if (effectiveLength > 0) {
								subMap.add(new FrameMapEntry(effectiveLength, at));
								sumLength += effectiveLength;
							}
							at += frame.length();
						}
						if (sumLength < entry.length) {
							subMap.add(new FrameMapEntry(entry.length - sumLength, -1));
						}
						return subMap.stream();
					}).collect(Collectors.toList()));
				}

				@Override
				public void remove() {
					child.remove();
					cleanup.forEach(c -> c.run());
					frameCleanup.forEach(c -> c.run());
				}

				@Override
				public void updateTime(List<FrameMapEntry> timeMap) {
					recalcTimes();
					super.updateTime(timeMap);
				}
			};
		} else if (object instanceof ImageNode) {
			return createEndTimeHandle();
		} else if (object instanceof ImageFrame) {
			throw new Assertion();
		} else
			throw new Assertion();
	}

	private TimeHandleNode createEndTimeHandle() {
		return new TimeHandleNode() {
			@Override
			public void updateTime(List<FrameMapEntry> timeMap) {
				context.timeMap = timeMap;
			}

			@Override
			public void remove() {
				context.timeMap = new ArrayList<>();
				context.timeMap.add(new FrameMapEntry(-1, 0));
			}
		};
	}
}
