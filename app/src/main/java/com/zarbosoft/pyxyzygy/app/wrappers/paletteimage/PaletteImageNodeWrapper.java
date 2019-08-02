package com.zarbosoft.pyxyzygy.app.wrappers.paletteimage;

import com.zarbosoft.automodel.lib.Listener;
import com.zarbosoft.automodel.lib.ProjectObject;
import com.zarbosoft.javafxbinders.BinderRoot;
import com.zarbosoft.javafxbinders.DoubleHalfBinder;
import com.zarbosoft.javafxbinders.HalfBinder;
import com.zarbosoft.javafxbinders.IndirectBinder;
import com.zarbosoft.javafxbinders.IndirectHalfBinder;
import com.zarbosoft.javafxbinders.ListHalfBinder;
import com.zarbosoft.javafxbinders.ListPropertyHalfBinder;
import com.zarbosoft.pyxyzygy.app.CanvasHandle;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.EditHandle;
import com.zarbosoft.pyxyzygy.app.GUILaunch;
import com.zarbosoft.pyxyzygy.app.PaletteTileHelp;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.Wrapper;
import com.zarbosoft.pyxyzygy.app.config.NodeConfig;
import com.zarbosoft.pyxyzygy.app.config.PaletteBrush;
import com.zarbosoft.pyxyzygy.app.config.PaletteImageNodeConfig;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.app.widgets.binders.ScalarHalfBinder;
import com.zarbosoft.pyxyzygy.app.wrappers.FrameFinder;
import com.zarbosoft.pyxyzygy.app.wrappers.PaletteWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.BaseImageCanvasHandle;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.BaseImageNodeWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.WrapTile;
import com.zarbosoft.pyxyzygy.core.PaletteImage;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.latest.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.latest.Palette;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteImageFrame;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteImageLayer;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteTile;
import com.zarbosoft.pyxyzygy.core.model.latest.ProjectLayer;
import com.zarbosoft.pyxyzygy.seed.Rectangle;
import com.zarbosoft.pyxyzygy.seed.Vector;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zarbosoft.javafxbinders.Helper.opt;
import static com.zarbosoft.pyxyzygy.app.GUILaunch.CACHE_OBJECT;
import static com.zarbosoft.pyxyzygy.app.config.PaletteImageNodeConfig.TOOL_BRUSH;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public class PaletteImageNodeWrapper
    extends BaseImageNodeWrapper<PaletteImageLayer, PaletteImageFrame, PaletteTile, PaletteImage> {
  final PaletteImageNodeConfig config;

  public static FrameFinder<PaletteImageLayer, PaletteImageFrame> frameFinder =
      new FrameFinder<PaletteImageLayer, PaletteImageFrame>() {
        @Override
        public int prelength(PaletteImageLayer node) {
          return node.prelength();
        }

        @Override
        public PaletteImageFrame frameGet(PaletteImageLayer node, int i) {
          return node.framesGet(i);
        }

        @Override
        public int frameCount(PaletteImageLayer node) {
          return node.framesLength();
        }

        @Override
        public int frameLength(PaletteImageFrame frame) {
          return frame.length();
        }
      };
  public final HalfBinder<PaletteBrush> brushBinder;
  public final IndirectBinder<Integer> paletteSelOffsetBinder;
  public final HalfBinder<ProjectObject> paletteSelectionBinder;
  public final HalfBinder<Integer> paletteSelOffsetFixedBinder;
  public final ScalarHalfBinder<Palette> paletteBinder;
  public PaletteWrapper palette;

  public PaletteImageNodeWrapper(
      Context context, Wrapper parent, int parentIndex, PaletteImageLayer node) {
    super(parent, parentIndex, node, frameFinder);
    config =
        (PaletteImageNodeConfig)
            context.config.nodes.computeIfAbsent(
                node.id(), k -> new PaletteImageNodeConfig(context));
    paletteBinder = new ScalarHalfBinder<Palette>(node, "palette");
    paletteBinder.addListener(p -> this.palette = PaletteWrapper.getPaletteWrapper(p));
    this.brushBinder =
        new DoubleHalfBinder<ObservableList<PaletteBrush>, Integer>(
                new ListPropertyHalfBinder<>(GUILaunch.profileConfig.paletteBrushes),
                new DoubleHalfBinder<>(config.tool, config.brush)
                    .map(
                        (t, index) -> {
                          if (!TOOL_BRUSH.equals(t)) return opt(null);
                          return opt(index);
                        }))
            .map(
                (brushes, index) -> {
                  if (index == null) return opt(null);
                  if (index >= brushes.size()) return opt(null);
                  return opt(brushes.get(index));
                });
    paletteSelOffsetBinder =
        new IndirectBinder<Integer>(
            new IndirectHalfBinder<Boolean>(
                brushBinder, b -> b == null ? opt(null) : opt(b.useColor)),
            b ->
                b == null
                    ? opt(null)
                    : opt(
                        (Boolean) b
                            ? brushBinder.get().get().paletteOffset
                            : config.paletteOffset));
    paletteSelOffsetFixedBinder =
        new DoubleHalfBinder<>(paletteSelOffsetBinder, paletteBinder)
            .map(
                (offset, palette) -> {
                  if (offset == null) return opt(null);
                  if (offset >= palette.entriesLength()) return opt(null);
                  return opt(offset);
                });
    paletteSelectionBinder =
        new DoubleHalfBinder<>(
                paletteSelOffsetBinder,
                new IndirectHalfBinder<List<ProjectObject>>(
                    paletteBinder,
                    palette -> opt(new ListHalfBinder<ProjectObject>(palette, "entries"))))
            .map(
                (offset, entries) -> {
                  if (entries == null || offset == null) return opt(null);
                  if (entries.size() <= offset) return opt(null);
                  return opt(entries.get(offset));
                });
  }

  @Override
  public CanvasHandle buildCanvas(Context context, Window window, CanvasHandle parent) {
    if (canvasHandle == null)
      canvasHandle =
          new BaseImageCanvasHandle<
              PaletteImageLayer, PaletteImageFrame, PaletteTile, PaletteImage>(context, this) {
            private final BinderRoot paletteChangeRoot =
                new IndirectHalfBinder<>(
                        paletteBinder, p -> opt(PaletteWrapper.getPaletteWrapper(p).selfBinder))
                    .addListener(
                        palette -> {
                          wrapTiles.forEach(
                              (k, t) -> {
                                t.update(
                                    t.getImage(
                                        context,
                                        wrapper.tileGet(
                                            frame,
                                            k))); // Image deserialization can't be done in parallel
                                // :( (global pixelreader state?)
                              });
                        });

            @Override
            public void remove(Context context, Wrapper excludeSubtree) {
              super.remove(context, excludeSubtree);
              paletteChangeRoot.destroy();
            }
          };
    canvasHandle.setParent(parent);
    return canvasHandle;
  }

  @Override
  public Listener.ScalarSet<PaletteImageLayer, Integer> addPrelengthSetListener(
      PaletteImageLayer node, Listener.ScalarSet<PaletteImageLayer, Integer> listener) {
    return node.addPrelengthSetListeners(listener);
  }

  @Override
  public void removePrelengthSetListener(Listener.ScalarSet<PaletteImageLayer, Integer> listener) {
    node.removePrelengthSetListeners(listener);
  }

  @Override
  public void dump(PaletteImage image, String name) {
    // TODO
    throw new Assertion();
  }

  @Override
  public <I> Runnable mirrorFrames(
      PaletteImageLayer node,
      List<I> list,
      Function<PaletteImageFrame, I> create,
      Consumer<I> remove,
      Consumer<Integer> change) {
    return node.mirrorFrames(list, create, remove, change);
  }

  @Override
  protected Listener.ScalarSet<PaletteImageFrame, Vector> addFrameOffsetSetListener(
      PaletteImageFrame frame, Listener.ScalarSet<PaletteImageFrame, Vector> listener) {
    return frame.addOffsetSetListeners(listener);
  }

  @Override
  public Listener.ScalarSet<PaletteImageFrame, Integer> addFrameLengthSetListener(
      PaletteImageFrame frame, Listener.ScalarSet<PaletteImageFrame, Integer> listener) {
    return frame.addLengthSetListeners(listener);
  }

  @Override
  public void removeFrameOffsetSetListener(
      PaletteImageFrame frame, Listener.ScalarSet<PaletteImageFrame, Vector> listener) {
    frame.removeOffsetSetListeners(listener);
  }

  @Override
  public void removeFrameLengthSetListener(
      PaletteImageFrame frame, Listener.ScalarSet<PaletteImageFrame, Integer> listener) {
    frame.removeLengthSetListeners(listener);
  }

  @Override
  public Listener.MapPutAll<PaletteImageFrame, Long, PaletteTile> addFrameTilesPutAllListener(
      PaletteImageFrame frame, Listener.MapPutAll<PaletteImageFrame, Long, PaletteTile> listener) {
    return frame.addTilesPutAllListeners(listener);
  }

  @Override
  public Listener.Clear<PaletteImageFrame> addFrameTilesClearListener(
      PaletteImageFrame frame, Listener.Clear<PaletteImageFrame> listener) {
    return frame.addTilesClearListeners(listener);
  }

  @Override
  public void removeFrameTilesPutAllListener(
      PaletteImageFrame frame, Listener.MapPutAll<PaletteImageFrame, Long, PaletteTile> listener) {
    frame.removeTilesPutAllListeners(listener);
  }

  @Override
  public void removeFrameTilesClearListener(
      PaletteImageFrame frame, Listener.Clear<PaletteImageFrame> listener) {
    frame.removeTilesClearListeners(listener);
  }

  @Override
  public WrapTile<PaletteTile> createWrapTile(int x, int y) {
    return new WrapTile<PaletteTile>(x, y) {
      @Override
      public Image getImage(Context context, ProjectObject tile) {
        return uncheck(
            () ->
                GUILaunch.imageCache.get(
                    Objects.hash(CACHE_OBJECT, tile.id(), palette.updatedAt),
                    () ->
                        HelperJFX.toImage(
                            PaletteTileHelp.getData(context, (PaletteTile) tile), palette.colors)));
      }
    };
  }

  @Override
  public PaletteTile tileGet(PaletteImageFrame frame, long key) {
    return frame.tilesGet(key);
  }

  @Override
  public void renderCompose(Context context, TrueColorImage gc, PaletteTile tile, int x, int y) {
    PaletteImage data = PaletteTileHelp.getData(context, (PaletteTile) tile);
    gc.compose(data, palette.colors, x, y, 1);
  }

  @Override
  public void imageCompose(PaletteImage image, PaletteImage other, int x, int y) {
    image.compose(other, x, y);
  }

  @Override
  public void drop(
      Context context,
      ChangeStepBuilder change,
      PaletteImageFrame frame,
      Rectangle unitBounds,
      PaletteImage image) {
    for (int x = 0; x < unitBounds.width; ++x) {
      for (int y = 0; y < unitBounds.height; ++y) {
        final int x0 = x;
        final int y0 = y;
        PaletteImage shot =
            image.copy(
                x0 * context.project.tileSize(),
                y0 * context.project.tileSize(),
                context.project.tileSize(),
                context.project.tileSize());
        change
            .paletteImageFrame(frame)
            .tilesPut(
                unitBounds.corner().plus(x0, y0).to1D(), PaletteTileHelp.create(context, shot));
      }
    }
  }

  @Override
  public PaletteImage grab(Context context, Rectangle unitBounds, Rectangle bounds) {
    PaletteImage canvas = PaletteImage.create(bounds.width, bounds.height);
    for (int x = 0; x < unitBounds.width; ++x) {
      for (int y = 0; y < unitBounds.height; ++y) {
        PaletteTile tile =
            (PaletteTile) tileGet(canvasHandle.frame, unitBounds.corner().plus(x, y).to1D());
        if (tile == null) continue;
        final int renderX = (x + unitBounds.x) * context.project.tileSize() - bounds.x;
        final int renderY = (y + unitBounds.y) * context.project.tileSize() - bounds.y;
        canvas.replace(PaletteTileHelp.getData(context, tile), renderX, renderY);
      }
    }
    return canvas;
  }

  @Override
  public NodeConfig getConfig() {
    return config;
  }

  @Override
  public EditHandle buildEditControls(Context context, Window window) {
    return new PaletteImageEditHandle(context, window, this);
  }

  @Override
  public ProjectLayer separateClone(Context context) {
    PaletteImageLayer clone = PaletteImageLayer.create(context.model);
    clone.initialNameSet(context.model, context.namer.uniqueName1(node.name()));
    clone.initialOffsetSet(context.model, node.offset());
    clone.initialPaletteSet(context.model, node.palette());
    clone.initialFramesAdd(
        context.model,
        node.frames().stream()
            .map(
                frame -> {
                  PaletteImageFrame newFrame = PaletteImageFrame.create(context.model);
                  newFrame.initialOffsetSet(context.model, frame.offset());
                  newFrame.initialLengthSet(context.model, frame.length());
                  newFrame.initialTilesPutAll(context.model, frame.tiles());
                  return newFrame;
                })
            .collect(Collectors.toList()));
    return clone;
  }

  @Override
  public void clear(Context context, PaletteImage image, Vector offset, Vector span) {
    image.clear(offset.x, offset.y, span.x, span.y);
  }
}
