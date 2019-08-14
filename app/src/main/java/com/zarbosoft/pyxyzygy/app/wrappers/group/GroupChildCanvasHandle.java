package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.zarbosoft.automodel.lib.Listener;
import com.zarbosoft.javafxbinders.BinderRoot;
import com.zarbosoft.javafxbinders.CustomBinding;
import com.zarbosoft.javafxbinders.DoubleHalfBinder;
import com.zarbosoft.javafxbinders.IndirectHalfBinder;
import com.zarbosoft.javafxbinders.PropertyHalfBinder;
import com.zarbosoft.pyxyzygy.app.CanvasHandle;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.DoubleRectangle;
import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.Wrapper;
import com.zarbosoft.pyxyzygy.app.widgets.binders.ScalarHalfBinder;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupChild;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupPositionFrame;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupTimeFrame;
import com.zarbosoft.pyxyzygy.seed.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.zarbosoft.rendaw.common.Common.opt;
import static com.zarbosoft.pyxyzygy.app.Global.NO_INNER;
import static com.zarbosoft.pyxyzygy.app.Global.opacityMax;
import static com.zarbosoft.pyxyzygy.app.Misc.moveTo;

public class GroupChildCanvasHandle extends CanvasHandle {
  private final BinderRoot opacityRoot;
  private final Listener.ScalarSet<GroupChild, Integer> timePrelengthListener;
  private final Listener.ScalarSet<GroupChild, Integer> positionPrelengthListener;
  private CanvasHandle parent;
  private final Listener.ListAdd<GroupChild, GroupPositionFrame> positionAddListener;
  private final Listener.ListRemove<GroupChild> positionRemoveListener;
  private final Listener.ListMoveTo<GroupChild> positionMoveListener;
  private final Listener.ListAdd<GroupChild, GroupTimeFrame> timeAddListener;
  private final Listener.ListRemove<GroupChild> timeRemoveListener;
  private final Listener.ListMoveTo<GroupChild> timeMoveListener;
  private final BinderRoot enabledListenerRoot;
  private int zoom;
  private CanvasHandle childCanvas;
  private final List<Runnable> positionCleanup;
  private final List<Runnable> timeCleanup;
  private GroupChildWrapper wrapper;

  public GroupChildCanvasHandle(Context context, Window window, GroupChildWrapper wrapper) {
    this.wrapper = wrapper;
    positionCleanup = new ArrayList<>();
    timeCleanup = new ArrayList<>();

    ScalarHalfBinder<Boolean> enabledBinder = new ScalarHalfBinder<>(wrapper.node, "enabled");
    ScalarHalfBinder<Integer> opacityBinder = new ScalarHalfBinder<>(wrapper.node, "opacity");
    enabledListenerRoot =
        new DoubleHalfBinder<>(
                new PropertyHalfBinder<>(wrapper.child),
                new DoubleHalfBinder<>(
                        enabledBinder,
                        new DoubleHalfBinder<>(
                            window.selectedForEditWrapperEnabledBinder,
                            new IndirectHalfBinder<GroupChild>(
                                window.selectedForEditWrapperEnabledBinder,
                                e -> {
                                  if (e instanceof GroupNodeWrapper) {
                                    return opt(((GroupNodeWrapper) e).specificChild);
                                  }
                                  return opt(null);
                                })))
                    .map(
                        (enabled, p) -> {
                          Wrapper edit = p.first;
                          GroupChild specificLayer = p.second;
                          if (enabled) return opt(true);
                          if (edit != null) {
                            Wrapper at = edit;
                            while (at != null) {
                              if (at == wrapper) return opt(true);
                              at = at.getParent();
                            }
                          }
                          if (specificLayer != null && specificLayer == wrapper.node)
                            return opt(true);
                          return opt(false);
                        }))
            .addListener(
                (child, enabled) -> {
                  if (childCanvas != null) {
                    paint.getChildren().clear();
                    overlay.getChildren().clear();
                    childCanvas.remove(context, null);
                  }
                  if (child != null && enabled) {
                    childCanvas = child.buildCanvas(context, window, this);
                    paint.getChildren().add(childCanvas.getPaintWidget());
                    overlay.getChildren().add(childCanvas.getOverlayWidget());
                    updateTime(context);
                    updatePosition(context);
                  }
                });

    // Don't need clear listeners because clear should never happen (1 frame must always be left)
    positionPrelengthListener =
        wrapper.node.addPositionPrelengthSetListeners(
            ((target, value) -> {
              updatePosition(context);
            }));
    positionAddListener =
        wrapper.node.addPositionFramesAddListeners(
            (target, at, value) -> {
              updatePosition(context);
              positionCleanup.addAll(
                  at,
                  value.stream()
                      .map(
                          v -> {
                            Listener.ScalarSet<GroupPositionFrame, Integer> lengthListener =
                                v.addLengthSetListeners(
                                    (target1, value1) -> {
                                      updatePosition(context);
                                    });
                            Listener.ScalarSet<GroupPositionFrame, Vector> offsetListener =
                                v.addOffsetSetListeners(
                                    (target12, value12) -> updatePosition(context));
                            return (Runnable)
                                () -> {
                                  v.removeLengthSetListeners(lengthListener);
                                  v.removeOffsetSetListeners(offsetListener);
                                };
                          })
                      .collect(Collectors.toList()));
            });
    positionRemoveListener =
        wrapper.node.addPositionFramesRemoveListeners(
            (target, at, count) -> {
              updatePosition(context);
              List<Runnable> tempClean = positionCleanup.subList(at, at + count);
              tempClean.forEach(r -> r.run());
              tempClean.clear();
            });
    positionMoveListener =
        wrapper.node.addPositionFramesMoveToListeners(
            (target, source, count, dest) -> {
              updatePosition(context);
              moveTo(positionCleanup, source, count, dest);
            });
    timePrelengthListener =
        wrapper.node.addTimePrelengthSetListeners(
            ((target, value) -> {
              updateTime(context);
            }));
    timeAddListener =
        wrapper.node.addTimeFramesAddListeners(
            (target, at, value) -> {
              updateTime(context);
              timeCleanup.addAll(
                  at,
                  value.stream()
                      .map(
                          v -> {
                            Listener.ScalarSet<GroupTimeFrame, Integer> lengthListener =
                                v.addLengthSetListeners(
                                    (target1, value1) -> {
                                      updateTime(context);
                                    });
                            Listener.ScalarSet<GroupTimeFrame, Integer> offsetListener =
                                v.addInnerOffsetSetListeners(
                                    (target1, value1) -> updateTime(context));
                            Listener.ScalarSet<GroupTimeFrame, Integer> loopListener =
                                v.addInnerLoopSetListeners(
                                    (target1, value1) -> updateTime(context));
                            return (Runnable)
                                () -> {
                                  v.removeLengthSetListeners(lengthListener);
                                  v.removeInnerOffsetSetListeners(offsetListener);
                                  v.removeInnerLoopSetListeners(loopListener);
                                };
                          })
                      .collect(Collectors.toList()));
            });
    timeRemoveListener =
        wrapper.node.addTimeFramesRemoveListeners(
            (target, at, count) -> {
              updateTime(context);
              List<Runnable> tempClean = timeCleanup.subList(at, at + count);
              tempClean.forEach(r -> r.run());
              tempClean.clear();
            });
    timeMoveListener =
        wrapper.node.addTimeFramesMoveToListeners(
            (target, source, count, dest) -> {
              updateTime(context);
              moveTo(timeCleanup, source, count, dest);
            });
    opacityRoot =
        CustomBinding.bind(
            paint.opacityProperty(),
            new DoubleHalfBinder<Boolean, Integer>(enabledBinder, opacityBinder)
                .map(
                    (enabled, opacity) ->
                        opt(((double) opacity) / opacityMax / (enabled ? 1 : 2))));
  }

  @Override
  public void setParent(CanvasHandle parent) {
    this.parent = parent;
  }

  private GroupPositionFrame findPosition() {
    return findPosition(time.get());
  }

  private GroupPositionFrame findPosition(int frame) {
    return wrapper.positionFrameFinder.findFrame(wrapper.node, frame).frame;
  }

  private void updateTime(Context context) {
    if (childCanvas != null)
      childCanvas.setViewedTime(context, GroupChildWrapper.toInnerTime(wrapper.node, time.get()));
  }

  @Override
  public void remove(Context context, Wrapper excludeSubtree) {
    if (childCanvas != null) {
      if (childCanvas.getWrapper() != excludeSubtree) childCanvas.remove(context, excludeSubtree);
      childCanvas = null;
    }
    wrapper.node.removePositionPrelengthSetListeners(positionPrelengthListener);
    wrapper.node.removePositionFramesAddListeners(positionAddListener);
    wrapper.node.removePositionFramesRemoveListeners(positionRemoveListener);
    wrapper.node.removePositionFramesMoveToListeners(positionMoveListener);
    wrapper.node.removeTimePrelengthSetListeners(timePrelengthListener);
    wrapper.node.removeTimeFramesAddListeners(timeAddListener);
    wrapper.node.removeTimeFramesRemoveListeners(timeRemoveListener);
    wrapper.node.removeTimeFramesMoveToListeners(timeMoveListener);
    opacityRoot.destroy();
    enabledListenerRoot.destroy();
    positionCleanup.forEach(r -> r.run());
    timeCleanup.forEach(r -> r.run());
    wrapper.canvasHandle = null;
  }

  @Override
  public void setViewport(Context context, DoubleRectangle newBounds, int positiveZoom) {
    this.zoom = positiveZoom;
    bounds.set(newBounds);
    updatePosition(context);
  }

  private void updatePosition(Context context) {
    if (time.get() == NO_INNER) return;
    GroupPositionFrame pos = findPosition();
    if (pos == null) return;
    if (bounds.get() == null) return;
    paint.setLayoutX(pos.offset().x);
    paint.setLayoutY(pos.offset().y);
    if (childCanvas != null) {
      childCanvas.overlay.setLayoutX(pos.offset().x);
      childCanvas.overlay.setLayoutY(pos.offset().y);
      childCanvas.setViewport(context, bounds.get().minus(pos.offset()), zoom);
    }
  }

  @Override
  public void setViewedTime(Context context, int outerTime) {
    this.time.set(outerTime);
    updateTime(context);
    updatePosition(context);
  }

  @Override
  public DoubleVector toInnerPosition(DoubleVector outerPosition) {
    GroupPositionFrame pos = findPosition();
    return outerPosition.minus(pos.offset());
  }

  @Override
  public GroupChildWrapper getWrapper() {
    return wrapper;
  }

  @Override
  public CanvasHandle getParent() {
    return parent;
  }
}
