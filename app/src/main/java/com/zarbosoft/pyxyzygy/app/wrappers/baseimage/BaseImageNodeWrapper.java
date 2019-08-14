package com.zarbosoft.pyxyzygy.app.wrappers.baseimage;

import com.zarbosoft.automodel.lib.Listener;
import com.zarbosoft.automodel.lib.ProjectObject;
import com.zarbosoft.pyxyzygy.app.CanvasHandle;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.DoubleRectangle;
import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.Wrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.FrameFinder;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.latest.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.latest.ProjectLayer;
import com.zarbosoft.pyxyzygy.seed.Rectangle;
import com.zarbosoft.pyxyzygy.seed.Vector;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.scene.control.TreeItem;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class BaseImageNodeWrapper<
        N extends ProjectLayer, F extends ProjectObject, T extends ProjectObject, L>
    extends Wrapper {
  public final N node;
  private final Wrapper parent;
  public BaseImageCanvasHandle<N, F, T, L> canvasHandle;
  public final FrameFinder<N, F> frameFinder;

  // Cache values when there's no canvas
  public BaseImageNodeWrapper(
      Wrapper parent, int parentIndex, N node, FrameFinder<N, F> frameFinder) {
    this.node = node;
    this.parent = parent;
    this.frameFinder = frameFinder;
    this.parentIndex = parentIndex;
    tree.set(new TreeItem<>(this));
  }

  public abstract <I> Runnable mirrorFrames(
      N node,
      List<I> list,
      Function<F, I> create,
      Consumer<I> remove,
      BiConsumer<Integer, Integer> change);

  protected abstract Listener.ScalarSet<F, Vector> addFrameOffsetSetListener(
      F frame, Listener.ScalarSet<F, Vector> listener);

  public abstract Listener.ScalarSet<F, Integer> addFrameLengthSetListener(
      F frame, Listener.ScalarSet<F, Integer> listener);

  @Override
  public Wrapper getParent() {
    return parent;
  }

  @Override
  public ProjectObject getValue() {
    return node;
  }

  @Override
  public CanvasHandle buildCanvas(Context context, Window window, CanvasHandle parent) {
    if (canvasHandle == null) canvasHandle = new BaseImageCanvasHandle<N, F, T, L>(context, this);
    canvasHandle.setParent(parent);
    return canvasHandle;
  }

  @Override
  public void remove(Context context) {}

  @Override
  public void deleteChild(Context context, ChangeStepBuilder change, int index) {
    throw new Assertion();
  }

  @Override
  public TakesChildren takesChildren() {
    return TakesChildren.NONE;
  }

  public abstract Listener.ScalarSet<N, Integer> addPrelengthSetListener(
      N node, Listener.ScalarSet<N, Integer> listener);

  public abstract void removePrelengthSetListener(Listener.ScalarSet<N, Integer> listener);

  public abstract void removeFrameOffsetSetListener(
      F frame, Listener.ScalarSet<F, Vector> listener);

  public abstract void removeFrameLengthSetListener(
      F frame, Listener.ScalarSet<F, Integer> listener);

  public abstract Listener.MapPutAll<F, Long, T> addFrameTilesPutAllListener(
      F frame, Listener.MapPutAll<F, Long, T> listener);

  public abstract Listener.Clear<F> addFrameTilesClearListener(F frame, Listener.Clear<F> listener);

  public abstract void removeFrameTilesPutAllListener(
      F frame, Listener.MapPutAll<F, Long, T> listener);

  public abstract void removeFrameTilesClearListener(F frame, Listener.Clear<F> listener);

  public abstract WrapTile<T> createWrapTile(int x, int y);

  public abstract T tileGet(F frame, long key);

  /**
   * Draw tile onto gc at the given position (just forward to TrueColorImage.compose)
   *
   * @param context
   * @param gc
   * @param tile
   * @param x
   * @param y
   */
  public abstract void renderCompose(Context context, TrueColorImage gc, T tile, int x, int y);

  public abstract void imageCompose(L image, L other, int x, int y);

  /**
   * Replace tiles with data from unit-multiple size image
   *
   * @param context
   * @param change
   * @param frame
   * @param unitBounds
   * @param image
   */
  public abstract void drop(
      Context context, ChangeStepBuilder change, F frame, Rectangle unitBounds, L image);

  /**
   * Create single image from tile data. Unit bounds is bounds / tileSize - as argument to avoid
   * recomputation if computed elsewhere. Bounds is what's actually captured and the size of the
   * returned image.
   *
   * @param context
   * @param unitBounds
   * @param bounds
   * @return
   */
  public abstract L grab(Context context, Rectangle unitBounds, Rectangle bounds);

  public L grab(Context context, Rectangle bounds) {
    return grab(context, bounds.divideContains(context.project.tileSize()), bounds);
  }

  /**
   * @param context
   * @param image
   * @param offset relative to image
   * @param span
   */
  public abstract void clear(Context context, L image, Vector offset, Vector span);

  @FunctionalInterface
  public interface DoubleModifyCallback<L> {
    public void accept(L image, DoubleVector corner);
  }

  @FunctionalInterface
  public interface IntModifyCallback<L> {
    public void accept(L image, Vector corner);
  }

  public abstract void dump(L image, String name);

  public void modify(
      Context context,
      ChangeStepBuilder change,
      DoubleRectangle bounds,
      DoubleModifyCallback<L> modify) {
    Rectangle unitBounds = bounds.divideContains(context.project.tileSize());
    Rectangle outerBounds = unitBounds.multiply(context.project.tileSize());
    L canvas = grab(context, unitBounds, outerBounds);
    modify.accept(canvas, DoubleVector.of(outerBounds.corner()));
    drop(context, change, canvasHandle.frame, unitBounds, canvas);
  }

  public void modify(
      Context context, ChangeStepBuilder change, Rectangle bounds, IntModifyCallback<L> modify) {
    Rectangle unitBounds = bounds.divideContains(context.project.tileSize());
    Rectangle outerBounds = unitBounds.multiply(context.project.tileSize());
    L canvas = grab(context, unitBounds, outerBounds);
    modify.accept(canvas, outerBounds.corner());
    drop(context, change, canvasHandle.frame, unitBounds, canvas);
  }
}
