package com.zarbosoft.shoedemo.structuretree;

import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.shoedemo.*;
import com.zarbosoft.shoedemo.model.*;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeLineCap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ImageNodeWrapper extends Wrapper {
	private final ProjectContext context;
	private final ImageNode node;
	private final ProjectNode.NameSetListener nameListener;
	private final ImageNode.FramesAddListener framesAddListener;
	private final ImageNode.FramesRemoveListener framesRemoveListener;
	private final ImageNode.FramesMoveToListener framesMoveListener;
	private ImageFrame frame;
	private final Wrapper parent;
	DoubleRectangle bounds;
	private TextField propertyName;
	private int frameNumber;

	class WrapTile {
		private final ImageView widget;

		WrapTile(ProjectContext context, Tile tile, int x, int y) {
			widget = new ImageView(tile.data);
			widget.setMouseTransparent(true);
			widget.setViewport(new Rectangle2D(tile.dataOffsetX, tile.dataOffsetY, context.tileSize, context.tileSize));
			widget.setLayoutX(x);
			widget.setLayoutY(y);
		}
	}

	Map<Long, WrapTile> wrapTiles = new HashMap<>();
	Pane draw;
	private ImageFrame.TilesPutAllListener tilesPutListener;

	public ImageNodeWrapper(ProjectContext context, Wrapper parent, int parentIndex, ImageNode node) {
		this.context = context;
		this.node = node;
		this.parent = parent;
		this.parentIndex = parentIndex;
		tree.set(new TreeItem<>(this));
		this.nameListener = node.addNameSetListeners((target, value) -> {
			if (propertyName != null) {
				propertyName.textProperty().setValue(value);
			}
		});
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
		if (draw == null)
			return;
		Rectangle oldBounds = oldBounds1.scale(3).descaleIntOuter(context.tileSize);
		Rectangle newBounds = newBounds1.scale(3).descaleIntOuter(context.tileSize);

		// Remove tiles outside view bounds
		for (int x = 0; x < oldBounds.width; ++x) {
			for (int y = 0; y < oldBounds.height; ++y) {
				if (newBounds.contains(x, y))
					continue;
				draw.getChildren().remove(wrapTiles.get(Main.morton.spack(oldBounds.x + x, oldBounds.y + y)));
			}
		}

		// Add missing tiles in bounds
		for (int x = 0; x < newBounds.width; ++x) {
			for (int y = 0; y < newBounds.height; ++y) {
				long key = Main.morton.spack(newBounds.x + x, newBounds.y + y);
				if (wrapTiles.containsKey(key))
					continue;
				Tile tile = frame.tilesGet(key);
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

	public void attachTiles() {
		frame.addTilesPutAllListeners(tilesPutListener = (target, put, remove) -> {
			for (Long key : remove) {
				WrapTile old = wrapTiles.remove(key);
				if (old != null)
					draw.getChildren().remove(old.widget);
			}
			Rectangle checkBounds = bounds.scale(3).descaleIntOuter(context.tileSize);
			for (Map.Entry<Long, Tile> entry : put.entrySet()) {
				long key = entry.getKey();
				Tile value = entry.getValue();
				WrapTile old = wrapTiles.get(key);
				if (old != null)
					draw.getChildren().remove(old.widget);
				long[] indexes = Main.morton.sunpack(key);
				if (!checkBounds.contains((int) indexes[0], (int) indexes[1]))
					continue;
				WrapTile wrap = new WrapTile(context,
						value,
						(int) indexes[0] * context.tileSize,
						(int) indexes[1] * context.tileSize
				);
				wrapTiles.put(key, wrap);
				draw.getChildren().add(wrap.widget);
			}
		});
	}

	@Override
	public Node buildCanvas(ProjectContext context, DoubleRectangle bounds) {
		this.bounds = bounds;
		draw = new Pane();
		attachTiles();
		return draw;
	}

	@Override
	public Node getCanvas() {
		return draw;
	}

	@Override
	public void destroyCanvas() {
		frame.removeTilesPutAllListeners(tilesPutListener);
		draw = null;
	}

	@Override
	public void markStart(ProjectContext context, DoubleVector start) {
		// nop
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
			this.context.change.project(this.context.project).topRemove(parentIndex, 1);
	}

	@Override
	public ProjectNode separateClone(ProjectContext context) {
		ImageNode clone = ImageNode.create(this.context);
		clone.initialFramesAdd(context, node.frames().stream().map(frame -> {
			ImageFrame newFrame = ImageFrame.create(this.context);
			newFrame.initialOffsetSet(context, frame.offset());
			newFrame.initialLengthSet(context, frame.length());
			newFrame.initialTilesPutAll(context, frame.tiles());
			return newFrame;
		}).collect(Collectors.toList()));
		clone.initialNameSet(context, String.format("%s (copy)", node.name()));
		return clone;
	}

	public Rectangle render(GraphicsContext gc, ImageFrame frame, Rectangle crop) {
		int tileOffsetX = Math.floorMod(crop.x, context.tileSize);
		int tileOffsetY = Math.floorMod(crop.y, context.tileSize);
		Rectangle tileBounds = crop.divide(context.tileSize);
		for (int x = 0; x < tileBounds.width; ++x) {
			for (int y = 0; y < tileBounds.height; ++y) {
				Tile tile = frame.tilesGet(Main.morton.spack(tileBounds.x + x, tileBounds.y + y));
				if (tile == null)
					continue;
				gc.drawImage(tile.data,
						tile.dataOffsetX,
						tile.dataOffsetY,
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
		SnapshotParameters parameters = new SnapshotParameters();
		parameters.setFill(Color.TRANSPARENT);
		return canvas.snapshot(parameters, null);
	}

	@Override
	public void mark(ProjectContext context, DoubleVector start, DoubleVector end) {
		// Get frame-local coordinates
		start = Main.toLocal(this, start);
		end = Main.toLocal(this, end);

		// Calculate mark bounds
		final double radius = 2;
		Rectangle bounds =
				new BoundsBuilder().circle(start, radius).circle(end, radius).quantize(context.tileSize).buildInt();

		// Copy tiles to canvas
		Canvas canvas = new Canvas();
		canvas.setWidth(bounds.width);
		canvas.setHeight(bounds.height);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		Rectangle tileBounds = render(gc, frame, bounds);

		// Do the stroke
		System.out.format("stroke %s %s to %s %s\n",start.x,start.y,end.x,end.y);
		gc.setLineCap(StrokeLineCap.ROUND);
		gc.setStroke(Color.BLACK);
		gc.setLineWidth(radius * 2);
		gc.strokeLine(start.x - bounds.x, start.y - bounds.y, end.x - bounds.x, end.y - bounds.y);
		WritableImage shot = snapshot(canvas);

		// Replace tiles in frame
		for (int x = 0; x < tileBounds.width; ++x) {
			for (int y = 0; y < tileBounds.height; ++y) {
				context.change.imageFrame(frame).tilesPut(Main.morton.spack(tileBounds.x + x, tileBounds.y + y),
						new Tile(context, shot, x * context.tileSize, y * context.tileSize)
				);
			}
		}
	}

	private ImageFrame findFrame(int frameNumber) {
		int at = 0;
		for (ImageFrame frame : node.frames()) {
			if (frameNumber >= at && (frame.length() == -1 || frameNumber < at + frame.length()))
				return frame;
			at += frame.length();
		}
		throw new Assertion();
	}

	@Override
	public void setFrame(ProjectContext context, int frameNumber) {
		this.frameNumber = frameNumber;
		ImageFrame found = findFrame(frameNumber);
		if (frame != found) {
			if (draw != null) {
				frame.removeTilesPutAllListeners(tilesPutListener);
				draw.getChildren().clear();
			}
			frame = found;
			if (draw != null) {
				attachTiles();
			}
		}
	}

	@Override
	public void remove(ProjectContext context) {
		node.removeNameSetListeners(nameListener);
		node.removeFramesAddListeners(framesAddListener);
		node.removeFramesRemoveListeners(framesRemoveListener);
		node.removeFramesMoveToListeners(framesMoveListener);
	}

	@Override
	public Node createProperties(ProjectContext context) {
		return new WidgetFormBuilder().text("Name", t -> {
			propertyName = t;
			t
					.textProperty()
					.addListener((observable, oldValue, newValue) -> this.context.change
							.projectNode(node)
							.nameSet(newValue));
			t.setText(node.name());
		}).build();
	}

	@Override
	public void destroyProperties() {
		propertyName = null;
	}

	@Override
	public void render(GraphicsContext gc, int frame, Rectangle crop) {
		render(gc, findFrame(frame), crop);
	}

	@Override
	public void removeChild(ProjectContext context, int index) {
		throw new Assertion();
	}
}
