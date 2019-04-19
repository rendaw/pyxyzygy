package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.WidgetHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.core.model.v0.ChangeStepBuilder;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;

public abstract class RowAdapter {
	public abstract ObservableValue<String> getName();

	public abstract boolean hasFrames();

	public abstract boolean hasNormalFrames();

	public abstract boolean createFrame(
			ProjectContext context, Window window, ChangeStepBuilder change, int outer
	);

	public abstract ObservableObjectValue<Image> getStateImage();

	public abstract void deselected();

	public abstract void selected();

	public abstract boolean duplicateFrame(
			ProjectContext context, Window window, ChangeStepBuilder change, int outer
	);

	public abstract WidgetHandle createRowWidget(ProjectContext context, Window window);

	/**
	 * @param context
	 * @param window
	 * @return Maximum frame in this row
	 */
	public abstract int updateTime(ProjectContext context, Window window);

	public abstract void updateFrameMarker(ProjectContext context, Window window);

	public abstract void remove(ProjectContext context);

	public abstract boolean frameAt(Window window, int outer);

	/**
	 * Only called if hasFrames (unique data for describing a change)
	 * @return
	 */
	public abstract Object getData();
}
