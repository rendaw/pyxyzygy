package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.Tool;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.core.model.v0.GroupChild;
import com.zarbosoft.pyxyzygy.core.model.v0.GroupPositionFrame;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.util.Callback;

import static com.zarbosoft.pyxyzygy.app.Misc.noopConsumer;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.centerCursor;

public class ToolLayerMove extends Tool {
	private final GroupNodeEditHandle editHandle;
	protected DoubleVector markStart;
	private Vector markStartOffset;
	private GroupNodeWrapper wrapper;
	private GroupPositionFrame pos;
	private final ListView<GroupChild> layerList;

	public ToolLayerMove(Window window, GroupNodeWrapper wrapper, GroupNodeEditHandle editHandle) {
		this.wrapper = wrapper;
		this.editHandle = editHandle;
		window.editorCursor.set(this, centerCursor("cursor-move32.png"));
		layerList = new ListView<>();
		layerList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		layerList.setCellFactory(new Callback<ListView<GroupChild>, ListCell<GroupChild>>() {
			@Override
			public ListCell<GroupChild> call(ListView<GroupChild> param) {
				return new ListCell<>() {
					@Override
					protected void updateItem(GroupChild item, boolean empty) {
						if (item != null && !empty) {
							setText(item.inner().name());
						} else {
							setText("");
						}
						super.updateItem(item, empty);
					}
				};
			}
		});
		wrapper.node.mirrorChildren(layerList.getItems(), l -> l, noopConsumer, noopConsumer);
		if (!layerList.getItems().isEmpty())
			layerList.getSelectionModel().select(0);
		editHandle.toolPropReplacer.set(this, "Move layer", new WidgetFormBuilder().span(() -> {
			return layerList;
		}).build());
	}

	@Override
	public void markStart(
			ProjectContext context, Window window, DoubleVector start, DoubleVector globalStart
	) {
		GroupChild specificLayer = layerList.getSelectionModel().getSelectedItem();
		if (specificLayer == null)
			return;
		pos = GroupChildWrapper.positionFrameFinder.findFrame(specificLayer,
				wrapper.canvasHandle.frameNumber.get()
		).frame;
		this.markStart = globalStart;
		this.markStartOffset = pos.offset();
	}

	@Override
	public void mark(
			ProjectContext context,
			Window window,
			DoubleVector start,
			DoubleVector end,
			DoubleVector globalStart,
			DoubleVector globalEnd
	) {
		context.change(new ProjectContext.Tuple(wrapper, "move-layer"),
				c -> c.groupPositionFrame(pos).offsetSet(globalEnd.minus(markStart).plus(markStartOffset).toInt())
		);
	}

	@Override
	public void remove(ProjectContext context, Window window) {
		window.editorCursor.clear(this);
		editHandle.toolPropReplacer.clear(this);
	}

	@Override
	public void cursorMoved(ProjectContext context, Window window, DoubleVector position) {

	}
}
