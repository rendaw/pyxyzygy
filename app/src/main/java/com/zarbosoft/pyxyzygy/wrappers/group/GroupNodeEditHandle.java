package com.zarbosoft.pyxyzygy.wrappers.group;

import com.zarbosoft.pyxyzygy.*;
import com.zarbosoft.pyxyzygy.config.GroupNodeConfig;
import com.zarbosoft.pyxyzygy.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.widgets.WidgetFormBuilder;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.zarbosoft.pyxyzygy.Misc.nodeFormFields;
import static com.zarbosoft.pyxyzygy.widgets.HelperJFX.pad;

public class GroupNodeEditHandle extends EditHandle {
	private Tab groupTab;
	public Tab toolTab;
	List<Runnable> cleanup = new ArrayList<>();
	Tool tool = null;

	Group overlay;

	ToolBar toolBar = new ToolBar();
	private GroupNodeWrapper wrapper;
	public final SimpleDoubleProperty mouseX = new SimpleDoubleProperty(0);
	public final SimpleDoubleProperty mouseY = new SimpleDoubleProperty(0);

	public GroupNodeEditHandle(final GroupNodeWrapper wrapper, ProjectContext context, TabPane tabPane) {
		// Canvas overlay
		overlay = new Group();
		wrapper.canvasHandle.overlay.getChildren().add(overlay);

		// Group tab
		groupTab = new Tab(
				"Group",
				pad(new WidgetFormBuilder().apply(b -> cleanup.add(nodeFormFields(context, b, wrapper))).build())
		);

		// Tool tab
		toolTab = new Tab();

		// Toolbar
		class ToolToggle extends HelperJFX.IconToggleButton {
			private final GroupNodeConfig.Tool value;

			public ToolToggle(String icon, String hint, GroupNodeConfig.Tool value) {
				super(icon, hint);
				this.value = value;
				selectedProperty().bind(Bindings.createBooleanBinding(() -> {
					return wrapper.config.tool.get() == value;
				}, wrapper.config.tool));
				selectedProperty().addListener(new ChangeListener<Boolean>() {
					{
						changed(null, null, selectedProperty().get());
					}

					@Override
					public void changed(
							ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue
					) {
						if (newValue)
							toolTab.setText(hint);
					}
				});
			}

			@Override
			public void fire() {
				wrapper.config.tool.set(value);
			}
		}
		toolBar.getItems().addAll(
				new ToolToggle("cursor-move.png", "Move", GroupNodeConfig.Tool.MOVE),
				new ToolToggle("stamper.png", "Stamp", GroupNodeConfig.Tool.STAMP)
		);

		wrapper.config.tool.addListener(new ChangeListener<GroupNodeConfig.Tool>() {
			{
				changed(null, null, wrapper.config.tool.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends GroupNodeConfig.Tool> observable,
					GroupNodeConfig.Tool oldValue,
					GroupNodeConfig.Tool newValue
			) {
				if (tool != null) {
					toolTab.setContent(null);
					tool.remove(context);
				}
				switch (newValue) {
					case MOVE:
						tool = new ToolMove(wrapper);
						break;
					case STAMP:
						tool = new ToolStamp(wrapper, context, GroupNodeEditHandle.this);
						break;
					default:
						throw new Assertion();
				}
			}
		});

		tabPane.getTabs().addAll(groupTab, toolTab);
		cleanup.add(() -> {
			tabPane.getTabs().removeAll(groupTab, toolTab);
		});
		this.wrapper = wrapper;
	}

	@Override
	public Node getProperties() {
		return toolBar;
	}

	@Override
	public void remove(ProjectContext context) {
		if (tool != null) {
			tool.remove(context);
			tool = null;
		}
		if (wrapper.canvasHandle != null)
			wrapper.canvasHandle.overlay.getChildren().remove(overlay);
		cleanup.forEach(c -> c.run());
	}

	@Override
	public Wrapper getWrapper() {
		return wrapper;
	}

	@Override
	public void markStart(ProjectContext context, Window window, DoubleVector start) {
		if (tool == null)
			return;
		start = Window.toLocal(wrapper.canvasHandle, start);
		tool.markStart(context, window, start);
	}

	@Override
	public CanvasHandle getCanvas() {
		return wrapper.canvasHandle;
	}

	@Override
	public void mark(ProjectContext context, Window window, DoubleVector start, DoubleVector end) {
		if (tool == null)
			return;
		start = Window.toLocal(wrapper.canvasHandle, start);
		end = Window.toLocal(wrapper.canvasHandle, end);
		tool.mark(context, window, start, end);
	}

	@Override
	public void cursorMoved(ProjectContext context, DoubleVector vector) {
		vector = Window.toLocal(wrapper.canvasHandle,vector);
		mouseX.set(vector.x);
		mouseY.set(vector.y);
	}

	@Override
	public Optional<Integer> previousFrame(int frame) {
		if (wrapper.specificLayer == null) return Optional.empty();
		if (wrapper.specificLayer.positionFramesLength() == 1) return Optional.empty();
		int p = GroupLayerWrapper.findPosition(wrapper.specificLayer, frame).at - 1;
		if (p == 0) p = wrapper.specificLayer.positionFramesLength() - 1;
		return Optional.of(p);
	}
}
