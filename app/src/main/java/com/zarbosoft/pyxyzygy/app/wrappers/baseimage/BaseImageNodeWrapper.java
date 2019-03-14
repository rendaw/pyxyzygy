package com.zarbosoft.pyxyzygy.app.wrappers.baseimage;

import com.zarbosoft.pyxyzygy.app.CanvasHandle;
import com.zarbosoft.pyxyzygy.app.DoubleRectangle;
import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.Wrapper;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.wrappers.FrameFinder;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectNode;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectObject;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import com.zarbosoft.pyxyzygy.seed.model.v0.Rectangle;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.scene.control.TreeItem;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class BaseImageNodeWrapper<N extends ProjectNode, F, T, L> extends Wrapper {
	public final N node;
	private final Wrapper parent;
	public BaseImageCanvasHandle<N, F, T, L> canvasHandle;
	public final FrameFinder<N, F> frameFinder;

	// Cache values when there's no canvas
	public BaseImageNodeWrapper(
			Wrapper parent, int parentIndex, N node, FrameFinder<N, F> frameFinder
	) {
		this.node = node;
		this.parent = parent;
		this.frameFinder = frameFinder;
		this.parentIndex = parentIndex;
		tree.set(new TreeItem<>(this));
	}

	public abstract <I> Runnable mirrorFrames(
			N node, List<I> list, Function<F, I> create, Consumer<I> remove, Consumer<Integer> change
	);

	protected abstract Listener.ScalarSet<F, Vector> addFrameOffsetSetListener(
			F frame, Listener.ScalarSet<F, Vector> listener
	);

	public abstract Listener.ScalarSet<F, Integer> addFrameLengthSetListener(
			F frame, Listener.ScalarSet<F, Integer> listener
	);

	@Override
	public Wrapper getParent() {
		return parent;
	}

	@Override
	public ProjectObject getValue() {
		return node;
	}

	@Override
	public CanvasHandle buildCanvas(ProjectContext context, CanvasHandle parent) {
		if (canvasHandle == null)
			canvasHandle = new BaseImageCanvasHandle<N, F, T, L>(context, parent, this);
		return canvasHandle;
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
			context.history.change(c -> c.project(context.project).topRemove(parentIndex, 1));
	}

	@Override
	public void remove(ProjectContext context) {
		context.config.nodes.remove(node.id());
	}

	@Override
	public void removeChild(ProjectContext context, int index) {
		throw new Assertion();
	}

	@Override
	public TakesChildren takesChildren() {
		return TakesChildren.NONE;
	}

	public abstract void removeFrameOffsetSetListener(F frame, Listener.ScalarSet<F, Vector> listener);

	public abstract void removeFrameLengthSetListener(F frame, Listener.ScalarSet<F, Integer> listener);

	public abstract Listener.MapPutAll<F, Long, T> addFrameTilesPutAllListener(
			F frame, Listener.MapPutAll<F, Long, T> listener
	);

	public abstract Listener.Clear<F> addFrameTilesClearListener(
			F frame, Listener.Clear<F> listener
	);

	public abstract void removeFrameTilesPutAllListener(F frame, Listener.MapPutAll<F, Long, T> listener);

	public abstract void removeFrameTilesClearListener(F frame, Listener.Clear<F> listener);

	public abstract WrapTile<T> createWrapTile(int x, int y);

	public abstract T tileGet(F frame, long key);

	/**
	 * Draw tile onto gc at the given position (just forward to TrueColorImage.compose)
	 * @param context
	 * @param gc
	 * @param tile
	 * @param x
	 * @param y
	 */
	public abstract void renderCompose(
			ProjectContext context, TrueColorImage gc, T tile, int x, int y
	);

	public abstract void imageCompose(L image, L other, int x, int y);

	/**
	 * Replace tiles with data from unit-multiple size image
	 * @param context
	 * @param frame
	 * @param unitBounds
	 * @param image
	 */
	public abstract void drop(ProjectContext context, F frame, Rectangle unitBounds, L image);

	/**
	 * Create single image from tile data (size multiple of tile)
	 * @param context
	 * @param unitBounds
	 * @param bounds
	 * @return
	 */
	public abstract L grab(ProjectContext context, Rectangle unitBounds, Rectangle bounds);

	/**
	 *
	 * @param context
	 * @param image
	 * @param offset relative to image
	 * @param span
	 */
	public abstract void clear(ProjectContext context, L image, Vector offset, Vector span);

	@FunctionalInterface
	public interface DoubleModifyCallback<L> {
		public void accept(L image, DoubleVector offset);
	}
	@FunctionalInterface
	public interface IntModifyCallback<L> {
		public void accept(L image, Vector offset);
	}

	public void modify(ProjectContext context, DoubleRectangle bounds, DoubleModifyCallback<L> modify) {
		Rectangle unitBounds = bounds.divideContains(context.tileSize);
		Rectangle outerBounds = unitBounds.multiply(context.tileSize);
		L canvas = grab(context, unitBounds, outerBounds);
		DoubleVector offset = bounds.corner().minus(outerBounds.corner());
		modify.accept(canvas,offset);
		drop(context, canvasHandle.frame, unitBounds, canvas);
	}
	public void modify(ProjectContext context, Rectangle bounds, IntModifyCallback<L> modify) {
		Rectangle unitBounds = bounds.divideContains(context.tileSize);
		Rectangle outerBounds = unitBounds.multiply(context.tileSize);
		L canvas = grab(context, unitBounds, outerBounds);
		Vector offset = bounds.corner().minus(outerBounds.corner());
		modify.accept(canvas,offset);
		drop(context, canvasHandle.frame, unitBounds, canvas);
	}
}
