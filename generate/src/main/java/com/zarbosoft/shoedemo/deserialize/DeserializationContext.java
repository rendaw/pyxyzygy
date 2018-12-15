package com.zarbosoft.shoedemo.deserialize;

import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.shoedemo.model.Change;
import com.zarbosoft.shoedemo.model.ProjectNode;
import com.zarbosoft.shoedemo.model.ProjectObject;
import com.zarbosoft.shoedemo.model.Tile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeserializationContext {
	/**
	 * For elements with ids (mutable) serialized flat at the top output level, return true.
	 * @param type
	 * @return
	 */
	public static boolean flattenPoint(TypeInfo type) {
		if (ProjectObject.class.isAssignableFrom((Class) type.type))
			return true;
		if (Tile.class.isAssignableFrom((Class) type.type))
			return true;
		return false;
	}

	public static String decideName(Class k) {
		return k.getSimpleName();
	}

	public abstract static class Finisher {
		public abstract void finish(DeserializationContext context);
	}
	public final List<Finisher> finishers = new ArrayList<>();
}
