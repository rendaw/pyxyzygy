package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import javafx.geometry.Point2D;

public class DoubleVector {
  public static DoubleVector zero = new DoubleVector(0, 0);
  public final double x;
  public final double y;

  public DoubleVector(double x, double y) {
    this.x = x;
    this.y = y;
  }

  public static DoubleVector of(Vector vector) {
    return new DoubleVector(vector.x, vector.y);
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
    return new Vector((int) Math.floor(x), (int) Math.floor(y));
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

  public Point2D toJfx() {
    return new Point2D(x, y);
  }
}
