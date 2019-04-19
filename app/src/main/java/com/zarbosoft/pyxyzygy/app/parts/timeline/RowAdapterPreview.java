package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.CustomBinding;
import com.zarbosoft.pyxyzygy.app.WidgetHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.Wrapper;
import com.zarbosoft.pyxyzygy.app.config.NodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.core.model.v0.ChangeStepBuilder;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.image.Image;

public class RowAdapterPreview extends RowAdapter {
	private final NodeConfig config;
	private final Wrapper data;
	RowTimeRangeWidget widget = null;
	private final Timeline timeline;

	public RowAdapterPreview(Timeline timeline, Wrapper edit) {
		this.timeline = timeline;
		this.config = edit.getConfig();
		this.data = edit;
	}

	@Override
	public ObservableValue<String> getName() {
		return new SimpleStringProperty("Preview");
	}

	@Override
	public boolean hasFrames() {
		return true;
	}

	@Override
	public boolean createFrame(
			ProjectContext context, Window window, ChangeStepBuilder change, int outer
	) {
		return false;
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

	@Override
	public boolean duplicateFrame(
			ProjectContext context, Window window, ChangeStepBuilder change, int outer
	) {
		return false;
	}

	@Override
	public WidgetHandle createRowWidget(
			ProjectContext context, Window window
	) {
		return new WidgetHandle() {
			private final Runnable startCleanup;
			private final Runnable lengthCleanup;

			{
				widget = new RowTimeRangeWidget(timeline);
				startCleanup = CustomBinding.bindBidirectional(
						new CustomBinding.PropertyBinder<>(config.previewStart),
						new CustomBinding.PropertyBinder<>(widget.start.asObject())
				);
				lengthCleanup = CustomBinding.bindBidirectional(
						new CustomBinding.PropertyBinder<>(config.previewLength),
						new CustomBinding.PropertyBinder<>(widget.length.asObject())
				);
			}

			@Override
			public Node getWidget() {
				return widget.base;
			}

			@Override
			public void remove() {
				startCleanup.run();
				lengthCleanup.run();
			}
		};
	}

	@Override
	public int updateTime(ProjectContext context, Window window) {
		return 0;
	}

	@Override
	public void updateFrameMarker(ProjectContext context, Window window) {
		if (widget != null)
			widget.updateFrameMarker(window);
	}

	@Override
	public void remove(ProjectContext context) {

	}

	@Override
	public boolean frameAt(Window window, int outer) {
		return false;
	}

	@Override
	public Object getData() {
		return data;
	}
}
