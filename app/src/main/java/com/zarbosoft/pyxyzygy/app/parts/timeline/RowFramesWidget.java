package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.FrameMapEntry;
import com.zarbosoft.pyxyzygy.app.Window;
import javafx.beans.binding.Bindings;
import javafx.scene.Group;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.zarbosoft.pyxyzygy.app.Global.NO_INNER;
import static com.zarbosoft.pyxyzygy.app.Global.NO_LENGTH;
import static com.zarbosoft.rendaw.common.Common.last;
import static com.zarbosoft.rendaw.common.Common.sublist;

public class RowFramesWidget extends Pane {
  final Timeline timeline;
  Group inner = new Group();
  final RowAdapter adapter;
  Rectangle frameMarker = new Rectangle(Timeline.baseSize, Timeline.baseSize);
  List<FrameWidget> frames = new ArrayList<>();

  public RowFramesWidget(Window window, Timeline timeline, RowAdapter adapter) {
    this.timeline = timeline;
    this.adapter = adapter;
    inner
        .layoutXProperty()
        .bind(
            Bindings.createDoubleBinding(
                () -> {
                  double corner = timeline.controlAlignment.localToScene(0, 0).getX();
                  return corner
                      - localToScene(0, 0).getX()
                      - timeline.timeScroll.getValue()
                      + Timeline.baseSize * 2;
                },
                localToSceneTransformProperty(),
                timeline.controlAlignment.localToSceneTransformProperty(),
                timeline.timeScroll.valueProperty()));
    setMinHeight(Timeline.baseSize);
    setPrefHeight(getMinHeight());
    setMaxHeight(getMinHeight());
    frameMarker.setFill(Timeline.frameMarkerColor);
    frameMarker.setBlendMode(BlendMode.MULTIPLY);
    inner.getChildren().add(frameMarker);
    getChildren().addAll(inner);
    updateFrameMarker(window);
  }

  /**
   * @param context
   * @param window
   * @return max frame encountered
   */
  public int updateTime(Context context, Window window, List<RowAdapterFrame> frameAdapters) {
    FrameWidget foundSelectedFrame = null;
    Object selectedId =
        Optional.ofNullable(timeline.selectedFrame.get())
            .filter(f -> f.row == this)
            .map(f -> f.frame.id())
            .orElse(null);

    int frameIndex = 0;
    int outerAt = 0;

    final int time = timeline.time.get();
    int previous = NO_INNER; // if main row only
    int next = NO_INNER; // if main row only

    for (FrameMapEntry outer : window.timeMap) {
      if (outer.innerOffset != NO_INNER) {
        int previousInnerAt = 0;
        int innerAt = 0;
        for (RowAdapterFrame inner : frameAdapters) {
          int offset = innerAt - outer.innerOffset;
          if (offset >= 0) {
            if (outer.length != NO_LENGTH && offset >= outer.length) {
              break;
            }
            FrameWidget frame;
            int useFrameIndex = frameIndex++;
            if (frames.size() <= useFrameIndex) {
              frames.add(frame = new FrameWidget(context, this));
              this.inner.getChildren().add(frame);
            } else {
              frame = frames.get(useFrameIndex);
            }
            int innerLeft = Math.max(previousInnerAt + 1, outer.innerOffset);
            int absStart = outerAt + innerLeft - outer.innerOffset;
            int absEnd = outer.length == NO_LENGTH ? NO_LENGTH : outerAt + outer.length;
            int minLength = innerLeft - previousInnerAt;
            int offset1 = innerAt - innerLeft;
            frame.set(timeline.zoom, useFrameIndex, inner, absStart, absEnd, minLength, offset1);
            if (inner.id() == selectedId && foundSelectedFrame == null) {
              foundSelectedFrame = frame;
            }
            if (adapter.isMain()) {
              int abs = frame.at.get();
              if (frame.at.get() < time) {
                previous = abs;
              }
              if (next == NO_LENGTH && frame.at.get() > time) {
                next = abs;
              }
            }
          }
          previousInnerAt = innerAt;
          innerAt += inner.length();
        }
      }
      outerAt += outer.length;
    }

    if (adapter.isMain()) {
      timeline.previousFrame.set(previous);
      timeline.nextFrame.set(next);
    }

    if (selectedId != null) {
      timeline.select(foundSelectedFrame);
    }

    if (frameIndex < frames.size()) {
      List<FrameWidget> remove = sublist(frames, frameIndex);
      this.inner.getChildren().removeAll(remove);
      remove.clear();
    }

    return last(frames).at.get();
  }

  public void updateFrameMarker(Window window) {
    if (window.getSelectedForView() == null) return;
    int time = timeline.time.get();
    frameMarker.setLayoutX(time * timeline.zoom);
    if (adapter.isMain()) {
      int previous = -1;
      int next = -1;
      for (FrameWidget frame : frames) {
        int at = frame.at.get();
        if (frame.at.get() < time) {
          previous = at;
        }
        if (frame.at.get() > time) {
          next = at;
          break;
        }
      }
      timeline.previousFrame.set(previous);
      timeline.nextFrame.set(next);
    }
  }
}
