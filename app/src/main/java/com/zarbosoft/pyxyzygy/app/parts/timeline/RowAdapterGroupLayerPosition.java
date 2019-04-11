package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.WidgetHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.wrappers.FrameFinder;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupLayerWrapper;
import com.zarbosoft.pyxyzygy.core.model.v0.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.v0.GroupLayer;
import com.zarbosoft.pyxyzygy.core.model.v0.GroupPositionFrame;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.zarbosoft.pyxyzygy.app.parts.timeline.Timeline.emptyStateImage;

public class RowAdapterGroupLayerPosition extends BaseFrameRowAdapter<GroupLayer, GroupPositionFrame> {
	private final GroupLayer layer;
	private final RowAdapterGroupLayer layerRowAdapter;

	public RowAdapterGroupLayerPosition(
			Timeline timeline, GroupLayer layer, RowAdapterGroupLayer layerRowAdapter
	) {
		super(timeline);
		this.layer = layer;
		this.layerRowAdapter = layerRowAdapter;
	}

	@Override
	public WidgetHandle createRowWidget(ProjectContext context, Window window) {
		return new WidgetHandle() {
			private VBox layout;
			Runnable framesCleanup;
			private List<Runnable> frameCleanup = new ArrayList<>();

			{
				framesCleanup = layer.mirrorPositionFrames(frameCleanup, f -> {
					Listener.ScalarSet<GroupPositionFrame, Integer> lengthListener =
							f.addLengthSetListeners((target, value) -> {
								updateTime(context, window);
							});
					return () -> {
						f.removeLengthSetListeners(lengthListener);
					};
				}, r -> {
					r.run();
				}, at -> {
					updateTime(context, window);
				});
				layout = new VBox();
				row = Optional.of(new RowFramesWidget(window, timeline, RowAdapterGroupLayerPosition.this));
				layout.getChildren().add(row.get());
			}

			@Override
			public Node getWidget() {
				return layout;
			}

			@Override
			public void remove() {
				framesCleanup.run();
				frameCleanup.forEach(c -> c.run());
			}
		};
	}

	@Override
	public ObservableValue<String> getName() {
		return new ObservableValueBase<String>() {
			@Override
			public String getValue() {
				return "Position";
			}
		};
	}

	@Override
	public ObservableObjectValue<Image> getStateImage() {
		return emptyStateImage;
	}

	@Override
	public void deselected() {
		layerRowAdapter.treeDeselected();
	}

	@Override
	public void selected() {
		layerRowAdapter.treeSelected();
	}

	@Override
	public void remove(ProjectContext context) {
	}

	@Override
	protected void addFrame(
			ChangeStepBuilder change, int index, GroupPositionFrame frame
	) {
		change.groupLayer(layer).positionFramesAdd(index, frame);
	}

	@Override
	protected GroupPositionFrame innerCreateFrame(
			ProjectContext context, GroupPositionFrame previousFrame
	) {
		GroupPositionFrame newFrame = GroupPositionFrame.create(context);
		newFrame.initialOffsetSet(context, previousFrame.offset());
		return newFrame;
	}

	@Override
	protected void setFrameLength(ChangeStepBuilder change, GroupPositionFrame frame, int length) {
		change.groupPositionFrame(frame).lengthSet(length);
	}

	@Override
	protected void setFrameInitialLength(
			ProjectContext context, GroupPositionFrame frame, int length
	) {
		frame.initialLengthSet(context, length);
	}

	@Override
	protected int getFrameLength(GroupPositionFrame frame) {
		return frame.length();
	}

	@Override
	protected GroupLayer getNode() {
		return layer;
	}

	@Override
	public FrameFinder<GroupLayer, GroupPositionFrame> getFrameFinder() {
		return GroupLayerWrapper.positionFrameFinder;
	}

	@Override
	protected GroupPositionFrame innerDuplicateFrame(
			ProjectContext context, GroupPositionFrame source
	) {
		GroupPositionFrame out = GroupPositionFrame.create(context);
		out.initialOffsetSet(context,source.offset());
		return out;
	}

	@Override
	protected void frameClear(
			ChangeStepBuilder change, GroupPositionFrame f
	) {
		change.groupPositionFrame(f).offsetSet(Vector.ZERO);
	}

	@Override
	protected int frameCount() {
		return layer.positionFramesLength();
	}

	@Override
	protected void removeFrame(ChangeStepBuilder change, int at, int count) {
		change.groupLayer(layer).positionFramesRemove(at, count);
	}

	@Override
	protected void moveFramesTo(ChangeStepBuilder change, int source, int count, int dest) {
		change.groupLayer(layer).positionFramesMoveTo(source, count, dest);
	}
}
