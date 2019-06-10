package com.zarbosoft.pyxyzygy.generate;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.zarbosoft.rendaw.common.Common.uncheck;

public abstract class TaskBase extends Task {
  public Path path;

  public void setPath(final String path) {
    this.path = Paths.get(path);
  }

  @Override
  public void execute() throws BuildException {
    uncheck(() -> Files.createDirectories(path));
    run();
  }

  public abstract void run();
}
