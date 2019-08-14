package com.zarbosoft.pyxyzygy.app;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zarbosoft.automodel.lib.Committable;
import com.zarbosoft.automodel.lib.Listener;
import com.zarbosoft.automodel.lib.ModelBase;
import com.zarbosoft.pyxyzygy.core.PaletteImage;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteTile;
import com.zarbosoft.pyxyzygy.core.model.latest.Project;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.zarbosoft.rendaw.common.Common.uncheck;

public class PaletteTileHelp {
  public static Cache<Long, TileData> cache =
      CacheBuilder.newBuilder().concurrencyLevel(1).weakValues().build();

  private static Path path(ModelBase context, PaletteTile tile) {
    return ((Project) context.root).tileDir().resolve(Objects.toString(tile.id()));
  }

  public static class TileData implements Committable, Listener.Destroy<PaletteTile> {
    private final PaletteTile tile;
    private final AtomicBoolean deleted = new AtomicBoolean(false);
    public final PaletteImage data;

    public TileData(PaletteTile tile, PaletteImage data) {
      this.tile = tile;
      this.data = data;
    }

    @Override
    public void commit(ModelBase context) {
      if (deleted.get()) return;
      data.serialize(path(context, tile).toString());
    }

    @Override
    public void accept(com.zarbosoft.automodel.lib.ModelBase context, PaletteTile target) {
      uncheck(
          () -> {
            try {
              Files.delete(path(context, target));
            } catch (NoSuchFileException e) {
              // nop
            }
          });
    }
  }

  public static PaletteTile create(Context context, PaletteImage data) {
    PaletteTile out = PaletteTile.create(context.model);
    TileData tileData = new TileData(out, data);
    out.addDestroyListeners(tileData);
    context.model.setDirty(tileData);
    cache.put(out.id(), tileData);
    return out;
  }

  public static PaletteImage getData(Context context, PaletteTile tile) {
    return uncheck(
            () ->
                cache.get(
                    tile.id(),
                    () -> {
                      PaletteImage data =
                          PaletteImage.deserialize(path(context.model, tile).toString());
                      return new TileData(tile, data);
                    }))
        .data;
  }
}
