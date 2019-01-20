package com.zarbosoft.shoedemo;

import com.zarbosoft.shoedemo.config.NodeConfig;
import com.zarbosoft.shoedemo.model.ProjectNode;
import com.zarbosoft.shoedemo.model.ProjectObject;
import com.zarbosoft.shoedemo.model.Rectangle;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;

import java.util.List;

public abstract class Wrapper {
	public int parentIndex;
	public final SimpleObjectProperty<TreeItem<Wrapper>> tree = new SimpleObjectProperty<>();
	public final SimpleBooleanProperty tagViewing = new SimpleBooleanProperty(false);
	public final SimpleBooleanProperty tagLifted = new SimpleBooleanProperty(false);
	public final SimpleBooleanProperty tagCopied = new SimpleBooleanProperty(false);

	public abstract Wrapper getParent();

	public abstract DoubleVector toInner(DoubleVector vector);

	public abstract ProjectObject getValue();

	public abstract NodeConfig getConfig();

	public abstract void setViewport(ProjectContext context, DoubleRectangle newBounds, int zoom);

	public abstract WidgetHandle buildCanvas(ProjectContext context);

	public abstract void mark(ProjectContext context, DoubleVector start, DoubleVector end);

	public abstract void setFrame(ProjectContext context, int frameNumber);

	public abstract void remove(ProjectContext context);

	/**
	 * Used in ProjectNode wrappers only
	 *
	 * @param context
	 * @param leftTabs
	 * @return
	 */
	public abstract Runnable createProperties(ProjectContext context, TabPane leftTabs);

	public abstract void markStart(ProjectContext context, DoubleVector start);

	/**
	 * Used in ProjectNode wrappers only
	 *
	 * @param context
	 * @param at
	 * @param child
	 * @return List containing placed children
	 */
	public abstract boolean addChildren(ProjectContext context, int at, List<ProjectNode> child);

	public abstract void delete(ProjectContext context);

	public abstract ProjectNode separateClone(ProjectContext context);

	/**
	 * @param gc      for canvas with w/h = crop w/h
	 * @param frame   frame to render
	 * @param crop    viewport of render
	 * @param opacity opacity of this subtree
	 */
	public abstract void render(TrueColorImage output, int frame, Rectangle crop, double opacity);

	public abstract void removeChild(ProjectContext context, int index);

	public abstract WidgetHandle buildCanvasProperties(ProjectContext context);

	public static enum TakesChildren {
		NONE,
		ANY
	}

	// TODO take this info to prevent calling addChildren if it wouldn't succeed, simplify that signature
	public abstract TakesChildren takesChildren();
}
