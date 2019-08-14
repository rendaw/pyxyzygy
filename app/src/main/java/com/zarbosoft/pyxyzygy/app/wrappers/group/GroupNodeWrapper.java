package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.zarbosoft.automodel.lib.ProjectObject;
import com.zarbosoft.javafxbinders.VariableBinder;
import com.zarbosoft.pyxyzygy.app.CanvasHandle;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.EditHandle;
import com.zarbosoft.pyxyzygy.app.Misc;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.Wrapper;
import com.zarbosoft.pyxyzygy.app.config.GroupNodeConfig;
import com.zarbosoft.pyxyzygy.app.config.NodeConfig;
import com.zarbosoft.pyxyzygy.core.model.latest.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupChild;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupLayer;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupPositionFrame;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupTimeFrame;
import com.zarbosoft.pyxyzygy.core.model.latest.ProjectLayer;
import com.zarbosoft.rendaw.common.Common;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import java.util.stream.Collectors;

public class GroupNodeWrapper extends Wrapper {
  private final Wrapper parent;
  public final GroupLayer node;
  final GroupNodeConfig config;
  final ObservableList<GroupChildWrapper> children = FXCollections.observableArrayList();
  private final Runnable childrenListenCleanup;

  public final VariableBinder<GroupChild> specificChild = new VariableBinder<>(null);
  public GroupNodeCanvasHandle canvasHandle;

  public GroupNodeWrapper(
    Context context, Wrapper parent, int parentIndex, GroupLayer node) {
    this.parentIndex = parentIndex;
    this.parent = parent;
    this.node = node;
    config = initConfig(context, node.id());

    tree.set(new TreeItem<>(this));

    childrenListenCleanup =
        node.mirrorChildren(
            children,
            child -> {
              return (GroupChildWrapper) Window.createNode(context, this, -1, child);
            },
            child -> {
              child.remove(context);
            },
            at -> {
              for (int i = at; i < children.size(); ++i) children.get(i).setParentIndex(i);
            });
    Misc.mirror(
        children,
        tree.get().getChildren(),
        child -> {
          child.tree.addListener(
              (observable, oldValue, newValue) -> {
                tree.get().getChildren().set(child.parentIndex, newValue);
              });
          return child.tree.get();
        },
        c -> {}, Common.noopConsumer
    );
  }

  protected GroupNodeConfig initConfig(Context context, long id) {
    return (GroupNodeConfig)
        context.config.nodes.computeIfAbsent(id, id1 -> new GroupNodeConfig(context));
  }

  @Override
  public Wrapper getParent() {
    return parent;
  }

  @Override
  public ProjectObject getValue() {
    return node;
  }

  @Override
  public NodeConfig getConfig() {
    return config;
  }

  @Override
  public CanvasHandle buildCanvas(Context context, Window window, CanvasHandle parent) {
    if (canvasHandle == null) canvasHandle = new GroupNodeCanvasHandle(context, window, this);
    return canvasHandle;
  }

  @Override
  public EditHandle buildEditControls(Context context, Window window) {
    return new GroupNodeEditHandle(context, window, this);
  }

  public void cloneSet(Context context, GroupLayer clone) {
    clone.initialNameSet(context.model, context.namer.uniqueName1(node.name()));
    clone.initialOffsetSet(context.model, node.offset());
    clone.initialChildrenAdd(
        context.model,
        node.children().stream()
            .map(
                child -> {
                  GroupChild newLayer = GroupChild.create(context.model);
                  newLayer.initialOpacitySet(context.model, child.opacity());
                  newLayer.initialEnabledSet(context.model, true);
                  newLayer.initialInnerSet(context.model, child.inner());
                  newLayer.initialPositionFramesAdd(
                      context.model,
                      child.positionFrames().stream()
                          .map(
                              frame -> {
                                GroupPositionFrame newFrame = GroupPositionFrame.create(context.model);
                                newFrame.initialLengthSet(context.model, frame.length());
                                newFrame.initialOffsetSet(context.model, frame.offset());
                                return newFrame;
                              })
                          .collect(Collectors.toList()));
                  newLayer.initialTimeFramesAdd(
                      context.model,
                      child.timeFrames().stream()
                          .map(
                              frame -> {
                                GroupTimeFrame newFrame = GroupTimeFrame.create(context.model);
                                newFrame.initialLengthSet(context.model, frame.length());
                                newFrame.initialInnerOffsetSet(context.model, frame.innerOffset());
                                newFrame.initialInnerLoopSet(context.model, 0);
                                return newFrame;
                              })
                          .collect(Collectors.toList()));
                  return newLayer;
                })
            .collect(Collectors.toList()));
  }

  @Override
  public ProjectLayer separateClone(Context context) {
    GroupLayer clone = GroupLayer.create(context.model);
    cloneSet(context, clone);
    return clone;
  }

  @Override
  public void deleteChild(Context context, ChangeStepBuilder change, int index) {
    change.groupLayer(node).childrenRemove(index, 1);
  }

  @Override
  public TakesChildren takesChildren() {
    return TakesChildren.ANY;
  }

  @Override
  public void remove(Context context) {
    childrenListenCleanup.run();
  }

  public void setSpecificChild(GroupChild child) {
    this.specificChild.set(child);
  }
}
