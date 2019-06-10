package com.zarbosoft.pyxyzygy.app.widgets;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.ImageCursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.io.File;

import static com.zarbosoft.rendaw.common.Common.uncheck;

public class CircleCursor extends ImageCursor {
  static final double outerThickness = 1.4;
  static final double innerThickness = 1.4;
  static final double sumThickness = (outerThickness + innerThickness);

  private static void circle(GraphicsContext gc, double x, double y, double r) {
    gc.strokeOval(x - r, y - r, r * 2, r * 2);
  }

  public static final ImageCursor create(double size) {
    Canvas canvas = new Canvas();
    canvas.setHeight(size + sumThickness + 1);
    canvas.setWidth(canvas.getHeight());
    final double c = canvas.getHeight() / 2 + 0.5;
    final double r = size / 2;
    GraphicsContext g = canvas.getGraphicsContext2D();

    g.setGlobalBlendMode(BlendMode.SRC_OVER);

    g.setStroke(Color.rgb(255, 255, 255, 0.8));
    g.setLineWidth(outerThickness + innerThickness);
    circle(g, c, c, r + outerThickness - sumThickness * 0.5);

    g.setGlobalBlendMode(BlendMode.SRC_ATOP);

    g.setLineWidth(innerThickness);
    g.setStroke(Color.rgb(0, 0, 0, 1));
    circle(g, c, c, r - innerThickness * 0.5);

    SnapshotParameters p = new SnapshotParameters();
    p.setFill(Color.TRANSPARENT);
    Image image = canvas.snapshot(p, null);
    uncheck(
        () -> ImageIO.write(SwingFXUtils.fromFXImage(image, null), "PNG", new File("cursor.png")));
    return new ImageCursor(image, canvas.getHeight() / 2, canvas.getHeight() / 2);
  }
}
