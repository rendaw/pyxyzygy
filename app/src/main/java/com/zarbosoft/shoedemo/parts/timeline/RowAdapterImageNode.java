package com.zarbosoft.shoedemo.parts.timeline;

import com.zarbosoft.shoedemo.ProjectContext;
import com.zarbosoft.shoedemo.WidgetHandle;
import com.zarbosoft.shoedemo.model.ImageFrame;
import com.zarbosoft.shoedemo.model.ImageNode;
import com.zarbosoft.shoedemo.structuretree.ImageNodeWrapper;
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

class RowAdapterImageNode extends RowAdapter {
	private final Timeline timeline;
	private final ImageNode node;
	Optional<RowFramesWidget> row = Optional.empty();

	public RowAdapterImageNode(Timeline timeline, ImageNode node) {
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
	public int updateTime(ProjectContext context) {
		return row.map(r -> {
			List<RowAdapterFrame> frameAdapters = new ArrayList<>();
			for (int i0 = 0; i0 < node.framesLength(); ++i0) {
				final int i = i0;
				ImageFrame f = node.framesGet(i);
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
						context.history.change(c -> c.imageFrame(f).lengthSet(length));
					}

					@Override
					public void remove(ProjectContext context) {
						if (i == 0)
							return;
						context.history.change(c -> c.imageNode(node).framesRemove(i, 1));
						if (i == node.framesLength())
							context.history.change(c -> c.imageFrame(last(node.frames())).lengthSet(-1));
					}

					@Override
					public void clear(ProjectContext context) {
						context.history.change(c -> c.imageFrame(f).tilesClear());
					}

					@Override
					public void moveLeft(ProjectContext context) {
						if (i == 0)
							return;
						ImageFrame frameBefore = node.framesGet(i - 1);
						context.history.change(c -> c.imageNode(node).framesMoveTo(i, 1, i - 1));
						final int lengthThis = f.length();
						if (lengthThis == -1) {
							final int lengthBefore = frameBefore.length();
							context.history.change(c -> c.imageFrame(f).lengthSet(lengthBefore));
							context.history.change(c -> c.imageFrame(frameBefore).lengthSet(lengthThis));
						}
					}

					@Override
					public void moveRight(ProjectContext context) {
						if (i == node.framesLength() - 1)
							return;
						ImageFrame frameAfter = node.framesGet(i + 1);
						context.history.change(c -> c.imageNode(node).framesMoveTo(i, 1, i + 1));
						final int lengthAfter = frameAfter.length();
						if (lengthAfter == -1) {
							final int lengthThis = f.length();
							context.history.change(c -> c.imageFrame(f).lengthSet(lengthAfter));
							context.history.change(c -> c.imageFrame(frameAfter).lengthSet(lengthThis));
						}
					}
				});
			}
			return r.updateTime(context, frameAdapters);
		}).orElse(0);
	}

	@Override
	public void updateFrameMarker(ProjectContext context) {
		row.ifPresent(r -> r.updateFrameMarker(context));
	}

	@Override
	public WidgetHandle createRowWidget(ProjectContext context) {
		return new WidgetHandle() {
			private VBox layout;
			private final Runnable framesCleanup;
			private final List<Runnable> frameCleanup = new ArrayList<>();

			{
				layout = new VBox();
				row = Optional.of(new RowFramesWidget(timeline));
				layout.getChildren().add(row.get());

				framesCleanup = node.mirrorFrames(frameCleanup, f -> {
					ImageFrame.LengthSetListener lengthListener = f.addLengthSetListeners((target, value) -> {
						updateTime(context);
					});
					return () -> {
						f.removeLengthSetListeners(lengthListener);
					};
				}, c -> c.run(), () -> {
					updateTime(context);
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
	public boolean createFrame(ProjectContext context, int outer) {
		return insertNewFrame(context, outer, previous -> {
			return ImageFrame.create(context);
		});
	}

	@Override
	public boolean duplciateFrame(ProjectContext context, int outer) {
		return insertNewFrame(context, outer, previous -> {
			ImageFrame created = ImageFrame.create(context);
			created.initialOffsetSet(context, previous.offset());
			created.initialTilesPutAll(context, previous.tiles());
			return created;
		});
	}

	private boolean insertNewFrame(ProjectContext context, int outer, Function<ImageFrame, ImageFrame> cb) {
		final int inner = context.timeToInner(outer);
		if (inner == NO_INNER) return false;
		ImageNodeWrapper.FrameResult previous = ImageNodeWrapper.findFrame(node, inner);
		ImageFrame newFrame = cb.apply(previous.frame);
		int offset = inner - previous.at;
		if (previous.frame.length() == -1) {
			context.history.change(c -> c.imageFrame(previous.frame).lengthSet(offset));
			newFrame.initialLengthSet(context, -1);
		} else {
			newFrame.initialLengthSet(context, previous.frame.length() - offset);
			context.history.change(c -> c.imageFrame(previous.frame).lengthSet(offset));
		}
		context.history.change(c -> c.imageNode(node).framesAdd(previous.frameIndex + 1, newFrame));
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
