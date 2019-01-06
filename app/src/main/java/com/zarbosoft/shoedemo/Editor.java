package com.zarbosoft.shoedemo;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;

import java.awt.*;

import static com.zarbosoft.shoedemo.Timeline.c;

public class Editor {
	private final ProjectContext context;
	private DoubleVector scroll = new DoubleVector(0, 0);
	public final Pane canvas;
	private final Group canvasInner;

	/**
	 * Returns a vector from a JavaFX canvas point relative to the image origin in image pixels
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	private DoubleVector getStandardVector(double x, double y) {
		return new DoubleVector((x - this.canvas.getLayoutBounds().getWidth() / 2) - scroll.x,
				(y - this.canvas.getLayoutBounds().getHeight() / 2) - scroll.y
		);
	}

	private DoubleVector normalizeEventCoordinates(MouseEvent e) {
		Point2D canvasCorner = canvas.getLocalToSceneTransform().transform(0, 0);
		return getStandardVector(e.getSceneX() - canvasCorner.getX(), e.getSceneY() - canvasCorner.getY());
	}

	public DoubleRectangle viewBounds() {
		return new BoundsBuilder()
				.circle(getStandardVector(0, 0), 0)
				.circle(getStandardVector(canvas.getLayoutBounds().getWidth(), canvas.getLayoutBounds().getHeight()), 0)
				.build();
	}

	public void updateScroll(DoubleVector scroll) {
		DoubleRectangle oldBounds = viewBounds();
		this.scroll = scroll;
		canvasInner.setLayoutX(scroll.x + canvas.widthProperty().get() / 2);
		canvasInner.setLayoutY(scroll.y + canvas.heightProperty().get() / 2);
		DoubleRectangle newBounds = viewBounds();
		if (context.selectedForView.get() != null)
			context.selectedForView.get().scroll(context, oldBounds, newBounds);
	}

	public void updateScroll() {
		updateScroll(scroll);
	}

	public Editor(ProjectContext context) {
		this.context = context;
		canvas = new Pane();
		canvas.setBackground(new Background(new BackgroundFill(c(new Color(99, 80, 97)), CornerRadii.EMPTY, Insets.EMPTY)));
		canvas.setFocusTraversable(true);
		canvasInner = new Group();
		canvas.getChildren().add(canvasInner);
		ChangeListener<Number> onResize = (observable, oldValue, newValue) -> updateScroll();
		canvas.widthProperty().addListener(onResize);
		canvas.heightProperty().addListener(onResize);

		class EventState {
			DoubleVector previous;
			DoubleVector lastClick;
			DoubleVector startScroll;
			DoubleVector startScrollClick;
		}
		EventState eventState = new EventState();
		canvas.addEventFilter(MouseEvent.ANY, e -> e.consume());
		canvas.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
			canvas.requestFocus();
			eventState.lastClick = eventState.previous = normalizeEventCoordinates(e);
			eventState.startScroll = scroll;
			eventState.startScrollClick = new DoubleVector(e.getSceneX(), e.getSceneY());
			if (e.getButton() == MouseButton.PRIMARY) {
				Wrapper edit = context.selectedForEdit.get();
				if (edit != null) {
					edit.markStart(context,eventState.previous);
					edit.mark(context, eventState.previous, eventState.previous);
				}
			}
		});
		canvas.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
			if (e.getButton() == MouseButton.PRIMARY)
				this.context.history.finishChange();
		});
		canvas.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
			DoubleVector end = normalizeEventCoordinates(e);
			if (e.getButton() == MouseButton.PRIMARY) {
				Wrapper edit = context.selectedForEdit.get();
				if (edit != null) {
					edit.mark(context, eventState.previous, end);
				}
			} else if (e.getButton() == MouseButton.MIDDLE) {
				updateScroll(eventState.startScroll.plus(new DoubleVector(e.getSceneX(),
						e.getSceneY()
				).minus(eventState.startScrollClick)));
			}
			eventState.previous = end;
		});

		context.selectedForView.addListener((observable, oldValue, newValue) -> {
			if (oldValue != null) {
				canvasInner.getChildren().remove(oldValue.getCanvas());
				oldValue.destroyCanvas();
			}
			updateScroll();
			canvasInner.getChildren().add(newValue.buildCanvas(context, viewBounds()));
		});
	}

	public Node getWidget() {
		return canvas;
	}
}
