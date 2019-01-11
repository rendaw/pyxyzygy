package com.zarbosoft.shoedemo.parts.timeline;

public abstract class TimeRangeAdapter {
	public abstract int getStart();

	public abstract int getLength();

	public abstract void changeStart(int value);

	public abstract void changeLength(int value);
}
