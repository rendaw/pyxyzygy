package com.zarbosoft.shoedemo;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;
import com.zarbosoft.shoedemo.model.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.zarbosoft.rendaw.common.Common.last;
import static com.zarbosoft.rendaw.common.Common.sublist;
import static javafx.scene.paint.Color.*;

public class Timeline {
	private final ProjectContext context;
	private double zoom = 16;
	private Main.Wrapper edit;
	private Runnable editHandle;

	VBox foreground = new VBox();
	ToolBar toolBar = new ToolBar();

	Pane scrub = new Pane();
	Group scrubElements = new Group();
	Rectangle frameMarker = new Rectangle(2, 0);
	Color frameMarkerColor = c(new java.awt.Color(159, 123, 130));
	List<Label> scrubOuterNumbers = new ArrayList<>();
	List<Label> scrubInnerNumbers = new ArrayList<>();
	List<Rectangle> scrubRegionMarkers = new ArrayList<>();
	List<Canvas> scrubRegions = new ArrayList<>();
	private List<FrameMapEntry> timeMap;
	Frame selected = null;
	Object selectedId = null;
	private int frame;
	private Main.Wrapper root;

	public abstract static class RowAdapterFrame {
		public abstract int length();

		public abstract void setLength(int length);

		public abstract void remove();

		public abstract void clear();

		public abstract void moveLeft();

		public abstract void moveRight();

		public abstract Object id();
	}

	public abstract static class RowAdapter {
		public abstract ObservableValue<String> getName();

		public abstract List<RowAdapterFrame> getFrames();

		public abstract boolean hasFrames();

		public abstract boolean createFrame(int outer);
	}

	TreeTableView<RowAdapter> tree = new TreeTableView<>();
	TreeTableColumn<RowAdapter, String> nameColumn = new TreeTableColumn();
	TreeTableColumn<RowAdapter, RowAdapter> framesColumn = new TreeTableColumn();
	ScrollBar timeScroll = new ScrollBar();
	private TimeHandleNode outerTimeHandle;

	List<Row> rows = new ArrayList<>();

	public Optional<Pair<Integer, FrameMapEntry>> findTime(int outer) {
		int outerAt = 0;
		for (FrameMapEntry outerFrame : timeMap) {
			if (outer >= outerAt && (outerFrame.length == -1 || outer < outerAt + outerFrame.length)) {
				return Optional.of(new Pair<>(outerAt, outerFrame));
			}
			outerAt += outerFrame.length;
		}
		return Optional.empty();
	}

	public static Color c(java.awt.Color source) {
		return Color.rgb(source.getRed(), source.getGreen(),source.getBlue() );
	}

	Timeline(ProjectContext context) {
		this.context = context;
		tree.setRoot(new TreeItem<>());
		tree.setShowRoot(false);
		tree.setColumnResizePolicy(TreeTableView.CONSTRAINED_RESIZE_POLICY);
		scrub.setBackground(Background.EMPTY);
		scrub.setMinHeight(30);
		scrub.getChildren().addAll(scrubElements);
		EventHandler<MouseEvent> mouseEventEventHandler = e -> {
			frame = Math.max(0, (int) ((e.getSceneX() - nameColumn.getWidth()) / zoom));
			updateFrameMarker();
		};
		scrub.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEventEventHandler);
		scrub.addEventFilter(MouseEvent.MOUSE_DRAGGED, mouseEventEventHandler);
		Button add = new Button("Add");
		add.setOnAction(e -> {
			tree
					.getSelectionModel()
					.getSelectedCells()
					.stream()
					.filter(c -> c.getTreeItem().getValue() != null && c.getTreeItem().getValue().createFrame(frame))
					.findFirst();
		});
		Button remove = new Button("Remove");
		remove.setOnAction(e -> {
			if (selected == null)
				return;
			selected.frame.remove();
			selected = null;
		});
		Button clear = new Button("Clear");
		clear.setOnAction(e -> {
			if (selected == null)
				return;
			selected.frame.clear();
		});
		Button left = new Button("Left");
		left.setOnAction(e -> {
			if (selected == null)
				return;
			selected.frame.moveLeft();
		});
		Button right = new Button("Right");
		right.setOnAction(e -> {
			if (selected == null)
				return;
			selected.frame.moveRight();
		});
		toolBar.getItems().addAll(add, remove, clear, left, right);
		tree.getColumns().addAll(nameColumn, framesColumn);
		nameColumn.setCellValueFactory(p -> p.getValue().getValue() == null ?
				new SimpleStringProperty() :
				p.getValue().getValue().getName());
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
								row.adapter = null;
								rows.remove(row);
							}
							return;
						}
						if (item.hasFrames()) {
							row.adapter = item;
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
		foreground.getChildren().addAll(toolBar, tree, scrub, timeScroll);

		timeScroll.setMin(0);
		scrubElements.layoutXProperty().bind(timeScroll.valueProperty().multiply(-1.0).add(nameColumn.widthProperty()));
		scrubElements.getChildren().add(frameMarker);
		frameMarker.heightProperty().bind(scrub.heightProperty());
		frameMarker.setFill(frameMarkerColor);
		timeScroll.visibleAmountProperty().bind(scrub.widthProperty().subtract(nameColumn.widthProperty()));
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
				if (selected != this) {
					if (selected != null)
						selected.deselect();
					selected = this;
					selectedId = this.frame.id();
					selected.select();
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
			setWidth(zoom);
			setHeight(zoom);
			deselect();
		}

		public void select() {
			GraphicsContext gc = getGraphicsContext2D();
			gc.clearRect(0, 0, getWidth(), getHeight());
			gc.setFill(PURPLE);
			gc.fillOval(2,2, zoom - 4, zoom - 4);
		}

		public void deselect() {
			GraphicsContext gc = getGraphicsContext2D();
			gc.clearRect(0, 0, getWidth(), getHeight());
			gc.setFill(BLACK);
			gc.strokeOval(2,2, zoom - 4, zoom - 4);
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
		private Rectangle frameMarker = new Rectangle(2, 0);
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
			for (FrameMapEntry outer : timeMap) {
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
						System.out.format(
								"fr set il %s; inn off %s; outer at %s; prev inn at %s; inn at %s\n",
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

			if (selected != foundSelectedFrame) {
				if (selected != null)
					selected.deselect();
				selected = foundSelectedFrame;
				if (selected != null)
					selected.select();
			}

			if (frameIndex < frames.size()) {
				List<Frame> remove = sublist(frames, frameIndex);
				for (Frame frame : remove) {
					if (selected == frame) {
						selected = null;
					}
				}
				this.inner.getChildren().removeAll(remove);
				remove.clear();
			}

			return outerAt;
		}

		public void updateFrameMarker() {
			frameMarker.setLayoutX(frame * zoom);
		}
	}

	public void setNodes(Main.Wrapper root, Main.Wrapper edit) {
		this.root = root;
		frame = 0;
		updateFrameMarker();

		this.edit = edit;
		tree.getRoot().getChildren().clear();

		// Prepare time translation
		if (outerTimeHandle != null)
			outerTimeHandle.remove();
		outerTimeHandle = createTimeHandle(root.getValue());
		outerTimeHandle.updateTime(ImmutableList.of(new FrameMapEntry(-1, 0)));

		// Prepare rows
		if (editHandle != null) {
			editHandle.run();
			editHandle = null;
		}
		ProjectNode editNode = (ProjectNode) edit.getValue();
		if (false) {
		} else if (editNode instanceof Camera) {
		} else if (editNode instanceof GroupNode) {
			abstract class GroupRowAdapter extends RowAdapter {
				List<Runnable> cleanup = new ArrayList<>();
			}
			editHandle = new Runnable() {
				List<Runnable> cleanup;

				{
					cleanup.add(((GroupNode) editNode).mirrorLayers(tree.getRoot().getChildren(), layer -> {
						TreeItem layerItem = new TreeItem(new GroupRowAdapter() {
							@Override
							public ObservableValue<String> getName() {
								SimpleStringProperty out = new SimpleStringProperty(editNode.name());
								ProjectNode.NameSetListener listener;
								editNode.addNameSetListeners(listener = (target, value) -> out.setValue(value));
								cleanup.add(() -> editNode.removeNameSetListeners(listener));
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
						});
						TreeItem timeFramesItem = new TreeItem(new RowAdapter() {
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
											context.change.groupTimeFrame(f).lengthSet(length);
										}

										@Override
										public void remove() {
											if (i == 0)
												return;
											context.change.groupLayer(layer).timeFramesRemove(i, 1);
											if (i == layer.timeFramesLength())
												context.change.groupTimeFrame(last(layer.timeFrames())).lengthSet(-1);
										}

										@Override
										public void clear() {
											context.change.groupTimeFrame(f).innerOffsetSet(0);
										}

										@Override
										public void moveLeft() {
											if (i == 0)
												return;
											GroupTimeFrame frameBefore = layer.timeFramesGet(i - 1);
											context.change.groupLayer(layer).timeFramesMoveTo(i, 1, i - 1);
											final int lengthThis = f.length();
											if (lengthThis == -1) {
												final int lengthBefore = frameBefore.length();
												context.change.groupTimeFrame(f).lengthSet(lengthBefore);
												context.change.groupTimeFrame(frameBefore).lengthSet(lengthThis);
											}
										}

										@Override
										public void moveRight() {
											if (i == layer.timeFramesLength() - 1)
												return;
											GroupTimeFrame frameAfter = layer.timeFramesGet(i + 1);
											context.change.groupLayer(layer).timeFramesMoveTo(i, 1, i + 1);
											final int lengthAfter = frameAfter.length();
											if (lengthAfter == -1) {
												final int lengthThis = f.length();
												context.change.groupTimeFrame(f).lengthSet(lengthAfter);
												context.change.groupTimeFrame(frameAfter).lengthSet(lengthThis);
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
								int outerAt = 0;
								for (FrameMapEntry outerFrame : timeMap) {
									if (outer >= outerAt &&
											(outerFrame.length == -1 || outer < outerAt + outerFrame.length)) {
										int inner = outer - outerAt + outerFrame.innerOffset;
										int innerAt = 0;
										for (int i = 0; i < layer.timeFramesLength(); ++i) {
											GroupTimeFrame innerFrame = layer.timeFramesGet(i);
											if (inner >= innerAt && (
													innerFrame.length() == -1 || inner < innerAt + innerFrame.length()
											)) {
												if (inner == innerAt)
													return false;
												GroupTimeFrame newFrame = GroupTimeFrame.create(context);
												int offset = inner - innerAt;
												if (innerFrame.length() == -1) {
													context.change.groupTimeFrame(innerFrame).lengthSet(offset);
													newFrame.initialLengthSet(-1);
												} else {
													newFrame.initialLengthSet(innerFrame.length() - offset);
													context.change.groupTimeFrame(innerFrame).lengthSet(offset);
												}
												newFrame.initialInnerOffsetSet(innerFrame.innerOffset() + offset);
												context.change.groupLayer(layer).timeFramesAdd(i + 1, newFrame);
												return true;
											}
											innerAt += innerFrame.length();
										}
									}
									outerAt += outerFrame.length;
								}
								throw new Assertion();
							}
						});
						layerItem.getChildren().add(timeFramesItem);
						TreeItem positionFramesItem = new TreeItem(new RowAdapter() {
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
								for (int i0 = 0; i0 < layer.timeFramesLength(); ++i0) {
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
											context.change.groupPositionFrame(f).lengthSet(length);
										}

										@Override
										public void remove() {
											if (i == 0)
												return;
											context.change.groupLayer(layer).positionFramesRemove(i, 1);
											if (i == layer.positionFramesLength())
												context.change
														.groupPositionFrame(last(layer.positionFrames()))
														.lengthSet(-1);
										}

										@Override
										public void clear() {
											context.change.groupPositionFrame(f).offsetSet(new Vector(0, 0));
										}

										@Override
										public void moveLeft() {
											if (i == 0)
												return;
											GroupPositionFrame frameBefore = layer.positionFramesGet(i - 1);
											context.change.groupLayer(layer).timeFramesMoveTo(i, 1, i - 1);
											final int lengthThis = f.length();
											if (lengthThis == -1) {
												final int lengthBefore = frameBefore.length();
												context.change.groupPositionFrame(f).lengthSet(lengthBefore);
												context.change.groupPositionFrame(frameBefore).lengthSet(lengthThis);
											}
										}

										@Override
										public void moveRight() {
											if (i == layer.timeFramesLength() - 1)
												return;
											GroupPositionFrame frameAfter = layer.positionFramesGet(i + 1);
											context.change.groupLayer(layer).timeFramesMoveTo(i, 1, i + 1);
											final int lengthAfter = frameAfter.length();
											if (lengthAfter == -1) {
												final int lengthThis = f.length();
												context.change.groupPositionFrame(f).lengthSet(lengthAfter);
												context.change.groupPositionFrame(frameAfter).lengthSet(lengthThis);
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
								Pair<Integer, FrameMapEntry> found = findTime(outer).get();
								int outerAt = found.first;
								FrameMapEntry outerFrame = found.second;
								int inner = outer - outerAt + outerFrame.innerOffset;
								int innerAt = 0;
								for (int i = 0; i < layer.timeFramesLength(); ++i) {
									GroupPositionFrame innerFrame = layer.positionFramesGet(i);
									if (inner >= innerAt && (
											innerFrame.length() == -1 || inner < innerAt + innerFrame.length()
									)) {
										if (inner == innerAt)
											return false;
										GroupPositionFrame newFrame = GroupPositionFrame.create(context);
										int offset = inner - innerAt;
										if (innerFrame.length() == -1) {
											context.change.groupPositionFrame(innerFrame).lengthSet(offset);
											newFrame.initialLengthSet(-1);
										} else {
											newFrame.initialLengthSet(innerFrame.length() - offset);
											context.change.groupPositionFrame(innerFrame).lengthSet(offset);
										}
										newFrame.initialOffsetSet(innerFrame.offset());
										context.change.groupLayer(layer).positionFramesAdd(i + 1, newFrame);
										return true;
									}
									innerAt += innerFrame.length();
								}
								throw new Assertion();
							}
						});
						layerItem.getChildren().add(positionFramesItem);

						GroupLayer.TimeFramesAddListener timeFramesAddListener = (target, at, value) -> updateTime();
						layer.addTimeFramesAddListeners(timeFramesAddListener);
						GroupLayer.TimeFramesRemoveListener timeFramesRemoveListener =
								(target, at, count) -> updateTime();
						layer.addTimeFramesRemoveListeners(timeFramesRemoveListener);
						GroupLayer.TimeFramesMoveToListener timeFramesMoveToListener =
								(target, source, count, dest) -> updateTime();
						layer.addTimeFramesMoveToListeners(timeFramesMoveToListener);
						GroupLayer.TimeFramesClearListener timeFramesClearListener = target -> updateTime();
						layer.addTimeFramesClearListeners(timeFramesClearListener);
						GroupLayer.PositionFramesAddListener positionFramesAddListener =
								(target, at, value) -> updateTime();
						layer.addPositionFramesAddListeners(positionFramesAddListener);
						GroupLayer.PositionFramesRemoveListener positionFramesRemoveListener =
								(target, at, count) -> updateTime();
						layer.addPositionFramesRemoveListeners(positionFramesRemoveListener);
						GroupLayer.PositionFramesMoveToListener positionFramesMoveToListener =
								(target, source, count, dest) -> updateTime();
						layer.addPositionFramesMoveToListeners(positionFramesMoveToListener);
						GroupLayer.PositionFramesClearListener positionFramesClearListener = target -> updateTime();
						layer.addPositionFramesClearListeners(positionFramesClearListener);
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
					}, item -> ((GroupRowAdapter) item.getValue()).cleanup.forEach(c -> c.run())));
				}

				@Override
				public void run() {
					cleanup.forEach(c -> c.run());
					tree
							.getRoot()
							.getChildren()
							.stream()
							.map(c -> (GroupRowAdapter) c.getValue())
							.forEach(v -> v.cleanup.forEach(c -> c.run()));
				}
			};
		} else if (editNode instanceof ImageNode) {
			List<Runnable> frameCleanup = new ArrayList<>();
			ImageNode.FramesAddListener framesAddListener =
					((ImageNode) editNode).addFramesAddListeners((target, at, value) -> {
						frameCleanup.addAll(at, value.stream().map(f -> {
							ImageFrame.LengthSetListener listener =
									f.addLengthSetListeners((target1, value1) -> updateTime());
							return (Runnable) () -> f.removeLengthSetListeners(listener);
						}).collect(Collectors.toList()));
						updateTime();
					});
			ImageNode.FramesRemoveListener framesRemoveListener =
					((ImageNode) editNode).addFramesRemoveListeners((target, at, count) -> {
						System.out.format("Frames removed %s %s\n", at, count);
						List<Runnable> temp = sublist(frameCleanup, at, at + count);
						temp.forEach(c -> c.run());
						temp.clear();
						updateTime();
					});
			ImageNode.FramesMoveToListener framesMoveToListener =
					((ImageNode) editNode).addFramesMoveToListeners((target, source, count, dest) -> {
						moveTo(frameCleanup, source, count, dest);
						updateTime();
					});
			ImageNode.FramesClearListener framesClearListener =
					((ImageNode) editNode).addFramesClearListeners(target -> {
						frameCleanup.forEach(c -> c.run());
						frameCleanup.clear();
						updateTime();
					});
			TreeItem imageItem = new TreeItem(new RowAdapter() {
				@Override
				public ObservableValue<String> getName() {
					return new SimpleStringProperty("Frames");
				}

				@Override
				public List<RowAdapterFrame> getFrames() {
					List<RowAdapterFrame> out = new ArrayList<>();
					for (int i0 = 0; i0 < ((ImageNode) editNode).framesLength(); ++i0) {
						final int i = i0;
						ImageFrame f = ((ImageNode) editNode).framesGet(i);
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
								context.change.imageFrame(f).lengthSet(length);
							}

							@Override
							public void remove() {
								if (i == 0)
									return;
								context.change.imageNode((ImageNode) editNode).framesRemove(i, 1);
								if (i == ((ImageNode) editNode).framesLength())
									context.change.imageFrame(last(((ImageNode) editNode).frames())).lengthSet(-1);
							}

							@Override
							public void clear() {
								context.change.imageFrame(f).tilesClear();
							}

							@Override
							public void moveLeft() {
								if (i == 0)
									return;
								ImageFrame frameBefore = ((ImageNode) editNode).framesGet(i - 1);
								context.change.imageNode((ImageNode) editNode).framesMoveTo(i, 1, i - 1);
								final int lengthThis = f.length();
								if (lengthThis == -1) {
									final int lengthBefore = frameBefore.length();
									context.change.imageFrame(f).lengthSet(lengthBefore);
									context.change.imageFrame(frameBefore).lengthSet(lengthThis);
								}
							}

							@Override
							public void moveRight() {
								if (i == ((ImageNode) editNode).framesLength() - 1)
									return;
								ImageFrame frameAfter = ((ImageNode) editNode).framesGet(i + 1);
								context.change.imageNode((ImageNode) editNode).framesMoveTo(i, 1, i + 1);
								final int lengthAfter = frameAfter.length();
								if (lengthAfter == -1) {
									final int lengthThis = f.length();
									context.change.imageFrame(f).lengthSet(lengthAfter);
									context.change.imageFrame(frameAfter).lengthSet(lengthThis);
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
					Pair<Integer, FrameMapEntry> found = findTime(outer).get();
					int outerAt = found.first;
					FrameMapEntry outerFrame = found.second;
					int inner = outer - outerAt + outerFrame.innerOffset;
					int innerAt = 0;
					for (int i = 0; i < ((ImageNode) editNode).framesLength(); ++i) {
						ImageFrame innerFrame = ((ImageNode) editNode).framesGet(i);
						System.out.format("Considering inner %s %s %s - %s\n", i, innerAt, innerFrame.length(), inner);
						System.out.flush();
						if (inner >= innerAt && (
								innerFrame.length() == -1 || inner < innerAt + innerFrame.length()
						)) {
							System.out.format("a %s %s\n", inner, innerAt);
							System.out.flush();
							if (inner == innerAt)
								return false;
							ImageFrame newFrame = ImageFrame.create(context);
							int offset = inner - innerAt;
							if (innerFrame.length() == -1) {
								context.change.imageFrame(innerFrame).lengthSet(offset);
								newFrame.initialLengthSet(-1);
							} else {
								newFrame.initialLengthSet(innerFrame.length() - offset);
								context.change.imageFrame(innerFrame).lengthSet(offset);
							}
							context.change.imageNode((ImageNode) editNode).framesAdd(i + 1, newFrame);
							System.out.format("frame added!!!!\n");
							return true;
						}
						innerAt += innerFrame.length();
					}
					throw new Assertion();
				}
			});
			tree.getRoot().getChildren().add(imageItem);
			editHandle = () -> {
				((ImageNode) editNode).removeFramesAddListeners(framesAddListener);
				((ImageNode) editNode).removeFramesRemoveListeners(framesRemoveListener);
				((ImageNode) editNode).removeFramesMoveToListeners(framesMoveToListener);
				((ImageNode) editNode).removeFramesClearListeners(framesClearListener);
				frameCleanup.forEach(c -> c.run());
			};
		}
	}

	public static void moveTo(List list, int source, int count, int dest) {
		List temp0 = list.subList(source, source + count);
		List temp1 = new ArrayList(temp0);
		temp0.clear();
		list.addAll(dest, temp1);
	}

	private void updateFrameMarker() {
		root.setFrame(frame);
		frameMarker.setLayoutX(frame * zoom);
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

	public static class FrameMapEntry {
		final int length;
		final int innerOffset;

		public FrameMapEntry(int length, int innerOffset) {
			this.length = length;
			this.innerOffset = innerOffset;
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
		for (FrameMapEntry frame : timeMap) {
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
		if (object == edit.getValue()) {
			return new TimeHandleNode() {
				@Override
				public void updateTime(List<FrameMapEntry> timeMap) {
					Timeline.this.timeMap = timeMap;
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
							child = createTimeHandle(value);
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
							child = createTimeHandle(value);
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
		} else if (object instanceof GroupNode) {
			return new TimeHandleNode() {
				private GroupNode.LayersClearListener layersClearListener;
				private GroupNode.LayersMoveToListener layersMoveToListener;
				private GroupNode.LayersRemoveListener layersRemoveListener;
				private GroupNode.LayersAddListener layersAddListener;
				TimeHandleNode child;

				{
					relocate();
				}

				public void relocate() {
					remove();
					layersAddListener = (target, at, value) -> relocate();
					((GroupNode) object).addLayersAddListeners(layersAddListener);
					layersRemoveListener = (target, at, count) -> relocate();
					((GroupNode) object).addLayersRemoveListeners(layersRemoveListener);
					layersMoveToListener = (target, source, count, dest) -> relocate();
					((GroupNode) object).addLayersMoveToListeners(layersMoveToListener);
					layersClearListener = target -> relocate();
					((GroupNode) object).addLayersClearListeners(layersClearListener);
					ProjectObject nextAncestor = null;
					Main.Wrapper at = edit;
					while (at != null && at.getValue() != object) {
						nextAncestor = at.getValue();
						at = at.getParent();
					}
					GroupLayer found = null;
					for (GroupLayer nextValue : ((GroupNode) object).layers()) {
						if (nextValue == nextAncestor) {
							found = nextValue;
							break;
						}
					}
					if (found == null)
						throw new Assertion();
					child = createTimeHandle(found);
				}

				@Override
				public void remove() {
					child.remove();
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
		} else if (object instanceof ImageNode) {
			throw new Assertion();
		} else if (object instanceof ImageFrame) {
			throw new Assertion();
		} else
			throw new Assertion();
	}
}
