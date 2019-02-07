package com.zarbosoft.pyxyzygy.gui.modelmirror;

import com.zarbosoft.pyxyzygy.*;
import com.zarbosoft.pyxyzygy.model.*;

public class MirrorGroupLayer extends ObjectMirror {
	private final ObjectMirror parent;
	private final GroupLayer node;
	private final GroupLayer.InnerSetListener innerSetListener;
	private ObjectMirror child;

	public MirrorGroupLayer(ProjectContext context,ObjectMirror.Context mirrorContext, ObjectMirror parent, GroupLayer node) {
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
