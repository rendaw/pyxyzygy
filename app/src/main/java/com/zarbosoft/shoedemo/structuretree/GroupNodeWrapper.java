package com.zarbosoft.shoedemo.structuretree;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.shoedemo.*;
import com.zarbosoft.shoedemo.model.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.zarbosoft.shoedemo.Main.*;
import static com.zarbosoft.shoedemo.ProjectContext.uniqueName1;

public class GroupNodeWrapper extends Wrapper {
	private final Wrapper parent;
	private final GroupNode node;
	private final ObservableList<Wrapper> children = FXCollections.observableArrayList();
	private final Runnable layerListenCleanup;

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

		layerListenCleanup = node.mirrorLayers(children,
				layer -> {
			return Window.createNode(context, this, -1, layer);
				},
				child -> child.remove(context),
				at -> {
					for (int i = at; i < children.size(); ++i)
						children.get(i).parentIndex = i;
				}
		);
		mirror(children, tree.get().getChildren(), child -> {
			child.tree.addListener((observable, oldValue, newValue) -> {
				tree.get().getChildren().set(child.parentIndex, newValue);
			});
			return child.tree.get();
		}, noopConsumer(), noopConsumer());
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
	public WidgetHandle buildCanvas(ProjectContext context, DoubleRectangle bounds) {
		return new WidgetHandle() {
			private Group canvas;
			private final Runnable layerListenCleanup;
			private final ProjectNode.OpacitySetListener opacityListener;
			private final ObservableList<WidgetHandle> childHandles = FXCollections.observableArrayList();

			{
				GroupNodeWrapper.this.bounds = bounds;
				canvas = new Group();
				layerListenCleanup = mirror(children,
						childHandles,
						c -> {
					return c.buildCanvas(context, GroupNodeWrapper.this.bounds);
						},
						h -> h.remove(),
						noopConsumer()
				);
				mirror(childHandles, canvas.getChildren(), h -> {
					return h.getWidget();}, noopConsumer(), noopConsumer());
				this.opacityListener = node.addOpacitySetListeners((target, value) -> {
					if (canvas != null) {
						canvas.setOpacity((double) value / opacityMax);
					}
				});
			}

			@Override
			public Node getWidget() {
				return canvas;
			}

			@Override
			public void remove() {
				canvas = null;
				childHandles.forEach(c -> c.remove());
				node.removeOpacitySetListeners(opacityListener);
				layerListenCleanup.run();
			}
		};
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
				newFrame.initialInnerLoopSet(context, 0);
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
		layerListenCleanup.run();
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
