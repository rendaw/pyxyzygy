package com.zarbosoft.pyxyzygy.generate.model.v0.premodel;

import com.zarbosoft.interface1.Configuration;

import java.util.List;

@Configuration
public class Palette extends ProjectObject {
	@Configuration
	public String name;

	@Configuration
	public List<ProjectObject> entries;
}
