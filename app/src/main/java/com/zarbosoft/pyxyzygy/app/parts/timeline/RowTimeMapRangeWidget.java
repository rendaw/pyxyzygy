package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.scene.transform.Affine;

import java.awt.*;

import static com.zarbosoft.pyxyzygy.app.Global.NO_INNER;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.c;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.icon;
import static com.zarbosoft.rendaw.common.Common.aeq;

public class RowTimeMapRangeWidget {
  static final javafx.scene.paint.Color outFill = c(new Color(157, 157, 157));
  static final javafx.scene.paint.Color inFill = c(new Color(220, 255, 192));
  static final javafx.scene.paint.Color inStroke = c(new Color(0, 0, 0));
  static final Image loopIcon = icon("loop-handle.png");

  final Pane base = new Pane();

  final Rectangle frameMarker = new Rectangle(Timeline.baseSize, Timeline.baseSize * 3);

  private final Rectangle background = new Rectangle(0, Math.ceil(Timeline.baseSize * 0.8));
  private final Rectangle inBackground = new Rectangle(0, Timeline.baseSize);

  private final Group alignment = new Group();

  private final Group outerA = new Group();
  private final Group outerB = new Group();
  private final ImageView stopMark = new ImageView(icon("no-inner.png"));
  private final Rectangle stopSeparator;

  private final Group inner = new Group();

  private final InBorder inBorder = new InBorder();
  private final Label inTime = new Label();
  private final Label loopTime = new Label();
  private final Timeline timeline;

  private DoubleVector dragMouseStart;
  private int dragFrameStart;
  private TimeRangeAdapter adapter;

  static class InBorder extends Canvas {
    static final double margin = 5;
    static final double round = Timeline.baseSize * 0.2;
    private double pad;
    private DoubleVector cursor = new DoubleVector(0, 0);
    private GraphicsContext gc = getGraphicsContext2D();
    private double baseX;
    private double baseY;

    {
      setHeight(Timeline.baseSize * 3 + margin * 2);
    }

    public void setX(double x) {
      baseX = x;
      setLayoutX(x - margin - pad);
    }

    public void setY(double y) {
      baseY = y;
      setLayoutY(y - margin);
    }

    public void draw(Timeline timeline, int start, int length) {
      pad = Timeline.baseSize * 0.2 + (Timeline.baseSize - timeline.zoom) * 0.5;
      setX(baseX);
      setY(baseY);
      if (start == NO_INNER) {
        setWidth(Timeline.baseSize + margin * 2 + pad * 2);
      } else {
        if (length == 0) {
          setWidth(Timeline.baseSize * 2 + margin * 2 + pad * 2);
        } else {
          setWidth((length + 1) * timeline.zoom + margin * 2 + pad * 2);
        }
      }
      GraphicsContext gc = getGraphicsContext2D();
      gc.setGlobalAlpha(1);
      gc.setTransform(new Affine());
      gc.clearRect(0, 0, getWidth(), getHeight());
      gc.translate(margin + pad, Timeline.baseSize + margin);
      figure(timeline, start, length, true);
      gc.setFill(inFill);
      gc.fill();
      figure(timeline, start, length, false);
      gc.setStroke(inStroke);
      gc.stroke();

      if (start != NO_INNER) {
        if (length == 0) {
          gc.setGlobalAlpha(0.5);
          gc.drawImage(
              loopIcon,
              0,
              0,
              loopIcon.getWidth(),
              loopIcon.getHeight(),
              0,
              Timeline.baseSize,
              loopIcon.getWidth(),
              loopIcon.getHeight());
          gc.setGlobalAlpha(1);
        } else {
          gc.drawImage(
              loopIcon,
              0,
              0,
              loopIcon.getWidth(),
              loopIcon.getHeight(),
              length * timeline.zoom + pad,
              Timeline.baseSize + round * 0.5,
              loopIcon.getWidth(),
              loopIcon.getHeight());
        }
      }
    }

    private void figure(Timeline timeline, int start, int length, boolean fill) {
      gc.beginPath();
      jump(-pad, timeline.zoom * 0.5);
      seg(timeline.zoom * 0.5 - Timeline.baseSize * 0.3, 0);
      line(timeline.zoom * 0.5, -Timeline.baseSize * 0.5);
      line(timeline.zoom * 0.5 + Timeline.baseSize * 0.3, 0);

      if (start == NO_INNER) {
        seg(timeline.zoom + pad, Timeline.baseSize * 0.5);
        seg(timeline.zoom * 0.5, Timeline.baseSize);
      } else {
        if (length == 0) {
          seg(Timeline.baseSize * 2, 0);
          if (fill) line(Timeline.baseSize * 2, Timeline.baseSize);
          else jump(Timeline.baseSize * 2, Timeline.baseSize);
          line(timeline.zoom + pad, Timeline.baseSize);
          seg(timeline.zoom * 0.5, Timeline.baseSize * 2);
        } else {
          seg(length * timeline.zoom + pad, Timeline.baseSize * 0.5);
          cursor =
              new DoubleVector(
                  length * timeline.zoom + Timeline.baseSize * 0.5 + pad,
                  Timeline.baseSize + round);
          gc.arcTo(length * timeline.zoom + pad, cursor.y, cursor.x, cursor.y, round * 2);
          seg(
              length * timeline.zoom + Timeline.baseSize + pad,
              Timeline.baseSize + Timeline.baseSize * 0.5);
          seg(
              length * timeline.zoom + Timeline.baseSize * 0.5 + pad,
              Timeline.baseSize + Timeline.baseSize);
          seg(length * timeline.zoom + pad, Timeline.baseSize + Timeline.baseSize * 0.5);
          line(length * timeline.zoom + pad, Timeline.baseSize);
        }
      }
      seg(-pad, Timeline.baseSize * 0.5);
    }

    private void jump(double x, double y) {
      gc.moveTo(x, y);
      cursor = new DoubleVector(x, y);
    }

    private void seg(double x, double y) {
      if (aeq(x, cursor.x) || aeq(y, cursor.y)) {
        line(x, y);
        return;
      } else if (x > cursor.x) {
        if (y < cursor.y) {
          gc.arcTo(cursor.x, y, x, y, round);
          // gc.lineTo(cursor.x, y);
          gc.lineTo(x, y);
        } else {
          gc.arcTo(x, cursor.y, x, y, round);
          // gc.lineTo(x, cursor.y);
          gc.lineTo(x, y);
        }
      } else {
        if (y < cursor.y) {
          gc.arcTo(x, cursor.y, x, y, round);
          // gc.lineTo(x, cursor.y);
          gc.lineTo(x, y);
        } else {
          gc.arcTo(cursor.x, y, x, y, round);
          // gc.lineTo(cursor.x, y);
          gc.lineTo(x, y);
        }
      }
      cursor = new DoubleVector(x, y);
    }

    private void line(double x, double y) {
      gc.lineTo(x, y);
      cursor = new DoubleVector(x, y);
    }
  }

  public RowTimeMapRangeWidget(ProjectContext context, Timeline timeline) {
    final double pad = 50;
    this.timeline = timeline;
    base.setMinHeight(Timeline.baseSize * 3);
    base.setPrefHeight(base.getMinHeight());
    base.setMaxHeight(base.getMinHeight());

    background.setFill(javafx.scene.paint.Color.TRANSPARENT);
    background.setStrokeType(StrokeType.INSIDE);
    background.setStroke(javafx.scene.paint.Color.GRAY);
    background.setBlendMode(BlendMode.MULTIPLY);
    background.widthProperty().bind(base.widthProperty().add(pad * 2));
    background.setLayoutX(-pad);
    background.setLayoutY((int) (Timeline.baseSize + Timeline.baseSize * 0.1));

    frameMarker.setFill(Timeline.frameMarkerColor);
    frameMarker.setBlendMode(BlendMode.MULTIPLY);

    inBackground.setFill(inFill);
    inBackground.setStroke(inStroke);
    inBackground.widthProperty().bind(base.widthProperty().add(pad * 2));
    inBackground
        .layoutXProperty()
        .bind(
            Bindings.createDoubleBinding(
                () -> Math.max(-pad, alignment.getLayoutX() + inner.getLayoutX()),
                inner.layoutXProperty(),
                alignment.layoutXProperty()));
    inBackground.setLayoutY(Timeline.baseSize);

    inBorder.setY(0);
    inBorder.setX(0);
    inTime.setLayoutX(Timeline.baseSize);
    inTime.setAlignment(Pos.BOTTOM_LEFT);
    inTime.setMinHeight(Timeline.baseSize);
    inTime.setMaxHeight(Timeline.baseSize);
    inTime.setTextFill(javafx.scene.paint.Color.BLACK);
    inTime.setPadding(new Insets(0, 0, 2, 2));
    loopTime.setLayoutY(Timeline.baseSize * 2);
    loopTime.setAlignment(Pos.TOP_LEFT);
    loopTime.setMinHeight(Timeline.baseSize);
    loopTime.setMaxHeight(Timeline.baseSize);
    loopTime.setTextFill(javafx.scene.paint.Color.BLACK);
    loopTime.setPadding(new Insets(2, 0, 0, 2));

    stopMark.setLayoutX(-Timeline.baseSize * 0.5 - stopMark.getImage().getWidth() * 0.5);
    stopMark.setLayoutY(
        Timeline.baseSize + Timeline.baseSize * 0.5 - stopMark.getImage().getHeight() * 0.5);
    stopSeparator = new Rectangle(1, Timeline.baseSize * 0.7);
    stopSeparator.setLayoutX(0);
    stopSeparator.setLayoutY(Timeline.baseSize + Timeline.baseSize * 0.15);
    outerA.getChildren().addAll(stopSeparator);
    outerB.getChildren().addAll(stopMark);
    outerB.layoutXProperty().bind(outerA.layoutXProperty());
    inner.getChildren().addAll(inBorder, inTime, loopTime);

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
    alignment.getChildren().addAll(outerA, inner, outerB, frameMarker);

    base.getChildren().addAll(background, inBackground, alignment);

    base.setMouseTransparent(false);
    base.addEventFilter(
        MouseEvent.MOUSE_PRESSED,
        e -> {
          dragFrameStart = adapter.getInnerStart();
          dragMouseStart = getRelative(e.getSceneX(), e.getSceneY());
        });
    base.addEventFilter(
        MouseEvent.MOUSE_RELEASED,
        e -> {
          dragMouseStart = null;
        });
    base.addEventFilter(
        MouseEvent.MOUSE_DRAGGED,
        e -> {
          context.change(
              new ProjectContext.Tuple(adapter.getData(), "timemap"),
              change -> {
                if (dragMouseStart.y < Timeline.baseSize) {
                  // nop
                } else if (dragMouseStart.y < Timeline.baseSize * 2) {
                  DoubleVector dragAt = getRelative(e.getSceneX(), e.getSceneY());
                  double diff = dragAt.x - dragMouseStart.x;
                  int quantized = (int) (diff / timeline.zoom);
                  adapter.changeStart(change, Math.max(-1, dragFrameStart + -quantized));
                } else {
                  DoubleVector dragAt = getRelative(e.getSceneX(), e.getSceneY());
                  int quantized = (int) (dragAt.x / timeline.zoom);
                  adapter.changeLength(change, Math.max(0, quantized - adapter.getOuterAt()));
                }
              });
        });
  }

  private DoubleVector getRelative(double x, double y) {
    Point2D corner = alignment.localToScene(0, 0);
    return new DoubleVector(x - corner.getX(), y - corner.getY());
  }

  public void set(TimeRangeAdapter adapter) {
    this.adapter = adapter;

    outerA.setLayoutX((adapter.getOuterAt() - adapter.getInnerStart()) * timeline.zoom);

    inner.setLayoutX(adapter.getOuterAt() * timeline.zoom);
    inBorder.draw(timeline, adapter.getInnerStart(), adapter.getInnerLength());

    if (adapter.getInnerStart() == NO_INNER) {
      inTime.setVisible(false);
      loopTime.setVisible(false);
      inBackground.setVisible(false);
    } else {
      inTime.setVisible(true);
      inTime.setText(Integer.toString(adapter.getInnerStart()));
      if (adapter.getInnerLength() == 0) {
        loopTime.setVisible(false);
        inBackground.setVisible(true);
      } else {
        loopTime.setVisible(true);
        loopTime.setLayoutX((adapter.getInnerLength() + 2) * timeline.zoom);
        loopTime.setText(Integer.toString(adapter.getInnerStart() + adapter.getInnerLength()));
        inBackground.setVisible(false);
      }
    }
  }

  public void updateFrameMarker(Window window) {
    if (window.getSelectedForView() == null) return;
    frameMarker.setLayoutX(timeline.frame.getValue() * timeline.zoom);
  }
}
