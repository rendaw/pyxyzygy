package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.pyxyzygy.app.config.NodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.core.model.v0.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectNode;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectObject;
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
	public void setParentIndex(int index) {
		parentIndex = index;
	}

	public abstract ProjectObject getValue();

	public abstract NodeConfig getConfig();

	public abstract CanvasHandle buildCanvas(
			ProjectContext context, Window window, CanvasHandle parent
	);

	public abstract EditHandle buildEditControls(
			ProjectContext context, Window window
	);

	public abstract void remove(ProjectContext context);

	/**
	 * Used in ProjectNode wrappers only
	 *
	 * @param context
	 * @param change
	 * @param at
	 * @param child
	 * @return List containing placed children
	 */
	public abstract boolean addChildren(
			ProjectContext context, ChangeStepBuilder change, int at, List<ProjectNode> child
	);

	public abstract void delete(ProjectContext context, ChangeStepBuilder change);

	public abstract ProjectNode separateClone(ProjectContext context);

	public abstract void removeChild(
			ProjectContext context, ChangeStepBuilder change, int index
	);

	public static enum TakesChildren {
		NONE,
		ANY
	}

	// TODO take this info to prevent calling addChildren if it wouldn't succeed, simplify that signature
	public abstract TakesChildren takesChildren();
}
