package com.zarbosoft.pyxyzygy.generate;

public class GenerateAll extends TaskBase {
  public static void main(final String[] args) {
    final GenerateAll t = new GenerateAll();
    t.setPath(args[0]);
    t.execute();
  }

  @Override
  public void run() {
    new GenerateModel() {
      {
        setPath(GenerateAll.this.path.toString());
      }
    }.run();
    new GenerateGraphicsProxy() {
      {
        setPath(GenerateAll.this.path.toString());
      }
    }.run();
  }
}
