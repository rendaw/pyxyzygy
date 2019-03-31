package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.zarbosoft.pyxyzygy.app.CanvasHandle;
import com.zarbosoft.pyxyzygy.app.EditHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.Wrapper;
import com.zarbosoft.pyxyzygy.app.config.NodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.core.model.v0.*;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.beans.property.SimpleObjectProperty;

import java.util.List;

public class GroupLayerWrapper extends Wrapper {
	private final Wrapper parent;
	public final GroupLayer node;
	public final SimpleObjectProperty<Wrapper> child = new SimpleObjectProperty<>();
	private final Listener.ScalarSet<GroupLayer, ProjectNode> innerSetListener;

	public GroupLayerWrapper(ProjectContext context, Wrapper parent, int parentIndex, GroupLayer node) {
		this.parent = parent;
		this.parentIndex = parentIndex;
		this.node = node;

		innerSetListener = node.addInnerSetListeners((target, value) -> {
			if (child.get() != null) {
				tree.unbind();
				child.get().remove(context);
				child.set(null);
			}
			if (value != null) {
				child.set(Window.createNode(context, GroupLayerWrapper.this, parentIndex, value));
				tree.bind(child.get().tree);
			}
		});
	}

	public static int findInnerFrame(GroupLayer node, int frame) {
		TimeResult result = findTime(node, frame);
		int offset = (frame - result.at);
		if (result.frame.innerLoop() != 0)
			offset = offset % result.frame.innerLoop();
		int innerFrame = result.frame.innerOffset() + offset;
		return innerFrame;
	}

	public int findInnerFrame(int frame) {
		return findInnerFrame(node, frame);
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
	public CanvasHandle buildCanvas(
			ProjectContext context, Window window, CanvasHandle parent
	) {
		return new GroupLayerCanvasHandle(context, window, this, parent);
	}

	@Override
	public boolean addChildren(
			ProjectContext context, ChangeStepBuilder change, int at, List<ProjectNode> child
	) {
		GroupNodeWrapper parent = (GroupNodeWrapper) this.parent;
		parent.addChildren(context, change, parentIndex + 1, child);
		return true;
	}

	@Override
	public void delete(ProjectContext context, ChangeStepBuilder change) {
		throw new Assertion();
	}

	@Override
	public ProjectNode separateClone(ProjectContext context) {
		throw new Assertion();
	}

	@Override
	public void removeChild(
			ProjectContext context, ChangeStepBuilder change, int index
	) {
		parent.removeChild(context, change, parentIndex);
	}

	@Override
	public void setParentIndex(int index) {
		this.parentIndex = index;
		if (child.get() == null)
			return;
		child.get().setParentIndex(index);
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
	public EditHandle buildEditControls(
			ProjectContext context, Window window
	) {
		return null;
	}

}
