package com.zarbosoft.pyxyzygy.app.wrappers;

import com.zarbosoft.rendaw.common.Assertion;

import static com.zarbosoft.pyxyzygy.app.Global.NO_INNER;

public abstract class FrameFinder<N, L> {
  public static class Result<L> {
    public final L frame;
    /** Time from layer start */
    public final int at;

    public final int frameIndex;

    public Result(L frame, int at, int frameIndex) {
      this.frame = frame;
      this.at = at;
      this.frameIndex = frameIndex;
    }
  }

  public Result<L> findFrame(N node, int time) {
    if (time == NO_INNER) throw new Assertion();
    int at = prelength(node);
    if (at > time) return new Result<>(null, 0, NO_INNER);
    final int frameCount = frameCount(node);
    for (int i = 0; i < frameCount; ++i) {
      L pos = frameGet(node, i);
      final int frameLength = frameLength(pos);
      if ((i == frameCount - 1) || (time >= at && (frameLength == -1 || time < at + frameLength))) {
        return new Result(pos, at, i);
      }
      at += frameLength;
    }
    throw new Assertion();
  }

  public abstract int prelength(N node);

  public int frameTime(N node, int index) {
    int at = prelength(node);
    for (int i = 0; i < index; ++i) {
      at += frameLength(frameGet(node, i));
    }
    return at;
  }

  public abstract L frameGet(N node, int i);

  public abstract int frameCount(N node);

  public abstract int frameLength(L frame);
}
