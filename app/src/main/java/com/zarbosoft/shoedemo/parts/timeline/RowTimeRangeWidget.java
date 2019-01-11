package com.zarbosoft.shoedemo.parts.timeline;

import com.zarbosoft.shoedemo.DoubleVector;
import com.zarbosoft.shoedemo.Main;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.function.Consumer;

public class RowTimeRangeWidget {
	final Pane base = new Pane();

	private final Group inner = new Group();

	//private final Rectangle maxFrameBackground = new Rectangle();
	private final Canvas outBorder = new Canvas();
	private final Rectangle stopMark = new Rectangle(Timeline.baseSize / 2, Timeline.baseSize / 2);
	private final Rectangle stopSeparator = new Rectangle(2, Timeline.baseSize * 0.8);
	private final Canvas inBorder = new Canvas();
	private final Label inTime = new Label();
	private final Canvas loopMarker = new Canvas();
	private final Label loopTime = new Label();

	private Consumer<MouseEvent> dragHandler;
	private TimeRangeAdapter adapter;

	public RowTimeRangeWidget(Timeline timeline) {
		base.setMinHeight(Timeline.baseSize * 3);
		base.setPrefHeight(base.getMinHeight());
		base.setMaxHeight(base.getMinHeight());
		base.setBackground(new Background(new BackgroundFill(Main.c(new java.awt.Color(255, 255, 255)),
				CornerRadii.EMPTY,
				Insets.EMPTY
		)));

		inner.layoutXProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double corner = timeline.controlAlignment.localToScene(0, 0).getX();
					return corner - base.localToScene(0, 0).getX() - timeline.timeScroll.getValue();
				},
				base.localToSceneTransformProperty(),
				timeline.controlAlignment.localToSceneTransformProperty(),
				timeline.timeScroll.valueProperty()
		));

		/*
		maxFrameBackground.setArcHeight(3);
		maxFrameBackground.setArcWidth(3);
		maxFrameBackground.setHeight(Timeline.baseSize);
		maxFrameBackground.setFill(Timeline.c(new java.awt.Color(1, 2, 3)));
		*/

		{
			outBorder.setHeight(Timeline.baseSize);
			outBorder.widthProperty().bind(base.widthProperty());
			Runnable redraw = () -> {
				GraphicsContext gc = outBorder.getGraphicsContext2D();
				gc.clearRect(0, 0, outBorder.getWidth(), outBorder.getHeight());
				/*
				gc.setFill(c(new java.awt.Color(1, 2, 3)));
				gc.fillRect(0, 0, outBorder.getWidth(), baseSize);
				*/
				gc.setStroke(Main.c(new java.awt.Color(58, 55, 57)));
				gc.strokeLine(0, 0, outBorder.getWidth(), 0);
				gc.strokeLine(0, Timeline.baseSize, outBorder.getWidth(), Timeline.baseSize);
			};
			outBorder.widthProperty().addListener((observable, oldValue, newValue) -> redraw.run());
			redraw.run();
		}
		{
			inBorder.setHeight(Timeline.baseSize);
			inBorder.widthProperty().bind(base.widthProperty());
			Runnable redraw = () -> {
				GraphicsContext gc = inBorder.getGraphicsContext2D();
				double triHalf = Timeline.baseSize * 0.3;
				Runnable triangle = () -> {
					gc.beginPath();
					gc.moveTo(Timeline.baseSize * 0.5 - triHalf, 0);
					gc.lineTo(Timeline.baseSize * 0.5, -Timeline.baseSize);
					gc.lineTo(Timeline.baseSize * 0.5 + triHalf, 0);
					gc.closePath();
				};
				gc.clearRect(0, 0, inBorder.getWidth(), inBorder.getHeight());
				/*
				gc.setFill(c(new java.awt.Color(1, 2, 3)));
				gc.fillRect(0, 0, inBorder.getWidth(), baseSize);
				*/
				gc.setStroke(Main.c(new java.awt.Color(0, 0, 0)));
				gc.strokeRect(0, 0, inBorder.getWidth(), Timeline.baseSize);
				triangle.run();
				gc.setFill(Color.TRANSPARENT);
				gc.fill();
				triangle.run();
				gc.stroke();
			};
			inBorder.widthProperty().addListener((observable, oldValue, newValue) -> redraw.run());
			redraw.run();
		}
		{
			loopMarker.setWidth(Timeline.baseSize);
			loopMarker.setHeight(Timeline.baseSize * 2);
			GraphicsContext gc = loopMarker.getGraphicsContext2D();
			gc.setFill(Main.c(new java.awt.Color(33, 30, 24)));
			gc.fillRect(0, 0, 1.5, Timeline.baseSize);
			gc.fillRoundRect(0, Timeline.baseSize, Timeline.baseSize, Timeline.baseSize, 2, 2);
			gc.fillRect(0, Timeline.baseSize, Timeline.baseSize, Timeline.baseSize * 0.5);
		}
		stopMark.setLayoutX(-Timeline.baseSize * 0.5 - stopMark.getWidth());
		stopMark.setLayoutY(-Timeline.baseSize * 0.5 - stopMark.getHeight());
		stopSeparator.setLayoutX(0);
		stopSeparator.setLayoutY(0);

		inner
				.getChildren()
				.addAll(/*maxFrameBackground,*/ outBorder,
						stopMark,
						stopSeparator,
						inBorder,
						loopMarker,
						inTime,
						loopTime
				);
		base.getChildren().addAll(inner);

		base.setMouseTransparent(false);
		base.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
			DoubleVector dragStart = getRelative(e.getSceneX(), e.getSceneY());
			if (dragStart.y < Timeline.baseSize) {
				dragHandler = e2 -> {
					DoubleVector dragAt = getRelative(e2.getSceneX(), e2.getSceneY());
					double diff = (dragAt.x - dragStart.x);
					int quantized = (int) (diff / Timeline.baseSize);
					adapter.changeStart(Math.max(-1, quantized));
				};
			} else {
				dragHandler = e2 -> {
					DoubleVector dragAt = getRelative(e2.getSceneX(), e2.getSceneY());
					double diff = (dragAt.x - dragStart.x);
					int quantized = (int) (diff / Timeline.baseSize);
					adapter.changeLength(Math.max(0, quantized - adapter.getStart()));
				};
			}
		});
		base.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> dragHandler.accept(e));
	}

	private DoubleVector getRelative(double x, double y) {
		Point2D corner = base.localToScene(0, 0);
		return new DoubleVector(x - corner.getX(), y - corner.getY());
	}

	/*
	public void setMaxFrame(int maxFrame) {
		maxFrameBackground.setWidth(maxFrame * Timeline.baseSize);
	}
	*/

	public void set(TimeRangeAdapter adapter) {
		this.adapter = adapter;

		int start = adapter.getStart();
		int end = adapter.getLength();

		inBorder.setLayoutX(start * Timeline.baseSize);
		inTime.setLayoutX(start * Timeline.baseSize);
		loopMarker.setLayoutX((start + end) * Timeline.baseSize);
		loopTime.setLayoutX((start + end) * Timeline.baseSize);
		inTime.setText(Integer.toString(start));

		loopMarker.setLayoutX((start + end) * Timeline.baseSize);
		if (end == 0) {
			loopTime.setVisible(false);
		} else {
			loopTime.setVisible(true);
			loopTime.setLayoutX((start + end) * Timeline.baseSize);
			loopTime.setText(Integer.toString(start + end));
		}
	}
}
