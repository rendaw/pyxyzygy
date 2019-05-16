package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectLayer;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ToolBar;

import static com.zarbosoft.pyxyzygy.app.Misc.mirror;
import static com.zarbosoft.pyxyzygy.app.Misc.noopConsumer;

public class GroupNodeCanvasHandle extends CanvasHandle {
	private final Runnable layerListenCleanup;
	private final ObservableList<CanvasHandle> childHandles = FXCollections.observableArrayList();
	private final CanvasHandle parent;
	final SimpleIntegerProperty positiveZoom = new SimpleIntegerProperty(0);
	private final Listener.ScalarSet<ProjectLayer, Vector> offsetListener;

	ToolBar toolBar = new ToolBar();
	private GroupNodeWrapper wrapper;

	public GroupNodeCanvasHandle(
			ProjectContext context, Window window, CanvasHandle parent, GroupNodeWrapper wrapper
	) {
		this.parent = parent;
		layerListenCleanup = mirror(wrapper.children, childHandles, c -> {
			final CanvasHandle canvasHandle = c.buildCanvas(context, window, this);
			canvasHandle.setViewport(context, bounds.get(), positiveZoom.get());
			return canvasHandle;
		}, h -> h.remove(context), noopConsumer());
		mirror(childHandles, paint.getChildren(), h -> {
			return h.getPaintWidget();
		}, noopConsumer(), noopConsumer());
		mirror(childHandles, overlay.getChildren(), h -> {
			return h.getOverlayWidget();
		}, noopConsumer(), noopConsumer());
		offsetListener = wrapper.node.addOffsetSetListeners((target, offset) -> {
			paint.setLayoutX(offset.x);
			paint.setLayoutY(offset.y);
			overlay.setLayoutX(offset.x);
			overlay.setLayoutY(offset.y);
		});
		this.wrapper = wrapper;
	}

	@Override
	public void setViewport(ProjectContext context, DoubleRectangle newBounds, int positiveZoom) {
		this.positiveZoom.set(positiveZoom);
		this.bounds.set(newBounds);
		childHandles.forEach(c -> c.setViewport(context, newBounds, positiveZoom));
	}

	@Override
	public void setFrame(ProjectContext context, int frameNumber) {
		this.frameNumber.set(frameNumber);
		childHandles.forEach(c -> c.setFrame(context, frameNumber));

		do {
			if (wrapper.specificChild == null) {
				previousFrame.set(-1);
				break;
			}
			if (wrapper.specificChild.positionFramesLength() == 1) {
				previousFrame.set(-1);
				break;
			}
			int frameIndex =
					GroupChildWrapper.positionFrameFinder.findFrame(wrapper.specificChild, frameNumber).frameIndex - 1;
			if (frameIndex == -1)
				frameIndex = wrapper.specificChild.positionFramesLength() - 1;
			int outFrame = 0;
			for (int i = 0; i < frameIndex; ++i) {
				outFrame += wrapper.specificChild.positionFramesGet(i).length();
			}
			previousFrame.set(outFrame);
		} while (false);
	}

	@Override
	public void remove(ProjectContext context) {
		wrapper.canvasHandle = null;
		childHandles.forEach(c -> c.remove(context));
		wrapper.node.removeOffsetSetListeners(offsetListener);
		layerListenCleanup.run();
	}

	@Override
	public Wrapper getWrapper() {
		return wrapper;
	}

	@Override
	public CanvasHandle getParent() {
		return parent;
	}

	@Override
	public DoubleVector toInner(DoubleVector vector) {
		return vector.minus(wrapper.node.offset());
	}
}
