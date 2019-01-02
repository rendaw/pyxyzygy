package com.zarbosoft.shoedemo;

import javafx.beans.value.ChangeListener;
import javafx.scene.Group;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

public class Editor {
	private final ProjectContext context;
	private DoubleVector scroll = new DoubleVector(0, 0);
	public final Pane canvas;
	private final Group canvasInner;
	private Main.Wrapper root;
	private Main.Wrapper edit;

	public void setNodes(Main.Wrapper root, Main.Wrapper edit) {
		if (this.root != null) {
			canvasInner.getChildren().remove(this.root.getCanvas());
			this.root.destroyCanvas();
		}
		this.root = root;
		updateScroll();
		canvasInner.getChildren().add(root.buildCanvas(this.context, viewBounds()));
		this.edit = edit;
	}

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
		root.scroll(context, oldBounds, newBounds);
	}

	public void updateScroll() {
		updateScroll(scroll);
	}

	public Editor(ProjectContext context) {
		this.context = context;
		canvas = new Pane();
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
			eventState.lastClick = eventState.previous = getStandardVector(e.getSceneX(), e.getSceneY());
			eventState.startScroll = scroll;
			eventState.startScrollClick = new DoubleVector(e.getSceneX(), e.getSceneY());
			if (e.getButton() == MouseButton.PRIMARY) {
				if (edit != null)
					edit.mark(context, eventState.previous, eventState.previous);
			}
		});
		canvas.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
			if (e.getButton() == MouseButton.PRIMARY)
				this.context.finishChange();
		});
		canvas.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
			DoubleVector end = getStandardVector(e.getSceneX(), e.getSceneY());
			if (e.getButton() == MouseButton.PRIMARY) {
				if (edit != null)
					edit.mark(context, eventState.previous, end);
			} else if (e.getButton() == MouseButton.MIDDLE) {
				updateScroll(eventState.startScroll.plus(new DoubleVector(
						e.getSceneX(),
						e.getSceneY()
				).minus(eventState.startScrollClick)));
			}
			eventState.previous = end;
		});
	}
}
