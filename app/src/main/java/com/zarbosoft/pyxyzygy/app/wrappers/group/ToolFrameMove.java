package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.Tool;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.core.model.v0.GroupPositionFrame;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;

import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.centerCursor;

public class ToolFrameMove extends Tool {
	protected DoubleVector markStart;
	private Vector markStartOffset;
	private GroupNodeWrapper wrapper;
	private GroupPositionFrame pos;

	public ToolFrameMove(Window window, GroupNodeWrapper wrapper) {
		this.wrapper = wrapper;
		window.editorCursor.set(this, centerCursor("cursor-move32.png"));
	}

	@Override
	public void markStart(
			ProjectContext context, Window window, DoubleVector start, DoubleVector globalStart
	) {
		if (wrapper.specificLayer == null)
			return;
		pos = GroupLayerWrapper.positionFrameFinder.findFrame(wrapper.specificLayer,
				wrapper.canvasHandle.frameNumber.get()
		).frame;
		this.markStart = globalStart;
		this.markStartOffset = pos.offset();
	}

	@Override
	public void mark(
			ProjectContext context,
			Window window,
			DoubleVector start,
			DoubleVector end,
			DoubleVector globalStart,
			DoubleVector globalEnd
	) {
		if (wrapper.specificLayer == null)
			return;
		context.change(new ProjectContext.Tuple(wrapper, "move-frame"),
				c -> c.groupPositionFrame(pos).offsetSet(globalEnd.minus(markStart).plus(markStartOffset).toInt())
		);
	}

	@Override
	public void remove(ProjectContext context, Window window) {
		window.editorCursor.clear(this);
	}

	@Override
	public void cursorMoved(ProjectContext context, Window window, DoubleVector position) {

	}
}
