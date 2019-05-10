package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.zarbosoft.pyxyzygy.app.CanvasHandle;
import com.zarbosoft.pyxyzygy.app.EditHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.Wrapper;
import com.zarbosoft.pyxyzygy.app.config.NodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.wrappers.FrameFinder;
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
	public static final FrameFinder<GroupLayer, GroupPositionFrame> positionFrameFinder =
			new FrameFinder<GroupLayer, GroupPositionFrame>() {
				@Override
				public GroupPositionFrame frameGet(GroupLayer node, int i) {
					return node.positionFramesGet(i);
				}

				@Override
				public int frameCount(GroupLayer node) {
					return node.positionFramesLength();
				}

				@Override
				public int frameLength(GroupPositionFrame frame) {
					return frame.length();
				}
			};
	public static final FrameFinder<GroupLayer, GroupTimeFrame> timeFrameFinder =
			new FrameFinder<GroupLayer, GroupTimeFrame>() {
				@Override
				public GroupTimeFrame frameGet(GroupLayer node, int i) {
					return node.timeFramesGet(i);
				}

				@Override
				public int frameCount(GroupLayer node) {
					return node.timeFramesLength();
				}

				@Override
				public int frameLength(GroupTimeFrame frame) {
					return frame.length();
				}
			};

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
		FrameFinder.Result<GroupTimeFrame> result = timeFrameFinder.findFrame(node, frame);
		int offset = (frame - result.at);
		if (result.frame.innerLoop() != 0)
			offset = offset % result.frame.innerLoop();
		int innerFrame = result.frame.innerOffset() + offset;
		return innerFrame;
	}

	public int findInnerFrame(int frame) {
		return findInnerFrame(node, frame);
	}

	@Override
	public Wrapper getParent() {
		return parent;
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
