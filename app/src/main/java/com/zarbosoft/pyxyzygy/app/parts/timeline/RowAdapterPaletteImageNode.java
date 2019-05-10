package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.WidgetHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.wrappers.FrameFinder;
import com.zarbosoft.pyxyzygy.app.wrappers.paletteimage.PaletteImageNodeWrapper;
import com.zarbosoft.pyxyzygy.core.model.v0.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.v0.PaletteImageFrame;
import com.zarbosoft.pyxyzygy.core.model.v0.PaletteImageLayer;
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

class RowAdapterPaletteImageNode extends BaseFrameRowAdapter<PaletteImageLayer, PaletteImageFrame> {
	private final PaletteImageLayer node;

	public RowAdapterPaletteImageNode(Timeline timeline, PaletteImageLayer node) {
		super(timeline);
		this.node = node;
	}

	@Override
	protected void frameClear(
			ChangeStepBuilder change, PaletteImageFrame paletteImageFrame
	) {
		change.paletteImageFrame(paletteImageFrame).tilesClear();
	}

	@Override
	protected int frameCount() {
		return node.framesLength();
	}

	@Override
	protected void removeFrame(ChangeStepBuilder change, int at, int count) {
		change.paletteImageLayer(node).framesRemove(at, count);
	}

	@Override
	protected void moveFramesTo(ChangeStepBuilder change, int source, int count, int dest) {
		change.paletteImageLayer(node).framesMoveTo(source, count, dest);
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
				row = Optional.of(new RowFramesWidget(window, timeline, RowAdapterPaletteImageNode.this));
				layout.getChildren().add(row.get());

				framesCleanup = node.mirrorFrames(frameCleanup, f -> {
					Listener.ScalarSet<PaletteImageFrame, Integer> lengthListener =
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
	protected PaletteImageFrame innerCreateFrame(
			ProjectContext context, PaletteImageFrame previousFrame
	) {
		PaletteImageFrame out = PaletteImageFrame.create(context);
		out.initialOffsetSet(context, Vector.ZERO);
		return out;
	}

	@Override
	protected void addFrame(ChangeStepBuilder change, int at, PaletteImageFrame frame) {
		change.paletteImageLayer(node).framesAdd(at, frame);
	}

	@Override
	protected void setFrameLength(ChangeStepBuilder change, PaletteImageFrame frame, int length) {
		change.paletteImageFrame(frame).lengthSet(length);
	}

	@Override
	protected void setFrameInitialLength(
			ProjectContext context, PaletteImageFrame frame, int length
	) {
		frame.initialLengthSet(context, length);
	}

	@Override
	protected int getFrameLength(PaletteImageFrame frame) {
		return frame.length();
	}

	@Override
	public FrameFinder<PaletteImageLayer, PaletteImageFrame> getFrameFinder() {
		return PaletteImageNodeWrapper.frameFinder;
	}

	@Override
	protected PaletteImageFrame innerDuplicateFrame(
			ProjectContext context, PaletteImageFrame source
	) {
		PaletteImageFrame created = PaletteImageFrame.create(context);
		created.initialOffsetSet(context, source.offset());
		created.initialTilesPutAll(context, source.tiles());
		return created;
	}

	@Override
	protected PaletteImageLayer getNode() {
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
