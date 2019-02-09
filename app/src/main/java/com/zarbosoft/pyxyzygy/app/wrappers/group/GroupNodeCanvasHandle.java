package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.core.model.ProjectNode;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ToolBar;

import static com.zarbosoft.pyxyzygy.app.Global.opacityMax;
import static com.zarbosoft.pyxyzygy.app.Misc.mirror;
import static com.zarbosoft.pyxyzygy.app.Misc.noopConsumer;

public class GroupNodeCanvasHandle extends CanvasHandle {
	private final Runnable layerListenCleanup;
	private final ProjectNode.OpacitySetListener opacityListener;
	private final ObservableList<CanvasHandle> childHandles = FXCollections.observableArrayList();
	private final CanvasHandle parent;
	final SimpleIntegerProperty positiveZoom = new SimpleIntegerProperty(0);

	ToolBar toolBar = new ToolBar();
	private GroupNodeWrapper wrapper;

	public GroupNodeCanvasHandle(
			ProjectContext context, CanvasHandle parent, GroupNodeWrapper wrapper
	) {
		this.parent = parent;
		layerListenCleanup = mirror(wrapper.children, childHandles, c -> {
			return c.buildCanvas(context, this);
		}, h -> h.remove(context), noopConsumer());
		mirror(childHandles, inner.getChildren(), h -> {
			return h.getWidget();
		}, noopConsumer(), noopConsumer());
		this.opacityListener = wrapper.node.addOpacitySetListeners((target, value) -> {
			inner.setOpacity((double) value / opacityMax);
		});
		this.wrapper = wrapper;
	}

	@Override
	public void setViewport(ProjectContext context, DoubleRectangle newBounds, int positiveZoom) {
		this.positiveZoom.set(positiveZoom);
		childHandles.forEach(c -> c.setViewport(context, newBounds, positiveZoom));
	}

	@Override
	public void setFrame(ProjectContext context, int frameNumber) {
		this.frameNumber.set(frameNumber);
		childHandles.forEach(c -> c.setFrame(context, frameNumber));
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
