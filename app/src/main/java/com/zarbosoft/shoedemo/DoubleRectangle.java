package com.zarbosoft.shoedemo;

import com.zarbosoft.shoedemo.model.Rectangle;
import com.zarbosoft.shoedemo.model.Vector;

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

	public Rectangle descaleIntOuter(int scale) {
		int outX = (int) (x / scale);
		int outY = (int) (y / scale);
		return new Rectangle(outX, outY, (int) ((x + width) / scale - outX), (int) ((y + height) / scale - outY));
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
}
