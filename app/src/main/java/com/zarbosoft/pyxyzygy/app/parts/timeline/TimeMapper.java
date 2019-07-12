package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.FrameMapEntry;

import java.util.List;
import java.util.stream.Collectors;

public abstract class TimeMapper {
  public List<FrameMapEntry> timeMap = null;

  public void remove() {}

  public void updateTime(List<FrameMapEntry> timeMap) {
    this.timeMap = timeMap;
  }
}
