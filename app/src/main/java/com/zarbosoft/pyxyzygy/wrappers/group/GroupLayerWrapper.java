package com.zarbosoft.pyxyzygy.wrappers.group;

import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.pyxyzygy.*;
import com.zarbosoft.pyxyzygy.config.NodeConfig;
import com.zarbosoft.pyxyzygy.model.*;
import javafx.scene.control.TabPane;

import java.util.List;

public class GroupLayerWrapper extends Wrapper {
	private final Wrapper parent;
	public final GroupLayer node;
	private final GroupLayer.InnerSetListener innerSetListener;
	public Wrapper child;

	public GroupLayerWrapper(ProjectContext context, Wrapper parent, int parentIndex, GroupLayer node) {
		this.parent = parent;
		this.parentIndex = parentIndex;
		this.node = node;

		this.innerSetListener = node.addInnerSetListeners((target, value) -> {
			if (child != null) {
				tree.unbind();
				child.remove(context);
			}
			if (value != null) {
				child = Window.createNode(context, GroupLayerWrapper.this, 0, value);
				tree.bind(child.tree);
			}
		});
	}

	public static int findInnerFrame(GroupLayer node, int frame) {
		TimeResult result = findTime(node, frame);
		int offset = (frame - result.at);
		if (result.frame.innerLoop() != 0) offset = offset % result.frame.innerLoop();
		int innerFrame = result.frame.innerOffset() + offset;
		return innerFrame;
	}
	public int findInnerFrame(int frame) {
		return findInnerFrame(node,frame );
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

	@Override
	public Wrapper getParent() {
		return parent;
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
	public ProjectObject getValue() {
		return node;
	}

	@Override
	public NodeConfig getConfig() {
		return null;
	}

	@Override
	public CanvasHandle buildCanvas(ProjectContext context, CanvasHandle parent) {
		return new GroupLayerCanvasHandle(this, parent,context);
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
	public void removeChild(ProjectContext context, int index) {
		if (parent == null)
			context.history.change(c -> c.project(context.project).topRemove(parentIndex, 1));
		else
			parent.removeChild(context, parentIndex);
	}

	@Override
	public TakesChildren takesChildren() {
		throw new Assertion();
	}

	@Override
	public void remove(ProjectContext context) {
		node.removeInnerSetListeners(innerSetListener);
	}

	@Override
	public EditHandle buildEditControls(ProjectContext context, TabPane leftTabs) {
		return null;
	}

}
