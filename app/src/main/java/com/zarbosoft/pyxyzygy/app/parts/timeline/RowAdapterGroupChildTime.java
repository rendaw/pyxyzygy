package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.WidgetHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.wrappers.FrameFinder;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupChildWrapper;
import com.zarbosoft.pyxyzygy.core.model.v0.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.v0.GroupChild;
import com.zarbosoft.pyxyzygy.core.model.v0.GroupTimeFrame;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.ObservableValueBase;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.zarbosoft.pyxyzygy.app.Global.NO_LOOP;

public class RowAdapterGroupChildTime extends BaseFrameRowAdapter<GroupChild, GroupTimeFrame> {
	// TODO setup listeners for inner subtree to keep track of the max inner frame (?)
	final GroupChild child;
	final RowAdapterGroupChild childRowAdapter;
	Optional<RowTimeMapRangeWidget> rowInnerRange = Optional.empty();

	public RowAdapterGroupChildTime(
			Timeline timeline, GroupChild child, RowAdapterGroupChild childRowAdapter
	) {
		super(timeline);
		this.child = child;
		this.childRowAdapter = childRowAdapter;
	}

	@Override
	public ObservableValue<String> getName() {
		return new ObservableValueBase<String>() {
			@Override
			public String getValue() {
				return "Time";
			}
		};
	}

	@Override
	protected GroupTimeFrame innerCreateFrame(
			ProjectContext context, GroupTimeFrame previousFrame
	) {
		GroupTimeFrame created = GroupTimeFrame.create(context);
		created.initialInnerOffsetSet(context, 0);
		created.initialInnerLoopSet(context, 0);
		return created;
	}

	@Override
	protected void addFrame(ChangeStepBuilder change, int at, GroupTimeFrame frame) {
		change.groupChild(child).timeFramesAdd(at, frame);
	}

	@Override
	protected void setFrameLength(ChangeStepBuilder change, GroupTimeFrame frame, int length) {
		change.groupTimeFrame(frame).lengthSet(length);
	}

	@Override
	protected void setFrameInitialLength(ProjectContext context, GroupTimeFrame frame, int length) {
		frame.initialLengthSet(context, length);
	}

	@Override
	protected int getFrameLength(GroupTimeFrame frame) {
		return frame.length();
	}

	@Override
	public FrameFinder<GroupChild, GroupTimeFrame> getFrameFinder() {
		return GroupChildWrapper.timeFrameFinder;
	}

	@Override
	protected GroupTimeFrame innerDuplicateFrame(
			ProjectContext context, GroupTimeFrame source
	) {
		GroupTimeFrame created = GroupTimeFrame.create(context);
		created.initialInnerOffsetSet(context, source.innerOffset());
		created.initialInnerLoopSet(context, source.innerLoop());
		return created;
	}

	@Override
	protected GroupChild getNode() {
		return child;
	}

	@Override
	public ObservableObjectValue<Image> getStateImage() {
		return Timeline.emptyStateImage;
	}

	@Override
	public void deselected() {
		childRowAdapter.treeDeselected();
	}

	@Override
	public void selected() {
		childRowAdapter.treeSelected();
	}

	@Override
	protected void frameClear(ChangeStepBuilder change, GroupTimeFrame groupTimeFrame) {
		change.groupTimeFrame(groupTimeFrame).innerOffsetSet(0);
		change.groupTimeFrame(groupTimeFrame).innerLoopSet(NO_LOOP);
	}

	@Override
	protected int frameCount() {
		return child.timeFramesLength();
	}

	@Override
	protected void removeFrame(ChangeStepBuilder change, int at, int count) {
		change.groupChild(child).timeFramesRemove(at, count);
	}

	@Override
	protected void moveFramesTo(ChangeStepBuilder change, int source, int count, int dest) {
		change.groupChild(child).timeFramesMoveTo(source, count, dest);
	}

	@Override
	public WidgetHandle createRowWidget(ProjectContext context, Window window) {
		return new WidgetHandle() {
			final VBox layout = new VBox();

			Runnable framesCleanup;
			List<Runnable> frameCleanup = new ArrayList<>();
			private ChangeListener<FrameWidget> selectedFrameListener;
			Runnable selectedFrameCleanup;

			{
				framesCleanup = child.mirrorTimeFrames(frameCleanup, f -> {
					Listener.ScalarSet<GroupTimeFrame, Integer> lengthListener =
							f.addLengthSetListeners((target, value) -> {
								updateTime(context, window);
							});
					return () -> {
						f.removeLengthSetListeners(lengthListener);
					};
				}, c -> c.run(), at -> {
					updateTime(context, window);
				});
				selectedFrameListener = (observable, oldValue, newValue) -> {
					if (selectedFrameCleanup != null) {
						selectedFrameCleanup.run();
						selectedFrameCleanup = null;
					}
					if (newValue == null ||
							!(newValue.frame instanceof BaseFrameRowAdapter.AdapterFrame) ||
							newValue.row.adapter != RowAdapterGroupChildTime.this) {
						rowInnerRange.ifPresent(w -> {
							layout.getChildren().remove(w.base);
							rowInnerRange = Optional.empty();
						});
					} else {
						if (!rowInnerRange.isPresent()) {
							rowInnerRange = Optional.of(new RowTimeMapRangeWidget(context, timeline));
							layout.getChildren().add(rowInnerRange.get().base);
						}
						GroupTimeFrame frame = ((AdapterFrame) newValue.frame).f;
						Runnable update = () -> {
							rowInnerRange.get().set(new TimeRangeAdapter() {
								@Override
								public int getOuterAt() {
									return newValue.at;
								}

								@Override
								public int getInnerStart() {
									return frame.innerOffset();
								}

								@Override
								public int getInnerLength() {
									return frame.innerLoop();
								}

								@Override
								public void changeStart(
										ChangeStepBuilder change, int value
								) {
									change.groupTimeFrame(frame).innerOffsetSet(value);
								}

								@Override
								public void changeLength(
										ChangeStepBuilder change, int value
								) {
									change.groupTimeFrame(frame).innerLoopSet(value);
								}

								@Override
								public Object getData() {
									return child;
								}
							});
						};
						Listener.ScalarSet<GroupTimeFrame, Integer> offsetListener =
								frame.addInnerOffsetSetListeners((target, value) -> update.run());
						Listener.ScalarSet<GroupTimeFrame, Integer> loopListener =
								frame.addInnerLoopSetListeners((target, value) -> update.run());
						selectedFrameCleanup = () -> {
							frame.removeInnerOffsetSetListeners(offsetListener);
							frame.removeInnerLoopSetListeners(loopListener);
						};
					}
				};
				timeline.selectedFrame.addListener(selectedFrameListener);
				selectedFrameListener.changed(null, null, timeline.selectedFrame.getValue());
				row = Optional.of(new RowFramesWidget(window, timeline, RowAdapterGroupChildTime.this));
				layout.getChildren().addAll(row.get());
			}

			@Override
			public Node getWidget() {
				return layout;
			}

			@Override
			public void remove() {
				framesCleanup.run();
				frameCleanup.forEach(c -> c.run());
				timeline.selectedFrame.removeListener(selectedFrameListener);
				if (selectedFrameCleanup != null)
					selectedFrameCleanup.run();
			}
		};
	}

	@Override
	public void remove(ProjectContext context) {
	}

	@Override
	public void updateFrameMarker(ProjectContext context, Window window) {
		super.updateFrameMarker(context, window);
		if (rowInnerRange.isPresent())
			rowInnerRange.get().updateFrameMarker(window);
	}
}
