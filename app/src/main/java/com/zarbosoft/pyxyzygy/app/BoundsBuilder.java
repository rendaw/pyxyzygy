package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.pyxyzygy.seed.model.v0.Rectangle;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;

public class BoundsBuilder {
	boolean first = true;
	private double xMin;
	private double xMax;
	private double yMin;
	private double yMax;

	public BoundsBuilder circle(DoubleVector v, double radius) {
		if (first) {
			xMin = v.x - radius;
			xMax = v.x + radius;
			yMin = v.y - radius;
			yMax = v.y + radius;
			first = false;
		} else {
			xMin = Math.min(xMin, v.x - radius);
			xMax = Math.max(xMax, v.x + radius);
			yMin = Math.min(yMin, v.y - radius);
			yMax = Math.max(yMax, v.y + radius);
		}
		return this;
	}

	public Rectangle buildInt() {
		return new Rectangle(
				(int) Math.floor(xMin),
				(int) Math.floor(yMin),
				(int) Math.ceil(xMax - xMin),
				(int) Math.ceil(yMax - yMin)
		);
	}

	public BoundsBuilder quantize(int step) {
		xMin = Math.floor(xMin / step) * step;
		xMax = Math.ceil(xMax / step) * step;
		yMin = Math.floor(yMin / step) * step;
		yMax = Math.ceil(yMax / step) * step;
		return this;
	}

	public BoundsBuilder scale(double scale) {
		double hw = 0.5 * (xMax - xMin);
		double hh = 0.5 * (yMax - yMin);
		xMin -= hw * scale;
		xMax += hw * scale;
		yMin -= hh * scale;
		yMax += hh * scale;
		return this;
	}

	public DoubleRectangle build() {
		return new DoubleRectangle(xMin, yMin, xMax - xMin, yMax - yMin);
	}

	public BoundsBuilder shift(DoubleVector delta) {
		xMin += delta.x;
		xMax += delta.x;
		yMin += delta.y;
		yMax += delta.y;
		return this;
	}

	public BoundsBuilder point(Vector v) {
		if (first) {
			xMin = v.x;
			xMax = v.x;
			yMin = v.y;
			yMax = v.y;
			first = false;
		} else {
			xMin = Math.min(xMin, v.x);
			xMax = Math.max(xMax, v.x);
			yMin = Math.min(yMin, v.y);
			yMax = Math.max(yMax, v.y);
		}
		return this;
	}
}
