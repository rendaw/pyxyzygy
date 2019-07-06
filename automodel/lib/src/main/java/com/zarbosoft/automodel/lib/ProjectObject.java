package com.zarbosoft.automodel.lib;

import com.zarbosoft.luxem.write.RawWriter;

import java.util.Deque;
import java.util.Iterator;

public abstract class ProjectObject {
  protected long id;
  protected int refCount;

  public long id() {
    return id;
  }

  public int refCount() {
    return refCount;
  }

  public abstract void incRef(ModelBase context);

  public abstract void decRef(ModelBase context);

  public abstract void serialize(RawWriter writer);

  public abstract void walk(Deque<Iterator<? extends ProjectObject>> queue);

  protected static long takeId(ModelBase context) {
    return context.nextId++;
  }
}
