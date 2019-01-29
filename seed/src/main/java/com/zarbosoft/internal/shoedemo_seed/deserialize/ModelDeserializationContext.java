package com.zarbosoft.internal.shoedemo_seed.deserialize;

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

	public <T> T getObject(Long key) {
		if (key == null) return null;
		return (T) objectMap.computeIfAbsent(key, k -> {
			throw new IllegalStateException(String.format("Can't find object %s in saved data.", key));
		});
	}
}