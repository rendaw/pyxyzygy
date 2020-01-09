package com.zarbosoft.pyxyzygy.app.widgets;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface DialogInterface {
  public DialogInterface addContent(Node node);

  public DialogInterface addAction(
      ButtonType type, boolean isDefault, Consumer<Button> configure, Supplier<Boolean> callback);

  public void close();

  public void go();

  public DialogInterface focus(Node node);
}
