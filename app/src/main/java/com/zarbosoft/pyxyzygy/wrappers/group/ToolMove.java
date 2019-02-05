package com.zarbosoft.pyxyzygy.wrappers.group;

import com.zarbosoft.pyxyzygy.DoubleVector;
import com.zarbosoft.pyxyzygy.ProjectContext;
import com.zarbosoft.pyxyzygy.model.GroupPositionFrame;
import com.zarbosoft.internal.pyxyzygy_seed.model.Vector;

public class ToolMove extends Tool {
	private DoubleVector markStart;
	private Vector markStartOffset;
	private GroupNodeWrapper wrapper;

	public ToolMove(GroupNodeWrapper wrapper) {
		this.wrapper = wrapper;
	}

	@Override
	public void markStart(ProjectContext context, DoubleVector start) {
		if (wrapper.specificLayer == null)
			return;
		this.markStart = start;
		GroupPositionFrame pos = GroupLayerWrapper.findPosition(
				wrapper.specificLayer,
				wrapper.canvasHandle.frameNumber.get()
		).frame;
		this.markStartOffset = pos.offset();
	}

	@Override
	public void mark(ProjectContext context, DoubleVector start, DoubleVector end) {
		if (wrapper.specificLayer == null)
			return;
		GroupPositionFrame pos = GroupLayerWrapper.findPosition(
				wrapper.specificLayer,
				wrapper.canvasHandle.frameNumber.get()
		).frame;
		context.history.change(c -> c
				.groupPositionFrame(pos)
				.offsetSet(end.minus(markStart).plus(markStartOffset).toInt()));
	}

	@Override
	public void remove(ProjectContext context) {
	}
}
