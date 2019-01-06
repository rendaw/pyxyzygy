package com.zarbosoft.shoedemo.model;

import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.luxem.write.RawWriter;

public class Vector {
	public final int x;

	public final int y;

	public Vector(int x, int y) {
		this.x = x;
		this.y = y;
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
				x = Integer.parseInt((String) value);
				return;
			}
			throw new IllegalStateException("Unknown key");
		}

		@Override
		public Object get() {
			return new Vector(x, y);
		}
	}
}
