package com.zarbosoft.pyxyzygy.seed;

import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.luxem.write.RawWriter;

public class Vector {
  public static final Vector ZERO = new Vector(0, 0);
  public final int x;

  public final int y;

  public Vector(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public Vector plus(int x, int y) {
    return new Vector(this.x + x, this.y + y);
  }

  public void serialize(RawWriter writer) {
    writer
        .recordBegin()
        .key("x")
        .primitive(Integer.toString(x))
        .key("y")
        .primitive(Integer.toString(y))
        .recordEnd();
  }

  public Vector multiply(int tileSize) {
    return new Vector(x * tileSize, y * tileSize);
  }

  public Vector plus(Vector vector) {
    return new Vector(x + vector.x, y + vector.y);
  }

  public Rectangle toRect(int width, int height) {
    return new Rectangle(x, y, width, height);
  }

  public Vector minus(Vector other) {
    return new Vector(x - other.x, y - other.y);
  }

  public Vector divide(int factor) {
    return new Vector(x / factor, y / factor);
  }

  public static class Deserializer extends StackReader.RecordState {
    int x;
    int y;

    @Override
    public void value(Object value) {
      if ("x".equals(key)) {
        x = Integer.parseInt((String) value);
        return;
      }
      if ("y".equals(key)) {
        y = Integer.parseInt((String) value);
        return;
      }
      throw new IllegalStateException("Unknown key");
    }

    @Override
    public Object get() {
      return new Vector(x, y);
    }
  }

  public long to1D() {
    return (((long) y) << 32) | Integer.toUnsignedLong(x);
  }

  public static Vector from1D(long source) {
    return new Vector((int) (source & 0xFFFFFFFF), (int) (source >>> 32));
  }

  @Override
  public String toString() {
    return String.format("v[%s %s]", x, y);
  }
}
