package com.zarbosoft.shoedemo.wrappers.group;

import com.zarbosoft.shoedemo.ProjectContext;
import com.zarbosoft.shoedemo.WidgetHandle;
import com.zarbosoft.shoedemo.model.ProjectNode;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ToolBar;

import static com.zarbosoft.shoedemo.Main.mirror;
import static com.zarbosoft.shoedemo.Main.noopConsumer;
import static com.zarbosoft.shoedemo.Main.opacityMax;

class GroupNodeCanvasHandle extends WidgetHandle {
	Group canvasOuter;
	private Group canvas;
	private final Runnable layerListenCleanup;
	private final ProjectNode.OpacitySetListener opacityListener;
	private final ObservableList<WidgetHandle> childHandles = FXCollections.observableArrayList();

	ToolBar toolBar = new ToolBar();
	private GroupNodeWrapper groupNodeWrapper;

	public GroupNodeCanvasHandle(GroupNodeWrapper groupNodeWrapper, ProjectContext context) {
		canvas = new Group();
		layerListenCleanup = mirror(groupNodeWrapper.children, childHandles, c -> {
			return c.buildCanvas(context);
		}, h -> h.remove(), noopConsumer());
		mirror(childHandles, canvas.getChildren(), h -> {
			return h.getWidget();
		}, noopConsumer(), noopConsumer());
		this.opacityListener = groupNodeWrapper.node.addOpacitySetListeners((target, value) -> {
			if (canvas != null) {
				canvas.setOpacity((double) value / opacityMax);
			}
		});
		canvasOuter = new Group();
		canvasOuter.getChildren().addAll(canvas);
		this.groupNodeWrapper = groupNodeWrapper;
	}

	@Override
	public Node getWidget() {
		return canvasOuter;
	}

	@Override
	public void remove() {
		childHandles.forEach(c -> c.remove());
		groupNodeWrapper.node.removeOpacitySetListeners(opacityListener);
		layerListenCleanup.run();
		groupNodeWrapper.canvasHandle = null;
	}
}
