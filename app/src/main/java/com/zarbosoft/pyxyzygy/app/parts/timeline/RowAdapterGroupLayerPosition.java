package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.ProjectContext;
import com.zarbosoft.pyxyzygy.app.WidgetHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupLayerWrapper;
import com.zarbosoft.pyxyzygy.core.model.GroupLayer;
import com.zarbosoft.pyxyzygy.core.model.GroupPositionFrame;
import com.zarbosoft.pyxyzygy.seed.model.Vector;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.zarbosoft.pyxyzygy.app.Global.NO_INNER;
import static com.zarbosoft.pyxyzygy.app.parts.timeline.Timeline.emptyStateImage;
import static com.zarbosoft.rendaw.common.Common.last;

public class RowAdapterGroupLayerPosition extends RowAdapter {
	private final Timeline timeline;
	private final GroupLayer layer;
	private final RowAdapterGroupLayer layerRowAdapter;
	Optional<RowFramesWidget> row = Optional.empty();

	public RowAdapterGroupLayerPosition(
			Timeline timeline, GroupLayer layer, RowAdapterGroupLayer layerRowAdapter
	) {
		this.timeline = timeline;
		this.layer = layer;
		this.layerRowAdapter = layerRowAdapter;
	}

	@Override
	public int updateTime(ProjectContext context, Window window) {
		return row.map(r -> {
			List<RowAdapterFrame> adapterFrames = new ArrayList<>();
			for (int i0 = 0; i0 < layer.positionFramesLength(); ++i0) {
				final int i = i0;
				GroupPositionFrame f = layer.positionFramesGet(i);
				adapterFrames.add(new RowAdapterFrame() {
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
						context.history.change(c -> c.groupPositionFrame(f).lengthSet(length));
					}

					@Override
					public void remove(ProjectContext context) {
						if (i == 0)
							return;
						context.history.change(c -> c.groupLayer(layer).positionFramesRemove(i, 1));
						if (i == layer.positionFramesLength())
							context.history.change(c -> c
									.groupPositionFrame(last(layer.positionFrames()))
									.lengthSet(-1));
					}

					@Override
					public void clear(ProjectContext context) {
						context.history.change(c -> c.groupPositionFrame(f).offsetSet(new Vector(0, 0)));
					}

					@Override
					public void moveLeft(ProjectContext context) {
						if (i == 0)
							return;
						GroupPositionFrame frameBefore = layer.positionFramesGet(i - 1);
						context.history.change(c -> c.groupLayer(layer).positionFramesMoveTo(i, 1, i - 1));
						final int lengthThis = f.length();
						if (lengthThis == -1) {
							final int lengthBefore = frameBefore.length();
							context.history.change(c -> c.groupPositionFrame(f).lengthSet(lengthBefore));
							context.history.change(c -> c.groupPositionFrame(frameBefore).lengthSet(lengthThis));
						}
					}

					@Override
					public void moveRight(ProjectContext context) {
						if (i == layer.positionFramesLength() - 1)
							return;
						GroupPositionFrame frameAfter = layer.positionFramesGet(i + 1);
						context.history.change(c -> c.groupLayer(layer).positionFramesMoveTo(i, 1, i + 1));
						final int lengthAfter = frameAfter.length();
						if (lengthAfter == -1) {
							final int lengthThis = f.length();
							context.history.change(c -> c.groupPositionFrame(f).lengthSet(lengthAfter));
							context.history.change(c -> c.groupPositionFrame(frameAfter).lengthSet(lengthThis));
						}
					}
				});
			}
			return r.updateTime(context, window, adapterFrames);
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
			Runnable framesCleanup;
			private List<Runnable> frameCleanup = new ArrayList<>();

			{
				framesCleanup = layer.mirrorPositionFrames(frameCleanup, f -> {
					GroupPositionFrame.LengthSetListener lengthListener = f.addLengthSetListeners((target, value) -> {
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
				row = Optional.of(new RowFramesWidget(timeline));
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
	public boolean hasFrames() {
		return true;
	}

	@Override
	public boolean createFrame(ProjectContext context, Window window, int outer) {
		return createFrame(context, window,outer, previous -> GroupPositionFrame.create(context));
	}

	@Override
	public boolean duplicateFrame(ProjectContext context, Window window, int outer) {
		return createFrame(context, window,outer, previous -> {
			GroupPositionFrame created = GroupPositionFrame.create(context);
			created.initialOffsetSet(context, previous.offset());
			return created;
		});
	}

	public boolean createFrame(
			ProjectContext context, Window window,int outer, Function<GroupPositionFrame, GroupPositionFrame> cb
	) {
		int inner = window.timeToInner(outer);
		if (inner == NO_INNER) return false;
		GroupLayerWrapper.PositionResult previous = GroupLayerWrapper.findPosition(layer, inner);
		GroupPositionFrame newFrame = cb.apply(previous.frame);
		int offset = inner - previous.at;
		if (offset <= 0)
			throw new Assertion();
		if (previous.frame.length() == -1) {
			newFrame.initialLengthSet(context, -1);
		} else {
			newFrame.initialLengthSet(context, previous.frame.length() - offset);
		}
		context.history.change(c -> c.groupPositionFrame(previous.frame).lengthSet(offset));
		newFrame.initialOffsetSet(context, previous.frame.offset());
		context.history.change(c -> c.groupLayer(layer).positionFramesAdd(previous.frameIndex + 1, newFrame));
		return true;
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
}
