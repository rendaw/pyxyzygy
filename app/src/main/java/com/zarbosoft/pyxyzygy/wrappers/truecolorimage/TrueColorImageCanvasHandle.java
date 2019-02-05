package com.zarbosoft.pyxyzygy.wrappers.truecolorimage;

import com.zarbosoft.internal.pyxyzygy_seed.model.Rectangle;
import com.zarbosoft.internal.pyxyzygy_seed.model.Vector;
import com.zarbosoft.pyxyzygy.*;
import com.zarbosoft.pyxyzygy.model.*;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.HashMap;
import java.util.Map;

import static com.zarbosoft.pyxyzygy.Launch.opacityMax;

public class TrueColorImageCanvasHandle extends Wrapper.CanvasHandle {
	final Wrapper.CanvasHandle parent;
	private final ProjectNode.OpacitySetListener opacityListener;
	private TrueColorImageNodeWrapper wrapper;
	private TrueColorImageFrame.TilesPutAllListener tilesPutListener;
	Map<Long, WrapTile> wrapTiles = new HashMap<>();
	private int frameNumber = 0;
	TrueColorImageFrame frame;
	private final TrueColorImageNode.FramesAddListener framesAddListener;
	private final TrueColorImageNode.FramesRemoveListener framesRemoveListener;
	private final TrueColorImageNode.FramesMoveToListener framesMoveListener;
	DoubleRectangle bounds = new DoubleRectangle(0, 0, 0, 0);
	SimpleIntegerProperty zoom = new SimpleIntegerProperty(1);

	public TrueColorImageCanvasHandle(
			ProjectContext context, Wrapper.CanvasHandle parent, TrueColorImageNodeWrapper wrapper
	) {
		this.parent = parent;
		this.wrapper = wrapper;
		this.opacityListener = wrapper.node.addOpacitySetListeners((target, value) -> {
			inner.setOpacity((double) value / opacityMax);
		});

		this.framesAddListener =
				wrapper.node.addFramesAddListeners((target, at, value) -> setFrame(context, frameNumber));
		this.framesRemoveListener =
				wrapper.node.addFramesRemoveListeners((target, at, count) -> setFrame(context, frameNumber));
		this.framesMoveListener =
				wrapper.node.addFramesMoveToListeners((target, source, count, dest) -> setFrame(context, frameNumber));

		attachTiles(context);
	}

	@Override
	public void remove(ProjectContext context) {
		wrapper.canvasHandle = null;
		wrapper.node.removeOpacitySetListeners(opacityListener);
		wrapper.node.removeFramesAddListeners(framesAddListener);
		wrapper.node.removeFramesRemoveListeners(framesRemoveListener);
		wrapper.node.removeFramesMoveToListeners(framesMoveListener);
		detachTiles();
	}

	@Override
	public Wrapper getWrapper() {
		return wrapper;
	}

	@Override
	public Wrapper.CanvasHandle getParent() {
		return parent;
	}

	@Override
	public void setViewport(ProjectContext context, DoubleRectangle newBounds1, int positiveZoom) {
		//System.out.format("set viewport; b %s old z %s, z %s\n", newBounds1, this.zoom.get(),positiveZoom);
		this.zoom.set(positiveZoom);
		DoubleRectangle oldBounds1 = this.bounds;
		this.bounds = newBounds1;

		Rectangle oldBounds = oldBounds1.scale(3).quantize(context.tileSize);
		Rectangle newBounds = newBounds1.scale(3).quantize(context.tileSize);

			// Remove tiles outside view bounds
			for (int x = 0; x < oldBounds.width; ++x) {
				for (int y = 0; y < oldBounds.height; ++y) {
					if (newBounds.contains(x, y))
						continue;
					long key = oldBounds.corner().to1D();
					inner.getChildren().remove(wrapTiles.get(key));
				}
			}

		// Add missing tiles in bounds
		for (int x = 0; x < newBounds.width; ++x) {
			for (int y = 0; y < newBounds.height; ++y) {
				Vector useIndexes = newBounds.corner().plus(x, y);
				long key = useIndexes.to1D();
				if (wrapTiles.containsKey(key)) {
					continue;
				}
				Tile tile = (Tile) frame.tilesGet(key);
				if (tile == null) {
					continue;
				}
				WrapTile wrapTile = new WrapTile(context,
						tile,
						useIndexes.x * context.tileSize,
						useIndexes.y * context.tileSize
				);
				wrapTiles.put(key, wrapTile);
				inner.getChildren().add(wrapTile.widget);
			}
		}
	}

	@Override
	public void setFrame(ProjectContext context, int frameNumber) {
		this.frameNumber = frameNumber;
		TrueColorImageFrame oldFrame = frame;
		frame = findFrame(frameNumber);
		System.out.format("set frame %s: %s vs %s\n", frameNumber, oldFrame, frame);
		if (oldFrame != frame) {
			detachTiles();
			attachTiles(context);
		}
	}

	public void attachTiles(ProjectContext context) {
		frame.addTilesPutAllListeners(tilesPutListener = (target, put, remove) -> {
			for (Long key : remove) {
				WrapTile old = wrapTiles.remove(key);
				if (old != null)
					inner.getChildren().remove(old.widget);
			}
			Rectangle checkBounds = bounds.scale(3).quantize(context.tileSize);
			//System.out.format("attach tiles: %s = q %s\n", wrapper.bounds, checkBounds);
			for (Map.Entry<Long, TileBase> entry : put.entrySet()) {
				long key = entry.getKey();
				Vector indexes = Vector.from1D(key);
				if (!checkBounds.contains(indexes.x, indexes.y)) {
					continue;
				}
				Tile value = (Tile) entry.getValue();
				WrapTile old = wrapTiles.get(key);
				if (old != null) {
					old.update(context, value);
				} else {
					WrapTile wrap = new WrapTile(context,
							value,
							indexes.x * context.tileSize,
							indexes.y * context.tileSize
					);
					wrapTiles.put(key, wrap);
					inner.getChildren().add(wrap.widget);
				}
			}
		});
	}

	public void detachTiles() {
		frame.removeTilesPutAllListeners(tilesPutListener);
		tilesPutListener = null;
		inner.getChildren().clear();
		wrapTiles.clear();
	}

	public Rectangle render(ProjectContext context, TrueColorImage gc, Rectangle crop) {
		Rectangle tileBounds = crop.quantize(context.tileSize);
		//System.out.format("render tb %s\n", tileBounds);
		for (int x = 0; x < tileBounds.width; ++x) {
			for (int y = 0; y < tileBounds.height; ++y) {
				Tile tile = (Tile) frame.tilesGet(tileBounds.corner().plus(x, y).to1D());
				if (tile == null)
					continue;
				final int renderX = (x + tileBounds.x) * context.tileSize - crop.x;
				final int renderY = (y + tileBounds.y) * context.tileSize - crop.y;
				//System.out.format("composing at %s %s op %s\n", renderX, renderY, opacity);
				System.out.flush();
				TrueColorImage data = tile.getData(context);
				gc.compose(data, renderX, renderY, (float) 1);
			}
		}
		return tileBounds;
	}

	TrueColorImageFrame findFrame(int frameNumber) {
		return wrapper.findFrame(wrapper.node, frameNumber).frame;
	}

	public void drop(ProjectContext context, Rectangle unitBounds, TrueColorImage image) {
		for (int x = 0; x < unitBounds.width; ++x) {
			for (int y = 0; y < unitBounds.height; ++y) {
				final int x0 = x;
				final int y0 = y;
				/*
				System.out.format("\tcopy %s %s: %s %s by %s %s\n",
						x0,
						y0,
						x0 * context.tileSize,
						y0 * context.tileSize,
						context.tileSize,
						context.tileSize
				);
				*/
				TrueColorImage shot =
						image.copy(x0 * context.tileSize, y0 * context.tileSize, context.tileSize, context.tileSize);
				context.history.change(c -> c
						.trueColorImageFrame(frame)
						.tilesPut(unitBounds.corner().plus(x0, y0).to1D(), Tile.create(context, shot)));
			}
		}
	}

	void clear(ProjectContext context, Rectangle bounds) {
		Rectangle quantizedBounds = bounds.quantize(context.tileSize);
		Rectangle clearBounds = quantizedBounds.multiply(context.tileSize);
		TrueColorImage clearCanvas = TrueColorImage.create(clearBounds.width, clearBounds.height);
		render(context, clearCanvas, clearBounds);
		Vector offset = bounds.corner().minus(clearBounds.corner());
		clearCanvas.clear(offset.x, offset.y, bounds.width, bounds.height);
		drop(context, quantizedBounds, clearCanvas);
	}

	@Override
	public DoubleVector toInner(DoubleVector vector) {
		return vector;
	}
}
