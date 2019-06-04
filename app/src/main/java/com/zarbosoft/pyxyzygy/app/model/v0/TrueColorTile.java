package com.zarbosoft.pyxyzygy.app.model.v0;

import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.luxem.write.RawWriter;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.v0.TrueColorTileBase;
import com.zarbosoft.pyxyzygy.seed.deserialize.ModelDeserializationContext;
import com.zarbosoft.pyxyzygy.seed.model.Dirtyable;
import com.zarbosoft.pyxyzygy.seed.model.v0.ProjectContextBase;

import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.zarbosoft.rendaw.common.Common.uncheck;

public class TrueColorTile extends TrueColorTileBase implements Dirtyable {
  AtomicBoolean deleted = new AtomicBoolean(false);
  public WeakReference<TrueColorImage> data;
  private TrueColorImage dirtyData;

  public static TrueColorTile create(ProjectContext context, TrueColorImage data) {
    TrueColorTile out = new TrueColorTile();
    out.id = context.nextId++;
    out.data = new WeakReference<>(data);
    out.dirtyData = data;
    context.setDirty(out);
    return out;
  }

  @Override
  public void incRef(ProjectContextBase project) {
    refCount += 1;
    if (refCount == 1) {
      project.objectMap.put(id, this);
    }
  }

  @Override
  public void decRef(ProjectContextBase project) {
    refCount -= 1;
    if (refCount == 0) {
      deleted.set(true);
      project.objectMap.remove(id);
      uncheck(
          () -> {
            try {
              Files.delete(path(project));
            } catch (NoSuchFileException e) {
              // nop
            }
          });
    }
  }

  @Override
  public void serialize(RawWriter writer) {
    writer.type("TrueColorTile");
    writer.recordBegin();
    writer.key("id").primitive(Long.toString(id));
    writer.key("refCount").primitive(Long.toString(refCount));
    writer.recordEnd();
  }

  @Override
  public void dirtyFlush(ProjectContextBase context) {
    if (deleted.get()) return;
    dirtyData.serialize(path(context).toString());
    dirtyData = null;
  }

  public TrueColorImage getData(ProjectContext context) {
    TrueColorImage data = this.data.get();
    if (data == null) {
      data = TrueColorImage.deserialize(path(context).toString());
      this.data = new WeakReference<>(data);
    }
    return data;
  }

  public static Path path(ProjectContextBase context, long id) {
    return context.tileDir.resolve(Objects.toString(id));
  }

  private Path path(ProjectContextBase context) {
    return path(context, id);
  }

  public static class Deserializer extends StackReader.RecordState {
    private final ModelDeserializationContext context;

    private final TrueColorTile out;

    public Deserializer(ModelDeserializationContext context) {
      this.context = context;
      out = new TrueColorTile();
    }

    @Override
    public void value(Object value) {
      if ("id".equals(key)) {
        out.id = Long.valueOf((String) value);
        return;
      }
      if ("refCount".equals(key)) {
        out.refCount = Long.valueOf((String) value);
        return;
      }
      throw new RuntimeException(String.format("Unknown key (%s)", key));
    }

    @Override
    public StackReader.State array() {
      throw new RuntimeException(String.format("Key (%s) is unknown or is not an array", key));
    }

    @Override
    public StackReader.State record() {
      throw new RuntimeException(String.format("Key (%s) is unknown or is not an record", key));
    }

    @Override
    public Object get() {
      out.data = new WeakReference<>(null);
      context.objectMap.put(out.id(), out);
      return out;
    }
  }
}
