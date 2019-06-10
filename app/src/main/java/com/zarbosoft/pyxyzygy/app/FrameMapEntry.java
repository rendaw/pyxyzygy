package com.zarbosoft.pyxyzygy.app;

import static com.zarbosoft.pyxyzygy.app.Global.NO_LENGTH;

public class FrameMapEntry {
  public final int length;
  public final int innerOffset;

  public FrameMapEntry(int length, int innerOffset) {
    this.length = length;
    this.innerOffset = innerOffset;
  }
}
