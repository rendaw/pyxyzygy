package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.zarbosoft.pyxyzygy.app.CustomBinding;
import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.Tool;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.parts.editor.Origin;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.core.model.v0.GroupPositionFrame;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.util.Callback;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;
import static com.zarbosoft.pyxyzygy.app.Misc.unopt;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.centerCursor;

public class ToolLayerMove extends Tool {
	private final GroupNodeEditHandle editHandle;
	private Runnable originCleanup;
	protected DoubleVector markStart;
	private Vector markStartOffset;
	private GroupNodeWrapper wrapper;
	private GroupPositionFrame pos;
	private final ListView<GroupChildWrapper> layerList;
	private final Origin origin;

	public ToolLayerMove(Window window, GroupNodeWrapper wrapper, GroupNodeEditHandle editHandle) {
		this.wrapper = wrapper;
		origin = new Origin(window, window.editor, 10);
		this.editHandle = editHandle;
		window.editorCursor.set(this, centerCursor("cursor-move32.png"));
		layerList = new ListView<>();
		layerList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		layerList.setCellFactory(new Callback<ListView<GroupChildWrapper>, ListCell<GroupChildWrapper>>() {
			@Override
			public ListCell<GroupChildWrapper> call(ListView<GroupChildWrapper> param) {
				return new ListCell<>() {
					@Override
					protected void updateItem(GroupChildWrapper item, boolean empty) {
						if (item != null && !empty) {
							setText(item.node.inner().name());
						} else {
							setText("");
						}
						super.updateItem(item, empty);
					}
				};
			}
		});
		layerList.setItems(wrapper.children);

		CustomBinding.PropertyHalfBinder<GroupChildWrapper> selectBinder =
				new CustomBinding.PropertyHalfBinder<>(layerList.getSelectionModel().selectedItemProperty());
		CustomBinding.bind(origin.visibleProperty(), selectBinder.map(s -> opt(s != null)));
		new CustomBinding.DoubleHalfBinder<>(selectBinder,wrapper.getConfig().frame).addListener((select, frame) -> {
			if (originCleanup != null) {
				originCleanup.run();
				originCleanup = null;
			}
			if (select == null) return;
			pos = GroupChildWrapper.positionFrameFinder.findFrame(select.node,
					wrapper.canvasHandle.frameNumber.get()
			).frame;
			originCleanup =
					CustomBinding.bind(origin.offset, new CustomBinding.ScalarHalfBinder<Vector>(pos, "offset"));
		});
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
		GroupChildWrapper specificLayer = layerList.getSelectionModel().getSelectedItem();
		if (specificLayer == null)
			return;
		pos = GroupChildWrapper.positionFrameFinder.findFrame(specificLayer.node,
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
		origin.remove();
		if (originCleanup != null) {
			originCleanup.run();
			originCleanup = null;
		}
	}

	@Override
	public void cursorMoved(ProjectContext context, Window window, DoubleVector position) {

	}
}
