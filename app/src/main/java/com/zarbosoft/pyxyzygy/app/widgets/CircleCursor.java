package com.zarbosoft.pyxyzygy.app.widgets;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.ImageCursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.io.File;

import static com.zarbosoft.rendaw.common.Common.uncheck;

public class CircleCursor extends ImageCursor {
	final static double thickness = 1;
	final public static ImageCursor create(double size) {
		Canvas c = new Canvas();
		c.setHeight(size + thickness + 1);
		c.setWidth(c.getHeight());
		GraphicsContext g = c.getGraphicsContext2D();
		g.setLineWidth(thickness);
		g.setStroke(Color.rgb(0, 0, 0, 0.5));
		g.strokeOval(0.5 + thickness * 0.5, 0.5 + thickness * 0.5, size, size);
		SnapshotParameters p = new SnapshotParameters();
		p.setFill(Color.TRANSPARENT);
		Image image = c.snapshot(p, null);
		uncheck(() -> ImageIO.write(SwingFXUtils.fromFXImage(image, null), "PNG", new File("cursor.png")));
		return new ImageCursor(image, c.getHeight() / 2, c.getHeight() / 2);
	}
}
