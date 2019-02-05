package com.zarbosoft.pyxyzygy.wrappers.group;

import com.zarbosoft.pyxyzygy.DoubleRectangle;
import com.zarbosoft.pyxyzygy.DoubleVector;
import com.zarbosoft.pyxyzygy.ProjectContext;
import com.zarbosoft.pyxyzygy.Wrapper;
import com.zarbosoft.pyxyzygy.model.ProjectNode;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ToolBar;

import static com.zarbosoft.pyxyzygy.Launch.opacityMax;
import static com.zarbosoft.pyxyzygy.Misc.mirror;
import static com.zarbosoft.pyxyzygy.Misc.noopConsumer;

public class GroupNodeCanvasHandle extends Wrapper.CanvasHandle {
	private final Runnable layerListenCleanup;
	private final ProjectNode.OpacitySetListener opacityListener;
	private final ObservableList<Wrapper.CanvasHandle> childHandles = FXCollections.observableArrayList();
	private final Wrapper.CanvasHandle parent;
	final SimpleIntegerProperty positiveZoom = new SimpleIntegerProperty(0);

	ToolBar toolBar = new ToolBar();
	private GroupNodeWrapper wrapper;

	public GroupNodeCanvasHandle(
			ProjectContext context, Wrapper.CanvasHandle parent, GroupNodeWrapper wrapper
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
	public Wrapper.CanvasHandle getParent() {
		return parent;
	}

	@Override
	public DoubleVector toInner(DoubleVector vector) {
		return vector;
	}
}
