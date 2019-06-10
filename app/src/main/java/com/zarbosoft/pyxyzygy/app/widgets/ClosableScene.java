package com.zarbosoft.pyxyzygy.app.widgets;

import javafx.scene.Parent;
import javafx.scene.Scene;

public class ClosableScene extends Scene {

  public ClosableScene(Parent root) {
    super(root);
  }

  public ClosableScene(Parent root, double width, double height) {
    super(root, width, height);
  }

  public void close() {}
}
