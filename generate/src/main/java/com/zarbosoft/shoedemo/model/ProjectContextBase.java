package com.zarbosoft.shoedemo.model;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ProjectContextBase {
	public long nextId;

	public Map<Long, ProjectObject> objectMap = new HashMap<>();
	public Path root;
	public Path tileDir;

}
