package com.zarbosoft.shoedemo.wrappers.group;

import com.zarbosoft.shoedemo.DoubleVector;
import com.zarbosoft.shoedemo.ProjectContext;
import com.zarbosoft.shoedemo.model.GroupPositionFrame;
import com.zarbosoft.shoedemo.model.Vector;
import javafx.scene.Node;

public class ToolMove extends Tool {
	private DoubleVector markStart;
	private Vector markStartOffset;
	private GroupNodeWrapper groupNodeWrapper;

	public ToolMove(GroupNodeWrapper groupNodeWrapper) {
		this.groupNodeWrapper = groupNodeWrapper;
	}

	@Override
	public void markStart(ProjectContext context, DoubleVector start) {
		if (groupNodeWrapper.specificLayer == null)
			return;
		this.markStart = start;
		GroupPositionFrame pos = GroupLayerWrapper.findPosition(
				groupNodeWrapper.specificLayer,
				groupNodeWrapper.currentFrame
		).frame;
		this.markStartOffset = pos.offset();
	}

	@Override
	public void mark(ProjectContext context, DoubleVector start, DoubleVector end) {
		if (groupNodeWrapper.specificLayer == null)
			return;
		GroupPositionFrame pos = GroupLayerWrapper.findPosition(
				groupNodeWrapper.specificLayer,
				groupNodeWrapper.currentFrame
		).frame;
		context.history.change(c -> c
				.groupPositionFrame(pos)
				.offsetSet(end.minus(markStart).plus(markStartOffset).toInt()));
	}

	@Override
	public Node getProperties() {
		return null;
	}

	@Override
	public void remove(ProjectContext context) {
	}
}
