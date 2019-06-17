package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.Tool;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.parts.editor.Origin;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.*;
import com.zarbosoft.pyxyzygy.core.model.v0.GroupChild;
import com.zarbosoft.pyxyzygy.core.model.v0.GroupPositionFrame;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.util.Callback;

import static com.zarbosoft.pyxyzygy.app.Global.localization;
import static com.zarbosoft.pyxyzygy.app.Misc.*;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.centerCursor;

public class ToolLayerMove extends Tool {
  private final GroupNodeEditHandle editHandle;
  private BinderRoot originCleanup;
  protected DoubleVector markStart;
  private Vector markStartOffset;
  private GroupNodeWrapper wrapper;
  private GroupPositionFrame pos;
  private final ListView<GroupChild> layerList;
  private final Origin origin;

  public ToolLayerMove(Window window, GroupNodeWrapper wrapper, GroupNodeEditHandle editHandle) {
    this.wrapper = wrapper;
    origin = new Origin(window, window.editor, 10);
    this.editHandle = editHandle;
    window.editorCursor.set(this, centerCursor("cursor-move32.png"));
    layerList = new ListView<>();
    layerList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    layerList.setCellFactory(
        new Callback<ListView<GroupChild>, ListCell<GroupChild>>() {
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
    ObservableList<GroupChild> children = FXCollections.observableArrayList();
    wrapper.node.mirrorChildren(children, c -> c, noopConsumer(), noopConsumer());
    layerList.setItems(children);

    CustomBinding.bindBidirectional(
        wrapper.specificChild, new SelectionModelBinder<>(layerList.getSelectionModel()));
    CustomBinding.bind(origin.visibleProperty(), wrapper.specificChild.map(s -> opt(s != null)));
    new DoubleHalfBinder<>(wrapper.specificChild, wrapper.getConfig().frame)
        .addListener(
            (select, frame) -> {
              if (originCleanup != null) {
                originCleanup.destroy();
                originCleanup = null;
              }
              if (select == null) return;
              pos =
                  GroupChildWrapper.positionFrameFinder.findFrame(
                          select, wrapper.canvasHandle.frameNumber.get())
                      .frame;
              originCleanup =
                  CustomBinding.bind(origin.offset, new ScalarHalfBinder<Vector>(pos, "offset"));
            });
    if (!layerList.getItems().isEmpty() && layerList.getSelectionModel().getSelectedItem() == null)
      layerList.getSelectionModel().select(0);
    editHandle.toolPropReplacer.set(
        this,
        localization.getString("move.layer"),
        new WidgetFormBuilder()
            .span(
                () -> {
                  return layerList;
                })
            .build());
    window.showLayerTab();
  }

  @Override
  public void markStart(
      ProjectContext context, Window window, DoubleVector start, DoubleVector globalStart) {
    GroupChild specificLayer = unopt(wrapper.specificChild.get());
    if (specificLayer == null) return;
    pos =
        GroupChildWrapper.positionFrameFinder.findFrame(
                specificLayer, wrapper.canvasHandle.frameNumber.get())
            .frame;
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
      DoubleVector globalEnd) {
    context.change(
        new ProjectContext.Tuple(wrapper, "move-layer"),
        c ->
            c.groupPositionFrame(pos)
                .offsetSet(globalEnd.minus(markStart).plus(markStartOffset).toInt()));
  }

  @Override
  public void remove(ProjectContext context, Window window) {
    window.editorCursor.clear(this);
    editHandle.toolPropReplacer.clear(this);
    origin.remove();
    if (originCleanup != null) {
      originCleanup.destroy();
      originCleanup = null;
    }
  }

  @Override
  public void cursorMoved(ProjectContext context, Window window, DoubleVector position) {}
}
