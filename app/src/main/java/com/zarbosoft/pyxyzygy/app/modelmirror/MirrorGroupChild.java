package com.zarbosoft.pyxyzygy.app.modelmirror;

import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.core.model.v0.GroupChild;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectLayer;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectObject;
import com.zarbosoft.pyxyzygy.seed.model.Listener;

public class MirrorGroupChild extends ObjectMirror {
	private final ObjectMirror parent;
	private final GroupChild node;
	private final Listener.ScalarSet<GroupChild, ProjectLayer> innerSetListener;
	private ObjectMirror child;

	public MirrorGroupChild(ProjectContext context, ObjectMirror.Context mirrorContext, ObjectMirror parent, GroupChild node) {
		this.parent = parent;
		this.parentIndex = -1;
		this.node = node;

		this.innerSetListener = node.addInnerSetListeners((target, value) -> {
			if (child != null) {
				tree.unbind();
				child.remove(context);
			}
			if (value != null) {
				child = mirrorContext.create(context, MirrorGroupChild.this, value);
				child.setParentIndex(parentIndex);
				tree.bind(child.tree);
			}
		});
	}

	@Override
	public void setParentIndex(int index) {
		super.setParentIndex(index);
		if (child != null) child.setParentIndex(index);
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
