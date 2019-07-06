package com.zarbosoft.automodel.lib;

import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.luxem.write.RawWriter;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Common;
import com.zarbosoft.rendaw.common.WeakCache;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.zarbosoft.automodel.lib.Logger.logger;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public class History {
  private final WeakCache<ChangeStep.CacheId, ChangeStep> stepLookup =
      new WeakCache<>(c -> c.cacheId);
  public final List<ChangeStep.CacheId> undoHistory;
  public final List<ChangeStep.CacheId> redoHistory;
  private final ModelBase context;
  public ChangeStep changeStep;
  private boolean inChange = false;
  private int maxUndo;
  private WeakList<Consumer<Integer>> undoSizeListeners = new WeakList<>();
  private WeakList<Consumer<Integer>> redoSizeListeners = new WeakList<>();

  public void setMaxUndo(int value) {
    this.maxUndo = value;
  }

  public History(
      ModelBase context,
      List<Long> undoHistory,
      List<Long> redoHistory,
      Long activeChange,
      int maxUndo) {
    this.context = context;
    this.maxUndo = maxUndo;
    if (activeChange == null) {
      changeStep = new ChangeStep(new ChangeStep.CacheId(context.nextId++));
    } else {
      changeStep = getNullable(new ChangeStep.CacheId(activeChange));
      if (changeStep == null) changeStep = new ChangeStep(new ChangeStep.CacheId(context.nextId++));
    }
    this.undoHistory =
        undoHistory.stream()
            .map(i -> new ChangeStep.CacheId(i))
            .collect(Collectors.toCollection(ArrayList::new));
    this.redoHistory =
        redoHistory.stream()
            .map(i -> new ChangeStep.CacheId(i))
            .collect(Collectors.toCollection(ArrayList::new));
    if (activeChange == null) context.committer.setDirty(changeStep);
  }

  public void addUndoSizeListener(Consumer<Integer> listener) {
    undoSizeListeners.add(listener);
    listener.accept(undoHistory.size());
  }
  public void removeUndoSizeListener(Consumer<Integer> listener) {
    undoSizeListeners.remove(listener);
  }
  public void addRedoSizeListener(Consumer<Integer> listener) {
    redoSizeListeners.add(listener);
    listener.accept(redoHistory.size());
  }
  public void removeRedoSizeListener(Consumer<Integer> listener) {
    redoSizeListeners.remove(listener);
  }

  private void notifyUndo() {
    new ArrayList<>(undoSizeListeners).forEach(c -> c.accept(undoHistory.size()));
    new ArrayList<>(redoSizeListeners).forEach(c -> c.accept(redoHistory.size()));
  }

  private Instant lastChangeTime = Instant.EPOCH;
  private History.Tuple lastChangeUnique = null;

  public void change(History.Tuple unique, Consumer<ChangeStep> consumer) {
    wrapHistory(
        () -> {
          Instant now = Instant.now();
          if (unique == null
              || lastChangeUnique == null
              || !unique.equals(lastChangeUnique)
              || now.isAfter(lastChangeTime.plusSeconds(1))) {
            lastChangeUnique = null;
            innerFinishChange();
          }
          lastChangeTime = now;
          lastChangeUnique = unique;
          innerChange(consumer);
        });
    notifyUndo();
  }

  private void wrapHistory(Common.UncheckedRunnable inner) {
    History.Tuple historicUnique = lastChangeUnique;
    try {
      inner.run();
    } catch (History.InChangeError e) {
      throw new Assertion(
          String.format(
              "Attempting concurrent changes! In %s, new change is %s.\n",
              historicUnique, lastChangeUnique));
    } catch (Exception e) {
      throw uncheck(e);
    }
  }

  public static class InChangeError extends Exception {}

  private void innerChange(Consumer<ChangeStep> cb) throws InChangeError {
    if (inChange) throw new InChangeError();
    clearRedo();
    inChange = true;
    context.lock.writeLock().lock();
    ChangeStep partial = new ChangeStep(this.changeStep.cacheId);
    try {
      cb.accept(partial);
    } catch (RuntimeException e) {
      partial.apply(context).remove(context); // Undo partial change
      context.debugCheckRefs();
      throw e;
    } finally {
      context.lock.writeLock().unlock();
      inChange = false;
    }
    for (Change change1 : partial.changes) this.changeStep.add(context, change1);
    context.debugCheckRefs();
    context.committer.setDirty(this.changeStep);
    context.committer.setDirty(context);
  }

  private void innerFinishChange() throws InChangeError {
    if (inChange) throw new InChangeError();
    if (changeStep.changes.isEmpty()) return;
    stepLookup.add(changeStep);
    undoHistory.add(changeStep.cacheId);
    clearRedo();
    List<ChangeStep.CacheId> excessUndo =
        undoHistory.subList(0, Math.max(0, undoHistory.size() - maxUndo));
    excessUndo.forEach(c -> get(c).remove(context));
    excessUndo.clear();
    changeStep = new ChangeStep(new ChangeStep.CacheId(context.nextId++));
    context.committer.setDirty(changeStep);
  }

  public ChangeStep getNullable(ChangeStep.CacheId id) {
    return get(id, true);
  }

  public ChangeStep get(ChangeStep.CacheId id) {
    return get(id, false);
  }

  public ChangeStep get(ChangeStep.CacheId id, boolean nullable) {
    Path path = ChangeStep.path(context, id.id);
    try {
      return stepLookup.getOrCreate(
          id,
          id1 ->
              uncheck(
                  () -> {
                    try (InputStream source = Files.newInputStream(path)) {
                      ChangeStep out = new ChangeStep(id1);
                      out.changes =
                          new StackReader()
                              .read(
                                  source,
                                  new StackReader.ArrayState() {
                                    @Override
                                    public void value(Object value) {
                                      if (!(value instanceof Change)) throw new Assertion();
                                      data.add(value);
                                    }

                                    @Override
                                    public StackReader.State record() {
                                      if (type == null)
                                        throw new IllegalStateException("Change has no type!");
                                      return context.deserializeChange(type);
                                    }
                                  });
                      return out;
                    }
                  }));
    } catch (Exception e) {
      if (nullable) {
        logger.writeException(e, "Failed to deserialize change %s", path);
        return null;
      }
      throw e;
    }
  }

  private void clearRedo() {
    for (ChangeStep.CacheId c : redoHistory) {
      get(c).remove(context);
    }
    redoHistory.clear();
  }

  private void innerClearHistory() throws InChangeError {
    innerFinishChange();
    for (ChangeStep.CacheId c : undoHistory) get(c).remove(context);
    undoHistory.clear();
    clearRedo();
  }

  private void innerUndo() throws InChangeError {
    innerFinishChange();
    if (undoHistory.isEmpty()) return;
    ChangeStep.CacheId selectedId = undoHistory.remove(undoHistory.size() - 1);
    ChangeStep selected = get(selectedId);
    ChangeStep redo = selected.apply(context);
    redoHistory.add(redo.cacheId);
    context.committer.setDirty(redo);
    context.committer.setDirty(context);
    stepLookup.add(redo);
    context.debugCheckRefs();
  }

  private void innerRedo() throws InChangeError {
    innerFinishChange();
    if (redoHistory.isEmpty()) return;
    ChangeStep.CacheId selectedId = redoHistory.remove(redoHistory.size() - 1);
    ChangeStep selected = get(selectedId);
    ChangeStep undo = selected.apply(context);
    context.committer.setDirty(undo);
    context.committer.setDirty(context);
    undoHistory.add(undo.cacheId);
    stepLookup.add(undo);
    context.debugCheckRefs();
  }

  public void undo() {
    wrapHistory(
        () -> {
          lastChangeUnique = null;
          innerUndo();
        });
    notifyUndo();
  }

  public void redo() {
    wrapHistory(
        () -> {
          lastChangeUnique = null;
          innerRedo();
        });
    notifyUndo();
  }

  public void finishChange() {
    wrapHistory(
        () -> {
          lastChangeUnique = null;
          innerFinishChange();
        });
    notifyUndo();
  }

  public void clearHistory() {
    wrapHistory(
        () -> {
          lastChangeUnique = null;
          innerClearHistory();
        });
    notifyUndo();
  }

  public void serialize(RawWriter writer) {
    writer.key("undo").arrayBegin();
    undoHistory.forEach(c -> writer.primitive(Objects.toString(c.id)));
    writer.arrayEnd();
    writer.key("redo").arrayBegin();
    redoHistory.forEach(c -> writer.primitive(Objects.toString(c.id)));
    writer.arrayEnd();
  }

  public static class Tuple {
    final List data;

    public Tuple(Object... data) {
      this.data = Arrays.asList(data);
    }

    @Override
    public boolean equals(Object obj) {
      Tuple other = (Tuple) obj;
      if (data.size() != other.data.size()) return false;
      for (int i = 0; i < data.size(); ++i)
        if (!Objects.equals(data.get(i), other.data.get(i))) return false;
      return true;
    }
  }
}
