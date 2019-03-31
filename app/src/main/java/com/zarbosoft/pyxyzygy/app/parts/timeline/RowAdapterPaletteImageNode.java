package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.WidgetHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.wrappers.FrameFinder;
import com.zarbosoft.pyxyzygy.app.wrappers.paletteimage.PaletteImageNodeWrapper;
import com.zarbosoft.pyxyzygy.core.model.v0.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.v0.PaletteImageFrame;
import com.zarbosoft.pyxyzygy.core.model.v0.PaletteImageNode;
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
import java.util.function.Function;

import static com.zarbosoft.pyxyzygy.app.Global.NO_INNER;
import static com.zarbosoft.rendaw.common.Common.last;

class RowAdapterPaletteImageNode extends RowAdapter {
	private final Timeline timeline;
	private final PaletteImageNode node;
	Optional<RowFramesWidget> row = Optional.empty();

	public RowAdapterPaletteImageNode(Timeline timeline, PaletteImageNode node) {
		this.timeline = timeline;
		this.node = node;
	}

	@Override
	public void remove(ProjectContext context) {
	}

	@Override
	public boolean frameAt(Window window, int outer) {
		final int inner = window.timeToInner(outer);
		if (inner == NO_INNER)
			return false;
		FrameFinder.Result<PaletteImageFrame> previous = PaletteImageNodeWrapper.frameFinder.findFrame(node, inner);
		return previous.at == inner;
	}

	@Override
	public Object getData() {
		return node;
	}

	@Override
	public ObservableValue<String> getName() {
		return new SimpleStringProperty("Frames");
	}

	@Override
	public int updateTime(ProjectContext context, Window window) {
		return row.map(r -> {
			List<RowAdapterFrame> frameAdapters = new ArrayList<>();
			for (int i0 = 0; i0 < node.framesLength(); ++i0) {
				final int i = i0;
				PaletteImageFrame f = node.framesGet(i);
				frameAdapters.add(new RowAdapterFrame() {
					@Override
					public Object id() {
						return f;
					}

					@Override
					public int length() {
						return f.length();
					}

					@Override
					public void setLength(
							ProjectContext context, ChangeStepBuilder change, int length
					) {
						change.paletteImageFrame(f).lengthSet(length);
					}

					@Override
					public void remove(
							ProjectContext context, ChangeStepBuilder change
					) {
						change.paletteImageNode(node).framesRemove(i, 1);
						if (i == node.framesLength())
							change.paletteImageFrame(last(node.frames())).lengthSet(-1);
					}

					@Override
					public void clear(
							ProjectContext context, ChangeStepBuilder change
					) {
						change.paletteImageFrame(f).tilesClear();
					}

					@Override
					public void moveLeft(
							ProjectContext context, ChangeStepBuilder change
					) {
						if (i == 0)
							return;
						PaletteImageFrame frameBefore = node.framesGet(i - 1);
						change.paletteImageNode(node).framesMoveTo(i, 1, i - 1);
						final int lengthThis = f.length();
						if (lengthThis == -1) {
							final int lengthBefore = frameBefore.length();
							change.paletteImageFrame(f).lengthSet(lengthBefore);
							change.paletteImageFrame(frameBefore).lengthSet(lengthThis);
						}
						timeline.select(row.get().frames.get(i - 1));
					}

					@Override
					public void moveRight(
							ProjectContext context, ChangeStepBuilder change
					) {
						if (i == node.framesLength() - 1)
							return;
						PaletteImageFrame frameAfter = node.framesGet(i + 1);
						change.paletteImageNode(node).framesMoveTo(i, 1, i + 1);
						final int lengthAfter = frameAfter.length();
						if (lengthAfter == -1) {
							final int lengthThis = f.length();
							change.paletteImageFrame(f).lengthSet(lengthAfter);
							change.paletteImageFrame(frameAfter).lengthSet(lengthThis);
						}
						timeline.select(row.get().frames.get(i + 1));
					}
				});
			}
			return r.updateTime(context, window, frameAdapters);
		}).orElse(0);
	}

	@Override
	public void updateFrameMarker(ProjectContext context, Window window) {
		row.ifPresent(r -> r.updateFrameMarker(window));
	}

	@Override
	public WidgetHandle createRowWidget(ProjectContext context, Window window) {
		return new WidgetHandle() {
			private VBox layout;
			private final Runnable framesCleanup;
			private final List<Runnable> frameCleanup = new ArrayList<>();

			{
				layout = new VBox();
				row = Optional.of(new RowFramesWidget(window, timeline));
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
	public boolean hasFrames() {
		return true;
	}

	@Override
	public boolean createFrame(
			ProjectContext context, Window window, ChangeStepBuilder change, int outer
	) {
		return insertNewFrame(context, window, change,outer, previous -> {
			PaletteImageFrame out = PaletteImageFrame.create(context);
			out.initialOffsetSet(context, new Vector(0, 0));
			return out;
		});
	}

	@Override
	public boolean duplicateFrame(
			ProjectContext context, Window window, ChangeStepBuilder change, int outer
	) {
		return insertNewFrame(context, window, change, outer, previous -> {
			PaletteImageFrame created = PaletteImageFrame.create(context);
			created.initialOffsetSet(context, previous.offset());
			created.initialTilesPutAll(context, previous.tiles());
			return created;
		});
	}

	private boolean insertNewFrame(
			ProjectContext context,
			Window window, ChangeStepBuilder change, int outer,
			Function<PaletteImageFrame, PaletteImageFrame> cb
	) {
		final int inner = window.timeToInner(outer);
		if (inner == NO_INNER)
			return false;
		FrameFinder.Result<PaletteImageFrame> previous = PaletteImageNodeWrapper.frameFinder.findFrame(node, inner);
		PaletteImageFrame newFrame = cb.apply(previous.frame);
		int offset = inner - previous.at;
		if (offset == 0) throw new AssertionError();
		if (previous.frame.length() == -1) {
			newFrame.initialLengthSet(context, -1);
		} else {
			newFrame.initialLengthSet(context, previous.frame.length() - offset);
		}
		change.paletteImageNode(node).framesAdd(previous.frameIndex + 1, newFrame);
		change.paletteImageFrame(previous.frame).lengthSet(offset);
		return true;
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
