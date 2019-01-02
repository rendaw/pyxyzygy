package com.zarbosoft.shoedemo;

import com.gojuno.morton.Morton64;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.shoedemo.model.*;
import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.SplitPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static com.zarbosoft.rendaw.common.Common.sublist;
import static com.zarbosoft.shoedemo.Timeline.moveTo;

public class Main extends Application {
	public static void main(String[] args) {
		Main.launch(args);
	}

	public abstract static class Wrapper {
		public abstract Wrapper getParent();

		List<Wrapper> children = new ArrayList<>();

		public abstract DoubleVector toInner(DoubleVector vector);

		public abstract ProjectObject getValue();

		public abstract void scroll(ProjectContext context, DoubleRectangle oldBounds, DoubleRectangle newBounds);

		public abstract Node buildCanvas(ProjectContext context, DoubleRectangle bounds);

		public abstract Node getCanvas();

		public abstract void destroyCanvas();

		public abstract void mark(ProjectContext context, DoubleVector start, DoubleVector end);

		public abstract void setFrame(int frameNumber);
	}

	ProjectContext context;
	Wrapper selectedRoot;

	@Override
	public void start(Stage primaryStage) throws Exception {
		Path path = Paths.get(this.getParameters().getUnnamed().get(0));
		context = ProjectContext.create(path);
		ImageNode imageNode = ImageNode.create(context);
		context.change.project(context.project).topAdd(imageNode);
		ImageFrame imageFrame = ImageFrame.create(context);
		context.change.imageNode(imageNode).framesAdd(imageFrame);
		context.change.imageFrame(imageFrame).lengthSet(-1);
		context.finishChange();
		context.clearHistory();

		selectedRoot = createNode(null, imageNode);
		selectedRoot.setFrame(0);

		Editor editor = new Editor(context);
		editor.setNodes(selectedRoot, selectedRoot);

		Timeline timeline = new Timeline(context);
		timeline.setNodes(selectedRoot, selectedRoot);

		SplitPane top = new SplitPane();
		top.setOrientation(Orientation.VERTICAL);
		top.getItems().addAll(editor.canvas, timeline.foreground);

		primaryStage.setTitle("Shoe Demo 2");
		Scene scene = new Scene(top, 600, 600);

		scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.U || (e.isControlDown() && e.getCode() == KeyCode.Z)) {
				this.context.undo();
			} else if (e.isControlDown() && (e.getCode() == KeyCode.R || e.getCode() == KeyCode.Y)) {
				this.context.redo();
			}
		});

		primaryStage.setScene(scene);
		primaryStage.show();
	}

	public static Morton64 morton = new Morton64(2, 32);

	public static List<Wrapper> getAncestors(Wrapper start, Wrapper target) {
		List<Wrapper> ancestors = new ArrayList<>();
		Wrapper at = target.getParent();
		while (at != start) {
			ancestors.add(at);
			at = at.getParent();
		}
		Collections.reverse(ancestors);
		return ancestors;
	}

	private DoubleVector toLocal(Main.Wrapper wrapper, DoubleVector v) {
		for (Main.Wrapper parent : getAncestors(null, wrapper)) {
			v = parent.toInner(v);
		}
		return v;
	}

	private Wrapper createNode(Wrapper parent, ProjectObject node) {
		if (false) {
			throw new Assertion();
		} else if (node instanceof ImageNode) {
			class ImageNodeWrapper extends Wrapper {
				DoubleRectangle bounds;

				public ImageNodeWrapper() {
					((ImageNode) node).addFramesAddListeners((target, at, value) -> {
						children.addAll(at, value
								.stream()
								.map(c -> createNode(ImageNodeWrapper.this, c))
								.collect(Collectors.toList()));
						findFrame();
					});
					((ImageNode) node).addFramesRemoveListeners((target, at, count) -> {
						sublist(children, at, at + count).clear();
						findFrame();
					});
					((ImageNode) node).addFramesMoveToListeners((target, source, count, dest) -> {
						moveTo(children, source, count, dest);
						findFrame();
					});
					((ImageNode) node).addFramesClearListeners(target -> {
						throw new Assertion();
					});
				}

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
				public void scroll(ProjectContext context, DoubleRectangle oldBounds, DoubleRectangle newBounds) {
					this.bounds = newBounds;
					children.forEach(c -> c.scroll(context, oldBounds, newBounds));
				}

				Group canvas;
				int frameNumber;
				Wrapper frame;

				@Override
				public Node buildCanvas(ProjectContext context, DoubleRectangle bounds) {
					this.bounds = bounds;
					canvas = new Group();
					updateCanvasFrame();
					return canvas;
				}

				public void updateCanvasFrame() {
					canvas.getChildren().clear();
					canvas.getChildren().add(frame.buildCanvas(context, bounds));
				}

				@Override
				public Node getCanvas() {
					return canvas;
				}

				@Override
				public void destroyCanvas() {
					frame.destroyCanvas();
					canvas = null;
				}

				@Override
				public void mark(ProjectContext context, DoubleVector start, DoubleVector end) {
					frame.mark(context, start, end);
				}

				public void findFrame() {
					int at = 0;
					ImageFrame found = null;
					for (ImageFrame frame : ((ImageNode) node).frames()) {
						if (at > frameNumber)
							break;
						found = frame;
						at += frame.length();
					}
					if (frame == null || frame.getValue() != found) {
						if (canvas != null)
							frame.destroyCanvas();
						frame = createNode(this, found);
						if (canvas != null)
							updateCanvasFrame();
					}
				}

				@Override
				public void setFrame(int frameNumber) {
					this.frameNumber = frameNumber;
					findFrame();
				}
			}
			return new ImageNodeWrapper();
		} else if (node instanceof ImageFrame) {
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
				private ImageFrame.TilesPutAllListener tilesPutListener;

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
					if (draw == null) return;
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
					((ImageFrame) node).addTilesPutAllListeners(tilesPutListener = (target,put, remove) -> {
						for (Long key : remove) {
							WrapTile old = wrapTiles.remove(key);
							if (old != null)
								draw.getChildren().remove(old.widget);
						}
						for (Map.Entry<Long, Tile> entry : put.entrySet()) {
							long key = entry.getKey();
							Tile value = entry.getValue();
							WrapTile old = wrapTiles.get(key);
							if (old != null)
								draw.getChildren().remove(old.widget);
							long[] indexes = morton.sunpack(key);
							WrapTile wrap = new WrapTile(
									context,
									value,
									(int) indexes[0] * context.tileSize,
									(int) indexes[1] * context.tileSize
							);
							wrapTiles.put(key, wrap);
							draw.getChildren().add(wrap.widget);
						}
					});
					scroll(context, bounds, bounds);
					return draw;
				}

				@Override
				public Node getCanvas() {
					return draw;
				}

				@Override
				public void destroyCanvas() {
					((ImageFrame) node).removeTilesPutAllListeners(tilesPutListener);
					draw = null;
				}

				@Override
				public void mark(ProjectContext context, DoubleVector start, DoubleVector end) {
					// Get frame-local coordinates
					start = toLocal(this, start);
					end = toLocal(this, end);

					// Calculate mark bounds
					final double radius = 2;
					Rectangle bounds = new BoundsBuilder()
							.circle(start, radius)
							.circle(end, radius)
							.quantize(context.tileSize)
							.buildInt();

					// Copy tiles to canvas
					Canvas canvas = new Canvas();
					canvas.setWidth(bounds.width);
					canvas.setHeight(bounds.height);
					GraphicsContext gc = canvas.getGraphicsContext2D();
					ImageFrame frame = (ImageFrame) node;
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
							context.change
									.imageFrame(frame)
									.tilesPut(morton.spack(boundsCornerX + x, boundsCornerY + y),
											new Tile(context, shot, x * context.tileSize, y * context.tileSize)
									);
						}
					}
				}

				@Override
				public void setFrame(int frameNumber) {
					// nop
				}
			};
		} else
			throw new Assertion();
	}

}
