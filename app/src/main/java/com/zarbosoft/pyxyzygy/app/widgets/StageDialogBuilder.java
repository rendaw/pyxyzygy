package com.zarbosoft.pyxyzygy.app.widgets;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.pad;

public class StageDialogBuilder extends Stage implements DialogInterface {

  private final VBox layout;
  private final ButtonBar buttons;

  public StageDialogBuilder(String title) {
    setTitle(title);
    buttons = new ButtonBar();
    layout = new VBox(buttons);
    layout.setFillWidth(true);
    layout.setSpacing(6);
    layout.setPadding(new Insets(3));
    setScene(new Scene(layout));
  }

  @Override
  public DialogInterface addContent(Node node) {
    layout.getChildren().add(0, pad(node));
    return this;
  }

  @Override
  public DialogInterface addAction(
      ButtonType type, boolean isDefault, Consumer<Button> configure, Supplier<Boolean> callback) {
    Button button = new Button(type.getText());
    if (isDefault) button.setDefaultButton(true);
    button.setOnAction(
        e -> {
          if (callback.get()) {
            close();
          }
        });
    ButtonBar.setButtonData(button, type.getButtonData());
    buttons.getButtons().add(button);
    configure.accept(button);
    return this;
  }

  @Override
  public void go() {
    showAndWait();
  }

  @Override
  public DialogInterface focus(Node node) {
    Platform.runLater(() -> node.requestFocus());
    return this;
  }
}
