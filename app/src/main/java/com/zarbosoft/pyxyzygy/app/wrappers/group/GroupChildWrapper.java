package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.zarbosoft.automodel.lib.Listener;
import com.zarbosoft.automodel.lib.ProjectObject;
import com.zarbosoft.pyxyzygy.app.CanvasHandle;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.EditHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.Wrapper;
import com.zarbosoft.pyxyzygy.app.config.NodeConfig;
import com.zarbosoft.pyxyzygy.app.wrappers.FrameFinder;
import com.zarbosoft.pyxyzygy.core.model.latest.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupChild;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupPositionFrame;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupTimeFrame;
import com.zarbosoft.pyxyzygy.core.model.latest.ProjectLayer;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.beans.property.SimpleObjectProperty;

import static com.zarbosoft.pyxyzygy.app.Global.NO_INNER;
import static com.zarbosoft.pyxyzygy.app.Global.NO_LOOP;

public class GroupChildWrapper extends Wrapper {
  private final Wrapper parent;
  public final GroupChild node;
  public final SimpleObjectProperty<Wrapper> child = new SimpleObjectProperty<>();
  private final Listener.ScalarSet<GroupChild, ProjectLayer> innerSetListener;
  public static final FrameFinder<GroupChild, GroupPositionFrame> positionFrameFinder =
      new FrameFinder<GroupChild, GroupPositionFrame>() {
        @Override
        public GroupPositionFrame frameGet(GroupChild node, int i) {
          return node.positionFramesGet(i);
        }

        @Override
        public int frameCount(GroupChild node) {
          return node.positionFramesLength();
        }

        @Override
        public int frameLength(GroupPositionFrame frame) {
          return frame.length();
        }

        @Override
        public int prelength(GroupChild node) {
          return node.positionPrelength();
        }
      };
  public static final FrameFinder<GroupChild, GroupTimeFrame> timeFrameFinder =
      new FrameFinder<GroupChild, GroupTimeFrame>() {
        @Override
        public int prelength(GroupChild node) {
          return node.timePrelength();
        }

        @Override
        public GroupTimeFrame frameGet(GroupChild node, int i) {
          return node.timeFramesGet(i);
        }

        @Override
        public int frameCount(GroupChild node) {
          return node.timeFramesLength();
        }

        @Override
        public int frameLength(GroupTimeFrame frame) {
          return frame.length();
        }
      };
  protected GroupChildCanvasHandle canvasHandle;

  public GroupChildWrapper(Context context, Wrapper parent, int parentIndex, GroupChild node) {
    this.parent = parent;
    this.parentIndex = parentIndex;
    this.node = node;

    innerSetListener =
        node.addInnerSetListeners(
            (target, value) -> {
              if (child.get() != null) {
                tree.unbind();
                child.get().remove(context);
                child.set(null);
              }
              if (value != null) {
                child.set(Window.createNode(context, GroupChildWrapper.this, parentIndex, value));
                tree.bind(child.get().tree);
              }
            });
  }

  public static int toInnerTime(GroupChild node, int time) {
    if (time == NO_INNER) return NO_INNER;
    FrameFinder.Result<GroupTimeFrame> result = timeFrameFinder.findFrame(node, time);
    int offset = (time - result.at);
    if (result.frame == null) {
      return offset;
    } else {
      if (result.frame.innerLoop() != NO_LOOP) offset = offset % result.frame.innerLoop();
      if (result.frame.innerOffset() == NO_INNER) return NO_INNER;
      return result.frame.innerOffset() + offset;
    }
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
    throw new Assertion();
  }

  @Override
  public CanvasHandle buildCanvas(Context context, Window window, CanvasHandle parent) {
    if (canvasHandle == null) canvasHandle = new GroupChildCanvasHandle(context, window, this);
    canvasHandle.setParent(parent);
    return canvasHandle;
  }

  @Override
  public ProjectLayer separateClone(Context context) {
    throw new Assertion();
  }

  @Override
  public void deleteChild(Context context, ChangeStepBuilder change, int index) {
    parent.deleteChild(context, change, parentIndex);
  }

  @Override
  public void setParentIndex(int index) {
    this.parentIndex = index;
    if (child.get() == null) return;
    child.get().setParentIndex(index);
  }

  @Override
  public TakesChildren takesChildren() {
    throw new Assertion();
  }

  @Override
  public void remove(Context context) {
    node.removeInnerSetListeners(innerSetListener);
  }

  @Override
  public EditHandle buildEditControls(Context context, Window window) {
    return null;
  }
}
