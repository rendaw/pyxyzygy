package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.modelmirror.*;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.PropertyHalfBinder;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.v0.*;
import com.zarbosoft.pyxyzygy.nearestneighborimageview.NearestNeighborImageView;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import com.zarbosoft.pyxyzygy.seed.model.v0.Rectangle;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Scale;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.centerCursor;

public class ToolStamp extends Tool {
  private final MirrorProject mirror;
  private final GroupNodeEditHandle editHandle;
  private ProjectLayer stampSource;
  private final SimpleObjectProperty<Rectangle> stampOverlayBounds =
      new SimpleObjectProperty<>(new Rectangle(0, 0, 0, 0));
  private final GroupNodeWrapper wrapper;
  private final Group overlayGroup;

  ToolStamp(
      ProjectContext context,
      Window window,
      GroupNodeWrapper wrapper,
      GroupNodeEditHandle editHandle) {
    this.editHandle = editHandle;
    List<ProjectObject> parents = new ArrayList<>();
    {
      Wrapper at = wrapper;
      while (at != null) {
        parents.add(at.getValue());
        at = at.getParent();
      }
    }
    final TreeView<ObjectMirror> tree = new TreeView<>();
    tree.setCellFactory(
        param ->
            new TreeCell<ObjectMirror>() {
              Runnable cleanup;

              {
                HelperJFX.bindStyle(this, "disable", new PropertyHalfBinder<>(disableProperty()));
              }

              @Override
              protected void updateItem(ObjectMirror item, boolean empty) {
                if (cleanup != null) {
                  cleanup.run();
                }
                if (item == null) {
                  setText("");
                } else {
                  setDisable(parents.contains(item.getValue()));
                  Listener.ScalarSet<ProjectLayer, String> nameListener =
                      (target, value) -> {
                        setText(value);
                      };
                  ((ProjectLayer) item.getValue()).addNameSetListeners(nameListener);
                  cleanup =
                      () -> {
                        ((ProjectLayer) item.getValue()).removeNameSetListeners(nameListener);
                      };
                }
                super.updateItem(item, empty);
              }
            });
    Map<Long, ObjectMirror> lookup = new HashMap<>();
    mirror =
        new MirrorProject(
            context,
            new ObjectMirror.Context() {
              @Override
              public ObjectMirror create(
                  ProjectContext context, ObjectMirror parent, ProjectObject object) {
                ObjectMirror out;
                if (false) {
                  throw new Assertion();
                } else if (object instanceof GroupLayer) {
                  out = new MirrorGroupNode(context, this, parent, (GroupLayer) object);
                } else if (object instanceof GroupChild) {
                  out = new MirrorGroupChild(context, this, parent, (GroupChild) object);
                } else if (object instanceof TrueColorImageLayer) {
                  out = new MirrorTrueColorImageNode(parent, (TrueColorImageLayer) object);
                } else if (object instanceof PaletteImageLayer) {
                  out = new MirrorPaletteImageNode(parent, (PaletteImageLayer) object);
                } else throw new Assertion();
                if (!parents.contains(object)) lookup.put(object.id(), out);
                return out;
              }
            },
            null,
            context.project);
    tree.setRoot(mirror.tree.get());
    tree.setShowRoot(false);
    ImageView stampOverlayImage = NearestNeighborImageView.create();
    stampOverlayImage.setOpacity(0.5);
    overlayGroup = new Group();
    wrapper.canvasHandle.positiveZoom.addListener(
        (observable, oldValue, newValue) ->
            overlayGroup
                .getTransforms()
                .setAll(
                    new Scale(
                        1.0 / wrapper.canvasHandle.positiveZoom.get(),
                        1.0 / wrapper.canvasHandle.positiveZoom.get())));
    overlayGroup.getChildren().addAll(stampOverlayImage);
    editHandle.overlay.getChildren().add(overlayGroup);
    Runnable updateStampImage =
        () -> {
          stampOverlayImage.setImage(null);
          TreeItem<ObjectMirror> item = tree.getSelectionModel().getSelectedItem();
          if (item == null) return;
          if (parents.contains(item.getValue().getValue())) return;
          wrapper.config.stampSource.set(item.getValue().getValue().id());
          stampSource = (ProjectLayer) item.getValue().getValue();
          stampOverlayBounds.set(Render.bounds(context, stampSource, 0));
          if (stampOverlayBounds.get().width == 0 || stampOverlayBounds.get().height == 0) return;
          TrueColorImage gc =
              TrueColorImage.create(
                  stampOverlayBounds.get().width, stampOverlayBounds.get().height);
          Render.render(context, stampSource, gc, 0, stampOverlayBounds.get(), 1);
          stampOverlayImage.setImage(HelperJFX.toImage(gc));
          stampOverlayImage.setLayoutX((double) stampOverlayBounds.get().x);
          stampOverlayImage.setLayoutY((double) stampOverlayBounds.get().y);
        };
    tree.getSelectionModel()
        .selectedItemProperty()
        .addListener((observable, oldValue, newValue) -> updateStampImage.run());
    wrapper.canvasHandle.positiveZoom.addListener(
        (observable, oldValue, newValue) -> updateStampImage.run());
    editHandle.toolPropReplacer.set(
        this,
        "Stamp",
        new WidgetFormBuilder()
            .span(
                () -> {
                  return tree;
                })
            .build());
    window.showLayerTab();
    this.wrapper = wrapper;
    window.editorCursor.set(this, centerCursor("stamper32.png"));

    ObjectMirror found = lookup.get(wrapper.config.stampSource.get());
    if (found != null) tree.getSelectionModel().select(found.tree.get());
    else
      lookup.values().stream()
          .findFirst()
          .ifPresent(o -> tree.getSelectionModel().select(o.tree.get()));
  }

  @Override
  public void markStart(
      ProjectContext context, Window window, DoubleVector start, DoubleVector globalStart) {
    if (stampSource == null) return;
    GroupChild layer = GroupChild.create(context);
    layer.initialInnerSet(context, stampSource);
    layer.initialEnabledSet(context, true);
    layer.initialOpacitySet(context, Global.opacityMax);
    GroupPositionFrame positionFrame = GroupPositionFrame.create(context);
    positionFrame.initialLengthSet(context, -1);
    positionFrame.initialOffsetSet(
        context, new Vector((int) Math.floor(start.x), (int) Math.floor(start.y)));
    layer.initialPositionFramesAdd(context, ImmutableList.of(positionFrame));
    GroupTimeFrame timeFrame = GroupTimeFrame.create(context);
    timeFrame.initialLengthSet(context, -1);
    timeFrame.initialInnerOffsetSet(context, 0);
    timeFrame.initialInnerLoopSet(context, 0);
    layer.initialTimeFramesAdd(context, ImmutableList.of(timeFrame));
    window.structure.suppressSelect = true;
    try {
      context.change(null, c -> c.groupLayer(wrapper.node).childrenAdd(layer));
    } finally {
      window.structure.suppressSelect = false;
    }
    wrapper.specificChild.set(layer);
  }

  @Override
  public void mark(
      ProjectContext context,
      Window window,
      DoubleVector start,
      DoubleVector end,
      DoubleVector globalStart,
      DoubleVector globalEnd) {}

  @Override
  public void remove(ProjectContext context, Window window) {
    editHandle.overlay.getChildren().remove(overlayGroup);
    mirror.remove(context);
    window.editorCursor.clear(this);
    editHandle.toolPropReplacer.clear(this);
  }

  @Override
  public void cursorMoved(ProjectContext context, Window window, DoubleVector position) {
    overlayGroup.setLayoutX(Math.floor(position.x));
    overlayGroup.setLayoutY(Math.floor(position.y));
  }
}
