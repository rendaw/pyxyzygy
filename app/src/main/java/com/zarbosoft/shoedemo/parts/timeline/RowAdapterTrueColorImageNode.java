package com.zarbosoft.shoedemo.parts.timeline;

import com.zarbosoft.shoedemo.ProjectContext;
import com.zarbosoft.shoedemo.WidgetHandle;
import com.zarbosoft.shoedemo.Window;
import com.zarbosoft.shoedemo.model.TrueColorImageFrame;
import com.zarbosoft.shoedemo.model.TrueColorImageNode;
import com.zarbosoft.shoedemo.model.Vector;
import com.zarbosoft.shoedemo.wrappers.truecolorimage.TrueColorImageNodeWrapper;
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

import static com.zarbosoft.rendaw.common.Common.last;
import static com.zarbosoft.shoedemo.Main.NO_INNER;

class RowAdapterTrueColorImageNode extends RowAdapter {
	private final Timeline timeline;
	private final TrueColorImageNode node;
	Optional<RowFramesWidget> row = Optional.empty();

	public RowAdapterTrueColorImageNode(Timeline timeline, TrueColorImageNode node) {
		this.timeline = timeline;
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
	public int updateTime(ProjectContext context, Window window) {
		return row.map(r -> {
			List<RowAdapterFrame> frameAdapters = new ArrayList<>();
			for (int i0 = 0; i0 < node.framesLength(); ++i0) {
				final int i = i0;
				TrueColorImageFrame f = node.framesGet(i);
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
					public void setLength(ProjectContext context, int length) {
						context.history.change(c -> c.trueColorImageFrame(f).lengthSet(length));
					}

					@Override
					public void remove(ProjectContext context) {
						if (i == 0)
							return;
						context.history.change(c -> c.trueColorImageNode(node).framesRemove(i, 1));
						if (i == node.framesLength())
							context.history.change(c -> c.trueColorImageFrame(last(node.frames())).lengthSet(-1));
					}

					@Override
					public void clear(ProjectContext context) {
						context.history.change(c -> c.trueColorImageFrame(f).tilesClear());
					}

					@Override
					public void moveLeft(ProjectContext context) {
						if (i == 0)
							return;
						TrueColorImageFrame frameBefore = node.framesGet(i - 1);
						context.history.change(c -> c.trueColorImageNode(node).framesMoveTo(i, 1, i - 1));
						final int lengthThis = f.length();
						if (lengthThis == -1) {
							final int lengthBefore = frameBefore.length();
							context.history.change(c -> c.trueColorImageFrame(f).lengthSet(lengthBefore));
							context.history.change(c -> c.trueColorImageFrame(frameBefore).lengthSet(lengthThis));
						}
					}

					@Override
					public void moveRight(ProjectContext context) {
						if (i == node.framesLength() - 1)
							return;
						TrueColorImageFrame frameAfter = node.framesGet(i + 1);
						context.history.change(c -> c.trueColorImageNode(node).framesMoveTo(i, 1, i + 1));
						final int lengthAfter = frameAfter.length();
						if (lengthAfter == -1) {
							final int lengthThis = f.length();
							context.history.change(c -> c.trueColorImageFrame(f).lengthSet(lengthAfter));
							context.history.change(c -> c.trueColorImageFrame(frameAfter).lengthSet(lengthThis));
						}
					}
				});
			}
			return r.updateTime(context, window, frameAdapters);
		}).orElse(0);
	}

	@Override
	public void updateFrameMarker(ProjectContext context, Window window) {
		row.ifPresent(r -> r.updateFrameMarker(context, window));
	}

	@Override
	public WidgetHandle createRowWidget(ProjectContext context, Window window) {
		return new WidgetHandle() {
			private VBox layout;
			private final Runnable framesCleanup;
			private final List<Runnable> frameCleanup = new ArrayList<>();

			{
				layout = new VBox();
				row = Optional.of(new RowFramesWidget(timeline));
				layout.getChildren().add(row.get());

				framesCleanup = node.mirrorFrames(frameCleanup, f -> {
					TrueColorImageFrame.LengthSetListener lengthListener = f.addLengthSetListeners((target, value) -> {
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
	public boolean createFrame(ProjectContext context, Window window, int outer) {
		return insertNewFrame(context, window, outer, previous -> {
			TrueColorImageFrame out = TrueColorImageFrame.create(context);
			out.initialOffsetSet(context, new Vector(0, 0));
			return out;
		});
	}

	@Override
	public boolean duplicateFrame(ProjectContext context, Window window, int outer) {
		return insertNewFrame(context, window, outer, previous -> {
			TrueColorImageFrame created = TrueColorImageFrame.create(context);
			created.initialOffsetSet(context, previous.offset());
			created.initialTilesPutAll(context, previous.tiles());
			return created;
		});
	}

	private boolean insertNewFrame(
			ProjectContext context,
			Window window,
			int outer,
			Function<TrueColorImageFrame, TrueColorImageFrame> cb
	) {
		final int inner = window.timeToInner(outer);
		if (inner == NO_INNER)
			return false;
		TrueColorImageNodeWrapper.FrameResult previous = TrueColorImageNodeWrapper.findFrame(node, inner);
		TrueColorImageFrame newFrame = cb.apply(previous.frame);
		int offset = inner - previous.at;
		if (previous.frame.length() == -1) {
			context.history.change(c -> c.trueColorImageFrame(previous.frame).lengthSet(offset));
			newFrame.initialLengthSet(context, -1);
		} else {
			newFrame.initialLengthSet(context, previous.frame.length() - offset);
			context.history.change(c -> c.trueColorImageFrame(previous.frame).lengthSet(offset));
		}
		context.history.change(c -> c.trueColorImageNode(node).framesAdd(previous.frameIndex + 1, newFrame));
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
