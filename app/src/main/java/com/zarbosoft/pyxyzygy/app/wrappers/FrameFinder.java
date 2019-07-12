package com.zarbosoft.pyxyzygy.app.wrappers;

import com.zarbosoft.rendaw.common.Assertion;

public abstract class FrameFinder<N, L> {
  public static class Result<L> {
    public final L frame;
    /**
     * Time from layer start
     */
    public final int at;
    public final int frameIndex;

    public Result(L frame, int at, int frameIndex) {
      this.frame = frame;
      this.at = at;
      this.frameIndex = frameIndex;
    }
  }

  public Result<L> findFrame(N node, int time) {
    if (time == -1) throw new Assertion();
    int at = 0;
    final int frameCount = frameCount(node);
    for (int i = 0; i < frameCount; ++i) {
      L pos = frameGet(node, i);
      final int frameLength = frameLength(pos);
      if ((i == frameCount - 1)
          || (time >= at && (frameLength == -1 || time < at + frameLength))) {
        return new Result(pos, at, i);
      }
      at += frameLength;
    }
    throw new Assertion();
  }

  public abstract L frameGet(N node, int i);

  public abstract int frameCount(N node);

  public abstract int frameLength(L frame);
}
