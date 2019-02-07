package com.zarbosoft.pyxyzygy.gui.parts.timeline;

import com.zarbosoft.pyxyzygy.ProjectContext;
import com.zarbosoft.pyxyzygy.WidgetHandle;
import com.zarbosoft.pyxyzygy.Window;
import com.zarbosoft.pyxyzygy.model.GroupLayer;
import com.zarbosoft.pyxyzygy.model.GroupTimeFrame;
import com.zarbosoft.pyxyzygy.wrappers.group.GroupLayerWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.zarbosoft.rendaw.common.Common.last;
import static com.zarbosoft.pyxyzygy.Launch.NO_INNER;

public class RowAdapterGroupLayerTime extends RowAdapter {
	// TODO setup listeners for inner subtree to keep track of the max inner frame (?)
	final GroupLayer layer;
	final RowAdapterGroupLayer layerRowAdapter;
	private final Timeline timeline;
	Optional<RowFramesWidget> row = Optional.empty();
	Optional<RowTimeRangeWidget> rowInnerRange = Optional.empty();

	public RowAdapterGroupLayerTime(
			Timeline timeline, GroupLayer layer, RowAdapterGroupLayer layerRowAdapter
	) {
		this.timeline = timeline;
		this.layer = layer;
		this.layerRowAdapter = layerRowAdapter;
	}

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
	public boolean hasFrames() {
		return true;
	}

	@Override
	public boolean createFrame(ProjectContext context, Window window, int outer) {
		return createFrame(context, window,outer, previous -> {
			GroupTimeFrame created =GroupTimeFrame.create(context);
			created.initialInnerOffsetSet(context, 0);
			created.initialInnerLoopSet(context, 0);
			return created;
		});
	}

	@Override
	public boolean duplicateFrame(ProjectContext context, Window window, int outer) {
		return createFrame(context, window,outer, previous -> {
			GroupTimeFrame created = GroupTimeFrame.create(context);
			created.initialInnerOffsetSet(context, previous.innerOffset());
			created.initialInnerLoopSet(context, previous.innerLoop());
			return created;
		});
	}

	public boolean createFrame(
			ProjectContext context, Window window, int outer, Function<GroupTimeFrame, GroupTimeFrame> cb
	) {
		int inner = window.timeToInner(outer);
		if (inner == NO_INNER) return false;
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
		context.history.change(c -> c.groupLayer(layer).timeFramesAdd(previous.frameIndex + 1, newFrame));
		return true;
	}

	@Override
	public ObservableObjectValue<Image> getStateImage() {
		return Timeline.emptyStateImage;
	}

	@Override
	public void deselected() {
		layerRowAdapter.treeDeselected();
	}

	@Override
	public void selected() {
		layerRowAdapter.treeSelected();
	}

	static class AdapterTimeFrame extends RowAdapterFrame {
		final int i;
		final GroupLayer layer;
		final GroupTimeFrame f;

		AdapterTimeFrame(int i, GroupLayer layer, GroupTimeFrame f) {
			this.i = i;
			this.layer = layer;
			this.f = f;
		}

		@Override
		public Object id() {
			return f;
		}

		@Override
		public int length() {
			return f.length();
		}

		@Override
		public void setLength(ProjectContext context, int length) {
			context.history.change(c -> c.groupTimeFrame(f).lengthSet(length));
		}

		@Override
		public void remove(ProjectContext context) {
			if (i == 0)
				return;
			context.history.change(c -> c.groupLayer(layer).timeFramesRemove(i, 1));
			if (i == layer.timeFramesLength())
				context.history.change(c -> c.groupTimeFrame(last(layer.timeFrames())).lengthSet(-1));
		}

		@Override
		public void clear(ProjectContext context) {
			context.history.change(c -> c.groupTimeFrame(f).innerOffsetSet(0));
		}

		@Override
		public void moveLeft(ProjectContext context) {
			if (i == 0)
				return;
			GroupTimeFrame frameBefore = layer.timeFramesGet(i - 1);
			context.history.change(c -> c.groupLayer(layer).timeFramesMoveTo(i, 1, i - 1));
			final int lengthThis = f.length();
			if (lengthThis == -1) {
				final int lengthBefore = frameBefore.length();
				context.history.change(c -> c.groupTimeFrame(f).lengthSet(lengthBefore));
				context.history.change(c -> c.groupTimeFrame(frameBefore).lengthSet(lengthThis));
			}
		}

		@Override
		public void moveRight(ProjectContext context) {
			if (i == layer.timeFramesLength() - 1)
				return;
			GroupTimeFrame frameAfter = layer.timeFramesGet(i + 1);
			context.history.change(c -> c.groupLayer(layer).timeFramesMoveTo(i, 1, i + 1));
			final int lengthAfter = frameAfter.length();
			if (lengthAfter == -1) {
				final int lengthThis = f.length();
				context.history.change(c -> c.groupTimeFrame(f).lengthSet(lengthAfter));
				context.history.change(c -> c.groupTimeFrame(frameAfter).lengthSet(lengthThis));
			}
		}
	}

	@Override
	public int updateTime(ProjectContext context, Window window) {
		return row.map(r -> {
			List<RowAdapterFrame> frameAdapters = new ArrayList<>();
			for (int i = 0; i < layer.timeFramesLength(); ++i) {
				GroupTimeFrame f = layer.timeFramesGet(i);
				frameAdapters.add(new AdapterTimeFrame(i, layer, f));
			}
			return r.updateTime(context, window, frameAdapters);
		}).orElse(0);
	}

	@Override
	public void updateFrameMarker(ProjectContext context, Window window) {
		row.ifPresent(r -> r.updateFrameMarker(context, window));
		rowInnerRange.ifPresent(r -> r.updateFrameMarker(window));
	}

	@Override
	public WidgetHandle createRowWidget(ProjectContext context, Window window) {
		return new WidgetHandle() {
			final VBox layout = new VBox();

			Runnable framesCleanup;
			List<Runnable> frameCleanup = new ArrayList<>();
			private ChangeListener<FrameWidget> selectedFrameListener;
			Runnable selectedFrameCleanup;

			{
				framesCleanup = layer.mirrorTimeFrames(frameCleanup, f -> {
					GroupTimeFrame.LengthSetListener lengthListener = f.addLengthSetListeners((target, value) -> {
						updateTime(context, window);
					});
					return () -> {
						f.removeLengthSetListeners(lengthListener);
					};
				}, c -> c.run(), at -> {
					updateTime(context, window);
				});
				selectedFrameListener = (observable, oldValue, newValue) -> {
					if (selectedFrameCleanup != null) {
						selectedFrameCleanup.run();
						selectedFrameCleanup = null;
					}
					if (newValue == null || !(newValue.frame instanceof AdapterTimeFrame)) {
						rowInnerRange.ifPresent(w -> {
							layout.getChildren().remove(w.base);
							rowInnerRange = Optional.empty();
						});
					} else {
						if (!rowInnerRange.isPresent()) {
							rowInnerRange = Optional.of(new RowTimeRangeWidget(timeline));
							layout.getChildren().add(rowInnerRange.get().base);
						}
						GroupTimeFrame frame = ((AdapterTimeFrame) newValue.frame).f;
						Runnable update = () -> {
							rowInnerRange.get().set(new TimeRangeAdapter() {
								@Override
								public int getOuterAt() {
									return newValue.at;
								}

								@Override
								public int getInnerStart() {
									return frame.innerOffset();
								}

								@Override
								public int getInnerLength() {
									return frame.innerLoop();
								}

								@Override
								public void changeStart(int value) {
									context.history.change(c -> c.groupTimeFrame(frame).innerOffsetSet(value));
								}

								@Override
								public void changeLength(int value) {
									context.history.change(c -> c.groupTimeFrame(frame).innerLoopSet(value));
								}
							});
						};
						GroupTimeFrame.InnerOffsetSetListener offsetListener =
								frame.addInnerOffsetSetListeners((target, value) -> update.run());
						GroupTimeFrame.InnerLoopSetListener loopListener =
								frame.addInnerLoopSetListeners((target, value) -> update.run());
						selectedFrameCleanup = () -> {
							frame.removeInnerOffsetSetListeners(offsetListener);
							frame.removeInnerLoopSetListeners(loopListener);
						};
					}
				};
				timeline.selectedFrame.addListener(selectedFrameListener);
				selectedFrameListener.changed(null, null, timeline.selectedFrame.getValue());
				row = Optional.of(new RowFramesWidget(timeline));
				layout.getChildren().addAll(row.get());
			}

			@Override
			public Node getWidget() {
				return layout;
			}

			@Override
			public void remove() {
				framesCleanup.run();
				frameCleanup.forEach(c -> c.run());
				timeline.selectedFrame.removeListener(selectedFrameListener);
				if (selectedFrameCleanup != null)
					selectedFrameCleanup.run();
			}
		};
	}

	@Override
	public void remove(ProjectContext context) {
	}
}
