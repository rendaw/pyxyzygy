package com.zarbosoft.pyxyzygy.app.widgets;

import javafx.scene.ImageCursor;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;

public class CircleCursor extends ImageCursor {
	final static double thickness = 1;

	final public static ImageCursor create(double size) {
		Canvas c = new Canvas();
		c.setHeight(size + thickness + 1);
		c.setWidth(c.getHeight());
		GraphicsContext g = c.getGraphicsContext2D();
		g.setLineWidth(thickness);
		g.setStroke(Color.rgb(0, 0, 0, 0.5));
		g.strokeArc(c.getHeight() / 2, c.getHeight() / 2, size / 2, size / 2, 0, 360, ArcType.OPEN);
		SnapshotParameters p = new SnapshotParameters();
		p.setFill(Color.TRANSPARENT);
		return new ImageCursor(c.snapshot(p, null), c.getHeight() / 2, c.getHeight() / 2);
	}
}
