package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.FrameMapEntry;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import javafx.beans.binding.Bindings;
import javafx.scene.Group;
import javafx.scene.effect.BlendMode;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.zarbosoft.rendaw.common.Common.sublist;

public class RowFramesWidget extends Pane {
	final Timeline timeline;
	Group inner = new Group();
	final RowAdapter adapter;
	Rectangle frameMarker = new Rectangle(Timeline.baseSize, Timeline.baseSize);
	List<FrameWidget> frames = new ArrayList<>();

	public RowFramesWidget(
			Window window, Timeline timeline, RowAdapter adapter
	) {
		this.timeline = timeline;
		this.adapter = adapter;
		inner.layoutXProperty().bind(Bindings.createDoubleBinding(
				() -> {
					double corner = timeline.controlAlignment.localToScene(0, 0).getX();
					return corner - localToScene(0, 0).getX() - timeline.timeScroll.getValue() + Timeline.baseSize * 2;
				},
				localToSceneTransformProperty(),
				timeline.controlAlignment.localToSceneTransformProperty(),
				timeline.timeScroll.valueProperty()
		));
		setMinHeight(Timeline.baseSize);
		setPrefHeight(getMinHeight());
		setMaxHeight(getMinHeight());
		frameMarker.setFill(Timeline.frameMarkerColor);
		frameMarker.setBlendMode(BlendMode.MULTIPLY);
		inner.getChildren().add(frameMarker);
		getChildren().addAll(inner);
		updateFrameMarker(window);
	}

	/**
	 * @param context
	 * @param window
	 * @return max frame encountered
	 */
	public int updateTime(
			ProjectContext context, Window window, List<RowAdapterFrame> frameAdapters
	) {
		FrameWidget foundSelectedFrame = null;
		Object selectedId = Optional
				.ofNullable(timeline.selectedFrame.get())
				.filter(f -> f.row == this)
				.map(f -> f.frame.id())
				.orElse(null);

		int frameIndex = 0;
		int outerAt = 0;
		for (FrameMapEntry outer : window.timeMap) {
			if (outer.innerOffset != -1) {
				int previousInnerAt = 0;
				int innerAt = 0;
				for (RowAdapterFrame inner : frameAdapters) {
					int innerLeft = Math.max(previousInnerAt + 1, outer.innerOffset);
					int offset = innerAt - outer.innerOffset;
					if (outer.length != -1 && offset >= outer.length)
						break;
					FrameWidget frame;
					int useFrameIndex = frameIndex++;
					if (frames.size() <= useFrameIndex) {
						frames.add(frame = new FrameWidget(context, this));
						this.inner.getChildren().add(frame);
					} else {
						frame = frames.get(useFrameIndex);
					}
					frame.set(
							timeline.zoom,
							useFrameIndex,
							inner,
							outerAt + innerLeft - outer.innerOffset,
							outer.length == -1 ? -1 : outerAt + outer.length,
							innerLeft - previousInnerAt,
							innerAt - innerLeft
					);
					if (inner.id() == selectedId && foundSelectedFrame == null) {
						foundSelectedFrame = frame;
					}
					previousInnerAt = innerAt;
					innerAt += inner.length();
				}
			}
			if (outer.length != -1)
				outerAt += outer.length;
		}

		if (selectedId != null) {
			timeline.select(foundSelectedFrame);
		}

		if (frameIndex < frames.size()) {
			List<FrameWidget> remove = sublist(frames, frameIndex);
			this.inner.getChildren().removeAll(remove);
			remove.clear();
		}

		return outerAt;
	}

	public void updateFrameMarker(Window window) {
		if (window.getSelectedForView() == null)
			return;
		frameMarker.setLayoutX(timeline.frame.getValue() * timeline.zoom);
	}
}
