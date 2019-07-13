package com.zarbosoft.pyxyzygy.app.wrappers.baseimage;

import com.zarbosoft.automodel.lib.Listener;
import com.zarbosoft.automodel.lib.ProjectObject;
import com.zarbosoft.pyxyzygy.app.CanvasHandle;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.DoubleRectangle;
import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.Wrapper;
import com.zarbosoft.pyxyzygy.app.widgets.binding.BinderRoot;
import com.zarbosoft.pyxyzygy.app.widgets.binding.DoubleHalfBinder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.ScalarHalfBinder;
import com.zarbosoft.pyxyzygy.app.wrappers.FrameFinder;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.latest.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.latest.ProjectLayer;
import com.zarbosoft.pyxyzygy.seed.Rectangle;
import com.zarbosoft.pyxyzygy.seed.Vector;
import javafx.beans.property.SimpleIntegerProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.zarbosoft.pyxyzygy.app.Global.NO_INNER;

public class BaseImageCanvasHandle<
        N extends ProjectLayer, F extends ProjectObject, T extends ProjectObject, L>
    extends CanvasHandle {
  private final Listener.ScalarSet<N, Integer> prelengthCleanup;
  CanvasHandle parent;
  private final Runnable mirrorCleanup;
  public final BaseImageNodeWrapper<N, F, T, L> wrapper;
  public Map<Long, WrapTile> wrapTiles = new HashMap<>();
  public F frame;
  private final List<Runnable> frameCleanup = new ArrayList<>();
  public SimpleIntegerProperty zoom = new SimpleIntegerProperty(1);
  private Listener.MapPutAll<F, Long, T> tilesPutListener;
  private Listener.Clear<F> tilesClearListener;
  private BinderRoot offsetCleanup;

  public BaseImageCanvasHandle(Context context, BaseImageNodeWrapper<N, F, T, L> wrapper) {
    this.wrapper = wrapper;
    prelengthCleanup =
        wrapper.addPrelengthSetListener(
            wrapper.node,
            ((target, value) -> {
              updateViewedTime(context);
            }));
    mirrorCleanup =
        wrapper.mirrorFrames(
            wrapper.node,
            frameCleanup,
            frame -> {
              Listener.ScalarSet<F, Vector> offsetListener =
                  wrapper.addFrameOffsetSetListener(
                      frame, (target, value) -> updateViewedTime(context));
              Listener.ScalarSet<F, Integer> lengthListener =
                  wrapper.addFrameLengthSetListener(
                      frame, (target, value) -> updateViewedTime(context));
              return () -> {
                wrapper.removeFrameOffsetSetListener(frame, offsetListener);
                wrapper.removeFrameLengthSetListener(frame, lengthListener);
              };
            },
            cleanup -> cleanup.run(),
            at -> {
              updateViewedTime(context);
            });

    attachTiles(context);
  }

  @Override
  public void setParent(CanvasHandle parent) {
    this.parent = parent;
  }

  @Override
  public void remove(Context context, Wrapper excludeSubtree) {
    wrapper.canvasHandle = null;
    wrapper.removePrelengthSetListener(prelengthCleanup);
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
  public void setViewport(Context context, DoubleRectangle newBounds1, int positiveZoom) {
    if (this.zoom.get() == positiveZoom && Objects.equals(this.bounds.get(), newBounds1)) return;
    this.zoom.set(positiveZoom);
    DoubleRectangle oldBounds1 = this.bounds.get();
    this.bounds.set(newBounds1);

    Rectangle oldBounds = oldBounds1.scale(3).divideContains(context.project.tileSize());
    Rectangle newBounds = newBounds1.scale(3).divideContains(context.project.tileSize());

    if (frame == null) return;

    // Remove tiles outside view bounds
    for (int x = 0; x < oldBounds.width; ++x) {
      for (int y = 0; y < oldBounds.height; ++y) {
        if (newBounds.contains(x, y)) continue;
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
            wrapper.createWrapTile(
                useIndexes.x * context.project.tileSize(),
                useIndexes.y * context.project.tileSize());
        wrapTile.update(wrapTile.getImage(context, tile)); // Image deserialization must be serial
        wrapTiles.put(key, wrapTile);
        paint.getChildren().add(wrapTile.widget);
      }
    }
  }

  @Override
  public void setViewedTime(Context context, int outerTime) {
    this.time.set(outerTime);
    updateViewedTime(context);
  }

  public void updateViewedTime(Context context) {
    if (time.get() != NO_INNER) {
      FrameFinder.Result<F> found = wrapper.frameFinder.findFrame(wrapper.node, time.get());
      F newFrame = found.frame;
      if (frame != newFrame) {
        detachTiles();
        frame = newFrame;
        attachTiles(context);
      }
    } else {
      detachTiles();
      frame = null;
    }
  }

  public void attachTiles(Context context) {
    if (frame == null) return;
    tilesPutListener =
        wrapper.addFrameTilesPutAllListener(
            frame,
            (target, put, remove) -> {
              for (Long key : remove) {
                WrapTile old = wrapTiles.remove(key);
                if (old != null) paint.getChildren().remove(old.widget);
              }
              Rectangle checkBounds =
                  bounds.get().scale(3).divideContains(context.project.tileSize());
              for (Map.Entry<Long, T> entry : put.entrySet()) {
                long key = entry.getKey();
                Vector indexes = Vector.from1D(key);
                if (!checkBounds.contains(indexes.x, indexes.y)) {
                  continue;
                }
                T value = entry.getValue();
                WrapTile<T> wrap = wrapTiles.get(key);
                if (wrap == null) {
                  wrap =
                      wrapper.createWrapTile(
                          indexes.x * context.project.tileSize(),
                          indexes.y * context.project.tileSize());
                  wrapTiles.put(key, wrap);
                  paint.getChildren().add(wrap.widget);
                }
                wrap.update(
                    wrap.getImage(
                        context, value)); // Image deserialization can't be done in parallel :(
              }
            });
    tilesClearListener =
        wrapper.addFrameTilesClearListener(
            frame,
            (target) -> {
              paint.getChildren().clear();
              wrapTiles.clear();
            });
    offsetCleanup =
        new DoubleHalfBinder<>(
                new ScalarHalfBinder<Vector>(wrapper.getValue(), "offset"),
                new ScalarHalfBinder<Vector>(frame, "offset"))
            .addListener(
                p -> {
                  paint.setLayoutX(p.first.x + p.second.x);
                  paint.setLayoutY(p.first.y + p.second.y);
                  overlay.setLayoutX(p.first.x + p.second.x);
                  overlay.setLayoutY(p.first.y + p.second.y);
                });
  }

  public void detachTiles() {
    if (tilesPutListener != null) {
      wrapper.removeFrameTilesPutAllListener(frame, tilesPutListener);
      tilesPutListener = null;
    }
    if (tilesClearListener != null) {
      wrapper.removeFrameTilesClearListener(frame, tilesClearListener);
      tilesClearListener = null;
    }
    if (offsetCleanup != null) {
      offsetCleanup.destroy();
      offsetCleanup = null;
    }
    paint.getChildren().clear();
    wrapTiles.clear();
  }

  public void render(Context context, TrueColorImage gc, Rectangle bounds, Rectangle unitBounds) {
    for (int x = 0; x < unitBounds.width; ++x) {
      for (int y = 0; y < unitBounds.height; ++y) {
        T tile = wrapper.tileGet(frame, unitBounds.corner().plus(x, y).to1D());
        if (tile == null) continue;
        final int renderX = (x + unitBounds.x) * context.project.tileSize() - bounds.x;
        final int renderY = (y + unitBounds.y) * context.project.tileSize() - bounds.y;
        wrapper.renderCompose(context, gc, tile, renderX, renderY);
      }
    }
  }

  public void clear(Context context, ChangeStepBuilder change, Rectangle bounds) {
    wrapper.modify(
        context,
        change,
        bounds,
        (image, corner) -> {
          wrapper.clear(context, image, bounds.corner().minus(corner), bounds.span());
        });
  }

  @Override
  public DoubleVector toInnerPosition(DoubleVector outerPosition) {
    return outerPosition;
  }

  @Override
  public int toInnerTime(int outerTime) {
    return outerTime;
  }
}
