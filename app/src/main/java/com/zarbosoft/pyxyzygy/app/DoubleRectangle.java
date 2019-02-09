package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.pyxyzygy.seed.model.v0.Rectangle;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;

import static com.zarbosoft.rendaw.common.Common.ceilDiv;

public class DoubleRectangle {
	final public double x;
	final public double y;
	final public double width;
	final public double height;

	public DoubleRectangle(double x, double y, double width, double height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public Rectangle quantize(int scale) {
		int outX = Math.floorDiv((int) x, scale);
		int outY = Math.floorDiv((int) y, scale);
		return new Rectangle(
				outX,
				outY,
				ceilDiv((int) Math.ceil(x + width), scale) - outX,
				ceilDiv((int) Math.ceil(y + height), scale) - outY
		);
	}

	public DoubleRectangle scale(double scale) {
		return new DoubleRectangle(x - width * (scale - 1) / 2,
				y - height * (scale - 1) / 2,
				width * scale,
				height * scale
		);
	}

	public DoubleRectangle plus(Vector offset) {
		return new DoubleRectangle(x + offset.x, y + offset.y, width, height);
	}

	public DoubleRectangle minus(Vector offset) {
		return new DoubleRectangle(x - offset.x, y - offset.y, width, height);
	}

	@Override
	public String toString() {
		return String.format("dr[%s %s %s %s]", x, y, width, height);
	}
}
