package com.zarbosoft.pyxyzygy.wrappers.group;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.internal.pyxyzygy_seed.model.Vector;
import com.zarbosoft.pyxyzygy.*;
import com.zarbosoft.pyxyzygy.config.GroupNodeConfig;
import com.zarbosoft.pyxyzygy.config.NodeConfig;
import com.zarbosoft.pyxyzygy.model.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;

import java.util.List;
import java.util.stream.Collectors;

import static com.zarbosoft.pyxyzygy.ProjectContext.uniqueName1;

public class GroupNodeWrapper extends Wrapper {
	private final Wrapper parent;
	final GroupNode node;
	final GroupNodeConfig config;
	final ObservableList<Wrapper> children = FXCollections.observableArrayList();
	private final Runnable layerListenCleanup;

	GroupLayer specificLayer;
	GroupNodeCanvasHandle canvasHandle;

	public GroupNodeWrapper(ProjectContext context, Wrapper parent, int parentIndex, GroupNode node) {
		this.parentIndex = parentIndex;
		this.parent = parent;
		this.node = node;
		config = (GroupNodeConfig) context.config.nodes.computeIfAbsent(node.id(), id -> new GroupNodeConfig());

		tree.set(new TreeItem<>(this));

		layerListenCleanup = node.mirrorLayers(children, layer -> {
			System.out.format("creating group node child 1.\n");
			return Window.createNode(context, this, -1, layer);
		}, child -> {
			System.out.format("removing group node child 1.\n");
			child.remove(context);
		}, at -> {
			for (int i = at; i < children.size(); ++i)
				children.get(i).parentIndex = i;
		});
		Misc.mirror(children, tree.get().getChildren(), child -> {
			System.out.format("creating group node child 2.\n");
			child.tree.addListener((observable, oldValue, newValue) -> {
				tree.get().getChildren().set(child.parentIndex, newValue);
			});
			return child.tree.get();
		}, c -> {
			System.out.format("removing group node child 2.\n");
		}, Misc.noopConsumer());
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
	public CanvasHandle buildCanvas(ProjectContext context, CanvasHandle parent) {
		return canvasHandle =new GroupNodeCanvasHandle(context, parent, this);
	}

	@Override
	public EditHandle buildEditControls(ProjectContext context, TabPane tabPane) {
		return new GroupNodeEditHandle(this, context, tabPane);
	}

	@Override
	public boolean addChildren(ProjectContext context, int at, List<ProjectNode> newChildren) {
		context.history.change(c -> c
				.groupNode(node)
				.layersAdd(at == -1 ? node.layersLength() : at, newChildren.stream().map(child -> {
					GroupLayer layer = GroupLayer.create(context);
					layer.initialInnerSet(context, child);
					GroupPositionFrame positionFrame = GroupPositionFrame.create(context);
					positionFrame.initialLengthSet(context, -1);
					positionFrame.initialOffsetSet(context, new Vector(0, 0));
					layer.initialPositionFramesAdd(context, ImmutableList.of(positionFrame));
					GroupTimeFrame timeFrame = GroupTimeFrame.create(context);
					timeFrame.initialLengthSet(context, -1);
					timeFrame.initialInnerOffsetSet(context, 0);
					timeFrame.initialInnerLoopSet(context, 0);
					layer.initialTimeFramesAdd(context, ImmutableList.of(timeFrame));
					return layer;
				}).collect(Collectors.toList())));
		return true;
	}

	@Override
	public void delete(ProjectContext context) {
		if (parent != null)
			parent.removeChild(context, parentIndex);
		else
			context.history.change(c -> c.project(context.project).topRemove(parentIndex, 1));
	}

	public void cloneSet(ProjectContext context, GroupNode clone) {
		clone.initialNameSet(context, uniqueName1(node.name()));
		clone.initialOpacitySet(context, node.opacity());
		clone.initialLayersAdd(context, node.layers().stream().map(layer -> {
			GroupLayer newLayer = GroupLayer.create(context);
			newLayer.initialInnerSet(context, layer.inner());
			newLayer.initialPositionFramesAdd(context, layer.positionFrames().stream().map(frame -> {
				GroupPositionFrame newFrame = GroupPositionFrame.create(context);
				newFrame.initialLengthSet(context, frame.length());
				newFrame.initialOffsetSet(context, frame.offset());
				return newFrame;
			}).collect(Collectors.toList()));
			newLayer.initialTimeFramesAdd(context, layer.timeFrames().stream().map(frame -> {
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
	public ProjectNode separateClone(ProjectContext context) {
		GroupNode clone = GroupNode.create(context);
		cloneSet(context, clone);
		return clone;
	}

	@Override
	public void removeChild(ProjectContext context, int index) {
		context.history.change(c -> c.groupNode(node).layersRemove(index, 1));
	}

	@Override
	public TakesChildren takesChildren() {
		return TakesChildren.ANY;
	}

	@Override
	public void remove(ProjectContext context) {
		layerListenCleanup.run();
		context.config.nodes.remove(node.id());
	}

	public void setSpecificLayer(GroupLayer layer) {
		this.specificLayer = layer;
	}
}
