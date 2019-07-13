package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.automodel.lib.Listener;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.WidgetHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.wrappers.FrameFinder;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupChildWrapper;
import com.zarbosoft.pyxyzygy.core.model.latest.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupChild;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupPositionFrame;
import com.zarbosoft.pyxyzygy.seed.Vector;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.zarbosoft.pyxyzygy.app.parts.timeline.Timeline.emptyStateImage;

public class RowAdapterGroupChildPosition
    extends BaseFrameRowAdapter<GroupChild, GroupPositionFrame> {
  private final GroupChild child;
  private final RowAdapterGroupChild childRowAdapter;

  public RowAdapterGroupChildPosition(
      Timeline timeline, GroupChild child, RowAdapterGroupChild childRowAdapter) {
    super(timeline);
    this.child = child;
    this.childRowAdapter = childRowAdapter;
  }

  @Override
  public WidgetHandle createRowWidget(Context context, Window window) {
    return new WidgetHandle() {
      private final Listener.ScalarSet<GroupChild, Integer> prelengthCleanup;
      private VBox layout;
      Runnable framesCleanup;
      private List<Runnable> frameCleanup = new ArrayList<>();

      {
        prelengthCleanup =
            child.addPositionPrelengthSetListeners(
                ((target, value) -> {
                  updateFrames(context, window);
                }));
        framesCleanup =
            child.mirrorPositionFrames(
                frameCleanup,
                f -> {
                  Listener.ScalarSet<GroupPositionFrame, Integer> lengthListener =
                      f.addLengthSetListeners(
                          (target, value) -> {
                            updateFrames(context, window);
                          });
                  return () -> {
                    f.removeLengthSetListeners(lengthListener);
                  };
                },
                r -> {
                  r.run();
                },
                at -> {
                  updateFrames(context, window);
                });
        layout = new VBox();
        row = Optional.of(new RowFramesWidget(window, timeline, RowAdapterGroupChildPosition.this));
        layout.getChildren().add(row.get());
      }

      @Override
      public Node getWidget() {
        return layout;
      }

      @Override
      public void remove() {
        framesCleanup.run();
        child.removePositionPrelengthSetListeners(prelengthCleanup);
        frameCleanup.forEach(c -> c.run());
      }
    };
  }

  @Override
  public ObservableValue<String> getName() {
    return new ObservableValueBase<String>() {
      @Override
      public String getValue() {
        return "Position";
      }
    };
  }

  @Override
  public ObservableObjectValue<Image> getStateImage() {
    return emptyStateImage;
  }

  @Override
  public void remove(Context context) {}

  @Override
  public boolean isMain() {
    return childRowAdapter.selected.get().orElse(false);
  }

  @Override
  protected void addFrame(ChangeStepBuilder change, int index, GroupPositionFrame frame) {
    change.groupChild(child).positionFramesAdd(index, frame);
  }

  @Override
  protected GroupPositionFrame innerCreateFrame(Context context, GroupPositionFrame previousFrame) {
    GroupPositionFrame newFrame = GroupPositionFrame.create(context.model);
    if (previousFrame != null)
    newFrame.initialOffsetSet(context.model, previousFrame.offset());
    return newFrame;
  }

  @Override
  protected void setFrameLength(ChangeStepBuilder change, GroupPositionFrame frame, int length) {
    change.groupPositionFrame(frame).lengthSet(length);
  }

  @Override
  protected void setFrameInitialLength(Context context, GroupPositionFrame frame, int length) {
    frame.initialLengthSet(context.model, length);
  }

  @Override
  protected int getFrameLength(GroupPositionFrame frame) {
    return frame.length();
  }

  @Override
  protected int getPrelength(GroupChild node) {
    return node.positionPrelength();
  }

  @Override
  protected GroupChild getNode() {
    return child;
  }

  @Override
  public FrameFinder<GroupChild, GroupPositionFrame> getFrameFinder() {
    return GroupChildWrapper.positionFrameFinder;
  }

  @Override
  protected GroupPositionFrame innerDuplicateFrame(Context context, GroupPositionFrame source) {
    GroupPositionFrame out = GroupPositionFrame.create(context.model);
    out.initialOffsetSet(context.model, source.offset());
    return out;
  }

  @Override
  protected void frameClear(ChangeStepBuilder change, GroupPositionFrame f) {
    change.groupPositionFrame(f).offsetSet(Vector.ZERO);
  }

  @Override
  protected int frameCount() {
    return child.positionFramesLength();
  }

  @Override
  protected void removeFrame(ChangeStepBuilder change, int at, int count) {
    change.groupChild(child).positionFramesRemove(at, count);
  }

  @Override
  protected void moveFramesTo(ChangeStepBuilder change, int source, int count, int dest) {
    change.groupChild(child).positionFramesMoveTo(source, count, dest);
  }

  @Override
  protected void setPrelength(ChangeStepBuilder change, GroupChild node, int length) {
    change.groupChild(node).positionPrelengthSet(length);
  }
}
