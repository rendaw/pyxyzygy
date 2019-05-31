package com.zarbosoft.pyxyzygy.app.wrappers.baseimage;

import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.binding.BinderRoot;
import com.zarbosoft.pyxyzygy.app.widgets.binding.DoubleHalfBinder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.ScalarHalfBinder;
import com.zarbosoft.pyxyzygy.app.wrappers.FrameFinder;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.v0.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectLayer;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectObject;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import com.zarbosoft.pyxyzygy.seed.model.v0.Rectangle;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.*;

public class BaseImageCanvasHandle<N extends ProjectLayer, F extends ProjectObject, T, L> extends CanvasHandle {
	final CanvasHandle parent;
	private final Runnable mirrorCleanup;
	public final BaseImageNodeWrapper<N, F, T, L> wrapper;
	public Map<Long, WrapTile> wrapTiles = new HashMap<>();
	public F frame;
	private final List<Runnable> frameCleanup = new ArrayList<>();
	public SimpleIntegerProperty zoom = new SimpleIntegerProperty(1);
	private Listener.MapPutAll<F, Long, T> tilesPutListener;
	private Listener.Clear<F> tilesClearListener;
	private BinderRoot offsetCleanup;

	public BaseImageCanvasHandle(
			ProjectContext context, CanvasHandle parent, BaseImageNodeWrapper<N, F, T, L> wrapper
	) {
		this.parent = parent;
		this.wrapper = wrapper;

		mirrorCleanup = wrapper.mirrorFrames(wrapper.node, frameCleanup, frame -> {
			Listener.ScalarSet<F, Vector> offsetListener =
					wrapper.addFrameOffsetSetListener(frame, (target, value) -> updateFrame(context));
			Listener.ScalarSet<F, Integer> lengthListener =
					wrapper.addFrameLengthSetListener(frame, (target, value) -> updateFrame(context));
			return () -> {
				wrapper.removeFrameOffsetSetListener(frame, offsetListener);
				wrapper.removeFrameLengthSetListener(frame, lengthListener);
			};
		}, cleanup -> cleanup.run(), at -> {
			updateFrame(context);
		});

		attachTiles(context);
	}

	@Override
	public void remove(ProjectContext context) {
		wrapper.canvasHandle = null;
		mirrorCleanup.run();
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
		if (this.zoom.get() == positiveZoom && Objects.equals(this.bounds.get(), newBounds1))
			return;
		this.zoom.set(positiveZoom);
		DoubleRectangle oldBounds1 = this.bounds.get();
		this.bounds.set(newBounds1);

		Rectangle oldBounds = oldBounds1.scale(3).divideContains(context.tileSize);
		Rectangle newBounds = newBounds1.scale(3).divideContains(context.tileSize);

		// Remove tiles outside view bounds
		for (int x = 0; x < oldBounds.width; ++x) {
			for (int y = 0; y < oldBounds.height; ++y) {
				if (newBounds.contains(x, y))
					continue;
				long key = oldBounds.corner().to1D();
				paint.getChildren().remove(wrapTiles.get(key));
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
				T tile = wrapper.tileGet(frame, key);
				if (tile == null) {
					continue;
				}
				WrapTile wrapTile =
						wrapper.createWrapTile(useIndexes.x * context.tileSize, useIndexes.y * context.tileSize);
				wrapTile.update(wrapTile.getImage(context, tile)); // Image deserialization must be serial
				wrapTiles.put(key, wrapTile);
				paint.getChildren().add(wrapTile.widget);
			}
		}
	}

	@Override
	public void setFrame(ProjectContext context, int frameNumber) {
		this.frameNumber.set(frameNumber);
		updateFrame(context);
	}

	public void updateFrame(ProjectContext context) {
		F oldFrame = frame;
		FrameFinder.Result<F> found = wrapper.frameFinder.findFrame(wrapper.node, frameNumber.get());
		frame = found.frame;
		if (oldFrame != frame) {
			detachTiles();
			attachTiles(context);
		}
		int frameCount = wrapper.frameFinder.frameCount(wrapper.node);
		do {
			if (frameCount == 1) {
				previousFrame.set(-1);
				nextFrame.set(-1);
				break;
			}

			{
				int frameIndex = found.frameIndex - 1;
				if (frameIndex == -1)
					frameIndex = frameCount - 1;
				int outFrame = 0;
				for (int i = 0; i < frameIndex; ++i) {
					outFrame += wrapper.frameFinder.frameLength(wrapper.frameFinder.frameGet(wrapper.node, i));
				}
				previousFrame.set(outFrame);
			}

			{
				int frameIndex = found.frameIndex + 1;
				if (frameIndex >= frameCount)
					frameIndex = 0;
				int outFrame = 0;
				for (int i = 0; i < frameIndex; ++i) {
					outFrame += wrapper.frameFinder.frameLength(wrapper.frameFinder.frameGet(wrapper.node, i));
				}
				nextFrame.set(outFrame);
			}
		} while (false);
	}

	public void attachTiles(ProjectContext context) {
		tilesPutListener = wrapper.addFrameTilesPutAllListener(frame, (target, put, remove) -> {
			for (Long key : remove) {
				WrapTile old = wrapTiles.remove(key);
				if (old != null)
					paint.getChildren().remove(old.widget);
			}
			Rectangle checkBounds = bounds.get().scale(3).divideContains(context.tileSize);
			for (Map.Entry<Long, T> entry : put.entrySet()) {
				long key = entry.getKey();
				Vector indexes = Vector.from1D(key);
				if (!checkBounds.contains(indexes.x, indexes.y)) {
					continue;
				}
				T value = entry.getValue();
				WrapTile<T> wrap = wrapTiles.get(key);
				if (wrap == null) {
					wrap = wrapper.createWrapTile(indexes.x * context.tileSize, indexes.y * context.tileSize);
					wrapTiles.put(key, wrap);
					paint.getChildren().add(wrap.widget);
				}
				wrap.update(wrap.getImage(context, value)); // Image deserialization can't be done in parallel :(
			}
		});
		tilesClearListener = wrapper.addFrameTilesClearListener(frame, (target) -> {
			paint.getChildren().clear();
			wrapTiles.clear();
		});
		offsetCleanup =
				new DoubleHalfBinder<>(new ScalarHalfBinder<Vector>(wrapper.getValue(),
						"offset"), new ScalarHalfBinder<Vector>(frame, "offset")).addListener(p -> {
					paint.setLayoutX(p.first.x + p.second.x);
					paint.setLayoutY(p.first.y + p.second.y);
					overlay.setLayoutX(p.first.x + p.second.x);
					overlay.setLayoutY(p.first.y + p.second.y);
				});
	}

	public void detachTiles() {
		wrapper.removeFrameTilesPutAllListener(frame, tilesPutListener);
		wrapper.removeFrameTilesClearListener(frame, tilesClearListener);
		if (offsetCleanup != null) {
			offsetCleanup.destroy();
			offsetCleanup = null;
		}
		tilesPutListener = null;
		paint.getChildren().clear();
		wrapTiles.clear();
	}

	public void render(ProjectContext context, TrueColorImage gc, Rectangle bounds) {
		render(context, gc, bounds, bounds.divideContains(context.tileSize));
	}

	public void render(ProjectContext context, TrueColorImage gc, Rectangle bounds, Rectangle unitBounds) {
		for (int x = 0; x < unitBounds.width; ++x) {
			for (int y = 0; y < unitBounds.height; ++y) {
				T tile = wrapper.tileGet(frame, unitBounds.corner().plus(x, y).to1D());
				if (tile == null)
					continue;
				final int renderX = (x + unitBounds.x) * context.tileSize - bounds.x;
				final int renderY = (y + unitBounds.y) * context.tileSize - bounds.y;
				wrapper.renderCompose(context, gc, tile, renderX, renderY);
			}
		}
	}

	public void clear(
			ProjectContext context, ChangeStepBuilder change, Rectangle bounds
	) {
		wrapper.modify(context, change, bounds, (image, corner) -> {
			wrapper.clear(context, image, bounds.corner().minus(corner), bounds.span());
		});
	}

	@Override
	public DoubleVector toInner(DoubleVector vector) {
		return vector;
	}
}
