package com.zarbosoft.shoedemo.structuretree;

import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.shoedemo.*;
import com.zarbosoft.shoedemo.model.*;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.zarbosoft.shoedemo.Timeline.moveTo;

public class GroupLayerWrapper extends Wrapper {
	private final Wrapper parent;
	private final GroupLayer node;
	private final GroupLayer.InnerSetListener innerSetListener;
	private final GroupLayer.PositionFramesAddListener positionAddListener;
	private final GroupLayer.PositionFramesRemoveListener positionRemoveListener;
	private final GroupLayer.PositionFramesMoveToListener positionMoveListener;
	private final GroupLayer.TimeFramesAddListener timeAddListener;
	private final GroupLayer.TimeFramesRemoveListener timeRemoveListener;
	private final GroupLayer.TimeFramesMoveToListener timeMoveListener;
	private final List<Runnable> positionCleanup = new ArrayList<>();
	private final List<Runnable> timeCleanup = new ArrayList<>();
	private int currentFrame;
	private Wrapper child;
	private Group canvas;
	private DoubleRectangle baseBounds;
	private DoubleRectangle bounds;
	private DoubleVector markStart;

	public GroupLayerWrapper(ProjectContext context, Wrapper parent, int parentIndex, GroupLayer node) {
		this.parent = parent;
		this.parentIndex = parentIndex;
		this.node = node;

		tree.set(new TreeItem<>(this));
		this.innerSetListener = node.addInnerSetListeners((target, value) -> {
			if (child != null) {
				if (canvas != null) {
					child.destroyCanvas();
					canvas.getChildren().clear();
				}
				tree.unbind();
				child.remove(context);
			}
			if (value != null) {
				child = Main.createNode(context, GroupLayerWrapper.this, 0, value);
				tree.bind(child.tree);
				if (canvas != null) {
					canvas.getChildren().add(child.buildCanvas(context, bounds));
				}
			}
		});
		this.positionAddListener = node.addPositionFramesAddListeners((target, at, value) -> {
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
		this.positionRemoveListener = node.addPositionFramesRemoveListeners((target, at, count) -> {
			updatePosition(context);
			List<Runnable> tempClean = positionCleanup.subList(at, at + count);
			tempClean.forEach(r -> r.run());
			tempClean.clear();
		});
		this.positionMoveListener = node.addPositionFramesMoveToListeners((target, source, count, dest) -> {
			updatePosition(context);
			moveTo(positionCleanup, source, count, dest);
		});
		this.timeAddListener = node.addTimeFramesAddListeners((target, at, value) -> {
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
		this.timeRemoveListener = node.addTimeFramesRemoveListeners((target, at, count) -> {
			updateTime(context);
			List<Runnable> tempClean = timeCleanup.subList(at, at + count);
			tempClean.forEach(r -> r.run());
			tempClean.clear();
		});
		this.timeMoveListener = node.addTimeFramesMoveToListeners((target, source, count, dest) -> {
			updateTime(context);
			moveTo(timeCleanup, source, count, dest);
		});
	}

	private int findInnerFrame(int frame) {
		int at = 0;
		for (GroupTimeFrame time : node.timeFrames()) {
			if (frame >= at && (time.length() == -1 || frame < at + time.length())) {
				return time.innerOffset() + frame - at;
			}
			at += time.length();
		}
		throw new Assertion();
	}

	private void updateTime(ProjectContext context) {
		if (child != null)
			child.setFrame(context, findInnerFrame(currentFrame));
	}

	@Override
	public Wrapper getParent() {
		return parent;
	}

	private GroupPositionFrame findPosition() {
		return findPosition(currentFrame);
	}

	private GroupPositionFrame findPosition(int frame) {
		int at = 0;
		for (GroupPositionFrame pos : node.positionFrames()) {
			if (frame >= at && (pos.length() == -1 || frame < at + pos.length())) {
				return pos;
			}
			at += pos.length();
		}
		throw new Assertion();
	}

	@Override
	public DoubleVector toInner(DoubleVector vector) {
		GroupPositionFrame pos = findPosition();
		return vector.plus(pos.offset());
	}

	@Override
	public ProjectObject getValue() {
		return node;
	}

	@Override
	public void scroll(
			ProjectContext context, DoubleRectangle oldBounds, DoubleRectangle newBounds
	) {
		baseBounds = newBounds;
		updatePosition(context);
	}

	private void updatePosition(ProjectContext context) {
		GroupPositionFrame pos = findPosition();
		if (baseBounds == null)
			return;
		DoubleRectangle newBounds = baseBounds.plus(pos.offset());
		if (child != null)
			child.scroll(context, bounds.plus(pos.offset()), newBounds);
		this.bounds = newBounds;
	}

	@Override
	public Node buildCanvas(ProjectContext context, DoubleRectangle bounds) {
		this.baseBounds = bounds;
		GroupPositionFrame pos = findPosition();
		this.bounds = baseBounds.plus(pos.offset());
		canvas = new Group();
		if (child != null)
			canvas.getChildren().add(child.buildCanvas(context, this.bounds));
		return canvas;
	}

	@Override
	public Node getCanvas() {
		return canvas;
	}

	@Override
	public void destroyCanvas() {
		if (child != null) {
			child.destroyCanvas();
		}
		canvas = null;
	}

	@Override
	public void markStart(ProjectContext context, DoubleVector start) {
		this.markStart = start;
	}

	@Override
	public boolean addChildren(ProjectContext context, int at, List<ProjectNode> child) {
		throw new Assertion();
	}

	@Override
	public void delete(ProjectContext context) {
		throw new Assertion();
	}

	@Override
	public ProjectNode separateClone(ProjectContext context) {
		throw new Assertion();
	}

	@Override
	public void render(GraphicsContext gc, int frame, Rectangle crop) {
		GroupPositionFrame pos = findPosition(frame);
		frame = findInnerFrame(frame);
		if (child != null)
			child.render(gc, frame, crop.plus(pos.offset()));
	}

	@Override
	public void removeChild(ProjectContext context, int index) {
		if (parent == null)
			context.change.project(context.project).topRemove(parentIndex, 1);
		else
			parent.removeChild(context, parentIndex);
	}

	@Override
	public void mark(ProjectContext context, DoubleVector start, DoubleVector end) {
		GroupPositionFrame pos = findPosition();
		context.change.groupPositionFrame(pos).offsetSet(end.minus(markStart).toInt());
	}

	@Override
	public void setFrame(ProjectContext context, int frameNumber) {
		this.currentFrame = frameNumber;
		updateTime(context);
	}

	@Override
	public void remove(ProjectContext context) {
		node.removeInnerSetListeners(innerSetListener);
		node.removePositionFramesAddListeners(positionAddListener);
		node.removePositionFramesRemoveListeners(positionRemoveListener);
		node.removePositionFramesMoveToListeners(positionMoveListener);
		node.removeTimeFramesAddListeners(timeAddListener);
		node.removeTimeFramesRemoveListeners(timeRemoveListener);
		node.removeTimeFramesMoveToListeners(timeMoveListener);
		positionCleanup.forEach(r -> r.run());
		timeCleanup.forEach(r -> r.run());
	}

	@Override
	public Node createProperties(ProjectContext context) {
		return new Group();
	}

	@Override
	public void destroyProperties() {

	}
}