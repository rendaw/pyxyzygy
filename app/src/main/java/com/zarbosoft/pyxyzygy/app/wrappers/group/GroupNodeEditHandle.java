package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.javafxbinders.BinderRoot;
import com.zarbosoft.pyxyzygy.app.CanvasHandle;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.EditHandle;
import com.zarbosoft.pyxyzygy.app.Tool;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.Wrapper;
import com.zarbosoft.pyxyzygy.app.config.GroupNodeConfig;
import com.zarbosoft.pyxyzygy.app.widgets.ContentReplacer;
import com.zarbosoft.pyxyzygy.app.widgets.TitledPane;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.app.wrappers.ToolMove;
import com.zarbosoft.pyxyzygy.seed.Vector;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

import static com.zarbosoft.pyxyzygy.app.Global.NO_INNER;
import static com.zarbosoft.pyxyzygy.app.Global.localization;
import static com.zarbosoft.pyxyzygy.app.Misc.nodeFormFields;
import static com.zarbosoft.pyxyzygy.app.Misc.separateFormField;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.pad;

public class GroupNodeEditHandle extends EditHandle {
  public final ContentReplacer<Node> toolPropReplacer;
  protected List<Runnable> cleanup = new ArrayList<>();
  protected List<BinderRoot> cleanup2 = new ArrayList<>();
  Tool tool = null;

  public GroupNodeWrapper wrapper;

  public GroupNodeEditHandle(Context context, Window window, final GroupNodeWrapper wrapper) {
    this.wrapper = wrapper;
    TitledPane toolProps = new TitledPane(null, null);
    toolProps.visibleProperty().bind(toolProps.contentProperty().isNotNull());
    this.toolPropReplacer =
        new ContentReplacer<Node>() {
          @Override
          protected void innerSet(String title, Node content) {
            toolProps.setText(title);
            toolProps.setContent(content);
          }

          @Override
          protected void innerClear() {
            toolProps.setContent(null);
          }
        };

    window.layerTabContent.set(this, pad(buildTab(context, window, toolProps)));

    // Toolbar
    window.toolBarChildren.set(this, createToolButtons(window));

    wrapper.config.tool.addListener(
        new ChangeListener<String>() {
          {
            changed(null, null, wrapper.config.tool.get());
          }

          @Override
          public void changed(
              ObservableValue<? extends String> observable, String oldValue, String newValue) {
            if (tool != null) {
              tool.remove(context, window);
              tool = null;
            }
            if (newValue == null) return;
            tool = createTool(context, window, newValue);
          }
        });

    cleanup.add(
        () -> {
          window.layerTabContent.clear(this);
        });
  }

  public VBox buildTab(Context context, Window window, TitledPane toolProps) {
    VBox tabBox = new VBox();
    tabBox
        .getChildren()
        .addAll(
            new TitledPane(
                localization.getString("layer"),
                new WidgetFormBuilder()
                    .apply(b -> cleanup.add(nodeFormFields(context, b, wrapper)))
                    .apply(b -> separateFormField(context, b, wrapper))
                    .build()),
            toolProps);
    return tabBox;
  }

  protected Tool createTool(Context context, Window window, String newValue) {
    if (GroupNodeConfig.TOOL_MOVE.equals(newValue)) {
      return new ToolMove(window, wrapper);
    } else if (GroupNodeConfig.TOOL_LAYER_MOVE.equals(newValue)) {
      return new ToolLayerMove(window, wrapper, this);
    } else if (GroupNodeConfig.TOOL_STAMP.equals(newValue)) {
      return new ToolStamp(context, window, wrapper, GroupNodeEditHandle.this);
    } else throw new Assertion();
  }

  protected List<Node> createToolButtons(Window window) {
    return ImmutableList.of(
        new Wrapper.ToolToggle(
            wrapper,
            "cursor-move16.png",
            localization.getString("move.group"),
            GroupNodeConfig.TOOL_MOVE),
        new Wrapper.ToolToggle(
            wrapper,
            "cursor-layer-move.png",
            localization.getString("move.child"),
            GroupNodeConfig.TOOL_LAYER_MOVE) {
          @Override
          public void fire() {
            super.fire();
            window.showLayerTab();
          }
        },
        new Wrapper.ToolToggle(
            wrapper, "stamper16.png", localization.getString("stamp"), GroupNodeConfig.TOOL_STAMP) {
          @Override
          public void fire() {
            super.fire();
            window.showLayerTab();
          }
        });
  }

  @Override
  public void remove(Context context, Window window) {
    if (tool != null) {
      tool.remove(context, window);
      tool = null;
    }
    cleanup.forEach(c -> c.run());
    cleanup2.forEach(c -> c.destroy());
    window.layerTabContent.clear(this);
    window.toolBarChildren.clear(this);
  }

  @Override
  public Wrapper getWrapper() {
    return wrapper;
  }

  private Vector offset() {
    return wrapper.node.offset();
  }

  @Override
  public void markStart(Context context, Window window, DoubleVector start) {
    if (getCanvas().time.get() == NO_INNER) return;
    if (tool == null) return;
    tool.markStart(
        context,
        window,
        Window.toLocal(window.getSelectedForView(), wrapper.canvasHandle, start).minus(offset()),
        start);
  }

  @Override
  public CanvasHandle getCanvas() {
    return wrapper.canvasHandle;
  }

  @Override
  public void mark(Context context, Window window, DoubleVector start, DoubleVector end) {
    if (getCanvas().time.get() == NO_INNER) return;
    if (tool == null) return;
    Vector offset = offset();
    tool.mark(
        context,
        window,
        Window.toLocal(window.getSelectedForView(), wrapper.canvasHandle, start).minus(offset),
        Window.toLocal(window.getSelectedForView(), wrapper.canvasHandle, end).minus(offset),
        start,
        end);
  }

  @Override
  public void cursorMoved(Context context, Window window, DoubleVector vector) {
    if (getCanvas().time.get() == NO_INNER) return;
    vector = Window.toLocal(window.getSelectedForView(), wrapper.canvasHandle, vector);
    tool.cursorMoved(context, window, vector);
  }
}
