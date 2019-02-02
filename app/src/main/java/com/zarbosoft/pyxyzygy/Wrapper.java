package com.zarbosoft.pyxyzygy;

import com.zarbosoft.pyxyzygy.config.NodeConfig;
import com.zarbosoft.pyxyzygy.model.ProjectNode;
import com.zarbosoft.pyxyzygy.model.ProjectObject;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.Node;
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

	public abstract static class CanvasHandle {
		public abstract DoubleVector toInner(DoubleVector vector);
		private final Group outer = new Group();
		public final Group inner = new Group();
		public final Group overlay = new Group();

		{
			outer.getChildren().addAll(inner, overlay);
		}

		final public Node getWidget() {
			return outer;
		}
		public abstract void setViewport(ProjectContext context, DoubleRectangle newBounds, int positiveZoom);
		public abstract void setFrame(ProjectContext context, int frameNumber);
		public abstract void remove(ProjectContext context);

		public abstract Wrapper getWrapper();

		public abstract CanvasHandle getParent();
	}

	public abstract CanvasHandle buildCanvas(ProjectContext context, CanvasHandle parent);

	public abstract static class EditHandle {
		public abstract void cursorMoved(ProjectContext context, DoubleVector vector);
		public abstract Wrapper getWrapper();
		public abstract Node getProperties();
		public abstract void remove(ProjectContext context);
		public abstract void mark(ProjectContext context, DoubleVector start, DoubleVector end);
		public abstract void markStart(ProjectContext context, DoubleVector start);
	}

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
