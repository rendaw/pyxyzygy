package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.zarbosoft.pyxyzygy.app.CanvasHandle;
import com.zarbosoft.pyxyzygy.app.DoubleRectangle;
import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.Wrapper;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectNode;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ToolBar;

import java.util.Optional;

import static com.zarbosoft.pyxyzygy.app.Global.opacityMax;
import static com.zarbosoft.pyxyzygy.app.Misc.mirror;
import static com.zarbosoft.pyxyzygy.app.Misc.noopConsumer;

public class GroupNodeCanvasHandle extends CanvasHandle {
	private final Runnable layerListenCleanup;
	private final ObservableList<CanvasHandle> childHandles = FXCollections.observableArrayList();
	private final CanvasHandle parent;
	final SimpleIntegerProperty positiveZoom = new SimpleIntegerProperty(0);
	private final Listener.ScalarSet<ProjectNode, Integer> opacityListener;

	ToolBar toolBar = new ToolBar();
	private GroupNodeWrapper wrapper;

	public GroupNodeCanvasHandle(
			ProjectContext context, CanvasHandle parent, GroupNodeWrapper wrapper
	) {
		this.parent = parent;
		layerListenCleanup = mirror(wrapper.children, childHandles, c -> {
			final CanvasHandle canvasHandle = c.buildCanvas(context, this);
			canvasHandle.setViewport(context,bounds.get(),positiveZoom.get());
			return canvasHandle;
		}, h -> h.remove(context), noopConsumer());
		mirror(childHandles, inner.getChildren(), h -> {
			return h.getWidget();
		}, noopConsumer(), noopConsumer());
		opacityListener = wrapper.node.addOpacitySetListeners((target, value) -> {
			inner.setOpacity((double) value / opacityMax);
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
			if (wrapper.specificLayer == null) {
				previousFrame.set(-1);
				break;
			}
			if (wrapper.specificLayer.positionFramesLength() == 1) {
				previousFrame.set(-1);
				break;
			}
			int frameIndex = GroupLayerWrapper.findPosition(wrapper.specificLayer, frameNumber).frameIndex - 1;
			if (frameIndex == -1)
				frameIndex = wrapper.specificLayer.positionFramesLength() - 1;
			int outFrame = 0;
			for (int i = 0; i < frameIndex; ++i) {
				outFrame += wrapper.specificLayer.positionFramesGet(i).length();
			}
			previousFrame.set(outFrame);
		} while (false);
	}

	@Override
	public void remove(ProjectContext context) {
		wrapper.canvasHandle = null;
		childHandles.forEach(c -> c.remove(context));
		wrapper.node.removeOpacitySetListeners(opacityListener);
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
		return vector;
	}
}
