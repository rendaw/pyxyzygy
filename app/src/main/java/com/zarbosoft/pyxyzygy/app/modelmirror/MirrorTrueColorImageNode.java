package com.zarbosoft.pyxyzygy.app.modelmirror;

import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectObject;
import com.zarbosoft.pyxyzygy.core.model.v0.TrueColorImageNode;
import javafx.scene.control.TreeItem;

public class MirrorTrueColorImageNode extends ObjectMirror {
	private final TrueColorImageNode node;
	private final ObjectMirror parent;

	public MirrorTrueColorImageNode(ObjectMirror parent, TrueColorImageNode node) {
		this.node = node;
		this.parent = parent;
		this.parentIndex = -1;
		tree.set(new TreeItem<>(this));
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
	}
}
