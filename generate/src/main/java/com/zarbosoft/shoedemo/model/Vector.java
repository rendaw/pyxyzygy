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
		writer.arrayBegin().primitive(Integer.toString(x)).primitive(Integer.toString(y)).arrayEnd();
	}

	public static class Deserializer extends StackReader.ArrayState {
		@Override
		public Object get() {
			return new Vector(Integer.parseInt((String) data.get(0)), Integer.parseInt((String) data.get(1)));
		}
	}
}
