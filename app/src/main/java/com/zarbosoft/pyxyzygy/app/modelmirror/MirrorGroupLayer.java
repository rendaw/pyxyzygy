package com.zarbosoft.pyxyzygy.app.modelmirror;

import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.core.model.v0.GroupLayer;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectNode;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectObject;
import com.zarbosoft.pyxyzygy.seed.model.Listener;

public class MirrorGroupLayer extends ObjectMirror {
	private final ObjectMirror parent;
	private final GroupLayer node;
	private final Listener.ScalarSet<GroupLayer, ProjectNode> innerSetListener;
	private ObjectMirror child;

	public MirrorGroupLayer(ProjectContext context, ObjectMirror.Context mirrorContext, ObjectMirror parent, GroupLayer node) {
		this.parent = parent;
		this.parentIndex = -1;
		this.node = node;

		this.innerSetListener = node.addInnerSetListeners((target, value) -> {
			if (child != null) {
				tree.unbind();
				child.remove(context);
			}
			if (value != null) {
				child = mirrorContext.create(context, MirrorGroupLayer.this, value);
				tree.bind(child.tree);
			}
		});
	}

	@Override
	public ObjectMirror getParent() {
		return parent;
	}

	@Override
	public ProjectObject getValue() {
		return node;
	}
	@Override
	public void remove(ProjectContext context) {
		node.removeInnerSetListeners(innerSetListener);
	}
}
