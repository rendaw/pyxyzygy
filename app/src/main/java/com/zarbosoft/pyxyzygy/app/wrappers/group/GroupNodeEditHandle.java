package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.config.GroupNodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.ContentReplacer;
import com.zarbosoft.pyxyzygy.app.widgets.TitledPane;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.BinderRoot;
import com.zarbosoft.pyxyzygy.app.wrappers.ToolMove;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

import static com.zarbosoft.pyxyzygy.app.Misc.nodeFormFields;
import static com.zarbosoft.pyxyzygy.app.Misc.separateFormField;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.pad;

public class GroupNodeEditHandle extends EditHandle {
  public final ContentReplacer<Node> toolPropReplacer;
  protected List<Runnable> cleanup = new ArrayList<>();
  protected List<BinderRoot> cleanup2 = new ArrayList<>();
  Tool tool = null;

  Group overlay;

  public GroupNodeWrapper wrapper;

  public GroupNodeEditHandle(
      ProjectContext context, Window window, final GroupNodeWrapper wrapper) {
    this.wrapper = wrapper;

    // Canvas overlay
    overlay = new Group();
    wrapper.canvasHandle.overlay.getChildren().add(overlay);

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
    window.toolBarChildren.set(this, createToolButtons());

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

  public VBox buildTab(ProjectContext context, Window window, TitledPane toolProps) {
    VBox tabBox = new VBox();
    tabBox
        .getChildren()
        .addAll(
            new TitledPane(
                "Layer",
                new WidgetFormBuilder()
                    .apply(b -> cleanup.add(nodeFormFields(context, b, wrapper)))
                    .apply(b -> separateFormField(context, b, wrapper))
                    .build()),
            toolProps);
    return tabBox;
  }

  protected Tool createTool(ProjectContext context, Window window, String newValue) {
    if (GroupNodeConfig.TOOL_MOVE.equals(newValue)) {
      return new ToolMove(window, wrapper);
    } else if (GroupNodeConfig.TOOL_LAYER_MOVE.equals(newValue)) {
      return new ToolLayerMove(window, wrapper, this);
    } else if (GroupNodeConfig.TOOL_STAMP.equals(newValue)) {
      return new ToolStamp(context, window, wrapper, GroupNodeEditHandle.this);
    } else throw new Assertion();
  }

  protected List<Node> createToolButtons() {
    return ImmutableList.of(
        new Wrapper.ToolToggle(wrapper, "cursor-move16.png", "Move", GroupNodeConfig.TOOL_MOVE),
        new Wrapper.ToolToggle(
            wrapper, "cursor-layer-move.png", "Move layer", GroupNodeConfig.TOOL_LAYER_MOVE),
        new Wrapper.ToolToggle(wrapper, "stamper16.png", "Stamp", GroupNodeConfig.TOOL_STAMP));
  }

  @Override
  public void remove(ProjectContext context, Window window) {
    if (tool != null) {
      tool.remove(context, window);
      tool = null;
    }
    if (wrapper.canvasHandle != null) wrapper.canvasHandle.overlay.getChildren().remove(overlay);
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
  public void markStart(ProjectContext context, Window window, DoubleVector start) {
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
  public void mark(ProjectContext context, Window window, DoubleVector start, DoubleVector end) {
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
  public void cursorMoved(ProjectContext context, Window window, DoubleVector vector) {
    vector = Window.toLocal(window.getSelectedForView(), wrapper.canvasHandle, vector);
    tool.cursorMoved(context, window, vector);
  }
}
