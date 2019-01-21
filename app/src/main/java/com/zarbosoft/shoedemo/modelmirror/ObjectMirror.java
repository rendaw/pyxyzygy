package com.zarbosoft.shoedemo.modelmirror;

import com.zarbosoft.shoedemo.*;
import com.zarbosoft.shoedemo.model.ProjectObject;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeItem;

public abstract class ObjectMirror {
	public int parentIndex;
	public final SimpleObjectProperty<TreeItem<ObjectMirror>> tree = new SimpleObjectProperty<>();

	public abstract ObjectMirror getParent();

	public abstract ProjectObject getValue();

	public abstract void remove(ProjectContext context);

	public abstract static class Context {
		public abstract ObjectMirror create(
				ProjectContext context, ObjectMirror parent, ProjectObject object
		);
	}
}
