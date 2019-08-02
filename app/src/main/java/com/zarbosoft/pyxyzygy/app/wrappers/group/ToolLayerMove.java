package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.zarbosoft.automodel.lib.History;
import com.zarbosoft.javafxbinders.BinderRoot;
import com.zarbosoft.javafxbinders.CustomBinding;
import com.zarbosoft.javafxbinders.DoubleHalfBinder;
import com.zarbosoft.javafxbinders.SelectionModelBinder;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.Tool;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.parts.editor.Origin;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.app.widgets.binders.ScalarHalfBinder;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupChild;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupPositionFrame;
import com.zarbosoft.pyxyzygy.seed.Vector;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.util.Callback;

import static com.zarbosoft.javafxbinders.Helper.unopt;
import static com.zarbosoft.pyxyzygy.app.Global.NO_INNER;
import static com.zarbosoft.pyxyzygy.app.Global.localization;
import static com.zarbosoft.pyxyzygy.app.Misc.noopConsumer;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.centerCursor;

public class ToolLayerMove extends Tool {
  private final GroupNodeEditHandle editHandle;
  private final BinderRoot offsetRoot;
  private final Runnable mirrorRoot;
  private final BinderRoot selectionRoot;
  private BinderRoot originCleanup;
  protected DoubleVector markStart;
  private Vector markStartOffset;
  private GroupNodeWrapper wrapper;
  private GroupPositionFrame pos;
  private final ListView<GroupChild> layerList;
  private final Origin origin;

  public ToolLayerMove(Window window, GroupNodeWrapper wrapper, GroupNodeEditHandle editHandle) {
    this.wrapper = wrapper;
    this.editHandle = editHandle;
    origin = new Origin(window.editor, 10);
    editHandle.getCanvas().overlay.getChildren().add(origin);
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
    mirrorRoot = wrapper.node.mirrorChildren(children, c -> c, noopConsumer(), noopConsumer());
    layerList.setItems(children);

    selectionRoot =
        CustomBinding.bindBidirectional(
            wrapper.specificChild, new SelectionModelBinder<>(layerList.getSelectionModel()));
    offsetRoot =
        new DoubleHalfBinder<>(wrapper.specificChild, wrapper.canvasHandle.time.asObject())
            .addListener(
                (select, time) -> {
                  if (originCleanup != null) {
                    originCleanup.destroy();
                    originCleanup = null;
                  }
                  origin.setVisible(false);
                  if (time == NO_INNER) return;
                  if (select == null) return;
                  pos =
                      GroupChildWrapper.positionFrameFinder.findFrame(
                              select, wrapper.canvasHandle.time.get())
                          .frame;
                  if (pos == null) return;
                  originCleanup =
                      CustomBinding.bind(
                          origin.offset, new ScalarHalfBinder<Vector>(pos, "offset"));
                  origin.setVisible(true);
                });
    if (!layerList.getItems().isEmpty() && layerList.getSelectionModel().getSelectedItem() == null)
      layerList.getSelectionModel().select(0);
    editHandle.toolPropReplacer.set(
        this,
        localization.getString("move.child"),
        new WidgetFormBuilder()
            .span(
                () -> {
                  return layerList;
                })
            .build());
  }

  @Override
  public void markStart(
      Context context, Window window, DoubleVector start, DoubleVector globalStart) {
    GroupChild specificLayer = unopt(wrapper.specificChild.get());
    if (specificLayer == null) return;
    pos =
        GroupChildWrapper.positionFrameFinder.findFrame(
                specificLayer, wrapper.canvasHandle.time.get())
            .frame;
    if (pos == null) return;
    this.markStart = globalStart;
    this.markStartOffset = pos.offset();
  }

  @Override
  public void mark(
      Context context,
      Window window,
      DoubleVector start,
      DoubleVector end,
      DoubleVector globalStart,
      DoubleVector globalEnd) {
    if (pos == null) return;
    context.change(
        new History.Tuple(wrapper, "move-layer"),
        c ->
            c.groupPositionFrame(pos)
                .offsetSet(globalEnd.minus(markStart).plus(markStartOffset).toInt()));
  }

  @Override
  public void remove(Context context, Window window) {
    window.editorCursor.clear(this);
    if (editHandle.getCanvas() != null) {
      editHandle.getCanvas().overlay.getChildren().remove(origin);
    }
    editHandle.toolPropReplacer.clear(this);
    offsetRoot.destroy();
    mirrorRoot.run();
    selectionRoot.destroy();
    origin.remove();
    if (originCleanup != null) {
      originCleanup.destroy();
      originCleanup = null;
    }
  }

  @Override
  public void cursorMoved(Context context, Window window, DoubleVector position) {}
}
