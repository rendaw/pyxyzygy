package com.zarbosoft.shoedemo;

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

	public DoubleVector divide(double scale) {
		return new DoubleVector(x / scale, y / scale);
	}
}
