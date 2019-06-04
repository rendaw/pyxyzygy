package com.zarbosoft.pyxyzygy.app.parts.editor;

import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.config.NodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupChildWrapper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

public class Editor {
	private final com.zarbosoft.pyxyzygy.app.Window window;
	private final VBox layout;
	public final StackPane outerCanvas;
	public final Pane canvas;
	private final Group canvasInnerPaint;
	private final Group canvasInnerOverlay;
	private final ReadOnlyObjectProperty<Bounds> sizeProperty;
	public final SimpleIntegerProperty positiveZoom = new SimpleIntegerProperty(1);
	public final SimpleDoubleProperty zoomFactor = new SimpleDoubleProperty(1);

	private Hotkeys.Action[] actions = new Hotkeys.Action[] {
			new Hotkeys.Action(Hotkeys.Scope.CANVAS,
					"flip-horizontal",
					"View horizontal flip",
					Hotkeys.Hotkey.create(KeyCode.H, false, false, false)
			) {
				@Override
				public void run(ProjectContext context, Window window) {
					CanvasHandle view = Editor.this.window.getSelectedForView();
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
				public void run(ProjectContext context, Window window) {
					CanvasHandle view = Editor.this.window.getSelectedForView();
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
				public void run(ProjectContext context, Window window) {
					context.config.maxCanvas.set(!context.config.maxCanvas.get());
				}
			},
			new Hotkeys.Action(Hotkeys.Scope.CANVAS,
					"ghost-previous",
					"Previous frame ghost",
					Hotkeys.Hotkey.create(KeyCode.B, false, false, false)
			) {
				@Override
				public void run(ProjectContext context, Window window) {
					EditHandle e = Editor.this.window.getSelectedForEdit();
					if (e == null)
						return;
					e.getWrapper().getConfig().onionLeft.set(!e.getWrapper().getConfig().onionLeft.get());
				}
			},
			new Hotkeys.Action(Hotkeys.Scope.CANVAS,
					"ghost-next",
					"Next frame ghost",
					Hotkeys.Hotkey.create(KeyCode.N, false, false, false)
			) {
				@Override
				public void run(ProjectContext context, Window window) {
					EditHandle e = Editor.this.window.getSelectedForEdit();
					if (e == null)
						return;
					e.getWrapper().getConfig().onionRight.set(!e.getWrapper().getConfig().onionRight.get());
				}
			},
			new Hotkeys.Action(Hotkeys.Scope.GLOBAL,
					"view-top",
					"View top",
					Hotkeys.Hotkey.create(KeyCode.T, false, false, false)
			) {

				@Override
				public void run(ProjectContext context, Window window) {
					Wrapper at = window.getSelectedForView().getWrapper();
					Wrapper next = at.getParent();
					while (next != null) {
						at = next;
						next = at.getParent();
					}
					window.selectForView(context, at);
				}
			},
			new Hotkeys.Action(Hotkeys.Scope.GLOBAL,
					"view-up",
					"View up",
					Hotkeys.Hotkey.create(KeyCode.P, false, false, false)
			) {

				@Override
				public void run(ProjectContext context, Window window) {
					Wrapper at = window.getSelectedForView().getWrapper();
					Wrapper next = at.getParent();
					while (next != null) {
						at = next;
						if (!(at instanceof GroupChildWrapper))
							break;
						next = at.getParent();
					}
					window.selectForView(context, at);
				}
			},
			new Hotkeys.Action(Hotkeys.Scope.GLOBAL,
					"view-selected",
					"View selected",
					Hotkeys.Hotkey.create(KeyCode.E, false, false, false)
			) {

				@Override
				public void run(ProjectContext context, Window window) {
					window.selectForView(context, window.getSelectedForEdit().getWrapper());
				}
			}
	};
	private OnionSkin onionSkinPrevious;
	private OnionSkin onionSkinNext;
	private Runnable zoomListenerCleanup;

	public void selectedForEditChanged(
			ProjectContext context, EditHandle newValue
	) {
		if (onionSkinPrevious != null) {
			onionSkinPrevious.remove();
			onionSkinPrevious = null;
		}
		if (newValue != null) {
			onionSkinPrevious = new OnionSkin(context, window.timeline, newValue, true);
		}
		if (onionSkinNext != null) {
			onionSkinNext.remove();
			onionSkinNext = null;
		}
		if (newValue != null) {
			onionSkinNext = new OnionSkin(context, window.timeline, newValue, false);
		}
	}

	public void selectedForViewChanged(ProjectContext context, CanvasHandle newView) {
		canvas.scaleXProperty().unbind();
		canvas.scaleYProperty().unbind();
		if (zoomListenerCleanup != null) {
			zoomListenerCleanup.run();
			zoomListenerCleanup = null;
		}
		if (newView == null)
			return;
		final ChangeListener<Number> zoomListener = (observable1, oldValue1, zoom0) -> {
			updateBounds(context);
			int zoom = zoom0.intValue();
			positiveZoom.set(zoom < 0 ? 1 : (zoom + 1));
			zoomFactor.set(zoom < 0 ? (1.0 / (1 + -zoom)) : (1 + zoom));
		};
		final NodeConfig config = newView.getWrapper().getConfig();
		config.zoom.addListener(zoomListener);
		zoomListener.changed(null, null, config.zoom.get());
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
		canvasInnerPaint.getChildren().add(newView.getPaintWidget());
		canvasInnerOverlay.getChildren().add(newView.getOverlayWidget());
		zoomListenerCleanup = () -> {
			newView.getWrapper().getConfig().zoom.removeListener(zoomListener);
			canvasInnerPaint.getChildren().clear();
			canvasInnerOverlay.getChildren().clear();
		};
	}

	public class MouseCoords {
		final DoubleVector global;
		final DoubleVector local;

		public MouseCoords(CanvasHandle view, MouseEvent e) {
			DoubleVector eVect = new DoubleVector(e.getSceneX(), e.getSceneY());
			DoubleVector transform = computeViewTransform(view.getWrapper());

			global = eVect.divide(transform);

			Point2D canvasCorner = outerCanvas.getLocalToSceneTransform().transform(0, 0);
			local = eVect
					/* relative to canvas viewport */
					.minus(new DoubleVector(canvasCorner.getX(), canvasCorner.getY()))
					/* relative to canvas center */
					.minus(new DoubleVector(sizeProperty.get().getWidth() / 2, sizeProperty.get().getHeight() / 2))
					/* scaled, flipped */
					.divide(transform)
					/* scrolled */
					.minus(window.getSelectedForView().getWrapper().getConfig().scroll.get());
		}
	}

	public void updateScroll(ProjectContext context, DoubleVector scroll) {
		CanvasHandle view = window.getSelectedForView();
		if (view == null)
			return;
		view.getWrapper().getConfig().scroll.set(scroll);
		updateBounds(context);
	}

	public void updateBounds(ProjectContext context) {
		CanvasHandle viewHandle = window.getSelectedForView();
		if (viewHandle == null)
			return;
		DoubleVector scroll = viewHandle.getWrapper().getConfig().scroll.get();
		canvasInnerPaint.setLayoutX(scroll.x + outerCanvas.widthProperty().get() / 2);
		canvasInnerPaint.setLayoutY(scroll.y + outerCanvas.heightProperty().get() / 2);
		canvasInnerOverlay.setLayoutX(scroll.x + outerCanvas.widthProperty().get() / 2);
		canvasInnerOverlay.setLayoutY(scroll.y + outerCanvas.heightProperty().get() / 2);
		double width = sizeProperty.get().getWidth() / zoomFactor.get();
		double height = sizeProperty.get().getHeight() / zoomFactor.get();
		viewHandle.setViewport(context,
				new DoubleRectangle(scroll.x - width / 2, scroll.y - height / 2, width, height),
				positiveZoom.get()
		);
	}

	public DoubleVector computeViewTransform(Wrapper view) {
		final NodeConfig config = view.getConfig();
		return new DoubleVector(zoomFactor.get() * (config.flipHorizontal.get() ? -1.0 : 1.0),
				zoomFactor.get() * (config.flipVertical.get() ? -1.0 : 1.0)
		);
	}

	public Editor(final ProjectContext context, Window window) {
		this.window = window;

		for (Hotkeys.Action action : actions)
			context.hotkeys.register(action);

		canvasInnerPaint = new Group();
		canvasInnerOverlay = new Group();

		canvas = new Pane();
		canvas.getChildren().addAll(canvasInnerPaint, canvasInnerOverlay);

		outerCanvas = new StackPane();
		VBox.setVgrow(outerCanvas, Priority.ALWAYS);
		outerCanvas.backgroundProperty().bind(Bindings.createObjectBinding(
				() -> new Background(new BackgroundFill(GUILaunch.profileConfig.backgroundColor.get().toJfx(),
						CornerRadii.EMPTY,
						Insets.EMPTY
				)),
				GUILaunch.profileConfig.backgroundColor
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
		this.sizeProperty = outerCanvas.layoutBoundsProperty();

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
			scrollEventState.view = window.getSelectedForView();
			if (scrollEventState.view == null)
				return;
			scrollEventState.startZoom = scrollEventState.view.getWrapper().getConfig().zoom.get();
		});
		outerCanvas.addEventFilter(ScrollEvent.SCROLL_FINISHED, e1 -> {
			scrollEventState.longScroll = false;
		});
		outerCanvas.addEventFilter(ScrollEvent.SCROLL, e1 -> {
			if (!scrollEventState.longScroll) {
				scrollEventState.view = window.getSelectedForView();
				if (scrollEventState.view == null)
					return;
				scrollEventState.view.getWrapper().getConfig().zoom.set(scrollEventState.view
						.getWrapper()
						.getConfig().zoom.get() + (int) (
						e1.getTextDeltaYUnits() == ScrollEvent.VerticalTextScrollUnits.NONE ?
								e1.getDeltaY() / 40 :
								e1.getTextDeltaY()
				));
			} else {
				if (scrollEventState.view == null)
					return;
				scrollEventState.view.getWrapper().getConfig().zoom.set(scrollEventState.startZoom +
						(int) (e1.getTotalDeltaY() / 40));
			}
		});
		class PointerEventState {
			public CanvasHandle view;
			boolean dragged = false;
			MouseButton button;
			MouseCoords previous;
			DoubleVector startScroll;
			MouseCoords startScrollClick;
		}
		PointerEventState pointerEventState = new PointerEventState();
		outerCanvas.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
			pointerEventState.view = window.getSelectedForView();
			if (pointerEventState.view == null)
				return;
			outerCanvas.requestFocus();
			pointerEventState.button = e.getButton();
			MouseCoords coords = new MouseCoords(pointerEventState.view, e);
			pointerEventState.previous = coords;
			pointerEventState.startScroll = pointerEventState.view.getWrapper().getConfig().scroll.get();
			pointerEventState.startScrollClick = coords;
			pointerEventState.dragged = false;
			if (e.getButton() == MouseButton.PRIMARY) {
				if (window.getSelectedForEdit() != null) {
					window.getSelectedForEdit().markStart(context, window, pointerEventState.previous.local);
				}
			}
		});
		outerCanvas.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
			EditHandle edit = window.getSelectedForEdit();
			if (edit == null)
				return;
			if (pointerEventState.button == MouseButton.PRIMARY) {
				if (!pointerEventState.dragged) {
					edit.mark(context, window, pointerEventState.previous.local, pointerEventState.previous.local);
				}
				context.finishChange();
			}
		});
		outerCanvas.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
			EditHandle edit = window.getSelectedForEdit();
			pointerEventState.dragged = true;
			MouseCoords end = new MouseCoords(pointerEventState.view, e);
			if (pointerEventState.button == MouseButton.PRIMARY) {
				if (edit != null) {
					edit.mark(context, window, pointerEventState.previous.local, end.local);
				}
			} else if (pointerEventState.button == MouseButton.MIDDLE) {
				updateScroll(context,
						pointerEventState.startScroll.plus(end.global.minus(pointerEventState.startScrollClick.global))
				);
			}
			pointerEventState.previous = end;
		});
		outerCanvas.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
			outerCanvas.requestFocus();
			EditHandle edit = window.getSelectedForEdit();
			if (edit == null)
				return;
			CanvasHandle view = window.getSelectedForView();
			if (view == null)
				return;
			edit.cursorMoved(context, window, new MouseCoords(view, e).local);
		});
		outerCanvas.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (context.hotkeys.event(context, window, Hotkeys.Scope.CANVAS, e))
				e.consume();
		});

		Origin origin = new Origin(window, this, 20);
		origin.visibleProperty().bind(GUILaunch.profileConfig.showOrigin);
	}

	public Node getWidget() {
		return layout;
	}
}
