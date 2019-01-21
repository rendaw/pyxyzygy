package com.zarbosoft.shoedemo.modelmirror;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.shoedemo.*;
import com.zarbosoft.shoedemo.config.GroupNodeConfig;
import com.zarbosoft.shoedemo.config.NodeConfig;
import com.zarbosoft.shoedemo.model.*;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.zarbosoft.shoedemo.Main.*;
import static com.zarbosoft.shoedemo.ProjectContext.uniqueName1;

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
		mirror(children, tree.get().getChildren(), child -> {
			child.tree.addListener((observable, oldValue, newValue) -> {
				tree.get().getChildren().set(child.parentIndex, newValue);
			});
			return child.tree.get();
		}, noopConsumer(), noopConsumer());
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
