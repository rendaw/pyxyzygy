package com.zarbosoft.shoedemo.structuretree;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.shoedemo.*;
import com.zarbosoft.shoedemo.model.*;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.zarbosoft.shoedemo.Main.nodeFormFields;
import static com.zarbosoft.shoedemo.Main.opacityMax;
import static com.zarbosoft.shoedemo.Timeline.moveTo;
import static com.zarbosoft.shoedemo.Timeline.moveWrapperTo;
import static com.zarbosoft.shoedemo.Window.uniqueName1;

public class GroupNodeWrapper extends Wrapper {
	private final Wrapper parent;
	private final GroupNode node;
	private final List<Wrapper> children = new ArrayList<>();
	private final ProjectNode.OpacitySetListener opacityListener;
	private final GroupNode.LayersAddListener layerAddListener;
	private final GroupNode.LayersRemoveListener layerRemoveListener;
	private final GroupNode.LayersMoveToListener layerMoveToListener;
	private final GroupNode.LayersClearListener layerClearListener;

	private Group canvas;
	private DoubleRectangle bounds;
	private GroupLayer specificLayer;
	private DoubleVector markStart;
	private Vector markStartOffset;
	private int currentFrame = 0;

	public GroupNodeWrapper(ProjectContext context, Wrapper parent, int parentIndex, GroupNode node) {
		this.parentIndex = parentIndex;
		this.parent = parent;
		this.node = node;

		tree.set(new TreeItem<>(this));

		this.opacityListener = node.addOpacitySetListeners((target, value) -> {
			if (canvas != null) {
				canvas.setOpacity((double)value / opacityMax);
			}
		});
		this.layerAddListener = node.addLayersAddListeners((target, at, value) -> {
			List<Wrapper> newChildren = new ArrayList<>();
			List<TreeItem<Wrapper>> newTreeChildren = new ArrayList<>();
			List<Node> newCanvas = new ArrayList<>();
			for (int i = 0; i < value.size(); ++i) {
				GroupLayer v = value.get(i);
				Wrapper child = Window.createNode(context, this, at + i, v);
				newChildren.add(child);
				child.tree.addListener((observable, oldValue, newValue) -> {
					tree.get().getChildren().set(child.parentIndex, newValue);
				});
				newTreeChildren.add(child.tree.getValue());
				if (canvas != null)
					newCanvas.add(child.buildCanvas(context, bounds));
			}
			children.addAll(at, newChildren);
			tree.get().getChildren().addAll(at, newTreeChildren);
			if (canvas != null)
				canvas.getChildren().addAll(at, newCanvas);
		});
		this.layerRemoveListener = node.addLayersRemoveListeners((target, at, count) -> {
			if (canvas != null)
				canvas.getChildren().subList(at, at + count).clear();
			List<Wrapper> temp = children.subList(at, at + count);
			temp.forEach(c -> {
				if (canvas != null)
					c.destroyCanvas();
				c.remove(context);
			});
			temp.clear();
			for (int i = at; i < children.size(); ++i)
				children.get(i).parentIndex = i;
			tree.get().getChildren().subList(at, at + count).clear();
		});
		this.layerMoveToListener = node.addLayersMoveToListeners((target, source, count, dest) -> {
			moveWrapperTo(children, source, count, dest);
			moveTo(tree.get().getChildren(), source, count, dest);
			if (canvas != null)
				moveTo(canvas.getChildren(), source, count, dest);
		});
		this.layerClearListener = node.addLayersClearListeners(target -> {
			children.forEach(c -> c.remove(context));
			children.clear();
			tree.get().getChildren().clear();
			if (canvas != null)
				canvas.getChildren().clear();
		});
	}

	@Override
	public Wrapper getParent() {
		return parent;
	}

	@Override
	public DoubleVector toInner(DoubleVector vector) {
		return vector;
	}

	@Override
	public ProjectObject getValue() {
		return node;
	}

	@Override
	public void scroll(
			ProjectContext context, DoubleRectangle oldBounds, DoubleRectangle newBounds
	) {
		this.bounds = newBounds;
		children.forEach(c -> c.scroll(context, oldBounds, newBounds));
	}

	@Override
	public Node buildCanvas(ProjectContext context, DoubleRectangle bounds) {
		this.bounds = bounds;
		canvas = new Group();
		canvas
				.getChildren()
				.addAll(children.stream().map(c -> c.buildCanvas(context, bounds)).collect(Collectors.toList()));
		return canvas;
	}

	@Override
	public Node getCanvas() {
		return canvas;
	}

	@Override
	public void destroyCanvas() {
		canvas = null;
		children.forEach(c -> c.destroyCanvas());
	}

	@Override
	public void markStart(ProjectContext context, DoubleVector start) {
		if (specificLayer == null)
			return;
		this.markStart = start;
		GroupPositionFrame pos = GroupLayerWrapper.findPosition(specificLayer, currentFrame).frame;
		this.markStartOffset = pos.offset();
	}

	@Override
	public void mark(ProjectContext context, DoubleVector start, DoubleVector end) {
		if (specificLayer == null)
			return;
		GroupPositionFrame pos = GroupLayerWrapper.findPosition(specificLayer, currentFrame).frame;
		context.history.change(c -> c
				.groupPositionFrame(pos)
				.offsetSet(end.minus(markStart).plus(markStartOffset).toInt()));
		System.out.format("group offset changed\n");
		specificLayer
				.positionFrames()
				.forEach(f -> System.out.format("  pos %s %s,%s\n", f.length(), f.offset().x, f.offset().y));
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

	@Override
	public ProjectNode separateClone(ProjectContext context) {
		GroupNode clone = GroupNode.create(context);
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
				return newFrame;
			}).collect(Collectors.toList()));
			return newLayer;
		}).collect(Collectors.toList()));
		return clone;
	}

	@Override
	public void render(GraphicsContext gc, int frame, Rectangle crop) {
		for (Wrapper child : children)
			child.render(gc, frame, crop);
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
	public void setFrame(ProjectContext context, int frameNumber) {
		this.currentFrame = frameNumber;
		children.forEach(c -> c.setFrame(context, frameNumber));
	}

	@Override
	public void remove(ProjectContext context) {
		node.removeOpacitySetListeners(opacityListener);
		node.removeLayersAddListeners(layerAddListener);
		node.removeLayersRemoveListeners(layerRemoveListener);
		node.removeLayersMoveToListeners(layerMoveToListener);
		node.removeLayersClearListeners(layerClearListener);
	}

	@Override
	public WidgetHandle createProperties(ProjectContext context) {
		List<Runnable> cleanup = new ArrayList<>();
		Node widget = new WidgetFormBuilder().apply(b -> cleanup.add(nodeFormFields(context, b, node))).build();
		return new WidgetHandle() {
			@Override
			public Node getWidget() {
				return widget;
			}

			@Override
			public void remove() {
				cleanup.forEach(c -> c.run());
			}
		};
	}

	public void setSpecificLayer(GroupLayer layer) {
		this.specificLayer = layer;
	}
}
