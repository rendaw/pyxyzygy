package com.zarbosoft.shoedemo.modelmirror;

import com.zarbosoft.shoedemo.*;
import com.zarbosoft.shoedemo.model.*;
import javafx.scene.control.*;

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
		context.config.nodes.remove(node.id());
	}
}