package com.zarbosoft.pyxyzygy.app.widgets;

import com.google.common.base.Throwables;
import com.zarbosoft.javafxbinders.CustomBinding;
import com.zarbosoft.javafxbinders.PropertyBinder;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.Global;
import com.zarbosoft.pyxyzygy.core.PaletteColors;
import com.zarbosoft.pyxyzygy.core.PaletteImage;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.seed.TrueColor;
import com.zarbosoft.rendaw.common.Pair;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Optional;

import static com.zarbosoft.pyxyzygy.app.Global.localization;
import static com.zarbosoft.rendaw.common.Common.opt;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public class HelperJFX {
  public static Constructor<Image> imageConstructor;

  static {
    imageConstructor = uncheck(() -> Image.class.getDeclaredConstructor(Object.class));
    imageConstructor.setAccessible(true);
  }

  public static Node pad(Node node) {
    VBox out = new VBox();
    out.setPadding(new Insets(3));
    out.getChildren().add(node);
    return out;
  }

  public static Color c(java.awt.Color source) {
    return Color.rgb(
        source.getRed(), source.getGreen(), source.getBlue(), source.getAlpha() / 255.0);
  }

  public static Cursor centerCursor(String res) {
    Image image = icon(res);
    return new ImageCursor(image, image.getWidth() / 2, image.getHeight() / 2);
  }

  public static Path resourcePath(String resource) {
    return Paths.get("resources").resolve(resource);
  }

  public static Image icon(String resource) {
    return Context.iconCache.computeIfAbsent(
        resource, r -> new Image(resourcePath("icons/" + resource).toUri().toString()));
  }

  public static Cursor topCenterCursor(String res) {
    Image image = icon(res);
    return new ImageCursor(image, image.getWidth() / 2, 0);
  }

  public static Cursor cornerCursor(String res) {
    Image image = icon(res);
    return new ImageCursor(image, 0, 0);
  }

  public static Pair<Node, SimpleObjectProperty<Integer>> nonlinearSlider(
      int min, int max, int precision, int divide) {
    Slider slider = new Slider();
    slider.setMin(0);
    slider.setMax(1);
    HBox.setHgrow(slider, Priority.ALWAYS);

    TextField text = new TextField();
    text.setMinWidth(50);
    text.setPrefWidth(50);
    HBox.setHgrow(text, Priority.NEVER);

    HBox out = new HBox();
    out.setSpacing(3);
    out.setAlignment(Pos.CENTER_LEFT);
    out.getChildren().addAll(text, slider);

    double range = max - min;
    SimpleObjectProperty<Integer> value = new SimpleObjectProperty<>(0);

    slider.setUserData(
        CustomBinding.bindBidirectional(
            new PropertyBinder<>(value),
            new PropertyBinder<>(slider.valueProperty())
                .<Integer>bimap(
                    n -> opt(n).map(v1 -> (int) (Math.pow(v1.doubleValue(), 2) * range + min)),
                    v2 -> opt(Math.pow((v2 - min) / range, 0.5)))));
    DecimalFormat textFormat = new DecimalFormat();
    textFormat.setMaximumFractionDigits(precision);
    text.setUserData(
        CustomBinding.bindBidirectional(
            new PropertyBinder<>(value),
            new PropertyBinder<>(text.textProperty())
                .bimap(
                    v -> {
                      try {
                        return opt((int) (Double.parseDouble(v) * divide));
                      } catch (NumberFormatException e) {
                        return Optional.empty();
                      }
                    },
                    v -> opt(textFormat.format((double) v.intValue() / divide)))));

    return new Pair<>(out, value);
  }

  public static MenuItem menuItem(String icon) {
    return new MenuItem(null, new ImageView(icon(icon)));
  }

  public static MenuButton menuButton(String icon) {
    return new MenuButton(null, new ImageView(icon(icon)));
  }

  public static Label title(String title) {
    Label out = new Label(title);
    out.getStyleClass().add("h2");
    return out;
  }

  public static Image toImage(PaletteImage image, PaletteColors palette) {
    final int width = image.getWidth();
    final int height = image.getHeight();
    return uncheck(
        () ->
            imageConstructor.newInstance(
                com.sun.prism.Image.fromByteBgraPreData(
                    image.dataPremultiplied(palette), width, height)));
  }

  public static Button button(String icon, String hint) {
    Button out = new Button(null, new ImageView(icon(icon)));
    Tooltip.install(out, new Tooltip(hint));
    return out;
  }

  public static ToggleButton toggleButton(String icon, String hint) {
    ToggleButton out = new ToggleButton(null, new ImageView(icon(icon)));
    Tooltip.install(out, new Tooltip(hint));
    return out;
  }

  public static Image toImage(TrueColorImage image) {
    final int width = image.getWidth();
    final int height = image.getHeight();
    return uncheck(
        () ->
            imageConstructor.newInstance(
                com.sun.prism.Image.fromByteBgraPreData(image.dataPremultiplied(), width, height)));
  }

  public static Image toImage(TrueColorImage image, TrueColor tint) {
    final int width = image.getWidth();
    final int height = image.getHeight();
    return uncheck(
        () ->
            imageConstructor.newInstance(
                com.sun.prism.Image.fromByteBgraPreData(
                    image.dataPremultipliedTint(tint.r, tint.g, tint.b), width, height)));
  }

  public static void exceptionPopup(
      Stage stage, Throwable e, String message, String shortDescription) {
    Alert alert = new Alert(Alert.AlertType.ERROR);
    if (stage != null) alert.initOwner(stage);
    alert.setTitle(String.format(localization.getString("s.error"), Global.getNameHuman()));
    alert.setHeaderText(message);
    alert.setContentText(shortDescription);
    TextArea textArea = new TextArea(Throwables.getStackTraceAsString(e));
    textArea.setEditable(false);
    textArea.setWrapText(true);
    textArea.setMaxWidth(Double.MAX_VALUE);
    textArea.setMaxHeight(Double.MAX_VALUE);
    alert.getDialogPane().setExpandableContent(textArea);
    alert.showAndWait();
  }

  public static void switchStage(
      Stage primaryStage,
      String title,
      Scene scene,
      boolean max,
      EventHandler<WindowEvent> onClose) {
    if (primaryStage.getScene() instanceof ClosableScene)
      ((ClosableScene) primaryStage.getScene()).close();
    primaryStage.hide(); // setMaximized only works if stage hidden first _(´ཀ`」 ∠)_
    primaryStage.setTitle(title);
    primaryStage.setOnCloseRequest(onClose);
    primaryStage.setScene(scene);
    primaryStage.show();
    primaryStage.setMaximized(max);
    primaryStage.sizeToScene();
  }

  public static class IconToggleButton extends ToggleButton {
    public IconToggleButton(String icon, String hint) {
      super(null, new ImageView(icon(icon)));
      Tooltip.install(this, new Tooltip(hint));
    }
  }
}
