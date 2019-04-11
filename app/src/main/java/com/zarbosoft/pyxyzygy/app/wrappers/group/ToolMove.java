package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.Tool;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.core.model.v0.GroupPositionFrame;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import javafx.scene.ImageCursor;

import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.centerCursor;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.icon;

public class ToolMove extends Tool {
	protected DoubleVector markStart;
	private Vector markStartOffset;
	private GroupNodeWrapper wrapper;

	public ToolMove(Window window, GroupNodeWrapper wrapper) {
		this.wrapper = wrapper;
		window.editorCursor.set(this,centerCursor("cursor-move32.png"));
	}

	@Override
	public void markStart(ProjectContext context, Window window, DoubleVector start) {
		if (wrapper.specificLayer == null)
			return;
		this.markStart = start;
		GroupPositionFrame pos = GroupLayerWrapper.positionFrameFinder.findFrame(
				wrapper.specificLayer,
				wrapper.canvasHandle.frameNumber.get()
		).frame;
		this.markStartOffset = pos.offset();
	}

	@Override
	public void mark(ProjectContext context, Window window, DoubleVector start, DoubleVector end) {
		if (wrapper.specificLayer == null)
			return;
		GroupPositionFrame pos = GroupLayerWrapper.positionFrameFinder.findFrame(
				wrapper.specificLayer,
				wrapper.canvasHandle.frameNumber.get()
		).frame;
		context.change(new ProjectContext.Tuple(wrapper,"move"), c -> c
				.groupPositionFrame(pos)
				.offsetSet(end.minus(markStart).plus(markStartOffset).toInt()));
	}

	@Override
	public void remove(ProjectContext context, Window window) {
		window.editorCursor.clear(this);
	}

	@Override
	public void cursorMoved(ProjectContext context, Window window, DoubleVector position) {

	}
}
