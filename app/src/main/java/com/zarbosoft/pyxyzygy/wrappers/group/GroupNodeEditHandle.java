package com.zarbosoft.pyxyzygy.wrappers.group;

import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.pyxyzygy.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.ProjectContext;
import com.zarbosoft.pyxyzygy.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.Wrapper;
import com.zarbosoft.pyxyzygy.config.GroupNodeConfig;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;

import java.util.ArrayList;
import java.util.List;

import static com.zarbosoft.pyxyzygy.widgets.HelperJFX.pad;
import static com.zarbosoft.pyxyzygy.Main.nodeFormFields;

public class GroupNodeEditHandle extends Wrapper.EditControlsHandle {
	private Tab groupTab;
	private Tab toolTab;
	List<Runnable> cleanup = new ArrayList<>();

	Group overlay;

	ToolBar toolBar = new ToolBar();
	private GroupNodeWrapper groupNodeWrapper;

	public GroupNodeEditHandle(final GroupNodeWrapper groupNodeWrapper, ProjectContext context, TabPane tabPane) {
		// Canvas overlay
		overlay = new Group();
		groupNodeWrapper.canvasHandle.canvasOuter.getChildren().add(overlay);

		// Group tab
		groupTab = new Tab(
				"Group",
				pad(new WidgetFormBuilder().apply(b -> cleanup.add(nodeFormFields(context, b, groupNodeWrapper.node))).build())
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
					return groupNodeWrapper.config.tool.get() == value;
				}, groupNodeWrapper.config.tool));
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
				groupNodeWrapper.config.tool.set(value);
			}
		}
		toolBar.getItems().addAll(
				new ToolToggle("cursor-move.png", "Move", GroupNodeConfig.Tool.MOVE),
				new ToolToggle("stamper.png", "Stamp", GroupNodeConfig.Tool.STAMP)
		);

		groupNodeWrapper.config.tool.addListener(new ChangeListener<GroupNodeConfig.Tool>() {
			{
				changed(null, null, groupNodeWrapper.config.tool.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends GroupNodeConfig.Tool> observable,
					GroupNodeConfig.Tool oldValue,
					GroupNodeConfig.Tool newValue
			) {
				if (groupNodeWrapper.tool != null) {
					toolTab.setContent(null);
					groupNodeWrapper.tool.remove(context);
				}
				switch (newValue) {
					case MOVE:
						groupNodeWrapper.tool = new ToolMove(groupNodeWrapper);
						break;
					case STAMP:
						groupNodeWrapper.tool = new ToolStamp(groupNodeWrapper, context, GroupNodeEditHandle.this);
						break;
					default:
						throw new Assertion();
				}
				Node tabContents = groupNodeWrapper.tool.getProperties();
				if (tabContents != null)
					toolTab.setContent(pad(tabContents));
			}
		});

		tabPane.getTabs().addAll(groupTab, toolTab);
		cleanup.add(() -> {
			tabPane.getTabs().removeAll(groupTab, toolTab);
		});
		this.groupNodeWrapper = groupNodeWrapper;
	}

	@Override
	public Node getProperties() {
		return toolBar;
	}

	@Override
	public void remove(ProjectContext context) {
		if (groupNodeWrapper.canvasHandle != null)
			groupNodeWrapper.canvasHandle.canvasOuter.getChildren().remove(overlay);
		if (groupNodeWrapper.tool != null) {
			groupNodeWrapper.tool.remove(context);
			groupNodeWrapper.tool = null;
		}
		cleanup.forEach(c -> c.run());
	}
}
