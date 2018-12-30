package com.zarbosoft.shoedemo.model;

import com.zarbosoft.luxem.write.RawWriter;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.scene.image.WritableImage;

/**
 * Shared, immutable
 */
public class Tile implements ProjectObjectInterface {
	public final long id;
	public final WritableImage data;
	public final int dataOffsetX;
	public final int dataOffsetY;

	private int refCount;

	public Tile(ProjectContextBase context, WritableImage data, int offsetX, int offsetY) {
		this.data = data;
		id = context.nextId++;
		dataOffsetX = offsetX;
		dataOffsetY = offsetY;
	}

	@Override
	public long id() {
		return id;
	}

	@Override
	public void serialize(RawWriter writer) {
		throw new Assertion();
	}

	@Override
	public void incRef(ProjectContextBase project) {
		refCount += 1;
	}

	@Override
	public void decRef(ProjectContextBase project) {
		refCount -= 1;
	}
}
