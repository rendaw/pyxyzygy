package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.config.GroupNodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

import static com.zarbosoft.pyxyzygy.app.Misc.nodeFormFields;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.pad;

public class GroupNodeEditHandle extends EditHandle {
	private Tab groupTab;
	public Tab toolTab;
	protected List<Runnable> cleanup = new ArrayList<>();
	Tool tool = null;

	Group overlay;

	ToolBar toolBar = new ToolBar();
	public GroupNodeWrapper wrapper;
	public final SimpleDoubleProperty mouseX = new SimpleDoubleProperty(0);
	public final SimpleDoubleProperty mouseY = new SimpleDoubleProperty(0);

	public GroupNodeEditHandle(
			ProjectContext context, Window window,final GroupNodeWrapper wrapper
	) {
		this.wrapper = wrapper;

		// Canvas overlay
		overlay = new Group();
		wrapper.canvasHandle.overlay.getChildren().add(overlay);

		VBox controls = new VBox();

		// Group tab
		groupTab = buildNodeTab(context);

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
					tool.remove(context, window);
				}
				switch (newValue) {
					case MOVE:
						tool = createToolMove(wrapper);
						break;
					case STAMP:
						tool = new ToolStamp(wrapper, context, GroupNodeEditHandle.this);
						break;
					default:
						throw new Assertion();
				}
			}
		});

		window.layerTabContent.set(this,controls);
		cleanup.add(() -> {
			window.layerTabContent.clear(this);
		});
	}

	protected ToolMove createToolMove(GroupNodeWrapper wrapper) {
		return new ToolMove(wrapper);
	}

	public Tab buildNodeTab(ProjectContext context) {
		return new Tab(
				"Layer",
				pad(new WidgetFormBuilder().apply(b -> cleanup.add(nodeFormFields(context, b, wrapper))).build())
		);
	}

	@Override
	public Node getProperties() {
		return toolBar;
	}

	@Override
	public void remove(ProjectContext context, Window window) {
		if (tool != null) {
			tool.remove(context, window);
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
		vector = Window.toLocal(wrapper.canvasHandle, vector);
		mouseX.set(vector.x);
		mouseY.set(vector.y);
	}
}
