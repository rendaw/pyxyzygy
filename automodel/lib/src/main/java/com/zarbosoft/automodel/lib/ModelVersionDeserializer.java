package com.zarbosoft.automodel.lib;

import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.DeadCode;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class ModelVersionDeserializer extends StackReader.RecordState
    implements DeserializeContext {
  // Persistent
  public Map<Long, ProjectObject> objectMap = new HashMap<>();
  protected List<Long> undoHistory = new ArrayList<>();
  protected List<Long> redoHistory = new ArrayList<>();
  protected Long activeChange = null;
  protected long nextId = 0;

  // State
  protected final int maxUndo;
  protected final Path path;

  public final List<Finisher> finishers = new ArrayList<>();

  @Override
  public <T> T getObject(Long key) {
    if (key == null) return null;
    return (T)
        objectMap.computeIfAbsent(
            key,
            k -> {
              throw new IllegalStateException(
                  String.format("Can't find object %s in saved data.", key));
            });
  }

  public abstract StackReader.State deserializeObject(
      ModelVersionDeserializer context, String type);

  public ModelVersionDeserializer(Path path, int maxUndo) {
    this.maxUndo = maxUndo;
    this.path = path;
  }

  @Override
  public void value(Object value) {
    if ("nextId".equals(key)) {
      nextId = Long.parseLong((String) value);
    } else if ("objects".equals(key)) {
    } else if ("undo".equals(key)) {
      undoHistory = (List<Long>) value;
    } else if ("redo".equals(key)) {
      redoHistory = (List<Long>) value;
    } else if ("activeChange".equals(key)) {
      activeChange = Long.parseLong((String) value);
    } else throw new Assertion();
  }

  @Override
  public StackReader.State array() {
    if (false) {
      throw new DeadCode();
    } else if ("objects".equals(key)) {
      return new StackReader.ArrayState() {
        @Override
        public void value(Object value) {
          data.add(value);
        }

        @Override
        public StackReader.State record() {
          if (type == null) throw new IllegalStateException("Object has no type!");
          return deserializeObject(ModelVersionDeserializer.this, type);
        }
      };
    } else if ("undo".equals(key)) {
      return new StackReader.ArrayState() {
        @Override
        public void value(Object value) {
          super.value(Long.parseLong((String) value));
        }
      };
    } else if ("redo".equals(key)) {
      return new StackReader.ArrayState() {
        @Override
        public void value(Object value) {
          super.value(Long.parseLong((String) value));
        }
      };
    } else throw new Assertion();
  }

  @Override
  public StackReader.State record() {
    throw new Assertion();
  }

  public abstract ModelBase generate();

  @Override
  public Object get() {
    finishers.forEach(finisher -> finisher.finish(this));
    final ModelBase out = generate();
    out.root = getObject(0L);
    boolean fixed = out.debugCheckRefsFix();
    out.debugCheckRefs();
    return new ModelBase.DeserializeResult(fixed, out);
  }

  public abstract static class Finisher {
    public abstract void finish(ModelVersionDeserializer context);
  }
}
