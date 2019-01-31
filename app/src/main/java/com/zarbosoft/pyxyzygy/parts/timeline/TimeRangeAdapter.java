package com.zarbosoft.pyxyzygy.parts.timeline;

public abstract class TimeRangeAdapter {
	public abstract int getOuterAt();

	public abstract int getInnerStart();

	public abstract int getInnerLength();

	public abstract void changeStart(int value);

	public abstract void changeLength(int value);
}
