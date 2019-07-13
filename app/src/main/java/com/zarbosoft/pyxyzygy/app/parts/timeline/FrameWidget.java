package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.Window;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.c;

public class FrameWidget extends Pane {
  private static final double sizePercent = 0.7;
  private static final double sizePercentComp = (1.0 - sizePercent) * 0.5;
  final Rectangle rectangle;
  private boolean dragged = false;
  double zoom;

  final RowFramesWidget row;
  int index;
  RowAdapterFrame frame;

  int absStart;
  int absEnd;
  SimpleIntegerProperty at = new SimpleIntegerProperty();

  public FrameWidget(Context context, Window window, RowFramesWidget row) {
    this.row = row;

    setWidth(Timeline.baseSize);
    setHeight(Timeline.baseSize);
    setMouseTransparent(false);

    rectangle = new Rectangle(0, Timeline.baseSize * sizePercent);
    rectangle.setStroke(c(new java.awt.Color(0, 0, 0)));
    rectangle.setFill(Color.TRANSPARENT);
    rectangle.setMouseTransparent(true);
    rectangle.setLayoutY(Timeline.baseSize * sizePercentComp);
    rectangle.setArcWidth(Timeline.baseSize * 0.4);
    rectangle.setArcHeight(rectangle.getArcWidth());
    getChildren().add(rectangle);

    addEventHandler(
        MouseEvent.MOUSE_PRESSED,
        e -> {
          dragged = false;
        });
    addEventHandler(
        MouseEvent.MOUSE_CLICKED,
        e -> {
          if (dragged) return;
          if (row.timeline.selectedFrame.get() == this) row.timeline.select(null);
          else row.timeline.select(this);
        });
    addEventHandler(
        MouseEvent.MOUSE_DRAGGED,
        e -> {
          double x = row.timeline.getTimelineX(e);
          int newAt = (int) (x / zoom);
          if (absEnd != -1) newAt = Math.min(newAt, absEnd - 1);
          newAt = Math.max(newAt, absStart);
          if (newAt == at.get()) return;
          dragged = true;
          this.frame.setAt(context, window, newAt);
        });
    deselect();
  }

  public void select() {
    rectangle.setFill(c(new java.awt.Color(52, 52, 52)));
  }

  public void deselect() {
    rectangle.setFill(Color.TRANSPARENT);
  }

  /**
   * The limits are because when working under a group the frame might be part of a loop and
   * dragging it outside the loop would cause it to disappear (in the current context). Prevent
   * dragging out of loops. Otherwise no rightward limits.
   *
   * @param index
   * @param frame
   * @param absStart Farthest left frame can be dragged
   * @param absEnd Farthest right frame can be dragged or NO_LENGTH
   * @param absAt Time of the frame
   */
  public void set(
      double zoom, int index, RowAdapterFrame frame, int absStart, int absEnd, int absAt) {
    this.zoom = zoom;
    rectangle.setWidth(zoom * sizePercent);
    rectangle.setLayoutX(zoom * sizePercentComp);
    this.index = index;
    this.frame = frame;
    this.absStart = absStart;
    this.absEnd = absEnd;
    at.set(absAt);
    setLayoutX(absAt * zoom);
  }
}
