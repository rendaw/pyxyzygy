package com.zarbosoft.internal.shoedemo_generate.premodel;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.shoedemo.model.ProjectObject;
import com.zarbosoft.shoedemo.model.ProjectObjectInterface;
import com.zarbosoft.shoedemo.model.Tile;
import com.zarbosoft.shoedemo.model.Vector;

import java.util.Map;

@Configuration
public class ImageFrame extends ProjectObject {
	@Configuration
	public int length;
	@Configuration
	public Vector offset;
	@Configuration
	public Map<String, Tile> tiles;
}
