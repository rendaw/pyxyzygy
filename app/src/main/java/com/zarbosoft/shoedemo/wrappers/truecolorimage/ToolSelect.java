package com.zarbosoft.shoedemo.wrappers.truecolorimage;

import com.zarbosoft.shoedemo.*;
import com.zarbosoft.shoedemo.model.Rectangle;
import com.zarbosoft.shoedemo.model.Vector;
import com.zarbosoft.shoedemo.widgets.HelperJFX;
import com.zarbosoft.shoedemo.widgets.WidgetFormBuilder;
import com.zarbosoft.shoedemo.wrappers.group.Tool;
import javafx.beans.binding.Bindings;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeType;
import javafx.scene.transform.Scale;

import static com.zarbosoft.rendaw.common.Common.uncheck;
import static com.zarbosoft.shoedemo.widgets.HelperJFX.icon;
import static com.zarbosoft.shoedemo.widgets.HelperJFX.pad;

public class ToolSelect extends Tool {
	final TrueColorImageEditHandle editHandle;
	final int handleSize = 10;
	final int handlePad = 3;

	abstract class State {
		public abstract void markStart(ProjectContext context, DoubleVector start);

		public abstract void mark(ProjectContext context, DoubleVector start, DoubleVector end);

		public abstract void remove(ProjectContext context);
	}

	abstract class Interactive {
		public abstract void mark(ProjectContext context, DoubleVector start, DoubleVector end);
	}

	class SelectRect extends javafx.scene.shape.Rectangle {
		{
			setStrokeType(StrokeType.OUTSIDE);
			setStroke(Color.GRAY);
			setFill(Color.TRANSPARENT);
			setVisible(false);
			setBlendMode(BlendMode.DIFFERENCE);
			strokeWidthProperty().bind(Bindings.divide(1.0, editHandle.wrapper.zoom));
		}
	}

	class SelectInside extends SelectRect {
		{
			getStrokeDashArray().setAll(5.0, 5.0);
		}
	}

	class SelectHandle extends SelectRect {
		{
			setArcHeight(3);
			setArcWidth(3);
			setStroke(Color.LIGHTGRAY);
			setOpacity(0.5);
		}
	}

	class SelectVHandle extends SelectHandle {
		{
			setHeight(25);
		}
	}

	class SelectHHandle extends SelectHandle {
		{
			setWidth(25);
		}
	}

	class StateMove extends State {
		final SelectInside originalRectangle = new SelectInside();
		final SelectInside rectangle = new SelectInside();
		final Group imageGroup = new Group();
		final TrueColorImage buffer;
		private Rectangle bounds;
		private DoubleVector start;
		private Vector startCorner;
		Hotkeys.Action[] actions = new Hotkeys.Action[] {
				new Hotkeys.Action(
						Hotkeys.Scope.EDITOR,
						"cancel",
						"Cancel",
						Hotkeys.Hotkey.create(KeyCode.ESCAPE, false, false, false)
				) {
					@Override
					public void run(ProjectContext context) {
						setState(context, new StateCreate(context));
					}
				},
				new Hotkeys.Action(Hotkeys.Scope.EDITOR,
						"place",
						"Place",
						Hotkeys.Hotkey.create(KeyCode.ENTER, false, false, true)
				) {
					@Override
					public void run(ProjectContext context) {
						setState(context, new StateCreate(context));
					}
				},
		};

		StateMove(ProjectContext context, Rectangle originalBounds, TrueColorImage buffer) {
			this.bounds = originalBounds;

			for (Hotkeys.Action action : actions)
				context.hotkeys.register(action);

			originalRectangle.setWidth(originalBounds.width);
			originalRectangle.setHeight(originalBounds.height);
			originalRectangle.setLayoutX(originalBounds.x);
			originalRectangle.setLayoutY(originalBounds.y);
			originalRectangle.setVisible(true);
			rectangle.setStroke(Color.WHITE);
			rectangle.setVisible(true);
			rectangle.setWidth(bounds.width);
			rectangle.setHeight(bounds.height);
			this.buffer = buffer;
			ImageView image = new ImageView();
			image
					.imageProperty()
					.bind(Bindings.createObjectBinding(() -> HelperJFX.toImage(buffer, editHandle.wrapper.zoom.get()),
							editHandle.wrapper.zoom
					));
			imageGroup.getChildren().add(image);
			editHandle.wrapper.zoom.addListener((observable, oldValue, newValue) -> imageGroup
					.getTransforms()
					.setAll(new Scale(1.0 / editHandle.wrapper.zoom.get(), 1.0 / editHandle.wrapper.zoom.get())));
			editHandle.overlay.getChildren().addAll(originalRectangle, rectangle, imageGroup);
			setPosition(bounds.corner());

			Runnable copy = () -> {
				ClipboardContent content = new ClipboardContent();
				content.putImage(HelperJFX.toImage(buffer, 1));
				Clipboard.getSystemClipboard().setContent(content);
			};

			editHandle.paintTab.setContent(pad(new WidgetFormBuilder().button(b -> {
				b.setText("Place");
				b.setGraphic(new ImageView(icon("arrow-collapse-down.svg")));
				b.setOnAction(e -> {
					editHandle.wrapper.clear(context, originalBounds);

					{
						Rectangle destQuantizedBounds = bounds.quantize(context.tileSize);
						Rectangle dropBounds = destQuantizedBounds.multiply(context.tileSize);
						TrueColorImage composeCanvas = TrueColorImage.create(dropBounds.width, dropBounds.height);
						editHandle.wrapper.render(context, composeCanvas, dropBounds);
						Vector offset = bounds.corner().minus(dropBounds.corner());
						composeCanvas.compose(buffer, offset.x, offset.y, 1);
						editHandle.wrapper.drop(context, destQuantizedBounds, composeCanvas);
					}
					setState(context, new StateCreate(context));
				});
			}).button(b -> {
				b.setText("Clear");
				b.setGraphic(new ImageView(icon("eraser-variant.svg")));
				b.setOnAction(e -> {
					editHandle.wrapper.clear(context, originalBounds);
					setState(context, new StateCreate(context));
				});
			}).button(b -> {
				b.setText("Cut");
				b.setGraphic(new ImageView(icon("content-cut.svg")));
				b.setOnAction(e -> uncheck(() -> {
					copy.run();
					editHandle.wrapper.clear(context, originalBounds);
					setState(context, new StateCreate(context));
				}));
			}).button(b -> {
				b.setText("Copy");
				b.setGraphic(new ImageView(icon("content-copy.svg")));
				b.setOnAction(e -> {
					copy.run();
				});
			}).separator().button(b -> {
				b.setText("Cancel");
				b.setOnAction(e -> {
					setState(context, new StateCreate(context));
				});
			}).build()));
		}

		@Override
		public void markStart(ProjectContext context, DoubleVector start) {
			if (!bounds.contains(start.toInt())) {
				setState(context, new StateCreate(context));
				return;
			}
			this.start = start;
			this.startCorner = bounds.corner();
		}

		@Override
		public void mark(ProjectContext context, DoubleVector __, DoubleVector end) {
			setPosition(startCorner.plus(end.minus(start).toInt()));
		}

		void setPosition(Vector vector) {
			bounds = vector.toRect(bounds.width, bounds.height);
			rectangle.setLayoutX(bounds.x);
			rectangle.setLayoutY(bounds.y);
			imageGroup.setLayoutX(bounds.x);
			imageGroup.setLayoutY(bounds.y);
		}

		@Override
		public void remove(ProjectContext context) {
			editHandle.overlay.getChildren().removeAll(originalRectangle, rectangle, imageGroup);
			for (Hotkeys.Action action : actions)
				context.hotkeys.unregister(action);
		}
	}

	class StateCreate extends State {
		SelectInside rectangle = new SelectInside();
		SelectHHandle left = new SelectHHandle();
		SelectHHandle right = new SelectHHandle();
		SelectVHandle top = new SelectVHandle();
		SelectVHandle bottom = new SelectVHandle();
		Rectangle inside = new Rectangle(0, 0, 0, 0);

		Interactive mark;

		Hotkeys.Action[] actions = new Hotkeys.Action[] {
				new Hotkeys.Action(
						Hotkeys.Scope.EDITOR,
						"cancel",
						"Cancel",
						Hotkeys.Hotkey.create(KeyCode.ESCAPE, false, false, false)
				) {
					@Override
					public void run(ProjectContext context) {
						setState(context, new StateCreate(context));
					}
				},
				new Hotkeys.Action(Hotkeys.Scope.EDITOR,
						"lift",
						"Lift",
						Hotkeys.Hotkey.create(KeyCode.ENTER, false, false, false)
				) {
					@Override
					public void run(ProjectContext context) {
						setState(context, new StateCreate(context));
					}
				},
		};

		class LeftHandle extends Interactive {
			final DoubleVector start;
			final Rectangle startInside;

			LeftHandle(DoubleVector start, Rectangle startInside) {
				this.start = start;
				this.startInside = startInside;
			}

			@Override
			public void mark(ProjectContext context, DoubleVector __, DoubleVector end) {
				int diffX = (int) (end.x - start.x);
				int newWidth = startInside.width - diffX;
				if (newWidth < 0)
					setInside(new Rectangle(startInside.x + startInside.width,
							startInside.y,
							-newWidth,
							startInside.height
					));
				else
					setInside(new Rectangle(startInside.x + diffX, startInside.y, newWidth, startInside.height));
			}
		}

		class RightHandle extends Interactive {
			final DoubleVector start;
			final Rectangle startInside;

			RightHandle(DoubleVector start, Rectangle startInside) {
				this.start = start;
				this.startInside = startInside;
			}

			@Override
			public void mark(ProjectContext context, DoubleVector __, DoubleVector end) {
				int diffX = (int) (end.x - start.x);
				int newWidth = startInside.width + diffX;
				if (newWidth < 0)
					setInside(new Rectangle(startInside.x + newWidth, startInside.y, -newWidth, startInside.height));
				else
					setInside(new Rectangle(startInside.x, startInside.y, newWidth, startInside.height));
			}
		}

		class TopHandle extends Interactive {
			final DoubleVector start;
			final Rectangle startInside;

			TopHandle(DoubleVector start, Rectangle startInside) {
				this.start = start;
				this.startInside = startInside;
			}

			@Override
			public void mark(ProjectContext context, DoubleVector __, DoubleVector end) {
				int diff = (int) (end.y - start.y);
				int newHeight = startInside.height - diff;
				if (newHeight < 0)
					setInside(new Rectangle(startInside.x,
							startInside.y + startInside.height,
							startInside.width,
							-newHeight
					));
				else
					setInside(new Rectangle(startInside.x, startInside.y + diff, startInside.width, newHeight));
			}
		}

		class BottomHandle extends Interactive {
			final DoubleVector start;
			final Rectangle startInside;

			BottomHandle(DoubleVector start, Rectangle startInside) {
				this.start = start;
				this.startInside = startInside;
			}

			@Override
			public void mark(ProjectContext context, DoubleVector __, DoubleVector end) {
				int diff = (int) (end.y - start.y);
				int newHeight = startInside.height + diff;
				if (newHeight < 0)
					setInside(new Rectangle(startInside.x, startInside.y + newHeight, startInside.width, -newHeight));
				else
					setInside(new Rectangle(startInside.x, startInside.y, startInside.width, newHeight));
			}
		}

		class FromScratchHandle extends Interactive {
			final DoubleVector start;

			FromScratchHandle(DoubleVector start) {
				this.start = start;
			}

			@Override
			public void mark(ProjectContext context, DoubleVector __, DoubleVector end) {
				setInside(new BoundsBuilder().circle(start, 0).circle(end, 0).buildInt());
			}
		}

		class MoveHandle extends Interactive {
			final DoubleVector start;
			final Rectangle startInside;

			MoveHandle(DoubleVector start, Rectangle startInside) {
				this.start = start;
				this.startInside = startInside;
			}

			@Override
			public void mark(ProjectContext context, DoubleVector __, DoubleVector end) {
				setInside(startInside.plus(end.minus(start).toInt()));
			}
		}

		TrueColorImage render(ProjectContext context) {
			TrueColorImage buffer = TrueColorImage.create(inside.width, inside.height);
			editHandle.wrapper.render(context, buffer, inside);
			return buffer;
		}

		StateCreate(ProjectContext context) {
			for (Hotkeys.Action action : actions)
				context.hotkeys.register(action);
			editHandle.overlay.getChildren().addAll(rectangle, left, right, top, bottom);

			Runnable copy = () -> {
				ClipboardContent content = new ClipboardContent();
				content.putImage(HelperJFX.toImage(render(context), 1));
				Clipboard.getSystemClipboard().setContent(content);
			};

			editHandle.paintTab.setContent(pad(new WidgetFormBuilder().button(b -> {
				b.setText("Lift");
				b.setGraphic(new ImageView(icon("arrow-expand-up.svg")));
				b.setOnAction(e -> {
					setState(context, new StateMove(context, inside, render(context)));
				});
			}).button(b -> {
				b.setText("Clear");
				b.setGraphic(new ImageView(icon("eraser-variant.svg")));
				b.setOnAction(e -> {
					editHandle.wrapper.clear(context, inside);
					setState(context, new StateCreate(context));
				});
			}).button(b -> {
				b.setText("Cut");
				b.setGraphic(new ImageView(icon("content-cut.svg")));
				b.setOnAction(e -> uncheck(() -> {
					copy.run();
					editHandle.wrapper.clear(context, inside);
					setState(context, new StateCreate(context));
				}));
			}).button(b -> {
				b.setText("Copy");
				b.setGraphic(new ImageView(icon("content-copy.svg")));
				b.setOnAction(e -> {
					copy.run();
				});
			}).separator().button(b -> {
				b.setText("Cancel");
				b.setOnAction(e -> {
					setState(context, new StateCreate(context));
				});
			}).build()));
		}

		@Override
		public void markStart(ProjectContext context, DoubleVector start) {
			if (inside.contains(start.toInt())) {
				mark = new MoveHandle(start, inside);
			} else if (left.getBoundsInParent().contains(start.toJfx())) {
				mark = new LeftHandle(start, inside);
			} else if (right.getBoundsInParent().contains(start.toJfx())) {
				mark = new RightHandle(start, inside);
			} else if (top.getBoundsInParent().contains(start.toJfx())) {
				mark = new TopHandle(start, inside);
			} else if (bottom.getBoundsInParent().contains(start.toJfx())) {
				mark = new BottomHandle(start, inside);
			} else {
				setInside(new Rectangle(0, 0, 0, 0));
				mark = new FromScratchHandle(start);
			}
		}

		private void setInside(Rectangle rectangle1) {
			inside = rectangle1;
			boolean visible = inside.width > 0 && inside.height > 0;
			rectangle.setVisible(visible);
			left.setVisible(visible);
			right.setVisible(visible);
			top.setVisible(visible);
			bottom.setVisible(visible);
			rectangle.setLayoutX(inside.x);
			rectangle.setLayoutY(inside.y);
			rectangle.setWidth(inside.width);
			rectangle.setHeight(inside.height);
			left.setLayoutX(inside.x - left.getWidth() - handlePad);
			left.setLayoutY(inside.y);
			left.setHeight(inside.height);
			right.setLayoutX(inside.x + inside.width + handlePad);
			right.setLayoutY(inside.y);
			right.setHeight(inside.height);
			top.setLayoutY(inside.y - top.getHeight() - handlePad);
			top.setLayoutX(inside.x);
			top.setWidth(inside.width);
			bottom.setLayoutY(inside.y + inside.height + handlePad);
			bottom.setLayoutX(inside.x);
			bottom.setWidth(inside.width);
		}

		@Override
		public void mark(ProjectContext context, DoubleVector start, DoubleVector end) {
			mark.mark(context, start, end);
		}

		@Override
		public void remove(ProjectContext context) {
			editHandle.overlay.getChildren().removeAll(rectangle, left, right, top, bottom);
			for (Hotkeys.Action action : actions)
				context.hotkeys.unregister(action);
		}
	}

	State state;

	ToolSelect(ProjectContext context, TrueColorImageEditHandle editHandle) {
		this.editHandle = editHandle;
		state = new StateCreate(context);
	}

	void setState(ProjectContext context, State newState) {
		state.remove(context);
		state = newState;
	}

	@Override
	public void markStart(ProjectContext context, DoubleVector start) {
		state.markStart(context, start);
	}

	@Override
	public void mark(ProjectContext context, DoubleVector start, DoubleVector end) {
		state.mark(context, start, end);
	}

	@Override
	public Node getProperties() {
		return null;
	}

	@Override
	public void remove(ProjectContext context) {
		state.remove(context);
	}
}
