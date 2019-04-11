package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.WidgetHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.wrappers.FrameFinder;
import com.zarbosoft.pyxyzygy.app.wrappers.truecolorimage.TrueColorImageNodeWrapper;
import com.zarbosoft.pyxyzygy.core.model.v0.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.v0.TrueColorImageFrame;
import com.zarbosoft.pyxyzygy.core.model.v0.TrueColorImageNode;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

class RowAdapterTrueColorImageNode extends BaseFrameRowAdapter<TrueColorImageNode, TrueColorImageFrame> {
	private final TrueColorImageNode node;

	public RowAdapterTrueColorImageNode(Timeline timeline, TrueColorImageNode node) {
		super(timeline);
		this.node = node;
	}

	@Override
	public void remove(ProjectContext context) {
	}

	@Override
	public ObservableValue<String> getName() {
		return new SimpleStringProperty("Frames");
	}

	@Override
	public WidgetHandle createRowWidget(ProjectContext context, Window window) {
		return new WidgetHandle() {
			private VBox layout;
			private final Runnable framesCleanup;
			private final List<Runnable> frameCleanup = new ArrayList<>();

			{
				layout = new VBox();
				row = Optional.of(new RowFramesWidget(window, timeline, RowAdapterTrueColorImageNode.this));
				layout.getChildren().add(row.get());

				framesCleanup = node.mirrorFrames(frameCleanup, f -> {
					Listener.ScalarSet<TrueColorImageFrame, Integer> lengthListener =
							f.addLengthSetListeners((target, value) -> {
								updateTime(context, window);
							});
					return () -> {
						f.removeLengthSetListeners(lengthListener);
					};
				}, c -> c.run(), at -> {
					updateTime(context, window);
				});
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
	protected TrueColorImageFrame innerCreateFrame(
			ProjectContext context, TrueColorImageFrame previousFrame
	) {
		TrueColorImageFrame out = TrueColorImageFrame.create(context);
		out.initialOffsetSet(context, Vector.ZERO);
		return out;
	}

	@Override
	protected void addFrame(
			ChangeStepBuilder change, int at, TrueColorImageFrame frame
	) {
		change.trueColorImageNode(node).framesAdd(at, frame);
	}

	@Override
	protected void setFrameLength(ChangeStepBuilder change, TrueColorImageFrame frame, int length) {
		change.trueColorImageFrame(frame).lengthSet(length);
	}

	@Override
	protected void setFrameInitialLength(
			ProjectContext context, TrueColorImageFrame frame, int length
	) {
		frame.initialLengthSet(context, length);
	}

	@Override
	protected int getFrameLength(TrueColorImageFrame frame) {
		return frame.length();
	}

	@Override
	protected void frameClear(
			ChangeStepBuilder change, TrueColorImageFrame trueColorImageFrame
	) {
		change.trueColorImageFrame(trueColorImageFrame).tilesClear();
	}

	@Override
	protected int frameCount() {
		return node.framesLength();
	}

	@Override
	protected void removeFrame(ChangeStepBuilder change, int at, int count) {
		change.trueColorImageNode(node).framesRemove(at, count);
	}

	@Override
	protected void moveFramesTo(ChangeStepBuilder change, int source, int count, int dest) {
		change.trueColorImageNode(node).framesMoveTo(source, count, dest);
	}

	@Override
	public FrameFinder<TrueColorImageNode, TrueColorImageFrame> getFrameFinder() {
		return TrueColorImageNodeWrapper.frameFinder;
	}

	@Override
	protected TrueColorImageFrame innerDuplicateFrame(
			ProjectContext context, TrueColorImageFrame source
	) {
		TrueColorImageFrame created = TrueColorImageFrame.create(context);
		created.initialOffsetSet(context, source.offset());
		created.initialTilesPutAll(context, source.tiles());
		return created;
	}

	@Override
	protected TrueColorImageNode getNode() {
		return node;
	}

	@Override
	public ObservableObjectValue<Image> getStateImage() {
		return Timeline.emptyStateImage;
	}

	@Override
	public void deselected() {

	}

	@Override
	public void selected() {

	}
}
