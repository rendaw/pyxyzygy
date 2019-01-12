package com.zarbosoft.shoedemo.structuretree;

import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.shoedemo.*;
import com.zarbosoft.shoedemo.model.*;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zarbosoft.shoedemo.Main.nodeFormFields;
import static com.zarbosoft.shoedemo.Main.opacityMax;
import static com.zarbosoft.shoedemo.ProjectContext.uniqueName1;

public class ImageNodeWrapper extends Wrapper {
	private final ProjectContext context;
	private final ImageNode node;
	private final ImageNode.FramesAddListener framesAddListener;
	private final ImageNode.FramesRemoveListener framesRemoveListener;
	private final ImageNode.FramesMoveToListener framesMoveListener;
	private ImageFrame frame;
	private final Wrapper parent;
	DoubleRectangle bounds;
	private int frameNumber;
	Map<Long, WrapTile> wrapTiles = new HashMap<>();
	Pane draw;
	private ImageFrame.TilesPutAllListener tilesPutListener;

	class WrapTile {
		private final ImageView widget;

		WrapTile(Tile tile, int x, int y) {
			widget = new ImageView();
			widget.setMouseTransparent(true);
			widget.setLayoutX(x);
			widget.setLayoutY(y);
			update(tile);
		}

		public void update(Tile tile) {
			widget.setImage(tile.getData(context));
		}
	}

	public ImageNodeWrapper(ProjectContext context, Wrapper parent, int parentIndex, ImageNode node) {
		this.context = context;
		this.node = node;
		this.parent = parent;
		this.parentIndex = parentIndex;
		tree.set(new TreeItem<>(this));
		this.framesAddListener = node.addFramesAddListeners((target, at, value) -> setFrame(context, frameNumber));
		this.framesRemoveListener =
				node.addFramesRemoveListeners((target, at, count) -> setFrame(context, frameNumber));
		this.framesMoveListener =
				node.addFramesMoveToListeners((target, source, count, dest) -> setFrame(context, frameNumber));
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
	public void scroll(ProjectContext context, DoubleRectangle oldBounds1, DoubleRectangle newBounds1) {
		this.bounds = newBounds1;
		System.out.format("image scroll; b %s\n", bounds);
		if (draw == null)
			return;
		Rectangle oldBounds = oldBounds1.scale(3).descaleIntOuter(context.tileSize);
		Rectangle newBounds = bounds.scale(3).descaleIntOuter(context.tileSize);

		// Remove tiles outside view bounds
		for (int x = 0; x < oldBounds.width; ++x) {
			for (int y = 0; y < oldBounds.height; ++y) {
				if (newBounds.contains(x, y))
					continue;
				long key = oldBounds.corner().to1D();
				draw.getChildren().remove(wrapTiles.get(key));
			}
		}

		// Add missing tiles in bounds
		for (int x = 0; x < newBounds.width; ++x) {
			for (int y = 0; y < newBounds.height; ++y) {
				Vector useIndexes = newBounds.corner().plus(x,y );
				long key = useIndexes.to1D();
				if (wrapTiles.containsKey(key)) {
					continue;
				}
				Tile tile = (Tile) frame.tilesGet(key);
				if (tile == null) {
					continue;
				}
				WrapTile wrapTile =
						new WrapTile(tile, useIndexes.x * context.tileSize, useIndexes.y * context.tileSize);
				wrapTiles.put(key, wrapTile);
				draw.getChildren().add(wrapTile.widget);
			}
		}
	}

	public void attachTiles() {
		frame.addTilesPutAllListeners(tilesPutListener = (target, put, remove) -> {
			for (Long key : remove) {
				WrapTile old = wrapTiles.remove(key);
				if (old != null)
					draw.getChildren().remove(old.widget);
			}
			Rectangle checkBounds = bounds.scale(3).descaleIntOuter(context.tileSize);
			for (Map.Entry<Long, TileBase> entry : put.entrySet()) {
				long key = entry.getKey();
				Vector indexes = Vector.from1D(key);
				if (!checkBounds.contains(indexes.x, indexes.y)) {
					continue;
				}
				Tile value = (Tile) entry.getValue();
				WrapTile old = wrapTiles.get(key);
				if (old != null) {
					old.update(value);
				} else {
					WrapTile wrap = new WrapTile(value, indexes.x * context.tileSize, indexes.y * context.tileSize);
					wrapTiles.put(key, wrap);
					draw.getChildren().add(wrap.widget);
				}
			}
		});
	}

	public void detachTiles() {
		frame.removeTilesPutAllListeners(tilesPutListener);
		tilesPutListener = null;
		draw.getChildren().clear();
		wrapTiles.clear();
	}

	@Override
	public WidgetHandle buildCanvas(ProjectContext context, DoubleRectangle bounds) {
		this.bounds = bounds;
		return new WidgetHandle() {
			private final ProjectNode.OpacitySetListener opacityListener;

			{
				draw = new Pane();
				this.opacityListener = node.addOpacitySetListeners((target, value) -> {
					draw.setOpacity((double) value / opacityMax);
				});
				attachTiles();
			}

			@Override
			public Node getWidget() {
				return draw;
			}

			@Override
			public void remove() {
				node.removeOpacitySetListeners(opacityListener);
				detachTiles();
				draw = null;
			}
		};
	}

	@Override
	public void markStart(ProjectContext context, DoubleVector start) {
		// nop
	}

	@Override
	public void mark(ProjectContext context, DoubleVector start, DoubleVector end) {
		// Get frame-local coordinates
		System.out.format("stroke start %s %s to %s %s\n", start.x, start.y, end.x, end.y);
		start = Window.toLocal(this, start);
		end = Window.toLocal(this, end);

		// Calculate mark bounds
		final double radius = 2;
		Rectangle bounds =
				new BoundsBuilder().circle(start, radius).circle(end, radius).quantize(context.tileSize).buildInt();

		// Copy tiles to canvas
		Canvas canvas = new Canvas();
		canvas.setWidth(bounds.width);
		canvas.setHeight(bounds.height);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		Rectangle tileBounds = render(gc, frame, bounds, 1);

		// Do the stroke
		System.out.format("stroke %s %s to %s %s\n", start.x, start.y, end.x, end.y);
		gc.setLineCap(StrokeLineCap.ROUND);
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(radius * 2);
		gc.strokeLine(start.x - bounds.x, start.y - bounds.y, end.x - bounds.x, end.y - bounds.y);

		// Replace tiles in frame
		for (int x = 0; x < tileBounds.width; ++x) {
			for (int y = 0; y < tileBounds.height; ++y) {
				final int x0 = x;
				final int y0 = y;
				WritableImage shot = snapshot(canvas,
						x0 * context.tileSize,
						y0 * context.tileSize,
						context.tileSize,
						context.tileSize
				);
				context.history.change(c -> c
						.imageFrame(frame)
						.tilesPut(tileBounds.corner().plus(x0, y0).to1D(), Tile.create(context, shot)));
			}
		}
	}

	@Override
	public boolean addChildren(ProjectContext context, int at, List<ProjectNode> child) {
		return false;
	}

	@Override
	public void delete(ProjectContext context) {
		if (parent != null)
			parent.removeChild(context, parentIndex);
		else
			this.context.history.change(c -> c.project(this.context.project).topRemove(parentIndex, 1));
	}

	@Override
	public ProjectNode separateClone(ProjectContext context) {
		ImageNode clone = ImageNode.create(this.context);
		clone.initialNameSet(context, uniqueName1(node.name()));
		clone.initialOpacitySet(context, node.opacity());
		clone.initialFramesAdd(context, node.frames().stream().map(frame -> {
			ImageFrame newFrame = ImageFrame.create(this.context);
			newFrame.initialOffsetSet(context, frame.offset());
			newFrame.initialLengthSet(context, frame.length());
			newFrame.initialTilesPutAll(context, frame.tiles());
			return newFrame;
		}).collect(Collectors.toList()));
		return clone;
	}

	public Rectangle render(GraphicsContext gc, ImageFrame frame, Rectangle crop, double opacity) {
		gc.setGlobalAlpha(opacity);
		int tileOffsetX = Math.floorMod(crop.x, context.tileSize);
		int tileOffsetY = Math.floorMod(crop.y, context.tileSize);
		Rectangle tileBounds = crop.divide(context.tileSize);
		for (int x = 0; x < tileBounds.width; ++x) {
			for (int y = 0; y < tileBounds.height; ++y) {
				Tile tile = (Tile) frame.tilesGet(tileBounds.corner().plus(x, y).to1D());
				if (tile == null)
					continue;
				gc.drawImage(tile.getData(context),
						0,
						0,
						context.tileSize,
						context.tileSize,
						x * context.tileSize - tileOffsetX,
						y * context.tileSize - tileOffsetY,
						context.tileSize,
						context.tileSize
				);
			}
		}
		return tileBounds;
	}

	public static WritableImage snapshot(Canvas canvas) {
		return snapshot(canvas, 0, 0, (int) canvas.getWidth(), (int) canvas.getHeight());
	}

	public static WritableImage snapshot(Canvas canvas, int x, int y, int w, int h) {
		SnapshotParameters parameters = new SnapshotParameters();
		parameters.setFill(Color.TRANSPARENT);
		parameters.setViewport(new Rectangle2D(x, y, w, h));
		return canvas.snapshot(parameters, null);
	}

	private ImageFrame findFrame(int frameNumber) {
		return findFrame(node, frameNumber).frame;
	}

	public static class FrameResult {
		public final ImageFrame frame;
		public final int at;
		public final int frameIndex;

		public FrameResult(ImageFrame frame, int at, int frameIndex) {
			this.frame = frame;
			this.at = at;
			this.frameIndex = frameIndex;
		}
	}

	public static FrameResult findFrame(ImageNode node, int frame) {
		int at = 0;
		for (int i = 0; i < node.framesLength(); ++i) {
			ImageFrame pos = node.frames().get(i);
			if ((i == node.framesLength() - 1) || (frame >= at && (pos.length() == -1 || frame < at + pos.length()))) {
				return new FrameResult(pos, at, i);
			}
			at += pos.length();
		}
		throw new Assertion();
	}

	@Override
	public void setFrame(ProjectContext context, int frameNumber) {
		this.frameNumber = frameNumber;
		ImageFrame found = findFrame(frameNumber);
		System.out.format("set frame %s: %s vs %s\n",frameNumber, frame, found);
		if (frame != found) {
			if (draw != null) {
				detachTiles();
			}
			frame = found;
			if (draw != null) {
				attachTiles();
			}
		}
	}

	@Override
	public void remove(ProjectContext context) {
		node.removeFramesAddListeners(framesAddListener);
		node.removeFramesRemoveListeners(framesRemoveListener);
		node.removeFramesMoveToListeners(framesMoveListener);
	}

	@Override
	public WidgetHandle createProperties(ProjectContext context) {
		List<Runnable> cleanup = new ArrayList<>();
		Node widget = new WidgetFormBuilder().apply(b -> cleanup.add(nodeFormFields(context, b, node))).build();
		return new WidgetHandle() {
			@Override
			public Node getWidget() {
				return widget;
			}

			@Override
			public void remove() {
				cleanup.forEach(c -> c.run());
			}
		};
	}

	@Override
	public void render(GraphicsContext gc, int frame, Rectangle crop) {
		render(gc, findFrame(frame), crop, (double) node.opacity() / opacityMax);
	}

	@Override
	public void removeChild(ProjectContext context, int index) {
		throw new Assertion();
	}

	@Override
	public TakesChildren takesChildren() {
		return TakesChildren.NONE;
	}
}
