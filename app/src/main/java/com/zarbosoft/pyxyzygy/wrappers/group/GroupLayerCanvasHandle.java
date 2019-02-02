package com.zarbosoft.pyxyzygy.wrappers.group;

import com.zarbosoft.pyxyzygy.*;
import com.zarbosoft.pyxyzygy.model.GroupLayer;
import com.zarbosoft.pyxyzygy.model.GroupPositionFrame;
import com.zarbosoft.pyxyzygy.model.GroupTimeFrame;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.zarbosoft.pyxyzygy.Misc.moveTo;

public class GroupLayerCanvasHandle extends Wrapper.CanvasHandle {
	private final Wrapper.CanvasHandle parent;
	private int zoom;
	private Wrapper.CanvasHandle childCanvas;
	private int currentFrame;
	private final GroupLayer.InnerSetListener innerSetListener;
	private final GroupLayer.PositionFramesAddListener positionAddListener;
	private final GroupLayer.PositionFramesRemoveListener positionRemoveListener;
	private final GroupLayer.PositionFramesMoveToListener positionMoveListener;
	private final GroupLayer.TimeFramesAddListener timeAddListener;
	private final GroupLayer.TimeFramesRemoveListener timeRemoveListener;
	private final GroupLayer.TimeFramesMoveToListener timeMoveListener;
	private final List<Runnable> positionCleanup;
	private final List<Runnable> timeCleanup;
	private GroupLayerWrapper wrapper;
	private DoubleRectangle baseBounds;

	public GroupLayerCanvasHandle(GroupLayerWrapper wrapper, Wrapper.CanvasHandle parent, ProjectContext context) {
		this.parent = parent;
		positionCleanup = new ArrayList<>();
		timeCleanup = new ArrayList<>();
		GroupPositionFrame pos = findPosition();
		if (wrapper.child != null) {
			childCanvas = wrapper.child.buildCanvas(context, this);
			inner.getChildren().add(childCanvas.getWidget());
			updateChildCanvasPosition(pos);
		}

		this.innerSetListener = wrapper.node.addInnerSetListeners((target, value) -> {
			if (wrapper.child != null) {
				if (childCanvas != null) {
					inner.getChildren().clear();
					childCanvas.remove(context);
				}
				wrapper.child.remove(context);
			}
			if (value != null) {
				wrapper.child = Window.createNode(context, wrapper, 0, value);
				childCanvas = wrapper.child.buildCanvas(context, this);
				inner.getChildren().add(childCanvas.getWidget());
				updateChildCanvasPosition(null);
			}
		});
		// Don't need clear listeners because clear should never happen (1 frame must always be left)
		this.positionAddListener = wrapper.node.addPositionFramesAddListeners((target, at, value) -> {
			updatePosition(context);
			positionCleanup.addAll(at, value.stream().map(v -> {
				GroupPositionFrame.LengthSetListener lengthListener = v.addLengthSetListeners((target1, value1) -> {
					updatePosition(context);
				});
				GroupPositionFrame.OffsetSetListener offsetListener =
						v.addOffsetSetListeners((target12, value12) -> updatePosition(context));
				return (Runnable) () -> {
					v.removeLengthSetListeners(lengthListener);
					v.removeOffsetSetListeners(offsetListener);
				};
			}).collect(Collectors.toList()));
		});
		this.positionRemoveListener = wrapper.node.addPositionFramesRemoveListeners((target, at, count) -> {
			updatePosition(context);
			List<Runnable> tempClean = positionCleanup.subList(at, at + count);
			tempClean.forEach(r -> r.run());
			tempClean.clear();
		});
		this.positionMoveListener = wrapper.node.addPositionFramesMoveToListeners((target, source, count, dest) -> {
			updatePosition(context);
			moveTo(positionCleanup, source, count, dest);
		});
		this.timeAddListener = wrapper.node.addTimeFramesAddListeners((target, at, value) -> {
			updateTime(context);
			timeCleanup.addAll(at, value.stream().map(v -> {
				GroupTimeFrame.LengthSetListener lengthListener = v.addLengthSetListeners((target1, value1) -> {
					updatePosition(context);
				});
				GroupTimeFrame.InnerOffsetSetListener offsetListener =
						v.addInnerOffsetSetListeners((target12, value12) -> updatePosition(context));
				return (Runnable) () -> {
					v.removeLengthSetListeners(lengthListener);
					v.removeInnerOffsetSetListeners(offsetListener);
				};
			}).collect(Collectors.toList()));
		});
		this.timeRemoveListener = wrapper.node.addTimeFramesRemoveListeners((target, at, count) -> {
			updateTime(context);
			List<Runnable> tempClean = timeCleanup.subList(at, at + count);
			tempClean.forEach(r -> r.run());
			tempClean.clear();
		});
		this.timeMoveListener = wrapper.node.addTimeFramesMoveToListeners((target, source, count, dest) -> {
			updateTime(context);
			moveTo(timeCleanup, source, count, dest);
		});
		this.wrapper = wrapper;
	}

	private GroupPositionFrame findPosition() {
		return findPosition(currentFrame);
	}

	private GroupPositionFrame findPosition(int frame) {
		return wrapper.findPosition(wrapper.node, frame).frame;
	}

	private void updateTime(ProjectContext context) {
		if (childCanvas != null)
			childCanvas.setFrame(context, wrapper.findInnerFrame(currentFrame));
	}

	@Override
	public void remove(ProjectContext context) {
		if (childCanvas != null) {
			childCanvas.remove(context);
			childCanvas = null;
		}
		wrapper.node.removeInnerSetListeners(innerSetListener);
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
		baseBounds = newBounds;
		updatePosition(context);
	}

	private void updatePosition(ProjectContext context) {
		GroupPositionFrame pos = findPosition();
		if (baseBounds == null)
			return;
		DoubleRectangle newBounds = baseBounds.minus(pos.offset());
		updateChildCanvasPosition(pos);
		if (childCanvas != null)
			childCanvas.setViewport(context, newBounds, zoom);
	}

	@Override
	public void setFrame(ProjectContext context, int frameNumber) {
		this.currentFrame = frameNumber;
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
	public Wrapper.CanvasHandle getParent() {
		return parent;
	}
}
