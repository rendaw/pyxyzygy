package com.zarbosoft.pyxyzygy.gui;

import com.zarbosoft.pyxyzygy.seed.model.Rectangle;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestRectangle {
	@Test
	public void testQuantizeReal1() {
		Rectangle r = new Rectangle(-160, -120, 320, 240).quantize(100);
		assertThat(r.x, equalTo(-2));
		assertThat(r.y, equalTo(-2));
		assertThat(r.width, equalTo(4));
		assertThat(r.height, equalTo(4));
	}

	private static void check(Rectangle r, int x, int width) {
		assertThat(r.x, equalTo(x));
		assertThat(r.y, equalTo(x));
		assertThat(r.width, equalTo(width));
		assertThat(r.height, equalTo(width));
	}

	@Test
	public void testQuantizeBaseWidth() {
		check(new Rectangle(0, 0, 150, 150).quantize(100), 0, 2);
	}

	@Test
	public void testQuantizeBaseX() {
		check(new Rectangle(100, 100, 100, 100).quantize(100), 1, 1);
	}
	@Test
	public void testQuantizeBaseSum() {
		check(new Rectangle(99, 99, 2, 2).quantize(100), 0, 2);
	}
}
