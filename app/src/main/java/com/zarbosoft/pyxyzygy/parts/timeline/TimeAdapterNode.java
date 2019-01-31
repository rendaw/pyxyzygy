package com.zarbosoft.pyxyzygy.parts.timeline;

import com.zarbosoft.pyxyzygy.FrameMapEntry;

import java.util.List;

public abstract class TimeAdapterNode {
	public List<FrameMapEntry> timeMap = null;

	public void remove() {
	}

	public void updateTime(List<FrameMapEntry> timeMap) {
		this.timeMap = timeMap;
	}
}
