package com.zarbosoft.shoedemo.parts.editor;

import com.zarbosoft.rendaw.common.DeadCode;
import com.zarbosoft.shoedemo.*;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

public class Editor {
	private final ProjectContext context;
	private final com.zarbosoft.shoedemo.Window window;
	private WidgetHandle viewHandle;
	private final VBox layout;
	public final StackPane outerCanvas;
	public final Pane canvas;
	private final Group canvasInner;
	private Runnable editCleanup;
	private Wrapper edit;

	/**
	 * Returns a vector from a JavaFX canvas point relative to the image origin in image pixels
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	private DoubleVector getStandardVector(Wrapper view, double x, double y) {
		DoubleVector scroll = window.selectedForView.get().getConfig().scroll.get();
		DoubleVector coordCentered = new DoubleVector((x - this.canvas.getLayoutBounds().getWidth() / 2),
				(y - this.canvas.getLayoutBounds().getHeight() / 2)
		);
		DoubleVector viewTransform = computeViewTransform(view);
		DoubleVector out = coordCentered.minus(scroll.divide(viewTransform)).divide(viewTransform);
		//System.out.format("standard vect %s %s = cen %s, minus scroll %s, view trans %s\n", x, y, coordCentered, coordCentered.minus(scroll), out);
		return out;
	}

	private DoubleVector normalizeEventCoordinates(Wrapper view, MouseEvent e) {
		/*
		Point2D canvasCorner =
				canvas.getParent().getLocalToSceneTransform().transform(canvas.getLayoutX(), canvas.getLayoutY());
				*/
		Point2D canvasCorner = outerCanvas.getLocalToSceneTransform().transform(0, 0);
		//System.out.format("norm corner %s %s: layout %s %s\n", canvasCorner.getX(), canvasCorner.getY(), canvas.getLayoutX(), canvas.getLayoutY());
		return getStandardVector(view, e.getSceneX() - canvasCorner.getX(), e.getSceneY() - canvasCorner.getY());
	}

	public void updateScroll(DoubleVector scroll) {
		Wrapper view = window.selectedForView.get();
		if (view == null)
			return;
		view.getConfig().scroll.set(scroll);
		updateBounds();
	}

	public void updateBounds() {
		Wrapper view = window.selectedForView.get();
		if (view == null)
			return;
		DoubleVector scroll = view.getConfig().scroll.get();
		canvasInner.setLayoutX(scroll.x + outerCanvas.widthProperty().get() / 2);
		canvasInner.setLayoutY(scroll.y + outerCanvas.heightProperty().get() / 2);
		DoubleRectangle newBounds = new BoundsBuilder()
				.circle(getStandardVector(view, 0, 0), 0)
				.circle(getStandardVector(view,
						outerCanvas.getLayoutBounds().getWidth(),
						outerCanvas.getLayoutBounds().getHeight()
				), 0)
				.build();
		if (window.selectedForView.get() != null)
			window.selectedForView.get().setViewport(context, newBounds, calculatePositiveZoom(view));
	}

	public int calculatePositiveZoom(Wrapper view) {
		int zoom = view.getConfig().zoom.get();
		return zoom < 0 ? 1 : (zoom + 1);
	}

	public DoubleVector computeViewTransform(Wrapper view) {
		int zoom = view.getConfig().zoom.get();
		double scaling = view.getConfig().zoom.get() < 0 ? (1.0 / (1 + -zoom)) : (1 + zoom);
		return new DoubleVector(scaling * (view.getConfig().flipHorizontal.get() ? -1.0 : 1.0),
				scaling * (view.getConfig().flipVertical.get() ? -1.0 : 1.0)
		);
	}

	public Editor(ProjectContext context, Window window) {
		this.context = context;
		this.window = window;

		canvasInner = new Group();

		canvas = new Pane();
		canvas.getChildren().addAll(canvasInner);

		outerCanvas = new StackPane();
		VBox.setVgrow(outerCanvas, Priority.ALWAYS);
		outerCanvas
				.backgroundProperty()
				.bind(Bindings.createObjectBinding(() -> new Background(new BackgroundFill(context.config.backgroundColor
								.get()
								.toJfx(), CornerRadii.EMPTY, Insets.EMPTY)),
						context.config.backgroundColor
				));
		outerCanvas.setMouseTransparent(false);
		outerCanvas.setFocusTraversable(true);
		Rectangle clip = new Rectangle();
		clip
				.widthProperty()
				.bind(Bindings.createDoubleBinding(
						() -> outerCanvas.widthProperty().get() /
								Math.abs(outerCanvas.scaleXProperty().get()),
						outerCanvas.widthProperty(),
						outerCanvas.scaleXProperty()
				));
		clip
				.heightProperty()
				.bind(Bindings.createDoubleBinding(
						() -> outerCanvas.heightProperty().get() /
								Math.abs(outerCanvas.scaleXProperty().get()),
						outerCanvas.heightProperty(),
						outerCanvas.scaleYProperty()
				));
		outerCanvas.setClip(clip);
		ChangeListener<Number> onResize = (observable, oldValue, newValue) -> updateBounds();
		outerCanvas.widthProperty().addListener(onResize);
		outerCanvas.heightProperty().addListener(onResize);
		outerCanvas.getChildren().addAll(canvas);

		layout = new VBox();
		layout.setFillWidth(true);
		layout.getChildren().addAll(outerCanvas);

		outerCanvas.addEventFilter(MouseEvent.ANY, e -> e.consume());

		class ScrollEventState {
			boolean longScroll;
			Wrapper view;
			int startZoom;
		}
		ScrollEventState scrollEventState = new ScrollEventState();
		outerCanvas.addEventFilter(ScrollEvent.SCROLL_STARTED, e -> {
			scrollEventState.longScroll = true;
			scrollEventState.view = window.selectedForView.get();
			if (scrollEventState.view == null)
				return;
			scrollEventState.startZoom = scrollEventState.view.getConfig().zoom.get();
		});
		outerCanvas.addEventFilter(ScrollEvent.SCROLL_FINISHED, e1 -> {
			scrollEventState.longScroll = false;
		});
		outerCanvas.addEventFilter(ScrollEvent.SCROLL, e1 -> {
			System.out.format("scroll %s\n", e1.getTotalDeltaY());
			if (!scrollEventState.longScroll) {
				scrollEventState.view = window.selectedForView.get();
				if (scrollEventState.view == null)
					return;
				scrollEventState.view.getConfig().zoom.set(scrollEventState.view.getConfig().zoom.get() + (int) (
						e1.getTextDeltaYUnits() == ScrollEvent.VerticalTextScrollUnits.NONE ?
								e1.getDeltaY() / 40 :
								e1.getTextDeltaY()
				));
				System.out.format("  zoom %s\n", scrollEventState.view.getConfig().zoom.get());
			} else {
				if (scrollEventState.view == null)
					return;
				scrollEventState.view.getConfig().zoom.set(scrollEventState.startZoom +
						(int) (e1.getTotalDeltaY() / 40));
			}
		});
		class PointerEventState {
			public Wrapper view;
			Wrapper edit;
			boolean dragged = false;
			MouseButton button;
			DoubleVector previous;
			DoubleVector startScroll;
			DoubleVector startScrollClick;
		}
		PointerEventState pointerEventState = new PointerEventState();
		outerCanvas.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
			pointerEventState.view = window.selectedForView.get();
			if (pointerEventState.view == null)
				return;
			outerCanvas.requestFocus();
			pointerEventState.button = e.getButton();
			pointerEventState.previous = normalizeEventCoordinates(pointerEventState.view, e);
			pointerEventState.startScroll = pointerEventState.view.getConfig().scroll.get();
			pointerEventState.startScrollClick = new DoubleVector(e.getSceneX(), e.getSceneY());
			pointerEventState.edit = window.selectedForEdit.get();
			if (e.getButton() == MouseButton.PRIMARY) {
				if (pointerEventState.edit != null) {
					pointerEventState.edit.markStart(context, pointerEventState.previous);
				}
			}
		});
		outerCanvas.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
			if (pointerEventState.button == MouseButton.PRIMARY) {
				if (!pointerEventState.dragged && pointerEventState.edit != null) {
					pointerEventState.edit.mark(context, pointerEventState.previous, pointerEventState.previous);
				}
				if (e.getButton() == MouseButton.PRIMARY)
					this.context.history.finishChange();
			}
		});
		outerCanvas.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
			pointerEventState.dragged = true;
			DoubleVector end = normalizeEventCoordinates(pointerEventState.view, e);
			if (pointerEventState.button == MouseButton.PRIMARY) {
				if (pointerEventState.edit != null) {
					pointerEventState.edit.mark(context, pointerEventState.previous, end);
				}
			} else if (pointerEventState.button == MouseButton.MIDDLE) {
				updateScroll(pointerEventState.startScroll.plus(new DoubleVector(e.getSceneX(), e.getSceneY())
						.minus(pointerEventState.startScrollClick)
						.divide(computeViewTransform(pointerEventState.view))));
			}
			pointerEventState.previous = end;
		});
		outerCanvas.addEventFilter(MouseEvent.MOUSE_MOVED,e -> {
			if (edit == null) return;
			Wrapper view = window.selectedForView.get();
			if (view == null) return;
			edit.cursorMoved(context, normalizeEventCoordinates(view, e));
		} );
		outerCanvas.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (false) {
				throw new DeadCode();
			} else if (e.getCode() == KeyCode.H) {
				Wrapper view = window.selectedForView.get();
				if (view == null)
					return;
				view.getConfig().flipHorizontal.set(!view.getConfig().flipHorizontal.get());
			} else if (e.getCode() == KeyCode.V) {
				Wrapper view = window.selectedForView.get();
				if (view == null)
					return;
				view.getConfig().flipVertical.set(!view.getConfig().flipVertical.get());
			} else {
				// pass
			}
		});

		window.selectedForView.addListener(new ChangeListener<Wrapper>() {
			Runnable cleanup;

			{
				changed(null, null, window.selectedForView.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends Wrapper> observable, Wrapper oldValue, Wrapper newView
			) {
				if (cleanup != null) {
					cleanup.run();
					cleanup = null;
				}
				if (newView != null) {
					final ChangeListener<Number> zoomListener = (observable1, oldValue1, newValue) -> updateBounds();
					newView.getConfig().zoom.addListener(zoomListener);
					canvas.scaleXProperty().bind(Bindings.createDoubleBinding(() -> computeViewTransform(newView).x,
							newView.getConfig().zoom,
							newView.getConfig().flipHorizontal
					));
					canvas.scaleYProperty().bind(Bindings.createDoubleBinding(() -> computeViewTransform(newView).y,
							newView.getConfig().zoom,
							newView.getConfig().flipVertical
					));
					updateBounds();
					viewHandle = newView.buildCanvas(context);
					canvasInner.getChildren().add(viewHandle.getWidget());
					cleanup = () -> {
						newView.getConfig().zoom.removeListener(zoomListener);
						canvasInner.getChildren().clear();
						viewHandle.remove();
						viewHandle = null;
					};
				} else {
					canvas.scaleXProperty().unbind();
					canvas.scaleYProperty().unbind();
				}
			}
		});
	}

	public Node getWidget() {
		return layout;
	}

	public void setEdit(Wrapper edit, Wrapper.EditControlsHandle editControls) {
		if (editCleanup != null) {
			editCleanup.run();
			editCleanup = null;
		}
		this.edit = edit;
		Node header = editControls.getProperties();
		VBox.setVgrow(header, Priority.NEVER);
		layout.getChildren().add(0, header);
		if (header != null) {
			editCleanup = () -> {
				layout.getChildren().remove(header);
			};
		}
	}
}
