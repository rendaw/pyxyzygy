package com.zarbosoft.shoedemo.modelmirror;

import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.shoedemo.*;
import com.zarbosoft.shoedemo.config.NodeConfig;
import com.zarbosoft.shoedemo.model.*;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.TabPane;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.zarbosoft.shoedemo.Main.moveTo;

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
