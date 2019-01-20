package com.zarbosoft.shoedemo.structuretree;

import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.shoedemo.*;
import com.zarbosoft.shoedemo.config.NodeConfig;
import com.zarbosoft.shoedemo.model.*;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.TabPane;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.zarbosoft.shoedemo.Main.moveTo;

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
	private WidgetHandle childCanvas;
	private int zoom;
	private DoubleRectangle baseBounds;

	public GroupLayerWrapper(ProjectContext context, Wrapper parent, int parentIndex, GroupLayer node) {
		this.parent = parent;
		this.parentIndex = parentIndex;
		this.node = node;

		this.innerSetListener = node.addInnerSetListeners((target, value) -> {
			if (child != null) {
				if (childCanvas != null) {
					canvas.getChildren().clear();
					childCanvas.remove();
				}
				tree.unbind();
				child.remove(context);
			}
			if (value != null) {
				child = Window.createNode(context, GroupLayerWrapper.this, 0, value);
				tree.bind(child.tree);
				if (canvas != null) {
					childCanvas = child.buildCanvas(context);
					canvas.getChildren().add(childCanvas.getWidget());
					updateChildCanvasPosition(null);
				}
			}
		});
		// Don't need clear listeners because clear should never happen (1 frame must always be left)
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
		TimeResult result = findTime(node, frame);
		int offset = (frame - result.at);
		if (result.frame.innerLoop() != 0) offset = offset % result.frame.innerLoop();
		int innerFrame = result.frame.innerOffset() + offset;
		return innerFrame;
	}

	public static class TimeResult {
		public final GroupTimeFrame frame;
		public final int at;
		public final int frameIndex;

		public TimeResult(GroupTimeFrame frame, int at, int frameIndex) {
			this.frame = frame;
			this.at = at;
			this.frameIndex = frameIndex;
		}
	}

	public static TimeResult findTime(GroupLayer node, int frame) {
		int at = 0;
		for (int i = 0; i < node.timeFramesLength(); ++i) {
			GroupTimeFrame pos = node.timeFrames().get(i);
			if ((i == node.timeFramesLength() - 1) ||
					(frame >= at && (pos.length() == -1 || frame < at + pos.length()))) {
				return new TimeResult(pos, at, i);
			}
			at += pos.length();
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
		return findPosition(node, frame).frame;
	}

	public static class PositionResult {
		public final GroupPositionFrame frame;
		public final int at;
		public final int frameIndex;

		public PositionResult(GroupPositionFrame frame, int at, int frameIndex) {
			this.frame = frame;
			this.at = at;
			this.frameIndex = frameIndex;
		}
	}

	public static PositionResult findPosition(GroupLayer node, int frame) {
		int at = 0;
		for (int i = 0; i < node.positionFramesLength(); ++i) {
			GroupPositionFrame pos = node.positionFrames().get(i);
			if ((i == node.positionFramesLength() - 1) ||
					(frame >= at && (pos.length() == -1 || frame < at + pos.length()))) {
				return new PositionResult(pos, at, i);
			}
			at += pos.length();
		}
		throw new Assertion();
	}

	@Override
	public DoubleVector toInner(DoubleVector vector) {
		GroupPositionFrame pos = findPosition();
		return vector.minus(pos.offset());
	}

	@Override
	public ProjectObject getValue() {
		return node;
	}

	@Override
	public NodeConfig getConfig() {
		return null;
	}

	@Override
	public void setViewport(
			ProjectContext context, DoubleRectangle newBounds, int zoom
	) {
		this.zoom = zoom;
		baseBounds = newBounds;
		updatePosition(context);
	}

	private void updateChildCanvasPosition(GroupPositionFrame pos) {
		if (pos == null)
			pos = findPosition();
		canvas.setLayoutX(pos.offset().x);
		canvas.setLayoutY(pos.offset().y);
	}

	private void updatePosition(ProjectContext context) {
		GroupPositionFrame pos = findPosition();
		if (baseBounds == null)
			return;
		DoubleRectangle newBounds = baseBounds.minus(pos.offset());
		if (canvas != null)
			updateChildCanvasPosition(pos);
		if (child != null)
			child.setViewport(context, newBounds, zoom);
	}

	@Override
	public WidgetHandle buildCanvas(ProjectContext context) {
		return new WidgetHandle() {
			{
				GroupPositionFrame pos = findPosition();
				canvas = new Group();
				if (child != null) {
					childCanvas = child.buildCanvas(context);
					canvas.getChildren().add(childCanvas.getWidget());
					updateChildCanvasPosition(pos);
				}
			}

			@Override
			public Node getWidget() {
				return canvas;
			}

			@Override
			public void remove() {
				if (childCanvas != null) {
					childCanvas.remove();
					childCanvas = null;
				}
				canvas = null;
			}
		};
	}

	@Override
	public void markStart(ProjectContext context, DoubleVector start) {
		throw new Assertion();
	}

	@Override
	public void mark(ProjectContext context, DoubleVector start, DoubleVector end) {
		throw new Assertion();
	}

	@Override
	public boolean addChildren(ProjectContext context, int at, List<ProjectNode> child) {
		return false;
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
	public void render(TrueColorImage output, int frame, Rectangle crop, double opacity) {
		GroupPositionFrame pos = findPosition(frame);
		frame = findInnerFrame(frame);
		if (child != null)
			child.render(output, frame, crop.plus(pos.offset()), opacity);
	}

	@Override
	public void removeChild(ProjectContext context, int index) {
		if (parent == null)
			context.history.change(c -> c.project(context.project).topRemove(parentIndex, 1));
		else
			parent.removeChild(context, parentIndex);
	}

	@Override
	public WidgetHandle buildCanvasProperties(ProjectContext context) {
		return null;
	}

	@Override
	public TakesChildren takesChildren() {
		throw new Assertion();
	}

	@Override
	public void setFrame(ProjectContext context, int frameNumber) {
		this.currentFrame = frameNumber;
		updateTime(context);
		updatePosition(context);
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
	public Runnable createProperties(ProjectContext context, TabPane leftTabs) {
		return () -> {};
	}
}
