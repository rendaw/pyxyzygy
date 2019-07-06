package com.zarbosoft.automodel.lib;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.luxem.write.RawWriter;

import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.zarbosoft.rendaw.common.Common.atomicWrite;
import static com.zarbosoft.rendaw.common.Common.last;
import static com.zarbosoft.rendaw.common.Common.reversed;
import static com.zarbosoft.rendaw.common.Common.uncheck;

@Configuration
public class ChangeStep implements Committable {
  public static class CacheId {
    public final long id;

    public CacheId(long id) {
      this.id = id;
    }
  }

  private final AtomicBoolean deleted = new AtomicBoolean(false);
  public final CacheId cacheId;
  public List<Change> changes = new ArrayList<>();

  public ChangeStep(CacheId id) {
    this.cacheId = id;
  }

  public void add(ModelBase context, Change change) {
    if (!changes.isEmpty() && last(changes).merge(context, change)) change.delete(context);
    else changes.add(change);
  }

  public ChangeStep apply(ModelBase context) {
    ChangeStep out = new ChangeStep(new CacheId(context.nextId++));
    for (Change change : reversed(changes)) change.apply(context, out);
    remove(context);
    return out;
  }

  public static Path path(ModelBase context, long id) {
    return context.changesDir.resolve(Long.toString(id));
  }

  private Path path(ModelBase context) {
    return path(context, cacheId.id);
  }

  public void remove(ModelBase context) {
    deleted.set(true);
    for (Change c : changes) c.delete(context);
    uncheck(
        () -> {
          try {
            Files.delete(path(context));
          } catch (NoSuchFileException e) {
            // nop
          }
        });
  }

  @Override
  public void commit(ModelBase context) {
    if (deleted.get()) return;
    atomicWrite(
        path(context),
        dest -> {
          RawWriter writer = new RawWriter(dest, (byte) ' ', 4);
          changes.forEach(c -> c.serialize(writer));
        });
  }
}
