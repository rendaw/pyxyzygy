package com.zarbosoft.shoedemo.parts.timeline;

import com.zarbosoft.shoedemo.ProjectContext;
import com.zarbosoft.shoedemo.WidgetHandle;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;

import java.util.List;

public abstract class RowAdapter {
	public abstract ObservableValue<String> getName();

	public abstract boolean hasFrames();

	public abstract boolean createFrame(ProjectContext context, int outer);

	public abstract ObservableObjectValue<Image> getStateImage();

	public abstract void deselected();

	public abstract void selected();

	public abstract boolean duplciateFrame(ProjectContext context, int outer);

	public abstract WidgetHandle createRowWidget(ProjectContext context);

	/**
	 * @param context
	 * @return Maximum frame in this row
	 */
	public abstract int updateTime(ProjectContext context);

	public abstract void updateFrameMarker(ProjectContext context);

	public abstract void remove(ProjectContext context);
}
