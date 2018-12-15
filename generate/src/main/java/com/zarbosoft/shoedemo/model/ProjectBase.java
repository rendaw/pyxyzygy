package com.zarbosoft.shoedemo.model;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ProjectBase {
	public long nextId;

	public Map<Long, ProjectNode> objectMap = new HashMap<>();
	public Path root;
	public Path tileDir;

}
