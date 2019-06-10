package com.zarbosoft.pyxyzygy.seed.model.v0;

import com.zarbosoft.pyxyzygy.seed.model.ProjectContext;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public abstract class ProjectContextBase extends ProjectContext {
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
