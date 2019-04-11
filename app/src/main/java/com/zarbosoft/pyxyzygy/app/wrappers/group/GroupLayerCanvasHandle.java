package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.core.model.v0.GroupLayer;
import com.zarbosoft.pyxyzygy.core.model.v0.GroupPositionFrame;
import com.zarbosoft.pyxyzygy.core.model.v0.GroupTimeFrame;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.zarbosoft.pyxyzygy.app.Misc.moveTo;

public class GroupLayerCanvasHandle extends CanvasHandle {
	private final CanvasHandle parent;
	private final Listener.ListAdd<GroupLayer, GroupPositionFrame> positionAddListener;
	private final Listener.ListRemove<GroupLayer> positionRemoveListener;
	private final Listener.ListMoveTo<GroupLayer> positionMoveListener;
	private final Listener.ListAdd<GroupLayer, GroupTimeFrame> timeAddListener;
	private final Listener.ListRemove<GroupLayer> timeRemoveListener;
	private final Listener.ListMoveTo<GroupLayer> timeMoveListener;
	private int zoom;
	private CanvasHandle childCanvas;
	private final List<Runnable> positionCleanup;
	private final List<Runnable> timeCleanup;
	private GroupLayerWrapper wrapper;

	public GroupLayerCanvasHandle(
			ProjectContext context, Window window, GroupLayerWrapper wrapper, CanvasHandle parent
	) {
		this.wrapper = wrapper;
		this.parent = parent;
		positionCleanup = new ArrayList<>();
		timeCleanup = new ArrayList<>();

		wrapper.child.addListener(new ChangeListener<Wrapper>() {
			{
				changed(null, null, wrapper.child.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends Wrapper> observable, Wrapper oldValue, Wrapper newValue
			) {
				if (oldValue != null) {
					if (childCanvas != null) {
						inner.getChildren().clear();
						childCanvas.remove(context);
					}
				}
				if (newValue != null) {
					childCanvas = newValue.buildCanvas(context, window, GroupLayerCanvasHandle.this);
					childCanvas.setViewport(context,bounds.get(),zoom);
					inner.getChildren().add(childCanvas.getWidget());
					GroupLayerCanvasHandle.this.updateChildCanvasPosition(null);
				}
			}
		});
		// Don't need clear listeners because clear should never happen (1 frame must always be left)
		positionAddListener = wrapper.node.addPositionFramesAddListeners((target, at, value) -> {
			updatePosition(context);
			positionCleanup.addAll(at, value.stream().map(v -> {
				Listener.ScalarSet<GroupPositionFrame, Integer> lengthListener =
						v.addLengthSetListeners((target1, value1) -> {
							updatePosition(context);
						});
				Listener.ScalarSet<GroupPositionFrame, Vector> offsetListener =
						v.addOffsetSetListeners((target12, value12) -> updatePosition(context));
				return (Runnable) () -> {
					v.removeLengthSetListeners(lengthListener);
					v.removeOffsetSetListeners(offsetListener);
				};
			}).collect(Collectors.toList()));
		});
		positionRemoveListener = wrapper.node.addPositionFramesRemoveListeners((target, at, count) -> {
			updatePosition(context);
			List<Runnable> tempClean = positionCleanup.subList(at, at + count);
			tempClean.forEach(r -> r.run());
			tempClean.clear();
		});
		positionMoveListener = wrapper.node.addPositionFramesMoveToListeners((target, source, count, dest) -> {
			updatePosition(context);
			moveTo(positionCleanup, source, count, dest);
		});
		timeAddListener = wrapper.node.addTimeFramesAddListeners((target, at, value) -> {
			updateTime(context);
			timeCleanup.addAll(at, value.stream().map(v -> {
				Listener.ScalarSet<GroupTimeFrame, Integer> lengthListener =
						v.addLengthSetListeners((target1, value1) -> {
							updatePosition(context);
						});
				Listener.ScalarSet<GroupTimeFrame, Integer> offsetListener =
						v.addInnerOffsetSetListeners((target12, value12) -> updatePosition(context));
				return (Runnable) () -> {
					v.removeLengthSetListeners(lengthListener);
					v.removeInnerOffsetSetListeners(offsetListener);
				};
			}).collect(Collectors.toList()));
		});
		timeRemoveListener = wrapper.node.addTimeFramesRemoveListeners((target, at, count) -> {
			updateTime(context);
			List<Runnable> tempClean = timeCleanup.subList(at, at + count);
			tempClean.forEach(r -> r.run());
			tempClean.clear();
		});
		timeMoveListener = wrapper.node.addTimeFramesMoveToListeners((target, source, count, dest) -> {
			updateTime(context);
			moveTo(timeCleanup, source, count, dest);
		});
	}

	private GroupPositionFrame findPosition() {
		return findPosition(frameNumber.get());
	}

	private GroupPositionFrame findPosition(int frame) {
		return wrapper.positionFrameFinder.findFrame(wrapper.node, frame).frame;
	}

	private void updateTime(ProjectContext context) {
		if (childCanvas != null)
			childCanvas.setFrame(context, wrapper.findInnerFrame(frameNumber.get()));
	}

	@Override
	public void remove(ProjectContext context) {
		if (childCanvas != null) {
			childCanvas.remove(context);
			childCanvas = null;
		}
		wrapper.node.removePositionFramesAddListeners(positionAddListener);
		wrapper.node.removePositionFramesRemoveListeners(positionRemoveListener);
		wrapper.node.removePositionFramesMoveToListeners(positionMoveListener);
		wrapper.node.removeTimeFramesAddListeners(timeAddListener);
		wrapper.node.removeTimeFramesRemoveListeners(timeRemoveListener);
		wrapper.node.removeTimeFramesMoveToListeners(timeMoveListener);
		positionCleanup.forEach(r -> r.run());
		timeCleanup.forEach(r -> r.run());
	}

	@Override
	public void setViewport(
			ProjectContext context, DoubleRectangle newBounds, int positiveZoom
	) {
		this.zoom = positiveZoom;
		bounds.set(newBounds);
		updatePosition(context);
	}

	private void updatePosition(ProjectContext context) {
		GroupPositionFrame pos = findPosition();
		if (bounds.get() == null)
			return;
		DoubleRectangle newBounds = bounds.get().minus(pos.offset());
		updateChildCanvasPosition(pos);
		if (childCanvas != null)
			childCanvas.setViewport(context, newBounds, zoom);
	}

	@Override
	public void setFrame(ProjectContext context, int frameNumber) {
		this.frameNumber.set(frameNumber);
		updateTime(context);
		updatePosition(context);
	}

	private void updateChildCanvasPosition(GroupPositionFrame pos) {
		if (pos == null)
			pos = findPosition();
		inner.setLayoutX(pos.offset().x);
		inner.setLayoutY(pos.offset().y);
	}

	@Override
	public DoubleVector toInner(DoubleVector vector) {
		GroupPositionFrame pos = findPosition();
		return vector.minus(pos.offset());
	}

	@Override
	public GroupLayerWrapper getWrapper() {
		return wrapper;
	}

	@Override
	public CanvasHandle getParent() {
		return parent;
	}
}
