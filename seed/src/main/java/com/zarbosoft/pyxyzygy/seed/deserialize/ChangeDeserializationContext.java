package com.zarbosoft.pyxyzygy.seed.deserialize;

import com.zarbosoft.pyxyzygy.seed.model.ProjectObjectInterface;

import java.util.Map;

public class ChangeDeserializationContext {
	public Map<Long, ProjectObjectInterface> objectMap;

	public <T> T getObject(long key) {
		return (T) objectMap.computeIfAbsent(key, k -> {
			throw new IllegalStateException(String.format("Can't find object %s in saved data.", key));
		});
	}
}
