package com.zarbosoft.shoedemo;

import com.zarbosoft.rendaw.common.Pair;
import com.zarbosoft.shoedemo.model.ProjectNode;
import com.zarbosoft.shoedemo.model.ProjectObject;
import com.zarbosoft.shoedemo.model.Rectangle;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TreeItem;

import java.util.List;

public abstract class Wrapper {
	public int parentIndex;
	public final SimpleObjectProperty<TreeItem<Wrapper>> tree = new SimpleObjectProperty<>();
	public final SimpleIntegerProperty frame = new SimpleIntegerProperty(0);
	public final SimpleBooleanProperty tagViewing = new SimpleBooleanProperty(false);
	public final SimpleBooleanProperty tagLifted = new SimpleBooleanProperty(false);
	public final SimpleBooleanProperty tagCopied = new SimpleBooleanProperty(false);

	public abstract Wrapper getParent();

	public abstract DoubleVector toInner(DoubleVector vector);

	public abstract ProjectObject getValue();

	public abstract void scroll(ProjectContext context, DoubleRectangle oldBounds, DoubleRectangle newBounds);

	public abstract WidgetHandle buildCanvas(ProjectContext context, DoubleRectangle bounds);

	public abstract void mark(ProjectContext context, DoubleVector start, DoubleVector end);

	public abstract void setFrame(ProjectContext context, int frameNumber);

	public abstract void remove(ProjectContext context);

	/**
	 * Used in ProjectNode wrappers only
	 * @param context
	 * @return
	 */
	public abstract WidgetHandle createProperties(ProjectContext context);

	public abstract void markStart(ProjectContext context, DoubleVector start);

	/**
	 * Used in ProjectNode wrappers only
	 * @param context
	 * @param at
	 * @param child
	 * @return List containing placed children
	 */
	public abstract boolean addChildren(ProjectContext context, int at, List<ProjectNode> child);

	public abstract void delete(ProjectContext context);

	public abstract ProjectNode separateClone(ProjectContext context);

	public abstract void render(GraphicsContext gc, int frame, Rectangle crop);

	public abstract void removeChild(ProjectContext context, int index);

	public static enum TakesChildren {
		NONE,
		ONE,
		ANY
	}

	// TODO take this info to prevent calling addChildren if it wouldn't succeed, simplify that signature
	public abstract TakesChildren takesChildren();
}
