package com.zarbosoft.pyxyzygy.app.widgets;

import com.zarbosoft.pyxyzygy.app.widgets.binding.BinderRoot;
import com.zarbosoft.pyxyzygy.app.widgets.binding.DoubleHalfBinder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.PropertyHalfBinder;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;

public class TrueColorPicker extends GridPane {
  public final DoubleProperty hue = new SimpleDoubleProperty(0);
  public final DoubleProperty sat = new SimpleDoubleProperty(0);
  public final DoubleProperty bright = new SimpleDoubleProperty(0);
  public final DoubleProperty alpha = new SimpleDoubleProperty(1);
  private final BinderRoot hueBarBackgroundRoot; // GC root
  private final BinderRoot alphaBarBackgroundRoot; // GC root
  private final BinderRoot bindStyleTrueDisabledRoot; // GC root
  private final BinderRoot bindStyleEmptyRoot; // GC root

  public boolean suppressProxyListeners;
  public final SimpleObjectProperty<Color> colorProxyProperty = new SimpleObjectProperty<>();

  private final Region sliceCursor;
  private final Region alphaBarMarker;

  public TrueColorPicker() {
    PropertyHalfBinder<Boolean> disableBinder = new PropertyHalfBinder<>(disableProperty());
    PropertyHalfBinder<Color> colorBinder = new PropertyHalfBinder<>(colorProxyProperty);
    DoubleHalfBinder<Boolean, Color> colorDisableBinder =
        new DoubleHalfBinder<>(disableBinder, colorBinder);

    bindStyleTrueDisabledRoot =
        HelperJFX.bindStyle(
            this,
            "true-disabled",
            colorDisableBinder.map((disable, color) -> opt(disable || color == null)));
    bindStyleEmptyRoot =
        HelperJFX.bindStyle(this, "empty", colorBinder.map(color -> opt(color == null)));

    ColorSwatch newColorDisplay = new ColorSwatch(1);
    newColorDisplay.colorProperty.bind(colorProxyProperty);
    newColorDisplay.disableProperty().bind(disableProperty());

    // Hue bar - select a hue for the slice
    Pane hueBar = new Pane();
    hueBar.getStyleClass().add("hue-bar");
    this.hueBarBackgroundRoot =
        colorDisableBinder.addListener(
            (disable, color) -> {
              if (disable || color == null) {
                hueBar.setBackground(null);
              } else {
                double offset;
                Stop[] stops = new Stop[255];
                for (int x = 0; x < 255; x++) {
                  offset = (double) ((1.0 / 255) * x);
                  int h = (int) ((x / 255.0) * 360);
                  stops[x] = new Stop(offset, Color.hsb(h, 1.0, 1.0));
                }
                hueBar.setBackground(
                    new Background(
                        new BackgroundFill(
                            new LinearGradient(0f, 0f, 1f, 0f, true, CycleMethod.NO_CYCLE, stops),
                            CornerRadii.EMPTY,
                            Insets.EMPTY)));
              }
            });
    Region hueBarMarker = new Region();
    hueBarMarker.setId("hue-bar-cursor");
    hueBarMarker.setMouseTransparent(true);
    hueBarMarker.setCache(true);
    hueBarMarker.layoutXProperty().bind(hue.divide(360.0).multiply(hueBar.widthProperty()));
    hueBar.getChildren().setAll(hueBarMarker);

    // Alpha bar
    Pane alphaBarColorLayer = new Pane();
    this.alphaBarBackgroundRoot =
        colorBinder.addListener(
            color -> {
              if (color == null) alphaBarColorLayer.setBackground(null);
              else
                alphaBarColorLayer.setBackground(
                    new Background(
                        new BackgroundFill(
                            new LinearGradient(
                                0f,
                                0f,
                                0f,
                                1f,
                                true,
                                CycleMethod.NO_CYCLE,
                                new Stop(
                                    0,
                                    Color.hsb(
                                        color.getHue(),
                                        color.getSaturation(),
                                        color.getBrightness(),
                                        1.0)),
                                new Stop(
                                    1,
                                    Color.hsb(
                                        color.getHue(),
                                        color.getSaturation(),
                                        color.getBrightness(),
                                        0.0))),
                            CornerRadii.EMPTY,
                            Insets.EMPTY)));
            });
    Pane alphaBarAlphaLayer = new Pane();
    alphaBarAlphaLayer.getStyleClass().addAll("true-color-transparent-pattern");
    Pane alphaBar = new StackPane();
    alphaBar.getStyleClass().addAll("alpha-bar");
    alphaBarMarker = new Region();
    alphaBarMarker.setId("alpha-bar-cursor");
    alphaBarMarker.setManaged(false);
    alphaBarMarker.setMouseTransparent(true);
    alphaBarMarker.setCache(true);
    alphaBarMarker
        .layoutYProperty()
        .bind(Bindings.subtract(1.0, alpha).multiply(alphaBar.heightProperty()));
    alphaBar.getChildren().setAll(alphaBarAlphaLayer, alphaBarColorLayer, alphaBarMarker);

    // Slice, chooses saturation and brightness
    Pane sliceLayerColor = new Pane();
    sliceLayerColor.setMouseTransparent(true);
    sliceLayerColor.getStyleClass().addAll("slice", "slice-fill");
    sliceLayerColor
        .backgroundProperty()
        .bind(
            Bindings.createObjectBinding(
                () -> {
                  return new Background(
                      new BackgroundFill(
                          Color.hsb(hue.getValue(), 1.0, 1.0), CornerRadii.EMPTY, Insets.EMPTY));
                },
                hue));

    Pane sliceLayerWhite = new Pane();
    sliceLayerWhite.setMouseTransparent(true);
    sliceLayerWhite.getStyleClass().addAll("slice", "slice-fill");
    sliceLayerWhite.setBackground(
        new Background(
            new BackgroundFill(
                new LinearGradient(
                    0,
                    0,
                    1,
                    0,
                    true,
                    CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(255, 255, 255, 1)),
                    new Stop(1, Color.rgb(255, 255, 255, 0))),
                CornerRadii.EMPTY,
                Insets.EMPTY)));

    Pane sliceLayerBlack = new Pane();
    sliceLayerBlack.setMouseTransparent(true);
    sliceLayerBlack.getStyleClass().addAll("slice", "slice-fill");
    sliceLayerBlack.setBackground(
        new Background(
            new BackgroundFill(
                new LinearGradient(
                    0,
                    0,
                    0,
                    1,
                    true,
                    CycleMethod.NO_CYCLE,
                    new Stop(0, Color.rgb(0, 0, 0, 0)),
                    new Stop(1, Color.rgb(0, 0, 0, 1))),
                CornerRadii.EMPTY,
                Insets.EMPTY)));

    Pane sliceLayerAlpha = new Pane();
    sliceLayerAlpha.setMouseTransparent(true);
    sliceLayerAlpha.getStyleClass().addAll("slice", "slice-fill", "true-color-transparent-pattern");

    final StackPane sliceOpaqueLayers = new StackPane();
    sliceOpaqueLayers.opacityProperty().bind(alpha);
    sliceOpaqueLayers.getChildren().setAll(sliceLayerColor, sliceLayerWhite, sliceLayerBlack);

    Pane sliceBorder = new Pane();
    sliceBorder.setMouseTransparent(true);
    sliceBorder.getStyleClass().addAll("slice", "slice-border");

    sliceCursor = new Region();
    sliceCursor.setId("slice-cursor");
    sliceCursor.setManaged(false);
    sliceCursor.setMouseTransparent(true);
    sliceCursor.setCache(true);

    StackPane slice = new StackPane();
    VBox.setVgrow(slice, Priority.SOMETIMES);
    slice.getChildren().setAll(sliceLayerAlpha, sliceOpaqueLayers, sliceBorder, sliceCursor);

    sliceCursor.layoutXProperty().bind(sat.multiply(slice.widthProperty()));
    sliceCursor
        .layoutYProperty()
        .bind(Bindings.subtract(1.0, bright).multiply(slice.heightProperty()));

    // Assemble
    getStyleClass().add("true-color-picker");
    addRow(0, hueBar, newColorDisplay);
    addRow(1, slice, alphaBar);

    // Event handling
    EventHandler<MouseEvent> hueBarMouseHandler =
        event -> {
          hue.set(clamp(event.getX() / hueBar.getWidth()) * 360);
        };
    hueBar.setOnMouseDragged(hueBarMouseHandler);
    hueBar.setOnMousePressed(hueBarMouseHandler);

    EventHandler<MouseEvent> alphaBarMouseHandler =
        event -> {
          alpha.set(1.0 - clamp(event.getY() / alphaBar.getHeight()));
        };
    alphaBar.setOnMouseDragged(alphaBarMouseHandler);
    alphaBar.setOnMousePressed(alphaBarMouseHandler);

    EventHandler<MouseEvent> sliceMouseHandler =
        event -> {
          sat.set(clamp(event.getX() / slice.getWidth()));
          bright.set(1.0 - clamp(event.getY() / slice.getHeight()));
        };
    sliceOpaqueLayers.setOnMouseDragged(sliceMouseHandler);
    sliceOpaqueLayers.setOnMousePressed(sliceMouseHandler);

    colorProxyProperty.addListener(
        (observable, oldValue, newValue) -> {
          if (suppressProxyListeners) return;
          suppressProxyListeners = true;
          try {
            if (newValue == null) return;
            hue.set(newValue.getHue());
            sat.set(newValue.getSaturation());
            bright.set(newValue.getBrightness());
            alpha.set(newValue.getOpacity());
          } finally {
            suppressProxyListeners = false;
          }
        });
    final ChangeListener<Number> hsbProxyListener =
        new ChangeListener<Number>() {
          {
            changed(null, null, null);
          }

          @Override
          public void changed(
              ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            if (suppressProxyListeners) return;
            suppressProxyListeners = true;
            try {
              Color out = Color.hsb(hue.get(), sat.get(), bright.get(), alpha.get());
              colorProxyProperty.set(out);
            } finally {
              suppressProxyListeners = false;
            }
          }
        };
    hue.addListener(hsbProxyListener);
    sat.addListener(hsbProxyListener);
    bright.addListener(hsbProxyListener);
    alpha.addListener(hsbProxyListener);
  }

  @Override
  protected void layoutChildren() {
    super.layoutChildren();
    sliceCursor.autosize();
    alphaBarMarker.autosize();
  }

  static double clamp(double value) {
    return Math.max(0, Math.min(1, value));
  }
}
