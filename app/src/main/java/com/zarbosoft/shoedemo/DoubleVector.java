package com.zarbosoft.shoedemo;

import com.zarbosoft.shoedemo.model.Vector;

public class DoubleVector {
	public final double x;
	public final double y;

	public DoubleVector(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public DoubleVector minus(DoubleVector other) {
		return new DoubleVector(x - other.x, y - other.y);
	}

	public DoubleVector plus(DoubleVector other) {
		return new DoubleVector(x + other.x, y + other.y);
	}

	public DoubleVector divide(double factor) {
		return new DoubleVector(x / factor, y / factor);
	}

	public DoubleVector plus(Vector offset) {
		return new DoubleVector(offset.x, offset.y);
	}

	public Vector toInt() {
		return new Vector((int) x, (int) y);
	}

	public DoubleVector multiply(double factor) {
		return new DoubleVector(x * factor, y * factor);
	}
}
