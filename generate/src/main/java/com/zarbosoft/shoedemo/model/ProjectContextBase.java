package com.zarbosoft.shoedemo.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static com.zarbosoft.rendaw.common.Common.uncheck;

public class ProjectContextBase {
	public final Path path;
	public final Path changesDir;
	public final Path tileDir;

	public long nextId;

	public Map<Long, ProjectObjectInterface> objectMap = new HashMap<>();

	public ProjectContextBase(Path path) {
		this.path = path;
		changesDir = path.resolve("changes");
		tileDir = path.resolve("tiles");
	}
}
