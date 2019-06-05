package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.luxem.write.RawWriter;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.core.model.v0.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.v0.DeserializeHelper;
import com.zarbosoft.pyxyzygy.seed.deserialize.ChangeDeserializationContext;
import com.zarbosoft.pyxyzygy.seed.model.Change;
import com.zarbosoft.pyxyzygy.seed.model.ChangeStep;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.WeakCache;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.zarbosoft.pyxyzygy.app.Global.logger;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public class History {
  private final ProjectContext context;
  private final WeakCache<ChangeStep.CacheId, ChangeStep> stepLookup =
      new WeakCache<>(c -> c.cacheId);
  public final List<ChangeStep.CacheId> undoHistory;
  public final List<ChangeStep.CacheId> redoHistory;
  public ChangeStep changeStep;
  private boolean inChange = false;

  public History(
      ProjectContext context, List<Long> undoHistory, List<Long> redoHistory, Long activeChange) {
    this.context = context;
    changeStep =
        activeChange == null
            ? new ChangeStep(new ChangeStep.CacheId(context.nextId++))
            : get(new ChangeStep.CacheId(activeChange));
    this.undoHistory =
        undoHistory.stream()
            .map(i -> new ChangeStep.CacheId(i))
            .collect(Collectors.toCollection(ArrayList::new));
    this.redoHistory =
        redoHistory.stream()
            .map(i -> new ChangeStep.CacheId(i))
            .collect(Collectors.toCollection(ArrayList::new));
    if (activeChange == null) context.setDirty(changeStep);
  }

  public static class InChangeError extends Exception {}

  public void change(Consumer<ChangeStepBuilder> cb) throws InChangeError {
    if (inChange) throw new InChangeError();
    clearRedo();
    inChange = true;
    context.lock.writeLock().lock();
    ChangeStepBuilder partial = new ChangeStepBuilder(context, new ChangeStep(changeStep.cacheId));
    try {
      cb.accept(partial);
    } catch (RuntimeException e) {
      partial.changeStep.apply(context).remove(context); // Undo partial change
      context.debugCheckRefs();
      throw e;
    } finally {
      context.lock.writeLock().unlock();
      inChange = false;
    }
    for (Change change1 : partial.changeStep.changes) changeStep.add(context, change1);
    context.debugCheckRefs();
    context.setDirty(changeStep);
    context.setDirty(context);
  }

  public void finishChange() throws InChangeError {
    if (inChange) throw new InChangeError();
    if (changeStep.changes.isEmpty()) return;
    stepLookup.add(changeStep);
    undoHistory.add(changeStep.cacheId);
    clearRedo();
    List<ChangeStep.CacheId> excessUndo =
        undoHistory.subList(0, Math.max(0, undoHistory.size() - GUILaunch.profileConfig.maxUndo));
    excessUndo.forEach(c -> get(c).remove(context));
    excessUndo.clear();
    changeStep = new ChangeStep(new ChangeStep.CacheId(context.nextId++));
    context.setDirty(changeStep);
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
                      ChangeDeserializationContext deserializationContext =
                          new ChangeDeserializationContext();
                      deserializationContext.objectMap = context.objectMap;
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
                                      return DeserializeHelper.deserializeChange(
                                          deserializationContext, type);
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

  public void clearHistory() throws InChangeError {
    finishChange();
    for (ChangeStep.CacheId c : undoHistory) get(c).remove(context);
    undoHistory.clear();
    clearRedo();
  }

  public void undo() throws InChangeError {
    finishChange();
    if (undoHistory.isEmpty()) return;
    ChangeStep.CacheId selectedId = undoHistory.remove(undoHistory.size() - 1);
    ChangeStep selected = get(selectedId);
    ChangeStep redo = selected.apply(context);
    redoHistory.add(redo.cacheId);
    context.setDirty(redo);
    context.setDirty(context);
    stepLookup.add(redo);
    context.debugCheckRefs();
  }

  public void redo() throws InChangeError {
    finishChange();
    if (redoHistory.isEmpty()) return;
    ChangeStep.CacheId selectedId = redoHistory.remove(redoHistory.size() - 1);
    ChangeStep selected = get(selectedId);
    ChangeStep undo = selected.apply(context);
    context.setDirty(undo);
    context.setDirty(context);
    undoHistory.add(undo.cacheId);
    stepLookup.add(undo);
    context.debugCheckRefs();
  }

  public void serialize(RawWriter writer) {
    writer.key("undo").arrayBegin();
    undoHistory.forEach(c -> writer.primitive(Objects.toString(c.id)));
    writer.arrayEnd();
    writer.key("redo").arrayBegin();
    redoHistory.forEach(c -> writer.primitive(Objects.toString(c.id)));
    writer.arrayEnd();
  }
}
