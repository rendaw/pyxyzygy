package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.Node;

public abstract class CanvasHandle {
	public final SimpleIntegerProperty frameNumber = new SimpleIntegerProperty(0);
	public final SimpleIntegerProperty previousFrame = new SimpleIntegerProperty(-1);
	public final SimpleObjectProperty<DoubleRectangle> bounds =
			new SimpleObjectProperty<>(new DoubleRectangle(0, 0, 0, 0));

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
