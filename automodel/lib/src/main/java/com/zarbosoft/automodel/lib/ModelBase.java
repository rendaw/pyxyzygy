package com.zarbosoft.automodel.lib;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.luxem.write.RawWriter;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Common;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.zarbosoft.automodel.lib.Logger.logger;
import static com.zarbosoft.rendaw.common.Common.atomicWrite;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public abstract class ModelBase implements Committable, DeserializeContext {
  // Persistent
  long nextId;

  // Session
  public final Map<Long, ProjectObject> objectMap;
  public ProjectObject root;
  public final ReadWriteLock lock = new ReentrantReadWriteLock();
  Committer committer = new Committer(lock, this);
  public final Path path;
  final Path changesDir;
  public final Path tileDir;
  final History history;
  final String vid;

  public static Path projectPath(Path base) {
    return base.resolve("project.luxem");
  }

  public void addUndoSizeListener(Consumer<Integer> listener) {
    history.addUndoSizeListener(listener);
  }

  public void addRedoSizeListener(Consumer<Integer> listener) {
    history.addRedoSizeListener(listener);
  }

  public void removeUndoSizeListener(Consumer<Integer> listener) {
    history.removeUndoSizeListener(listener);
  }

  public void removeRedoSizeListener(Consumer<Integer> listener) {
    history.removeRedoSizeListener(listener);
  }

  public void setDirty(Committable object) {
    committer.setDirty(object);
  }

  public abstract StackReader.State deserializeChange(String type);

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

  public ModelBase(
      Path path,
      long nextId,
      Map<Long, ProjectObject> objectMap,
      List<Long> undoHistory,
      List<Long> redoHistory,
      Long activeChange,
      int maxUndo,
      String vid) {
    this.path = path;
    this.nextId = nextId;
    changesDir = path.resolve("changes");
    tileDir = path.resolve("tiles");
    this.objectMap = objectMap;
    this.vid = vid;
    uncheck(
        () -> {
          Files.createDirectories(path);
          Files.createDirectories(changesDir);
          Files.createDirectories(tileDir);
        });
    this.history = new History(this, undoHistory, redoHistory, activeChange, maxUndo);
  }

  public static class TestMarkerArg {}

  /**
   * Constructor for running tests
   *
   * @param a
   */
  public ModelBase(TestMarkerArg a) {
    objectMap = new HashMap<>();
    path = null;
    changesDir = null;
    tileDir = null;
    vid = "test";
    history = new History(this, new ArrayList<>(), new ArrayList<>(), null, 0);
  }

  public void close() {
    committer.commitAll();
  }

  public void undo() {
    history.undo();
  }

  public void redo() {
    history.redo();
  }

  public void finishChange() {
    history.finishChange();
  }

  public void clearHistory() {
    history.clearHistory();
  }

  public void change(History.Tuple unique, Consumer<ChangeStep> consumer) {
    history.change(unique, consumer);
  }

  public boolean debugCheckRefsFix() {
    return debugCheckRefs(true);
  }

  public void debugCheckRefs() {
    debugCheckRefs(false);
  }

  protected void walkTree(ProjectObject root, Function<ProjectObject, Boolean> handler) {
    Deque<Iterator<? extends ProjectObject>> d = new ArrayDeque<>();
    d.addLast(ImmutableList.of(root).iterator());
    while (!d.isEmpty()) {
      Iterator<? extends ProjectObject> iter = d.getFirst();
      if (!iter.hasNext()) {
        d.removeFirst();
        continue;
      }
      ProjectObject o = iter.next();
      boolean recurse = handler.apply(o);
      if (!recurse) continue;
      o.walk(d);
    }
  }

  @Override
  public void commit(ModelBase context) {
    debugCheckRefs();
    atomicWrite(
        projectPath(path),
        dest -> {
          RawWriter writer = new RawWriter(dest, (byte) ' ', 4);
          writer.type(vid);
          writer.recordBegin();
          writer.key("nextId").primitive(Long.toString(nextId));
          writer.key("objects").arrayBegin();
          for (ProjectObject object : objectMap.values()) object.serialize(writer);
          writer.arrayEnd();
          history.serialize(writer);
          writer.key("activeChange").primitive(Long.toString(history.changeStep.cacheId.id));
          writer.recordEnd();
        });
  }

  public boolean debugCheckRefs(boolean fix) {
    Common.Mutable<Boolean> hadErrors = new Common.Mutable<>(false);
    Consumer<Function<ProjectObject, Boolean>> walkAll =
        consumer -> {
          walkTree(root, consumer);
          history.changeStep.changes.forEach(
              change -> change.debugRefCounts(c -> walkTree(c, consumer)));
          for (Iterator<ChangeStep.CacheId> i = history.undoHistory.iterator(); i.hasNext(); ) {
            ChangeStep.CacheId id = i.next();
            ChangeStep step = history.get(id, fix);
            if (step == null) {
              i.remove();
              continue;
            }
            step.changes.forEach(change -> change.debugRefCounts(c -> walkTree(c, consumer)));
          }
          for (Iterator<ChangeStep.CacheId> i = history.redoHistory.iterator(); i.hasNext(); ) {
            ChangeStep.CacheId id = i.next();
            ChangeStep step = history.get(id, fix);
            if (step == null) {
              i.remove();
              continue;
            }
            step.changes.forEach(change -> change.debugRefCounts(c -> walkTree(c, consumer)));
          }
        };

    // Sum expected reference counts
    Map<Long, Long> counts = new HashMap<>();
    Function<ProjectObject, Boolean> incCount =
        o -> {
          Long count = counts.get(o.id());
          if (count != null) {
            counts.put(o.id(), count + 1);
            return false;
          }
          counts.put(o.id(), 1L);
          return true;
        };
    walkAll.accept(incCount);

    // Compare (and fix) actual reference counts
    {
      // If fixing ref counts, start at root and move to leaves - leaves never modify other counts
      // but if a leaf is decremented first and reaches 0, it will go negative if/when parents dec
      // and also reach 0
      LinkedHashMap<ProjectObject, Void> treeSorted0 = new LinkedHashMap<>(objectMap.size());
      walkTree(
          root,
          o -> {
            treeSorted0.remove(o);
            treeSorted0.put(o, null);
            return true;
          });
      List<ProjectObject> treeSorted = new ArrayList<>(treeSorted0.keySet());
      {
        Collections.reverse(treeSorted);
        Set<Long> seen = new HashSet<>();
        for (Iterator<ProjectObject> iter = treeSorted.iterator(); iter.hasNext(); ) {
          ProjectObject o = iter.next();
          if (seen.contains(o.id())) iter.remove();
          seen.add(o.id());
        }
        Collections.reverse(treeSorted);
      }
      for (ProjectObject o : treeSorted) {
        long got = o.refCount();
        long expected = counts.getOrDefault(o.id(), 0L);
        if (got != expected) {
          hadErrors.value = true;
          String error =
              String.format("Ref count for %s id %s : %s should be %s", o, o.id(), got, expected);
          if (fix) {
            logger.write(error);
            long diff = expected - got;
            if (diff >= 0)
              for (long i = 0; i < diff; ++i) {
                o.incRef(this);
              }
            else
              for (long i = 0; i < -diff; ++i) {
                o.decRef(this);
              }
          } else {
            throw new Assertion(error);
          }
        }
      }
    }

    // Check rootedness
    {
      Set<Long> seen = new HashSet<>();
      walkAll.accept(
          (ProjectObject o) -> {
            if (!objectMap.containsKey(o.id())) {
              hadErrors.value = true;
              String error = String.format("%s (id %s) missing from object map", o, o.id());
              if (fix) {
                objectMap.put(o.id(), o);
                logger.write(error);
              } else {
                throw new Assertion(error);
              }
            }
            if (seen.contains(o.id())) return false;
            seen.add(o.id());
            return true;
          });
    }

    return hadErrors.value;
  }

  public int undoSize() {
    return history.undoHistory.size();
  }

  public int redoSize() {
    return history.redoHistory.size();
  }

  public abstract boolean needsMigrate();

  public abstract ModelBase migrate();

  public void commitCurrentChange() {
    setDirty(history.changeStep);
  }

  public static class DeserializeResult {
    public final boolean fixed;
    public final ModelBase model;

    public DeserializeResult(boolean fixed, ModelBase model) {
      this.fixed = fixed;
      this.model = model;
    }
  }

  public static DeserializeResult deserialize(
      Path path, Function<String, StackReader.State> handleVersion) {
    return uncheck(
        () -> {
          try (InputStream source = Files.newInputStream(projectPath(path))) {
            return (DeserializeResult)
                new StackReader()
                    .read(
                        source,
                        new StackReader.ArrayState() {
                          @Override
                          public StackReader.State array() {
                            throw new IllegalStateException("Project data should be a record.");
                          }

                          @Override
                          public void value(Object value) {
                            data.add(value);
                          }

                          @Override
                          public StackReader.State record() {
                            if (type == null)
                              throw new IllegalStateException("Project has no version");
                            return handleVersion.apply(type);
                          }
                        })
                    .get(0);
          }
        });
  }
}
