package com.zarbosoft.shoedemo.parts.timeline;

import com.zarbosoft.shoedemo.DoubleVector;
import com.zarbosoft.shoedemo.ProjectContext;
import com.zarbosoft.shoedemo.Window;
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
import javafx.scene.transform.Affine;

import java.awt.*;

import static com.zarbosoft.rendaw.common.Common.aeq;
import static com.zarbosoft.shoedemo.Main.NO_INNER;
import static com.zarbosoft.shoedemo.Main.c;
import static com.zarbosoft.shoedemo.Window.icon;

public class RowTimeRangeWidget {
	private static final javafx.scene.paint.Color outFill = c(new Color(127, 127, 127));
	private static final javafx.scene.paint.Color outStroke = c(new Color(79, 79, 79));
	private static final javafx.scene.paint.Color inFill = c(new Color(255, 255, 255));
	private static final javafx.scene.paint.Color inStroke = c(new Color(0, 0, 0));
	private static final Image loopIcon = icon("loop-handle.svg");

	final Pane base = new Pane();

	final Rectangle frameMarker = new Rectangle(Timeline.baseSize, Timeline.baseSize * 3);

	private final Rectangle background = new Rectangle(0, Timeline.baseSize);
	private final Rectangle inBackground = new Rectangle(0, Timeline.baseSize);

	private final Group alignment = new Group();

	private final Group outerA = new Group();
	private final Group outerB = new Group();
	private final ImageView stopMark = new ImageView(icon("no-inner.svg"));
	private final Rectangle stopSeparator;

	private final Group inner = new Group();

	private final InBorder inBorder = new InBorder();
	private final Label inTime = new Label();
	private final Label loopTime = new Label();
	private final Timeline timeline;

	private DoubleVector dragMouseStart;
	private int dragFrameStart;
	private TimeRangeAdapter adapter;

	private static class InBorder extends Canvas {
		private final double margin = 5;
		private final double round = Timeline.baseSize * 0.2;
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
							loopIcon.getHeight()
					);
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
							loopIcon.getHeight()
					);
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
					if (fill)
						line(Timeline.baseSize * 2, Timeline.baseSize);
					else
						jump(Timeline.baseSize * 2, Timeline.baseSize);
					line(timeline.zoom + pad, Timeline.baseSize);
					seg(timeline.zoom * 0.5, Timeline.baseSize * 2);
				} else {
					seg(length * timeline.zoom + pad, Timeline.baseSize * 0.5);
					cursor = new DoubleVector(length * timeline.zoom + Timeline.baseSize * 0.5 + pad,
							Timeline.baseSize + round
					);
					gc.arcTo(length * timeline.zoom + pad, cursor.y, cursor.x, cursor.y, round * 2);
					seg(length * timeline.zoom + Timeline.baseSize + pad, Timeline.baseSize + Timeline.baseSize * 0.5);
					seg(length * timeline.zoom + Timeline.baseSize * 0.5 + pad, Timeline.baseSize + Timeline.baseSize);
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
					//gc.lineTo(cursor.x, y);
					gc.lineTo(x, y);
				} else {
					gc.arcTo(x, cursor.y, x, y, round);
					//gc.lineTo(x, cursor.y);
					gc.lineTo(x, y);
				}
			} else {
				if (y < cursor.y) {
					gc.arcTo(x, cursor.y, x, y, round);
					//gc.lineTo(x, cursor.y);
					gc.lineTo(x, y);
				} else {
					gc.arcTo(cursor.x, y, x, y, round);
					//gc.lineTo(cursor.x, y);
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

	public RowTimeRangeWidget(Timeline timeline) {
		final double pad = 50;
		this.timeline = timeline;
		base.setMinHeight(Timeline.baseSize * 3);
		base.setPrefHeight(base.getMinHeight());
		base.setMaxHeight(base.getMinHeight());

		background.setFill(outFill);
		background.setStroke(outStroke);
		background.widthProperty().bind(base.widthProperty().add(pad * 2));
		background.setLayoutX(-pad);
		background.setLayoutY(Timeline.baseSize);

		frameMarker.setFill(Timeline.frameMarkerColor);
		frameMarker.setBlendMode(BlendMode.MULTIPLY);

		inBackground.setFill(inFill);
		inBackground.setStroke(inStroke);
		inBackground.widthProperty().bind(base.widthProperty().add(pad * 2));
		inBackground
				.layoutXProperty()
				.bind(Bindings.createDoubleBinding(() -> Math.max(-pad, alignment.getLayoutX() + inner.getLayoutX()),
						inner.layoutXProperty(),
						alignment.layoutXProperty()
				));
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
		stopMark.setLayoutY(Timeline.baseSize + Timeline.baseSize * 0.5 - stopMark.getImage().getHeight() * 0.5);
		stopSeparator = new Rectangle(1, Timeline.baseSize * 0.7);
		stopSeparator.setLayoutX(0);
		stopSeparator.setLayoutY(Timeline.baseSize + Timeline.baseSize * 0.15);
		outerA.getChildren().addAll(stopSeparator);
		outerB.getChildren().addAll(stopMark);
		outerB.layoutXProperty().bind(outerA.layoutXProperty());
		inner.getChildren().addAll(inBorder, inTime, loopTime);

		alignment.layoutXProperty().bind(Bindings.createDoubleBinding(() -> {
					double corner = timeline.controlAlignment.localToScene(0, 0).getX();
					return corner - base.localToScene(0, 0).getX() - timeline.timeScroll.getValue() +
							Timeline.baseSize * 2;
				},
				base.localToSceneTransformProperty(),
				timeline.controlAlignment.localToSceneTransformProperty(),
				timeline.timeScroll.valueProperty()
		));
		alignment.getChildren().addAll(frameMarker, outerA, inner, outerB);

		base.getChildren().addAll(background, inBackground, alignment);

		base.setMouseTransparent(false);
		base.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
			dragFrameStart = adapter.getInnerStart();
			dragMouseStart = getRelative(e.getSceneX(), e.getSceneY());
		});
		base.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
			dragMouseStart = null;
		});
		base.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
			//System.out.format("dra y %s : ba %s\n", dragMouseStart.y, Timeline.baseSize);
			if (dragMouseStart.y < Timeline.baseSize) {
				// nop
			} else if (dragMouseStart.y < Timeline.baseSize * 2) {
				DoubleVector dragAt = getRelative(e.getSceneX(), e.getSceneY());
				double diff = dragAt.x - dragMouseStart.x;
				int quantized = (int) (diff / timeline.zoom);
				//System.out.format("drag main: start %s, at %s, quantized %s\n", dragMouseStart.x, dragAt.x, quantized);
				adapter.changeStart(Math.max(-1, dragFrameStart + -quantized));
			} else {
				DoubleVector dragAt = getRelative(e.getSceneX(), e.getSceneY());
				int quantized = (int) (dragAt.x / timeline.zoom);
				//System.out.format("drag end: start %s, quantized %s\n", dragMouseStart.x, quantized);
				adapter.changeLength(Math.max(0, quantized - adapter.getOuterAt()));
			}
		});
	}

	private DoubleVector getRelative(double x, double y) {
		Point2D corner = alignment.localToScene(0, 0);
		return new DoubleVector(x - corner.getX(), y - corner.getY());
	}

	public void set(TimeRangeAdapter adapter) {
		this.adapter = adapter;

		System.out.format("set at %s s %s l %s\n",
				adapter.getOuterAt(),
				adapter.getInnerStart(),
				adapter.getInnerLength()
		);
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
		if (window.selectedForView.get() == null)
			return;
		frameMarker.setLayoutX(window.selectedForView.get().frame.getValue() * timeline.zoom);
	}
}