package com.zarbosoft.pyxyzygy.generate.model.v0.premodel;

import com.zarbosoft.interface1.Configuration;

import java.util.List;

@Configuration
public class Project extends ProjectObject {
	@Configuration
	public List<ProjectLayer> top;

	@Configuration
	public List<Palette> palettes;
}
