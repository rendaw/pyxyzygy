package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.Window;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;

import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.c;

public class RowTimeRangeWidget {
  public static final SimpleIntegerProperty start = new SimpleIntegerProperty();
  public static final SimpleIntegerProperty length = new SimpleIntegerProperty();
  static final Color inFill = c(new java.awt.Color(203, 238, 255, 255));

  final Pane base = new Pane();
  private final Group alignment = new Group();

  final Rectangle frameMarker = new Rectangle(Timeline.baseSize, Timeline.baseSize);

  private final Group inner = new Group();
  private final Rectangle loopRange = new Rectangle();
  private final Rectangle loopHandle = new Rectangle();
  private final ImageView loopImage = new ImageView(RowTimeMapRangeWidget.loopIcon);

  private final Timeline timeline;
  private int mouseDragDivideStart;
  private int mouseDragQuantizeStart;

  public RowTimeRangeWidget(Timeline timeline) {
    this.timeline = timeline;
    base.setMinHeight(Timeline.baseSize);
    base.setPrefHeight(base.getMinHeight());
    base.setMaxHeight(base.getMinHeight());

    frameMarker.setFill(Timeline.frameMarkerColor);
    frameMarker.setBlendMode(BlendMode.MULTIPLY);

    loopRange.setHeight(Timeline.baseSize);
    loopRange.setFill(inFill);
    loopRange.setStroke(RowTimeMapRangeWidget.inStroke);
    loopRange.setStrokeType(StrokeType.OUTSIDE);
    loopRange.setArcHeight(RowTimeMapRangeWidget.InBorder.round);
    loopRange.setArcWidth(RowTimeMapRangeWidget.InBorder.round);

    loopHandle.setHeight(Math.floor(Timeline.baseSize * 0.9));
    loopHandle.setWidth(Math.ceil(Timeline.baseSize * 1.5));
    loopHandle.setStrokeType(StrokeType.OUTSIDE);
    loopHandle
        .layoutXProperty()
        .bind(
            Bindings.add(loopRange.layoutXProperty(), loopRange.widthProperty())
                .subtract(Timeline.baseSize * 0.5));
    loopHandle.setLayoutY(Timeline.baseSize * 0.5 - loopHandle.getHeight() / 2);
    loopHandle.setFill(Color.TRANSPARENT);
    loopHandle.setStroke(RowTimeMapRangeWidget.inStroke);
    loopHandle.setArcHeight(RowTimeMapRangeWidget.InBorder.round);
    loopHandle.setArcWidth(RowTimeMapRangeWidget.InBorder.round);

    loopImage.setLayoutY(Timeline.baseSize / 2 - loopImage.getImage().getHeight() / 2);
    loopImage
        .layoutXProperty()
        .bind(
            Bindings.add(loopRange.layoutXProperty(), loopRange.widthProperty())
                .add(Timeline.baseSize / 2 - loopImage.getImage().getWidth() / 2));

    inner.getChildren().addAll(loopHandle, loopRange, loopImage);

    alignment
        .layoutXProperty()
        .bind(
            Bindings.createDoubleBinding(
                () -> {
                  double corner = timeline.controlAlignment.localToScene(0, 0).getX();
                  return corner
                      - base.localToScene(0, 0).getX()
                      - timeline.timeScroll.getValue()
                      + Timeline.baseSize * 2;
                },
                base.localToSceneTransformProperty(),
                timeline.controlAlignment.localToSceneTransformProperty(),
                timeline.timeScroll.valueProperty()));
    alignment.getChildren().addAll(inner, frameMarker);

    base.getChildren().addAll(alignment);

    base.setMouseTransparent(false);
    base.addEventFilter(
        MouseEvent.MOUSE_PRESSED,
        e -> {
          DoubleVector dragAt = getRelative(e.getSceneX(), e.getSceneY());
          mouseDragQuantizeStart = (int) (dragAt.x / timeline.zoom);
          mouseDragDivideStart = start.get() + length.get();
        });
    base.addEventFilter(
        MouseEvent.MOUSE_DRAGGED,
        e -> {
          DoubleVector dragAt = getRelative(e.getSceneX(), e.getSceneY());
          int quantized = (int) (dragAt.x / timeline.zoom);
          if (mouseDragQuantizeStart < mouseDragDivideStart) {
            start.set(Math.max(0, quantized));
          } else {
            length.set(Math.max(1, quantized - start.get()));
          }
        });
    inner.layoutXProperty().bind(start.multiply(timeline.zoom));
    length.addListener(
        new ChangeListener<Number>() {
          {
            changed(null, null, length.get());
          }

          @Override
          public void changed(
              ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
            int newLength = newValue.intValue();
            loopRange.setWidth(newLength * timeline.zoom);
          }
        });
  }

  private DoubleVector getRelative(double x, double y) {
    Point2D corner = alignment.localToScene(0, 0);
    return new DoubleVector(x - corner.getX(), y - corner.getY());
  }

  public void updateFrameMarker(Window window) {
    if (window.getSelectedForView() == null) return;
    frameMarker.setLayoutX(timeline.time.getValue() * timeline.zoom);
  }
}
