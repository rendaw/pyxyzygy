package com.zarbosoft.pyxyzygy.app;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zarbosoft.automodel.lib.Committable;
import com.zarbosoft.automodel.lib.Listener;
import com.zarbosoft.automodel.lib.ModelBase;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.latest.TrueColorTile;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.zarbosoft.rendaw.common.Common.uncheck;

public class TrueColorTileHelp {
  private static Cache<Long, TileData> cache =
    CacheBuilder.newBuilder().concurrencyLevel(1).weakValues().build();

  private static Path path(ModelBase context, TrueColorTile tile) {
    return context.tileDir.resolve(Objects.toString(tile.id()));
  }

  public static class TileData implements Committable, Listener.Destroy<TrueColorTile> {
    private final TrueColorTile tile;
    private final AtomicBoolean deleted = new AtomicBoolean(false);
    public final TrueColorImage data;

    public TileData(TrueColorTile tile, TrueColorImage data) {
      this.tile = tile;
      this.data = data;
    }

    @Override
    public void commit(ModelBase context) {
      if (deleted.get()) return;
      data.serialize(path(context, tile).toString());
    }

    @Override
    public void accept(com.zarbosoft.automodel.lib.ModelBase context, TrueColorTile target) {
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

  public static TrueColorTile create(Context context, TrueColorImage data) {
    TrueColorTile out = TrueColorTile.create(context.model);
    TileData tileData = new TileData(out, data);
    out.addDestroyListeners(tileData);
    context.model.setDirty(tileData);
    cache.put(out.id(), tileData);
    return out;
  }

  public static TrueColorImage getData(Context context, TrueColorTile tile) {
    return uncheck(
      () ->
        cache.get(
          tile.id(),
          () -> {
            TrueColorImage data = TrueColorImage.deserialize(path(context.model, tile).toString());
            return new TileData(tile, data);
          }))
      .data;
  }
}
