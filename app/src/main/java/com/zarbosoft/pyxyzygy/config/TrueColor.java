package com.zarbosoft.pyxyzygy.config;

import com.zarbosoft.interface1.Configuration;
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
		return Color.rgb(Byte.toUnsignedInt(r), Byte.toUnsignedInt(g), Byte.toUnsignedInt(b), Byte.toUnsignedInt(a) / 255.0);
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
		return String.format("c[%s %s %s %s]", r,g,b,a);
	}
}
