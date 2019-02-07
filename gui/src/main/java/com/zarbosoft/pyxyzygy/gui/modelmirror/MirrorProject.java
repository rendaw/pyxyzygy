package com.zarbosoft.pyxyzygy.gui.modelmirror;

import com.zarbosoft.pyxyzygy.ProjectContext;
import com.zarbosoft.pyxyzygy.model.Project;
import com.zarbosoft.pyxyzygy.model.ProjectObject;
import javafx.scene.control.TreeItem;

public class MirrorProject extends ObjectMirror {
	private final ObjectMirror parent;
	private final Project object;
	private final Runnable cleanup;

	public MirrorProject(
			ProjectContext context, ObjectMirror.Context mirrorContext, ObjectMirror parent, Project object
	) {
		this.parent = parent;
		this.object = object;
		tree.set(new TreeItem<>(this));
		cleanup = context.project.mirrorTop(
				tree.get().getChildren(),
				o -> new TreeItem<>(mirrorContext.create(context, this, o)),
				i -> i.getValue().remove(context),
				index -> {
					for (int i = index; i < tree.get().getChildren().size(); ++i) {
						tree.get().getChildren().get(i).getValue().parentIndex = i;
					}
				}
		);
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
		cleanup.run();
	}
}
