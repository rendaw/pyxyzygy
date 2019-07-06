package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.pyxyzygy.seed.Rectangle;
import com.zarbosoft.pyxyzygy.seed.Vector;

import static com.zarbosoft.rendaw.common.Common.ceilDiv;

public class DoubleRectangle {
  public final double x;
  public final double y;
  public final double width;
  public final double height;

  public DoubleRectangle(double x, double y, double width, double height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (obj.getClass() != getClass()) return false;
    return ((DoubleRectangle) obj).x == x
        && ((DoubleRectangle) obj).y == y
        && ((DoubleRectangle) obj).width == width
        && ((DoubleRectangle) obj).height == height;
  }

  public Rectangle divideContains(int scale) {
    int outX = Math.floorDiv((int) Math.floor(x), scale);
    int outY = Math.floorDiv((int) Math.floor(y), scale);
    return new Rectangle(
        outX,
        outY,
        ceilDiv((int) Math.ceil(x + width), scale) - outX,
        ceilDiv((int) Math.ceil(y + height), scale) - outY);
  }

  public DoubleRectangle scale(double scale) {
    return new DoubleRectangle(
        x - width * (scale - 1) / 2, y - height * (scale - 1) / 2, width * scale, height * scale);
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

  public DoubleVector corner() {
    return new DoubleVector(x, y);
  }

  public DoubleVector span() {
    return new DoubleVector(width, height);
  }

  public boolean contains(DoubleVector vector) {
    return vector.x >= x && vector.y >= y && vector.x < x + width && vector.y < y + height;
  }
}
