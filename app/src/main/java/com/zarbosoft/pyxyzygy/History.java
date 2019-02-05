package com.zarbosoft.pyxyzygy;

import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.luxem.write.RawWriter;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.WeakCache;
import com.zarbosoft.internal.pyxyzygy_seed.deserialize.ChangeDeserializationContext;
import com.zarbosoft.internal.pyxyzygy_seed.model.Change;
import com.zarbosoft.internal.pyxyzygy_seed.model.ChangeStep;
import com.zarbosoft.pyxyzygy.model.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.model.DeserializeHelper;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.zarbosoft.rendaw.common.Common.uncheck;

public class History {
	private final ProjectContext context;
	private final WeakCache<ChangeStep.CacheId, ChangeStep> stepLookup = new WeakCache<>(c -> c.cacheId);
	public final List<ChangeStep.CacheId> undoHistory;
	public final List<ChangeStep.CacheId> redoHistory;
	public ChangeStepBuilder change;
	private boolean inChange = false;

	public History(ProjectContext context, List<Long> undoHistory, List<Long> redoHistory, Long activeChange) {
		this.context = context;
		change = new ChangeStepBuilder(
				context,
				activeChange == null ?
						new ChangeStep(new ChangeStep.CacheId(context.nextId++)) :
						get(new ChangeStep.CacheId(activeChange))
		);
		this.undoHistory = undoHistory
				.stream()
				.map(i -> new ChangeStep.CacheId(i))
				.collect(Collectors.toCollection(ArrayList::new));
		this.redoHistory = redoHistory
				.stream()
				.map(i -> new ChangeStep.CacheId(i))
				.collect(Collectors.toCollection(ArrayList::new));
		if (activeChange == null) context.setDirty(change.changeStep);
	}

	public void change(Consumer<ChangeStepBuilder> cb) {
		if (inChange)
			throw new Assertion();
		clearRedo();
		inChange = true;
		context.lock.writeLock().lock();
		try {
			cb.accept(change);
		} finally {
			context.lock.writeLock().unlock();
		}
		context.setDirty(change.changeStep);
		context.setDirty(context);
		context.debugCheckRefCounts();
		inChange = false;
	}

	public void finishChange() {
		if (change.changeStep.changes.isEmpty())
			return;
		stepLookup.add(change.changeStep);
		undoHistory.add(change.changeStep.cacheId);
		clearRedo();
		List<ChangeStep.CacheId> excessUndo = undoHistory.subList(0,
				Math.max(0, undoHistory.size() - Launch.config.maxUndo)
		);
		excessUndo.forEach(c -> get(c).remove(context));
		excessUndo.clear();
		change = new ChangeStepBuilder(context, new ChangeStep(new ChangeStep.CacheId(context.nextId++)));
		context.setDirty(change.changeStep);
		System.out.format("change done; undo %s, redo %s\n", undoHistory.size(), redoHistory.size());
	}

	public ChangeStep get(ChangeStep.CacheId id) {
		return stepLookup.getOrCreate(id, id1 -> uncheck(() -> {
			try (InputStream source = Files.newInputStream(ChangeStep.path(context, id1.id))) {
				ChangeDeserializationContext deserializationContext = new ChangeDeserializationContext();
				deserializationContext.objectMap = context.objectMap;
				ChangeStep out = new ChangeStep(id1);
				out.changes = new StackReader().read(source, new StackReader.ArrayState() {
					@Override
					public void value(Object value) {
						if (!(value instanceof Change))
							throw new Assertion();
						data.add(value);
					}

					@Override
					public StackReader.State record() {
						return DeserializeHelper.deserializeChange(deserializationContext, type);
					}
				});
				return out;
			}
		}));
	}

	private void clearRedo() {
		for (ChangeStep.CacheId c : redoHistory) {
			get(c).remove(context);
		}
		redoHistory.clear();
	}

	public void clearHistory() {
		finishChange();
		for (ChangeStep.CacheId c : undoHistory)
			get(c).remove(context);
		undoHistory.clear();
		clearRedo();
	}

	public void undo() {
		if (undoHistory.isEmpty())
			return;
		ChangeStep.CacheId selectedId = undoHistory.remove(undoHistory.size() - 1);
		ChangeStep selected = get(selectedId);
		ChangeStep redo = selected.apply(context);
		redoHistory.add(redo.cacheId);
		context.setDirty(redo);
		context.setDirty(context);
		stepLookup.add(redo);
		System.out.format("undo done; undo %s, redo %s\n", undoHistory.size(), redoHistory.size());
		context.debugCheckRefCounts();
	}

	public void redo() {
		if (redoHistory.isEmpty())
			return;
		ChangeStep.CacheId selectedId = redoHistory.remove(redoHistory.size() - 1);
		ChangeStep selected = get(selectedId);
		ChangeStep undo = selected.apply(context);
		context.setDirty(undo);
		context.setDirty(context);
		undoHistory.add(undo.cacheId);
		stepLookup.add(undo);
		System.out.format("redo done; undo %s, redo %s\n", undoHistory.size(), redoHistory.size());
		context.debugCheckRefCounts();
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
