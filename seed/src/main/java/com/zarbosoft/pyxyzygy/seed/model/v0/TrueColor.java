package com.zarbosoft.pyxyzygy.seed.model.v0;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.luxem.write.RawWriter;
import javafx.scene.paint.Color;

@Configuration(name = "true-color")
public class TrueColor {
	@Configuration
	public byte r;
	@Configuration
	public byte g;
	@Configuration
	public byte b;
	@Configuration
	public byte a;

	public static TrueColor rgba(int r, int g, int b, int a) {
		TrueColor out = new TrueColor();
		out.r = (byte) r;
		out.g = (byte) g;
		out.b = (byte) b;
		out.a = (byte) a;
		return out;
	}

	public Color toJfx() {
		return Color.rgb(Byte.toUnsignedInt(r),
				Byte.toUnsignedInt(g),
				Byte.toUnsignedInt(b),
				Byte.toUnsignedInt(a) / 255.0
		);
	}

	public static TrueColor fromJfx(Color other) {
		TrueColor out = new TrueColor();
		out.r = (byte) (other.getRed() * 255);
		out.g = (byte) (other.getGreen() * 255);
		out.b = (byte) (other.getBlue() * 255);
		out.a = (byte) (other.getOpacity() * 255);
		return out;
	}

	@Override
	public String toString() {
		return String.format("c[%s %s %s %s]", r, g, b, a);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (obj.getClass() != getClass())
			return false;
		return r == ((TrueColor) obj).r &&
				g == ((TrueColor) obj).g &&
				b == ((TrueColor) obj).b &&
				a == ((TrueColor) obj).a;
	}

	public void serialize(RawWriter writer) {
		writer.recordBegin();
		writer.key("r").primitive(Byte.toString(r));
		writer.key("g").primitive(Byte.toString(g));
		writer.key("b").primitive(Byte.toString(b));
		writer.key("a").primitive(Byte.toString(a));
		writer.recordEnd();
	}

	public static class Deserializer extends StackReader.State {
		TrueColor out = new TrueColor();
		String key;

		@Override
		public void key(String value) {
			key = value;
		}

		@Override
		public void value(Object value) {
			String text = (String) value;
			if ("r".equals(key))
				out.r = (byte) Integer.parseInt(text);
			else if ("g".equals(key))
				out.g = (byte) Integer.parseInt(text);
			else if ("b".equals(key))
				out.b = (byte) Integer.parseInt(text);
			else if ("a".equals(key))
				out.a = (byte) Integer.parseInt(text);
			else
				throw new RuntimeException(String.format("Unknown field %s", key));
			key = null;
		}

		@Override
		public void type(String value) {

		}

		@Override
		public Object get() {
			return out;
		}
	}
}
