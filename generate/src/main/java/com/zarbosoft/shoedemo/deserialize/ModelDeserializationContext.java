package com.zarbosoft.shoedemo.deserialize;

import com.zarbosoft.shoedemo.model.ChangeStep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModelDeserializationContext {
	public Map<Long, Object> objectMap = new HashMap<>();
	public List<Long> undoHistory = new ArrayList<>();
	public List<Long> redoHistory = new ArrayList<>();
	public long activeChange;

	public abstract static class Finisher {
		public abstract void finish(ModelDeserializationContext context);
	}

	public final List<Finisher> finishers = new ArrayList<>();

	public <T> T getObject(long key) {
		return (T) objectMap.computeIfAbsent(key, k -> {
			throw new IllegalStateException(String.format("Can't find object %s in saved data.", key));
		});
	}
}
