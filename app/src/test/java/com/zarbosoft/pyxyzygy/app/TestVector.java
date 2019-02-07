package com.zarbosoft.pyxyzygy.app;

import com.google.common.io.BaseEncoding;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import com.zarbosoft.pyxyzygy.seed.model.Vector;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class TestVector {
	private static void check(Vector v) {
		long temp = v.to1D();
		System.out.format(
				"%s %s = %s\n",
				BaseEncoding.base16().encode(Ints.toByteArray(v.x)),
				BaseEncoding.base16().encode(Ints.toByteArray(v.y)),
				BaseEncoding.base16().encode(Longs.toByteArray(temp))
		);
		Vector result = Vector.from1D(temp);
		assertThat(result.x, equalTo(v.x));
		assertThat(result.y, equalTo(v.y));
	}

	@Test
	public void testBase() {
		Vector v = new Vector(0, 0);
		long out = v.to1D();
		assertThat(out, equalTo(0L));
		check(v);
	}

	@Test
	public void testPosX() {
		check(new Vector(1, 0));
	}

	@Test
	public void testPosY() {
		check(new Vector(0, 1));
	}

	@Test
	public void testPosXY() {
		check(new Vector(1, 1));
	}

	@Test
	public void testNegX() {
		check(new Vector(-1, 0));
	}

	@Test
	public void testNegY() {
		check(new Vector(0, -1));
	}

	@Test
	public void testNegXPosY() {
		check(new Vector(-1, 1));
	}

	@Test
	public void testPosXNegY() {
		check(new Vector(1, -1));
	}

	@Test
	public void testLargeX() {
		check(new Vector(Integer.MAX_VALUE, 0));
	}

	@Test
	public void testLargeY() {
		check(new Vector(0, Integer.MAX_VALUE));
	}

	@Test
	public void testLargeNegX() {
		check(new Vector(Integer.MIN_VALUE, 0));
	}

	@Test
	public void testLargeNegY() {
		check(new Vector(0, Integer.MIN_VALUE));
	}
}
