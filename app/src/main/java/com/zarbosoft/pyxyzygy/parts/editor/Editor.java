package com.zarbosoft.pyxyzygy.parts.editor;

import com.zarbosoft.pyxyzygy.*;
import com.zarbosoft.pyxyzygy.config.NodeConfig;
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

import static com.zarbosoft.rendaw.common.Common.uncheck;

public class Editor {
	private final com.zarbosoft.pyxyzygy.Window window;
	private final VBox layout;
	public final StackPane outerCanvas;
	public final Pane canvas;
	private final Group canvasInner;
	private Runnable editCleanup;
	private Hotkeys.Action[] actions = new Hotkeys.Action[] {
			new Hotkeys.Action(Hotkeys.Scope.CANVAS,
					"flip-horizontal",
					"View horizontal flip",
					Hotkeys.Hotkey.create(KeyCode.H, false, false, false)
			) {
				@Override
				public void run(ProjectContext context) {
					CanvasHandle view = window.selectedForView.get();
					if (view == null)
						return;
					view.getWrapper().getConfig().flipHorizontal.set(!view
							.getWrapper()
							.getConfig().flipHorizontal.get());
				}
			},
			new Hotkeys.Action(Hotkeys.Scope.CANVAS,
					"flip-vertical",
					"View vertical flip",
					Hotkeys.Hotkey.create(KeyCode.V, false, false, false)
			) {
				@Override
				public void run(ProjectContext context) {
					CanvasHandle view = window.selectedForView.get();
					if (view == null)
						return;
					view.getWrapper().getConfig().flipVertical.set(!view.getWrapper().getConfig().flipVertical.get());
				}
			},
			new Hotkeys.Action(Hotkeys.Scope.CANVAS,
					"max-editor",
					"Maximize canvas",
					Hotkeys.Hotkey.create(KeyCode.TAB, false, false, false)
			) {
				@Override
				public void run(ProjectContext context) {
					context.config.maxCanvas.set(!context.config.maxCanvas.get());
				}
			},
			new Hotkeys.Action(Hotkeys.Scope.CANVAS,
					"onion-skin",
					"Onion skin",
					Hotkeys.Hotkey.create(KeyCode.O, false, false, false)
			) {
				@Override
				public void run(ProjectContext context) {
					EditHandle e = window.selectedForEdit.get();
					if (e == null)
						return;
					e.getWrapper().getConfig().onionSkin.set(!e.getWrapper().getConfig().onionSkin.get());
				}
			}
	};
	private OnionSkin onionSkin;

	/**
	 * Returns a vector from a JavaFX canvas point relative to the image origin in image pixels
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	private DoubleVector getStandardVector(CanvasHandle view, double x, double y) {
		DoubleVector scroll = window.selectedForView.get().getWrapper().getConfig().scroll.get();
		DoubleVector coordCentered = new DoubleVector((x - this.canvas.getLayoutBounds().getWidth() / 2),
				(y - this.canvas.getLayoutBounds().getHeight() / 2)
		);
		DoubleVector viewTransform = computeViewTransform(view.getWrapper());
		DoubleVector out = coordCentered.divide(viewTransform).minus(scroll);
		//System.out.format("  standard vect %s %s = cen %s, minus scroll %s, view trans %s\n", x, y, coordCentered, coordCentered.minus(scroll), out);
		return out;
	}

	private DoubleVector normalizeEventCoordinates(CanvasHandle view, MouseEvent e) {
		Point2D canvasCorner = outerCanvas.getLocalToSceneTransform().transform(0, 0);
		//System.out.format("  norm corner %s %s: layout %s %s\n", canvasCorner.getX(), canvasCorner.getY(), canvas.getLayoutX(), canvas.getLayoutY());
		return getStandardVector(view, e.getSceneX() - canvasCorner.getX(), e.getSceneY() - canvasCorner.getY());
	}

	public void updateScroll(ProjectContext context, DoubleVector scroll) {
		CanvasHandle view = window.selectedForView.get();
		if (view == null)
			return;
		view.getWrapper().getConfig().scroll.set(scroll);
		updateBounds(context);
	}

	public void updateBounds(ProjectContext context) {
		CanvasHandle viewHandle = window.selectedForView.get();
		if (viewHandle == null)
			return;
		DoubleVector scroll = viewHandle.getWrapper().getConfig().scroll.get();
		canvasInner.setLayoutX(scroll.x + outerCanvas.widthProperty().get() / 2);
		canvasInner.setLayoutY(scroll.y + outerCanvas.heightProperty().get() / 2);
		DoubleRectangle newBounds =
				new BoundsBuilder().circle(getStandardVector(viewHandle, 0, 0), 0).circle(getStandardVector(viewHandle,
						outerCanvas.getLayoutBounds().getWidth(),
						outerCanvas.getLayoutBounds().getHeight()
				), 0).build();
		viewHandle.setViewport(context, newBounds, calculatePositiveZoom(viewHandle.getWrapper()));
	}

	public int calculatePositiveZoom(Wrapper view) {
		int zoom = view.getConfig().zoom.get();
		return zoom < 0 ? 1 : (zoom + 1);
	}

	public static DoubleVector computeViewTransform(Wrapper view) {
		final NodeConfig config = view.getConfig();
		int zoom = config.zoom.get();
		double scaling = config.zoom.get() < 0 ? (1.0 / (1 + -zoom)) : (1 + zoom);
		return new DoubleVector(scaling * (config.flipHorizontal.get() ? -1.0 : 1.0),
				scaling * (config.flipVertical.get() ? -1.0 : 1.0)
		);
	}

	public Editor(final ProjectContext context, Window window) {
		this.window = window;

		for (Hotkeys.Action action : actions)
			context.hotkeys.register(action);

		new Origin(window);

		canvasInner = new Group();

		canvas = new Pane();
		canvas.getChildren().addAll(canvasInner);

		outerCanvas = new StackPane();
		VBox.setVgrow(outerCanvas, Priority.ALWAYS);
		outerCanvas.backgroundProperty().bind(Bindings.createObjectBinding(
				() -> new Background(new BackgroundFill(context.config.backgroundColor.get().toJfx(),
						CornerRadii.EMPTY,
						Insets.EMPTY
				)),
				context.config.backgroundColor
		));
		outerCanvas.setMouseTransparent(false);
		outerCanvas.setFocusTraversable(true);
		Rectangle clip = new Rectangle();
		clip
				.widthProperty()
				.bind(Bindings.createDoubleBinding(() -> outerCanvas.widthProperty().get() /
								Math.abs(outerCanvas.scaleXProperty().get()),
						outerCanvas.widthProperty(),
						outerCanvas.scaleXProperty()
				));
		clip
				.heightProperty()
				.bind(Bindings.createDoubleBinding(() -> outerCanvas.heightProperty().get() /
								Math.abs(outerCanvas.scaleXProperty().get()),
						outerCanvas.heightProperty(),
						outerCanvas.scaleYProperty()
				));
		outerCanvas.setClip(clip);
		ChangeListener<Number> onResize = (observable, oldValue, newValue) -> updateBounds(context);
		outerCanvas.widthProperty().addListener(onResize);
		outerCanvas.heightProperty().addListener(onResize);
		outerCanvas.getChildren().addAll(canvas);

		layout = new VBox();
		layout.setFillWidth(true);
		layout.getChildren().addAll(outerCanvas);

		outerCanvas.addEventFilter(MouseEvent.ANY, e -> e.consume());

		class ScrollEventState {
			boolean longScroll;
			CanvasHandle view;
			int startZoom;
		}
		ScrollEventState scrollEventState = new ScrollEventState();
		outerCanvas.addEventFilter(ScrollEvent.SCROLL_STARTED, e -> {
			scrollEventState.longScroll = true;
			scrollEventState.view = window.selectedForView.get();
			if (scrollEventState.view == null)
				return;
			scrollEventState.startZoom = scrollEventState.view.getWrapper().getConfig().zoom.get();
		});
		outerCanvas.addEventFilter(ScrollEvent.SCROLL_FINISHED, e1 -> {
			scrollEventState.longScroll = false;
		});
		outerCanvas.addEventFilter(ScrollEvent.SCROLL, e1 -> {
			//System.out.format("scroll %s\n", e1.getTotalDeltaY());
			if (!scrollEventState.longScroll) {
				scrollEventState.view = window.selectedForView.get();
				if (scrollEventState.view == null)
					return;
				scrollEventState.view.getWrapper().getConfig().zoom.set(scrollEventState.view
						.getWrapper()
						.getConfig().zoom.get() + (int) (
						e1.getTextDeltaYUnits() == ScrollEvent.VerticalTextScrollUnits.NONE ?
								e1.getDeltaY() / 40 :
								e1.getTextDeltaY()
				));
				//System.out.format("  (editor) zoomed %s\n", scrollEventState.view.getWrapper().getConfig().zoom.get());
			} else {
				if (scrollEventState.view == null)
					return;
				scrollEventState.view.getWrapper().getConfig().zoom.set(scrollEventState.startZoom +
						(int) (e1.getTotalDeltaY() / 40));
			}
		});
		class PointerEventState {
			public CanvasHandle view;
			EditHandle edit;
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
			pointerEventState.startScroll = pointerEventState.view.getWrapper().getConfig().scroll.get();
			pointerEventState.startScrollClick = new DoubleVector(e.getSceneX(), e.getSceneY());
			pointerEventState.edit = window.selectedForEdit.get();
			pointerEventState.dragged = false;
			if (e.getButton() == MouseButton.PRIMARY) {
				if (pointerEventState.edit != null) {
					pointerEventState.edit.markStart(context, window, pointerEventState.previous);
				}
			}
		});
		outerCanvas.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
			if (pointerEventState.edit == null)
				return;
			if (pointerEventState.button == MouseButton.PRIMARY) {
				if (!pointerEventState.dragged) {
					pointerEventState.edit.mark(context,
							window,
							pointerEventState.previous,
							pointerEventState.previous
					);
				}
				context.history.finishChange();
			}
		});
		outerCanvas.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
			pointerEventState.dragged = true;
			DoubleVector end = normalizeEventCoordinates(pointerEventState.view, e);
			if (pointerEventState.button == MouseButton.PRIMARY) {
				if (pointerEventState.edit != null) {
					System.out.format("edi %s\n", end);
					pointerEventState.edit.mark(context, window, pointerEventState.previous, end);
				}
			} else if (pointerEventState.button == MouseButton.MIDDLE) {
				updateScroll(context,
						pointerEventState.startScroll.plus(new DoubleVector(e.getSceneX(), e.getSceneY())
								.minus(pointerEventState.startScrollClick)
								.divide(computeViewTransform(pointerEventState.view.getWrapper())))
				);
			}
			pointerEventState.previous = end;
		});
		outerCanvas.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
			outerCanvas.requestFocus();
			EditHandle edit = window.selectedForEdit.get();
			if (edit == null)
				return;
			CanvasHandle view = window.selectedForView.get();
			if (view == null)
				return;
			//System.out.format("mouse 1: %s %s\n", e.getSceneX(), e.getSceneY());
			edit.cursorMoved(context, normalizeEventCoordinates(view, e));
		});
		outerCanvas.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (context.hotkeys.event(context, Hotkeys.Scope.CANVAS, e))
				e.consume();
		});

		window.selectedForView.addListener(new ChangeListener<CanvasHandle>() {
			Runnable cleanup;

			{
				changed(null, null, window.selectedForView.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends CanvasHandle> observable,
					CanvasHandle oldValue,
					CanvasHandle newView
			) {
				if (cleanup != null) {
					cleanup.run();
					cleanup = null;
				}
				if (newView != null) {
					final ChangeListener<Number> zoomListener =
							(observable1, oldValue1, newValue) -> updateBounds(context);
					final NodeConfig config = newView.getWrapper().getConfig();
					config.zoom.addListener(zoomListener);
					canvas
							.scaleXProperty()
							.bind(Bindings.createDoubleBinding(() -> computeViewTransform(newView.getWrapper()).x,
									config.zoom,
									config.flipHorizontal
							));
					canvas
							.scaleYProperty()
							.bind(Bindings.createDoubleBinding(() -> computeViewTransform(newView.getWrapper()).y,
									config.zoom,
									config.flipVertical
							));
					updateBounds(context);
					canvasInner.getChildren().add(newView.getWidget());
					cleanup = () -> {
						newView.getWrapper().getConfig().zoom.removeListener(zoomListener);
						canvasInner.getChildren().clear();
					};
				} else {
					canvas.scaleXProperty().unbind();
					canvas.scaleYProperty().unbind();
				}
			}
		});

		window.selectedForEdit.addListener(new ChangeListener<EditHandle>() {
			{
				changed(null, null, window.selectedForEdit.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends EditHandle> observable,
					EditHandle oldValue,
					EditHandle newValue
			) {
				if (editCleanup != null) {
					editCleanup.run();
					editCleanup = null;
				}
				if (newValue != null) {
					onionSkin = new OnionSkin(context, newValue);
					Node header = newValue.getProperties();
					VBox.setVgrow(header, Priority.NEVER);
					layout.getChildren().add(0, header);
					if (header != null) {
						editCleanup = () -> {
							onionSkin.remove();
							onionSkin = null;
							layout.getChildren().remove(header);
						};
					}
				}
			}
		});
	}

	public Node getWidget() {
		return layout;
	}
}
