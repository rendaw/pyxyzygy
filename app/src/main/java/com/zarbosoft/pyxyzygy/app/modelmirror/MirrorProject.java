package com.zarbosoft.pyxyzygy.app.modelmirror;

import com.zarbosoft.pyxyzygy.app.Misc;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.core.model.v0.Project;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectObject;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

public class MirrorProject extends ObjectMirror {
	private final ObjectMirror parent;
	private final Project object;
	private final ObservableList<ObjectMirror> children = FXCollections.observableArrayList();
	private final Runnable topListenCleanup;

	public MirrorProject(
			ProjectContext context, ObjectMirror.Context mirrorContext, ObjectMirror parent, Project object
	) {
		this.parent = parent;
		this.object = object;

		tree.set(new TreeItem<>(this));

		topListenCleanup = object.mirrorTop(children, layer -> {
			return mirrorContext.create(context, this, layer);
		}, child -> child.remove(context), at -> {
			for (int i = at; i < children.size(); ++i)
				children.get(i).setParentIndex(i);
		});
		Misc.mirror(children, tree.get().getChildren(), child -> {
			child.tree.addListener((observable, oldValue, newValue) -> {
				tree.get().getChildren().set(child.parentIndex, newValue);
			});
			return child.tree.get();
		}, Misc.noopConsumer(), Misc.noopConsumer());
	}

	@Override
	public ObjectMirror getParent() {
		return this.parent;
	}

	@Override
	public ProjectObject getValue() {
		return object;
	}

	@Override
	public void remove(ProjectContext context) {
		topListenCleanup.run();
	}
}
