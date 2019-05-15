package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.config.GroupNodeConfig;
import com.zarbosoft.pyxyzygy.app.config.NodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.core.model.v0.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;

import java.util.stream.Collectors;

import static com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext.uniqueName1;

public class GroupNodeWrapper extends Wrapper {
	private final Wrapper parent;
	final GroupLayer node;
	final GroupNodeConfig config;
	final ObservableList<GroupChildWrapper> children = FXCollections.observableArrayList();
	private final Runnable childrenListenCleanup;

	GroupChild specificChild;
	public GroupNodeCanvasHandle canvasHandle;

	public GroupNodeWrapper(ProjectContext context, Wrapper parent, int parentIndex, GroupLayer node) {
		this.parentIndex = parentIndex;
		this.parent = parent;
		this.node = node;
		config = initConfig(context, node.id());

		tree.set(new TreeItem<>(this));

		childrenListenCleanup = node.mirrorChildren(children, child -> {
			return (GroupChildWrapper)Window.createNode(context, this, -1, child);
		}, child -> {
			child.remove(context);
		}, at -> {
			for (int i = at; i < children.size(); ++i)
				children.get(i).setParentIndex(i);
		});
		Misc.mirror(children, tree.get().getChildren(), child -> {
			child.tree.addListener((observable, oldValue, newValue) -> {
				tree.get().getChildren().set(child.parentIndex, newValue);
			});
			return child.tree.get();
		}, c -> {
		}, Misc.noopConsumer());
	}

	protected GroupNodeConfig initConfig(ProjectContext context, long id) {
		return (GroupNodeConfig) context.config.nodes.computeIfAbsent(id, id1 -> new GroupNodeConfig(context));
	}

	@Override
	public Wrapper getParent() {
		return parent;
	}

	@Override
	public ProjectObject getValue() {
		return node;
	}

	@Override
	public NodeConfig getConfig() {
		return config;
	}

	@Override
	public CanvasHandle buildCanvas(
			ProjectContext context, Window window, CanvasHandle parent
	) {
		if (canvasHandle == null)
			canvasHandle = new GroupNodeCanvasHandle(context, window, parent, this);
		return canvasHandle;
	}

	@Override
	public EditHandle buildEditControls(
			ProjectContext context, Window window
	) {
		return new GroupNodeEditHandle(context, window, this);
	}

	public void cloneSet(ProjectContext context, GroupLayer clone) {
		clone.initialNameSet(context, uniqueName1(node.name()));
		clone.initialOffsetSet(context, node.offset());
		clone.initialChildrenAdd(context, node.children().stream().map(child -> {
			GroupChild newLayer = GroupChild.create(context);
			newLayer.initialOpacitySet(context, child.opacity());
			newLayer.initialEnabledSet(context, true);
			newLayer.initialInnerSet(context, child.inner());
			newLayer.initialPositionFramesAdd(context, child.positionFrames().stream().map(frame -> {
				GroupPositionFrame newFrame = GroupPositionFrame.create(context);
				newFrame.initialLengthSet(context, frame.length());
				newFrame.initialOffsetSet(context, frame.offset());
				return newFrame;
			}).collect(Collectors.toList()));
			newLayer.initialTimeFramesAdd(context, child.timeFrames().stream().map(frame -> {
				GroupTimeFrame newFrame = GroupTimeFrame.create(context);
				newFrame.initialLengthSet(context, frame.length());
				newFrame.initialInnerOffsetSet(context, frame.innerOffset());
				newFrame.initialInnerLoopSet(context, 0);
				return newFrame;
			}).collect(Collectors.toList()));
			return newLayer;
		}).collect(Collectors.toList()));
	}

	@Override
	public ProjectLayer separateClone(ProjectContext context) {
		GroupLayer clone = GroupLayer.create(context);
		cloneSet(context, clone);
		return clone;
	}

	@Override
	public void deleteChild(
			ProjectContext context, ChangeStepBuilder change, int index
	) {
		change.groupLayer(node).childrenRemove(index, 1);
	}

	@Override
	public TakesChildren takesChildren() {
		return TakesChildren.ANY;
	}

	@Override
	public void remove(ProjectContext context) {
		childrenListenCleanup.run();
	}

	public void setSpecificChild(GroupChild child) {
		this.specificChild = child;
	}
}
