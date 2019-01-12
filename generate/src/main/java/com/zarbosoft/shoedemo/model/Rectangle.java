package com.zarbosoft.shoedemo.model;

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

	public Rectangle plus(Vector offset) {
		return new Rectangle(x + offset.x, y+offset.y,width ,height );
	}

	public Vector corner() {
		return new Vector(x,y );
	}
}
