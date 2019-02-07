package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.pyxyzygy.app.config.NodeConfig;
import com.zarbosoft.pyxyzygy.core.model.ProjectNode;
import com.zarbosoft.pyxyzygy.core.model.ProjectObject;
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

	public abstract ProjectObject getValue();

	public abstract NodeConfig getConfig();

	public abstract CanvasHandle buildCanvas(ProjectContext context, CanvasHandle parent);

	public abstract EditHandle buildEditControls(ProjectContext context, TabPane leftTabs);

	public abstract void remove(ProjectContext context);

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

	public abstract void removeChild(ProjectContext context, int index);

	public static enum TakesChildren {
		NONE,
		ANY
	}

	// TODO take this info to prevent calling addChildren if it wouldn't succeed, simplify that signature
	public abstract TakesChildren takesChildren();
}
