package com.zarbosoft.pyxyzygy.gui.modelmirror;

import com.zarbosoft.pyxyzygy.*;
import com.zarbosoft.pyxyzygy.model.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

public class MirrorGroupNode extends ObjectMirror {
	private final ObjectMirror parent;
	private final GroupNode node;
	private final ObservableList<ObjectMirror> children = FXCollections.observableArrayList();
	private final Runnable layerListenCleanup;

	public MirrorGroupNode(ProjectContext context, ObjectMirror.Context mirrorContext, ObjectMirror parent, GroupNode node) {
		this.parentIndex = -1;
		this.parent = parent;
		this.node = node;

		tree.set(new TreeItem<>(this));

		layerListenCleanup = node.mirrorLayers(children, layer -> {
			return mirrorContext.create(context, this, layer);
		}, child -> child.remove(context), at -> {
			for (int i = at; i < children.size(); ++i)
				children.get(i).parentIndex = i;
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
		return parent;
	}

	@Override
	public ProjectObject getValue() {
		return node;
	}

	@Override
	public void remove(ProjectContext context) {
		layerListenCleanup.run();
	}
}
