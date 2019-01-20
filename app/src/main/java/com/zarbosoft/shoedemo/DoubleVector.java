package com.zarbosoft.shoedemo;

import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.luxem.write.RawWriter;
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

	public DoubleVector minus(Vector offset) {
		return new DoubleVector(x - offset.x, y - offset.y);
	}

	public DoubleVector plus(DoubleVector other) {
		return new DoubleVector(x + other.x, y + other.y);
	}

	public DoubleVector plus(Vector offset) {
		return new DoubleVector(x + offset.x, y + offset.y);
	}

	public Vector toInt() {
		return new Vector((int) x, (int) y);
	}

	public DoubleVector divide(double factor) {
		return new DoubleVector(x / factor, y / factor);
	}

	public DoubleVector multiply(double factor) {
		return new DoubleVector(x * factor, y * factor);
	}

	@Override
	public String toString() {
		return String.format("dv[%s %s]", x, y);
	}

	public DoubleVector divide(DoubleVector factors) {
		return new DoubleVector(x / factors.x, y / factors.y);
	}

	public DoubleVector multiply(DoubleVector factors) {
		return new DoubleVector(x * factors.x, y * factors.y);
	}
}
