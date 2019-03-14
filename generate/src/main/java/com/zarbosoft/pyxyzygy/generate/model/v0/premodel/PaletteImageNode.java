package com.zarbosoft.pyxyzygy.generate.model.v0.premodel;

import com.zarbosoft.interface1.Configuration;

import java.util.List;

@Configuration
public class PaletteImageNode extends ProjectNode {
	@Configuration
	public List<PaletteImageFrame> frames;

	@Configuration
	public Palette palette;
}
