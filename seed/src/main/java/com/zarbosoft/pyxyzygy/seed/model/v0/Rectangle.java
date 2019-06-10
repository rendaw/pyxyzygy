package com.zarbosoft.pyxyzygy.seed.model.v0;

import java.util.Objects;

import static com.zarbosoft.rendaw.common.Common.ceilDiv;

public class Rectangle {
  public final int x;
  public final int y;
  public final int width;
  public final int height;

  public Rectangle(int x, int y, int width, int height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  public boolean contains(int x, int y) {
    return x >= this.x && y >= this.y && x < this.x + width && y < this.y + height;
  }

  public Rectangle divide(int val) {
    return new Rectangle(x / val, y / val, width / val, height / val);
  }

  public Rectangle multiply(int factor) {
    return new Rectangle(x * factor, y * factor, width * factor, height * factor);
  }

  /**
   * Returns a new rect that would encompass this rect if multiplied by the step value
   *
   * @param step
   * @return
   */
  public Rectangle divideContains(int step) {
    int newX = Math.floorDiv(this.x, step);
    int newY = Math.floorDiv(this.y, step);
    return new Rectangle(
        newX, newY, ceilDiv(this.x + width, step) - newX, ceilDiv(this.y + height, step) - newY);
  }

  public Rectangle shift(Vector offset) {
    return new Rectangle(x + offset.x, y + offset.y, width, height);
  }

  public Rectangle unshift(Vector offset) {
    return new Rectangle(x - offset.x, y - offset.y, width, height);
  }

  public Vector corner() {
    return new Vector(x, y);
  }

  @Override
  public String toString() {
    return String.format("r[%s %s %s %s]", x, y, width, height);
  }

  public Rectangle expand(Rectangle bounds) {
    final int x = Math.min(this.x, bounds.x);
    final int y = Math.min(this.y, bounds.y);
    return new Rectangle(
        x,
        y,
        Math.max(this.x + width, bounds.x + bounds.width) - x,
        Math.max(this.y + height, bounds.y + bounds.height) - y);
  }

  public boolean contains(Vector vector) {
    return contains(vector.x, vector.y);
  }

  public Vector span() {
    return new Vector(width, height);
  }

  @Override
  public int hashCode() {
    return Objects.hash(239034092, x, y, width, height);
  }

  public boolean empty() {
    return width == 0 && height == 0;
  }
}
