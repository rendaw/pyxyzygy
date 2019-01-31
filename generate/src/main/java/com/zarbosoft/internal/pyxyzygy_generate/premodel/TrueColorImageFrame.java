package com.zarbosoft.internal.pyxyzygy_generate.premodel;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.internal.pyxyzygy_seed.model.Vector;

import java.util.Map;

@Configuration
public class TrueColorImageFrame extends ProjectObject {
	@Configuration
	public int length;
	@Configuration
	public Vector offset;
	@Configuration
	public Map<Long, TileBase> tiles;
}
