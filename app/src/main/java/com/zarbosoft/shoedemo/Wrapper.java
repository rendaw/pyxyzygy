package com.zarbosoft.shoedemo;

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

	public abstract Node buildCanvas(ProjectContext context, DoubleRectangle bounds);

	public abstract Node getCanvas();

	public abstract void destroyCanvas();

	public abstract void mark(ProjectContext context, DoubleVector start, DoubleVector end);

	public abstract void setFrame(ProjectContext context, int frameNumber);

	public abstract void remove(ProjectContext context);

	/**
	 * Used in ProjectNode wrappers only
	 * @param context
	 * @return
	 */
	public abstract Node createProperties(ProjectContext context);

	/**
	 * Used in ProjectNode wrappers only
	 */
	public abstract void destroyProperties();

	public abstract void markStart(ProjectContext context, DoubleVector start);

	/**
	 * Used in ProjectNode wrappers only
	 * @param context
	 * @param at
	 * @param child
	 * @return
	 */
	public abstract boolean addChildren(ProjectContext context, int at, List<ProjectNode> child);

	public abstract void delete(ProjectContext context);

	public abstract ProjectNode separateClone(ProjectContext context);

	public abstract void render(GraphicsContext gc, int frame, Rectangle crop);

	public abstract void removeChild(ProjectContext context, int index);
}