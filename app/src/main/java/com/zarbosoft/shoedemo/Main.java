package com.zarbosoft.shoedemo;

import com.gojuno.morton.Morton64;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.shoedemo.model.*;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.*;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Main extends Application {
	public static void main(String[] args) {
		Main.launch(args);
	}

	public abstract static class Wrapper {
		public abstract Wrapper getParent();

		List<Wrapper> children;

		public abstract DoubleVector toInner(DoubleVector vector);

		public abstract ProjectObject getValue();

		public abstract void scroll(ProjectContext context, DoubleRectangle oldBounds, DoubleRectangle newBounds);

		public abstract Node buildCanvas(ProjectContext context, DoubleRectangle bounds);
	}

	ProjectContext context;
	Wrapper selectedRoot;
	public Pane canvas;
	private Group canvasInner;
	private Wrapper imageFrameWrapper;
	DoubleVector scroll = new DoubleVector(0, 0);

	public DoubleRectangle viewBounds() {
		return new BoundsBuilder()
				.circle(getStandardVector(0, 0), 0)
				.circle(getStandardVector(canvas.getLayoutBounds().getWidth(), canvas.getLayoutBounds().getHeight()), 0)
				.build();
	}

	public void updateScroll(DoubleVector scroll) {
		DoubleRectangle oldBounds = viewBounds();
		this.scroll = scroll;
		canvasInner.setLayoutX(scroll.x  + canvas.widthProperty().get() / 2);
		canvasInner.setLayoutY(scroll.y  + canvas.heightProperty().get() / 2);
		DoubleRectangle newBounds = viewBounds();
		imageFrameWrapper.scroll(context, oldBounds, newBounds);
	}

	public void updateScroll() {
		updateScroll(scroll);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		Path path = Paths.get(this.getParameters().getUnnamed().get(0));
		context = ProjectContext.create(path);
		ImageNode imageNode = ImageNode.create(context);
		context.change.project(context.project).topAdd(imageNode);
		ImageFrame imageFrame = ImageFrame.create(context);
		context.change.imageNode(imageNode).framesAdd(imageFrame);
		context.finishChange();
		context.clearHistory();

		canvas = new Pane();
		canvas.setFocusTraversable(true);
		canvasInner = new Group();
		canvas.getChildren().add(canvasInner);
		ChangeListener<Number> onResize = new ChangeListener<Number>() {
			@Override
			public void changed(
					ObservableValue<? extends Number> observable, Number oldValue, Number newValue
			) {
				updateScroll();
			}
		};
		canvas.widthProperty().addListener(onResize);
		canvas.heightProperty().addListener(onResize);
		imageFrameWrapper = createNode(null, imageFrame);
		canvasInner.getChildren().add(imageFrameWrapper.buildCanvas(context, viewBounds()));

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
				markFrame(context, imageFrameWrapper, eventState.previous, eventState.previous);
			}
		});
		canvas.addEventFilter(MouseEvent.MOUSE_RELEASED,e -> {
			if (e.getButton() == MouseButton.PRIMARY) context.finishChange();
		} );
		canvas.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
			DoubleVector end = getStandardVector(e.getSceneX(), e.getSceneY());
			if (e.getButton() == MouseButton.PRIMARY) {
				markFrame(context, imageFrameWrapper, eventState.previous, end);
			} else if (e.getButton() == MouseButton.MIDDLE) {
				updateScroll(eventState.startScroll.plus(new DoubleVector(e.getSceneX(), e.getSceneY())
						.minus(eventState.startScrollClick)
						));
			}
			eventState.previous = end;
		});
		canvas.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			System.out.format("%s\n", e);
			if (e.getCode() == KeyCode.U || (e.isControlDown() && e.getCode() == KeyCode.Z)) {
				context.undo();
			} else if (e.isControlDown() && (e.getCode() == KeyCode.R || e.getCode() == KeyCode.Y)) {
				context.redo();
			}
		});

		primaryStage.setScene(new Scene(canvas, 600, 600));
		primaryStage.show();
	}

	/**
	 * Returns a vector from a JavaFX canvas point relative to the image origin in image pixels
	 *
	 * @param x
	 * @param y
	 * @return
	 */
	private DoubleVector getStandardVector(double x, double y) {
		return new DoubleVector((x - this.canvas.getLayoutBounds().getWidth() / 2)  - scroll.x,
				(y - this.canvas.getLayoutBounds().getHeight() / 2)  - scroll.y
		);
	}

	public static Morton64 morton = new Morton64(2, 32);

	private DoubleVector toLocal(Wrapper wrapper, DoubleVector v) {
		List<Wrapper> parents = new ArrayList<>();
		Wrapper at = wrapper.getParent();
		while (at != null) {
			parents.add(at);
			at = at.getParent();
		}
		Collections.reverse(parents);
		for (Wrapper parent : parents) {
			v = parent.toInner(v);
		}
		return v;
	}

	private void markFrame(ProjectContext context, Wrapper wrapper, DoubleVector start, DoubleVector end) {
		// Get frame-local coordinates
		start = toLocal(wrapper, start);
		end = toLocal(wrapper, end);

		// Calculate mark bounds
		final double radius = 2;
		Rectangle bounds =
				new BoundsBuilder().circle(start, radius).circle(end, radius).quantize(context.tileSize).buildInt();

		// Copy tiles to canvas
		Canvas canvas = new Canvas();
		canvas.setWidth(bounds.width);
		canvas.setHeight(bounds.height);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		ImageFrame frame = (ImageFrame) wrapper.getValue();
		int boundsCornerX = bounds.x / context.tileSize;
		int boundsCornerY = bounds.y / context.tileSize;
		int tileCols = bounds.width / context.tileSize;
		int tileRows = bounds.height / context.tileSize;
		for (int x = 0; x < tileCols; ++x) {
			for (int y = 0; y < tileRows; ++y) {
				Tile tile = frame.tilesGet(morton.spack(boundsCornerX + x, boundsCornerY + y));
				if (tile == null)
					continue;
				gc.drawImage(tile.data,
						tile.dataOffsetX,
						tile.dataOffsetY,
						context.tileSize,
						context.tileSize,
						x * context.tileSize,
						y * context.tileSize,
						context.tileSize,
						context.tileSize
				);
			}
		}

		// Do the stroke
		gc.setLineCap(StrokeLineCap.ROUND);
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(radius * 2);
		gc.strokeLine(start.x - bounds.x, start.y - bounds.y, end.x - bounds.x, end.y - bounds.y);
		WritableImage shot = canvas.snapshot(null, null);

		// Replace tiles in frame
		for (int x = 0; x < tileCols; ++x) {
			for (int y = 0; y < tileRows; ++y) {
				context.change.imageFrame(frame).tilesPut(morton.spack(boundsCornerX + x, boundsCornerY + y),
						new Tile(context, shot, x * context.tileSize, y * context.tileSize)
				);
			}
		}
	}

	private Wrapper createNode(Wrapper parent, ProjectObject node) {
		if (node instanceof ImageFrame) {
			return new Wrapper() {
				class WrapTile {
					private final ImageView widget;

					WrapTile(ProjectContext context, Tile tile, int x, int y) {
						widget = new ImageView(tile.data);
						widget.setMouseTransparent(true);
						widget.setViewport(new Rectangle2D(tile.dataOffsetX,
								tile.dataOffsetY,
								context.tileSize,
								context.tileSize
						));
						widget.setLayoutX(x);
						widget.setLayoutY(y);
					}
				}

				Map<Long, WrapTile> wrapTiles = new HashMap<>();
				Pane draw;

				@Override
				public Wrapper getParent() {
					return parent;
				}

				@Override
				public DoubleVector toInner(DoubleVector vector) {
					return vector;
				}

				@Override
				public ProjectObject getValue() {
					return node;
				}

				@Override
				public void scroll(ProjectContext context, DoubleRectangle oldBounds1, DoubleRectangle newBounds1) {
					Rectangle oldBounds = oldBounds1.scale(3).descaleIntOuter(context.tileSize);
					Rectangle newBounds = newBounds1.scale(3).descaleIntOuter(context.tileSize);

					// Remove tiles outside view bounds
					for (int x = 0; x < oldBounds.width; ++x) {
						for (int y = 0; y < oldBounds.height; ++y) {
							if (newBounds.contains(x, y))
								continue;
							draw.getChildren().remove(wrapTiles.get(morton.spack(oldBounds.x + x, oldBounds.y + y)));
						}
					}

					// Add missing tiles in bounds
					for (int x = 0; x < newBounds.width; ++x) {
						for (int y = 0; y < newBounds.height; ++y) {
							long key = morton.spack(newBounds.x + x, newBounds.y + y);
							if (wrapTiles.containsKey(key))
								continue;
							Tile tile = ((ImageFrame) node).tilesGet(key);
							if (tile == null)
								continue;
							WrapTile wrapTile = new WrapTile(context,
									tile,
									(newBounds.x + x) * context.tileSize,
									(newBounds.y + y) * context.tileSize
							);
							wrapTiles.put(key, wrapTile);
							draw.getChildren().add(wrapTile.widget);
						}
					}
				}

				@Override
				public Node buildCanvas(ProjectContext context, DoubleRectangle bounds) {
					draw = new Pane();
					((ImageFrame) node).addTilesPutListeners(new ImageFrame.TilesPutListener() {
						@Override
						public void accept(ImageFrame target, Long key, Tile value) {
							WrapTile old = wrapTiles.get(key);
							if (old != null)
								draw.getChildren().remove(old.widget);
							long[] indexes = morton.sunpack(key);
							WrapTile wrap = new WrapTile(context,
									value,
									(int) indexes[0] * context.tileSize,
									(int) indexes[1] * context.tileSize
							);
							wrapTiles.put(key, wrap);
							draw.getChildren().add(wrap.widget);
						}
					});
					((ImageFrame) node).addTilesRemoveListeners(new ImageFrame.TilesRemoveListener() {
						@Override
						public void accept(ImageFrame target, Long key) {
							WrapTile old = wrapTiles.remove(key);
							if (old != null)
								draw.getChildren().remove(old.widget);
						}
					});
					scroll(context, bounds, bounds);
					return draw;
				}
			};
		} else
			throw new Assertion();
	}
}
