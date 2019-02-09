package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.WidgetHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;

public abstract class RowAdapter {
	public abstract ObservableValue<String> getName();

	public abstract boolean hasFrames();

	public abstract boolean createFrame(ProjectContext context, Window window, int outer);

	public abstract ObservableObjectValue<Image> getStateImage();

	public abstract void deselected();

	public abstract void selected();

	public abstract boolean duplicateFrame(ProjectContext context, Window window, int outer);

	public abstract WidgetHandle createRowWidget(ProjectContext context, Window window);

	/**
	 * @param context
	 * @param window
	 * @return Maximum frame in this row
	 */
	public abstract int updateTime(ProjectContext context, Window window);

	public abstract void updateFrameMarker(ProjectContext context, Window window);

	public abstract void remove(ProjectContext context);
}
