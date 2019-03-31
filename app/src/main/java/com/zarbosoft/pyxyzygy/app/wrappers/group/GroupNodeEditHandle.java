package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.config.GroupNodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.ContentReplacer;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.app.widgets.TitledPane;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

import static com.zarbosoft.pyxyzygy.app.Misc.nodeFormFields;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.pad;

public class GroupNodeEditHandle extends EditHandle {
	public final ContentReplacer<Node> toolPropReplacer;
	protected List<Runnable> cleanup = new ArrayList<>();
	Tool tool = null;

	Group overlay;

	public GroupNodeWrapper wrapper;

	protected class ToolToggle extends HelperJFX.IconToggleButton {
		private final String value;

		public ToolToggle(String icon, String hint, String value) {
			super(icon, hint);
			this.value = value;
			selectedProperty().bind(Bindings.createBooleanBinding(() -> {
				return value.equals(wrapper.config.tool.get());
			}, wrapper.config.tool));
		}

		@Override
		public void fire() {
			wrapper.config.tool.set(value);
		}
	}

	public GroupNodeEditHandle(
			ProjectContext context, Window window, final GroupNodeWrapper wrapper
	) {
		this.wrapper = wrapper;

		// Canvas overlay
		overlay = new Group();
		wrapper.canvasHandle.overlay.getChildren().add(overlay);

		VBox controls = new VBox();

		VBox toolProps = new VBox();
		this.toolPropReplacer = new ContentReplacer<Node>() {
			@Override
			protected void innerSet(Node content) {
				toolProps.getChildren().addAll(content);
			}

			@Override
			protected void innerClear() {
				toolProps.getChildren().clear();
			}
		};

		VBox tabBox = new VBox();
		tabBox
				.getChildren()
				.addAll(new TitledPane(
						"Layer",
						new WidgetFormBuilder().apply(b -> cleanup.add(nodeFormFields(context, b, wrapper))).build()
				), toolProps);
		window.layerTabContent.set(this, pad(tabBox));

		// Toolbar
		window.toolBarChildren.set(this,createToolButtons());

		wrapper.config.tool.addListener(new ChangeListener<String>() {
			{
				changed(null, null, wrapper.config.tool.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends String> observable,
					String oldValue,
					String newValue
			) {
				if (tool != null) {
					tool.remove(context, window);
					tool = null;
				}
				if (newValue == null) return;
				tool = createTool(context,window,newValue);
			}
		});

		window.layerTabContent.set(this, controls);
		cleanup.add(() -> {
			window.layerTabContent.clear(this);
		});
	}

	protected Tool createTool(ProjectContext context, Window window ,String newValue) {
		if (GroupNodeConfig.toolMove.equals(newValue)) {
			return new ToolMove(window, wrapper);
		} else if (GroupNodeConfig.toolStamp.equals(newValue)) {
			return new ToolStamp(context, window, wrapper, GroupNodeEditHandle.this);
		} else throw new Assertion();
	}

	protected List<Node> createToolButtons() {
		return ImmutableList.of(
				new ToolToggle("cursor-move16.png", "Move", GroupNodeConfig.toolMove),
				new ToolToggle("stamper16.png", "Stamp", GroupNodeConfig.toolStamp)
		);
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
		window.layerTabContent.clear(this);
		window.toolBarChildren.clear(this);
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
	public void cursorMoved(ProjectContext context, Window window, DoubleVector vector) {
		vector = Window.toLocal(wrapper.canvasHandle, vector);
		tool.cursorMoved(context, window, vector);
	}
}
