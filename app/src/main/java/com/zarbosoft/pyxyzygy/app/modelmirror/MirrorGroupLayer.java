package com.zarbosoft.pyxyzygy.app.modelmirror;

import com.zarbosoft.pyxyzygy.app.ProjectContext;
import com.zarbosoft.pyxyzygy.core.model.GroupLayer;
import com.zarbosoft.pyxyzygy.core.model.ProjectObject;

public class MirrorGroupLayer extends ObjectMirror {
	private final ObjectMirror parent;
	private final GroupLayer node;
	private final GroupLayer.InnerSetListener innerSetListener;
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
