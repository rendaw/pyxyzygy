package com.zarbosoft.pyxyzygy.app.wrappers.truecolorimage;

import com.zarbosoft.pyxyzygy.app.CanvasHandle;
import com.zarbosoft.pyxyzygy.app.DoubleRectangle;
import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.Wrapper;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.model.v0.Tile;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectNode;
import com.zarbosoft.pyxyzygy.core.model.v0.TileBase;
import com.zarbosoft.pyxyzygy.core.model.v0.TrueColorImageFrame;
import com.zarbosoft.pyxyzygy.core.model.v0.TrueColorImageNode;
import com.zarbosoft.pyxyzygy.seed.model.v0.Rectangle;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.*;

import static com.zarbosoft.pyxyzygy.app.Global.opacityMax;

public class TrueColorImageCanvasHandle extends CanvasHandle {
	final CanvasHandle parent;
	private final ProjectNode.OpacitySetListener opacityListener;
	private TrueColorImageNodeWrapper wrapper;
	private TrueColorImageFrame.TilesPutAllListener tilesPutListener;
	private TrueColorImageFrame.TilesClearListener tilesClearListener;
	Map<Long, WrapTile> wrapTiles = new HashMap<>();
	TrueColorImageFrame frame;
	private final TrueColorImageNode.FramesAddListener framesAddListener;
	private final TrueColorImageNode.FramesRemoveListener framesRemoveListener;
	private final TrueColorImageNode.FramesMoveToListener framesMoveListener;
	private final List<Runnable> frameCleanup = new ArrayList<>();
	SimpleIntegerProperty zoom = new SimpleIntegerProperty(1);

	public TrueColorImageCanvasHandle(
			ProjectContext context, CanvasHandle parent, TrueColorImageNodeWrapper wrapper
	) {
		this.parent = parent;
		this.wrapper = wrapper;
		this.opacityListener = wrapper.node.addOpacitySetListeners((target, value) -> {
			inner.setOpacity((double) value / opacityMax);
		});

		wrapper.node.mirrorFrames(frameCleanup,frame -> {
			TrueColorImageFrame.OffsetSetListener offsetListener =
					frame.addOffsetSetListeners((target, value) -> updateFrame(context));
			TrueColorImageFrame.LengthSetListener lengthListener =
					frame.addLengthSetListeners((target, value) -> updateFrame(context));
			return () ->{
				frame.removeOffsetSetListeners(offsetListener);
				frame.removeLengthSetListeners(lengthListener);
			};
		},cleanup -> cleanup.run(), at -> {
			updateFrame(context);
		});
		this.framesAddListener = wrapper.node.addFramesAddListeners((target, at, value) -> updateFrame(context));
		this.framesRemoveListener = wrapper.node.addFramesRemoveListeners((target, at, count) -> updateFrame(context));
		this.framesMoveListener =
				wrapper.node.addFramesMoveToListeners((target, source, count, dest) -> updateFrame(context));

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
	public CanvasHandle getParent() {
		return parent;
	}

	@Override
	public void setViewport(ProjectContext context, DoubleRectangle newBounds1, int positiveZoom) {
		if (this.zoom.get() == positiveZoom && Objects.equals(this.bounds.get(), newBounds1)) return;
		this.zoom.set(positiveZoom);
		DoubleRectangle oldBounds1 = this.bounds.get();
		this.bounds.set(newBounds1);

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
				WrapTile wrapTile =
						new WrapTile(context, tile, useIndexes.x * context.tileSize, useIndexes.y * context.tileSize);
				wrapTiles.put(key, wrapTile);
				inner.getChildren().add(wrapTile.widget);
			}
		}
	}

	@Override
	public void setFrame(ProjectContext context, int frameNumber) {
		this.frameNumber.set(frameNumber);
		updateFrame(context);
	}

	public void updateFrame(ProjectContext context) {
		TrueColorImageFrame oldFrame = frame;
		TrueColorImageNodeWrapper.FrameResult found =
				TrueColorImageNodeWrapper.findFrame(wrapper.node, frameNumber.get());
		frame = found.frame;
		if (oldFrame != frame) {
			detachTiles();
			attachTiles(context);
		}
		do {
			if (wrapper.node.framesLength() == 1) {
				previousFrame.set(-1);
				break;
			}
			int frameIndex = found.frameIndex - 1;
			if (frameIndex == -1)
				frameIndex = wrapper.node.framesLength() - 1;
			int outFrame = 0;
			for (int i = 0; i < frameIndex; ++i) {
				outFrame += wrapper.node.framesGet(i).length();
			}
			previousFrame.set(outFrame);
		} while (false);
	}

	public void attachTiles(ProjectContext context) {
		frame.addTilesPutAllListeners(tilesPutListener = (target, put, remove) -> {
			for (Long key : remove) {
				WrapTile old = wrapTiles.remove(key);
				if (old != null)
					inner.getChildren().remove(old.widget);
			}
			Rectangle checkBounds = bounds.get().scale(3).quantize(context.tileSize);
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
					WrapTile wrap =
							new WrapTile(context, value, indexes.x * context.tileSize, indexes.y * context.tileSize);
					wrapTiles.put(key, wrap);
					inner.getChildren().add(wrap.widget);
				}
			}
		});
		frame.addTilesClearListeners(tilesClearListener = (target) -> {
			inner.getChildren().clear();
			wrapTiles.clear();
		});
	}

	public void detachTiles() {
		frame.removeTilesPutAllListeners(tilesPutListener);
		frame.removeTilesClearListeners(tilesClearListener);
		tilesPutListener = null;
		inner.getChildren().clear();
		wrapTiles.clear();
	}

	public Rectangle render(ProjectContext context, TrueColorImage gc, Rectangle crop) {
		Rectangle tileBounds = crop.quantize(context.tileSize);
		for (int x = 0; x < tileBounds.width; ++x) {
			for (int y = 0; y < tileBounds.height; ++y) {
				Tile tile = (Tile) frame.tilesGet(tileBounds.corner().plus(x, y).to1D());
				if (tile == null)
					continue;
				final int renderX = (x + tileBounds.x) * context.tileSize - crop.x;
				final int renderY = (y + tileBounds.y) * context.tileSize - crop.y;
				TrueColorImage data = tile.getData(context);
				gc.compose(data, renderX, renderY, (float) 1);
			}
		}
		return tileBounds;
	}

	public void drop(ProjectContext context, Rectangle unitBounds, TrueColorImage image) {
		drop(context, frame, unitBounds, image);
	}

	public static void drop(
			ProjectContext context,
			TrueColorImageFrame dest,
			Rectangle unitBounds,
			TrueColorImage image
	) {
		for (int x = 0; x < unitBounds.width; ++x) {
			for (int y = 0; y < unitBounds.height; ++y) {
				final int x0 = x;
				final int y0 = y;
				TrueColorImage shot =
						image.copy(x0 * context.tileSize, y0 * context.tileSize, context.tileSize, context.tileSize);
				context.history.change(c -> c
						.trueColorImageFrame(dest)
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
