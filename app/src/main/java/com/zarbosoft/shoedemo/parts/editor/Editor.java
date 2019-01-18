package com.zarbosoft.shoedemo.parts.editor;

import com.zarbosoft.shoedemo.*;
import com.zarbosoft.shoedemo.Window;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

import java.awt.*;

import static com.zarbosoft.shoedemo.HelperJFX.c;

public class Editor {
	private final ProjectContext context;
	private final com.zarbosoft.shoedemo.Window window;
	private WidgetHandle viewHandle;
	private WidgetHandle propertiesHandle;
	private final VBox layout;
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
		if (window.selectedForView.get() != null)
			window.selectedForView.get().scroll(context, oldBounds, newBounds);
	}

	public void updateScroll() {
		updateScroll(scroll);
	}

	public Editor(ProjectContext context, Window window) {
		this.context = context;
		this.window = window;

		canvas = new Pane();
		canvas.setBackground(new Background(new BackgroundFill(c(new Color(99, 80, 97)),
				CornerRadii.EMPTY,
				Insets.EMPTY
		)));
		canvas.setMouseTransparent(false);
		canvas.setFocusTraversable(true);
		Rectangle clip = new Rectangle();
		clip.widthProperty().bind(canvas.widthProperty());
		clip.heightProperty().bind(canvas.heightProperty());
		canvas.setClip(clip);
		canvasInner = new Group();
		canvas.getChildren().add(canvasInner);
		ChangeListener<Number> onResize = (observable, oldValue, newValue) -> updateScroll();
		canvas.widthProperty().addListener(onResize);
		canvas.heightProperty().addListener(onResize);
		VBox.setVgrow(canvas, Priority.ALWAYS);

		layout = new VBox();
		layout.setFillWidth(true);
		layout.getChildren().addAll(canvas);

		class EventState {
			Wrapper edit;
			boolean dragged = false;
			MouseButton button;
			DoubleVector previous;
			DoubleVector lastClick;
			DoubleVector startScroll;
			DoubleVector startScrollClick;
		}
		EventState eventState = new EventState();
		canvas.addEventFilter(MouseEvent.ANY, e -> e.consume());
		canvas.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
			canvas.requestFocus();
			eventState.button = e.getButton();
			eventState.lastClick = eventState.previous = normalizeEventCoordinates(e);
			eventState.startScroll = scroll;
			eventState.startScrollClick = new DoubleVector(e.getSceneX(), e.getSceneY());
			eventState.edit = window.selectedForEdit.get();
			if (e.getButton() == MouseButton.PRIMARY) {
				if (eventState.edit != null) {
					eventState.edit.markStart(context, eventState.previous);
				}
			}
		});
		canvas.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
			if (eventState.button == MouseButton.PRIMARY) {
				if (!eventState.dragged && eventState.edit != null) {
					eventState.edit.mark(context, eventState.previous, eventState.previous);
				}
				if (e.getButton() == MouseButton.PRIMARY)
					this.context.history.finishChange();
			}
		});
		canvas.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
			eventState.dragged = true;
			DoubleVector end = normalizeEventCoordinates(e);
			if (eventState.button == MouseButton.PRIMARY) {
				if (eventState.edit != null) {
					eventState.edit.mark(context, eventState.previous, end);
				}
			} else if (eventState.button == MouseButton.MIDDLE) {
				updateScroll(eventState.startScroll.plus(new DoubleVector(e.getSceneX(),
						e.getSceneY()
				).minus(eventState.startScrollClick)));
			}
			eventState.previous = end;
		});

		window.selectedForView.addListener(new ChangeListener<Wrapper>() {
			{
				changed(null, null, window.selectedForView.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends Wrapper> observable, Wrapper oldValue, Wrapper newValue
			) {
				if (viewHandle != null) {
					canvasInner.getChildren().clear();
					viewHandle.remove();
					viewHandle = null;
				}
				if (propertiesHandle != null) {
					layout.getChildren().remove(propertiesHandle.getWidget());
					propertiesHandle.remove();
					propertiesHandle = null;
				}
				if (newValue != null) {
					viewHandle = newValue.buildCanvas(context, viewBounds());
					canvasInner.getChildren().add(viewHandle.getWidget());
					updateScroll();
					propertiesHandle = newValue.buildCanvasProperties(context);
					if (propertiesHandle != null) {
						VBox.setVgrow(propertiesHandle.getWidget(), Priority.NEVER);
						layout.getChildren().add(0, propertiesHandle.getWidget());
					}
				}
			}
		});
	}

	public Node getWidget() {
		return layout;
	}
}
