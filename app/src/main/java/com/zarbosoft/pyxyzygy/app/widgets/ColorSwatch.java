package com.zarbosoft.pyxyzygy.app.widgets;

import com.zarbosoft.javafxbinders.BinderRoot;
import com.zarbosoft.javafxbinders.DoubleHalfBinder;
import com.zarbosoft.javafxbinders.HalfBinder;
import com.zarbosoft.javafxbinders.PropertyHalfBinder;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.Optional;

import static com.zarbosoft.javafxbinders.CustomBinding.bindStyle;
import static com.zarbosoft.rendaw.common.Common.opt;

public class ColorSwatch extends StackPane {
  public final double gapScaler;
  public final SimpleObjectProperty<Color> colorProperty = new SimpleObjectProperty<>();

  @SuppressWarnings("unused")
  private final BinderRoot bindStyleRoot; // GC root

  private Node createClip() {
    class ClipRectangle extends Rectangle {
      @SuppressWarnings("unused")
      private final BinderRoot rootLayout; // GC root

      @SuppressWarnings("unused")
      private final BinderRoot rootArc; // GC root

      ClipRectangle() {
        HalfBinder<CornerRadii> radiiBinder =
            new DoubleHalfBinder<>(backgroundProperty(), borderProperty())
                .map(
                    (bg, border) -> {
                      if (bg != null) return opt(bg.getFills().get(0).getRadii());
                      if (border != null)
                        if (border.getStrokes().isEmpty()) return opt(CornerRadii.EMPTY);
                        else return opt(border.getStrokes().get(0).getRadii());
                      return opt(CornerRadii.EMPTY);
                    });
        HalfBinder<Double> borderWidthBinder =
            new PropertyHalfBinder<>(borderProperty())
                .map(
                    b0 ->
                        Optional.of(
                            Optional.ofNullable(b0)
                                .flatMap(
                                    b ->
                                        b.getStrokes().isEmpty()
                                            ? Optional.empty()
                                            : Optional.of(b.getStrokes().get(0)))
                                .map(b -> b.getWidths().getTop())
                                .orElse(0.)));
        rootArc =
            new DoubleHalfBinder<>(borderWidthBinder, radiiBinder)
                .addListener(
                    (w, r) -> {
                      this.setArcWidth(r.getTopLeftHorizontalRadius() * 2 - w * gapScaler);
                      this.setArcHeight(r.getTopLeftVerticalRadius() * 2 - w * gapScaler);
                    });
        rootLayout =
            new DoubleHalfBinder<>(borderWidthBinder, ColorSwatch.this.layoutBoundsProperty())
                .addListener(
                    (borderWidth, bounds) -> {
                      this.setWidth(bounds.getWidth() - borderWidth * 2.0 * gapScaler);
                      this.setHeight(bounds.getHeight() - borderWidth * 2.0 * gapScaler);
                      this.setLayoutX(bounds.getWidth() * 0.5 - this.getWidth() * 0.5);
                      this.setLayoutY(bounds.getHeight() * 0.5 - this.getHeight() * 0.5);
                    });
      }
    };
    return new ClipRectangle();
  }

  public ColorSwatch(double gapScaler) {
    PropertyHalfBinder<Color> colorBinder = new PropertyHalfBinder<>(colorProperty);

    bindStyleRoot = bindStyle(this, "empty", colorBinder.map(color -> opt(color == null)));

    this.gapScaler = gapScaler;
    getStyleClass().addAll("color-swatch");

    Pane bgLayer = new Pane();
    bgLayer.getStyleClass().add("true-color-transparent-pattern");
    bgLayer.setClip(createClip());

    final Pane colorLayer = new Pane();
    colorProperty.addListener(
        new ChangeListener<Color>() {
          {
            changed(null, null, colorProperty.get());
          }

          @Override
          public void changed(
              ObservableValue<? extends Color> observable, Color oldValue, Color newValue) {
            if (newValue == null) return;
            colorLayer.setBackground(
                new Background(new BackgroundFill(newValue, CornerRadii.EMPTY, Insets.EMPTY)));
          }
        });
    colorLayer.setClip(createClip());

    getChildren().addAll(bgLayer, colorLayer);

    disableProperty()
        .addListener(
            (observable, oldValue, newValue) -> {
              if (newValue) {
                colorLayer.setEffect(new ColorAdjust(0, -1, 0, 0));
              } else {
                colorLayer.setEffect(null);
              }
            });
    layoutBoundsProperty()
        .addListener(
            new ChangeListener<Bounds>() {
              {
                changed(null, null, layoutBoundsProperty().getValue());
              }

              @Override
              public void changed(
                  ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) {
                bgLayer.setMinWidth(newValue.getWidth());
                bgLayer.setMinHeight(newValue.getHeight());
                colorLayer.setMinWidth(newValue.getWidth());
                colorLayer.setMinHeight(newValue.getHeight());
              }
            });
  }
}
