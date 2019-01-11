package com.zarbosoft.shoedemo.deserialize;

import com.zarbosoft.shoedemo.model.ProjectObjectInterface;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangeDeserializationContext {
	public Map<Long, ProjectObjectInterface> objectMap;

	public <T> T getObject(long key) {
		return (T) objectMap.computeIfAbsent(key, k -> {
			throw new IllegalStateException(String.format("Can't find object %s in saved data.", key));
		});
	}
}
