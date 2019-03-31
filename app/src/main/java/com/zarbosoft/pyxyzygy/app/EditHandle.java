package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;

import java.util.Optional;

public abstract class EditHandle {
	public abstract void cursorMoved(
			ProjectContext context, Window window, DoubleVector vector
	);

	public abstract Wrapper getWrapper();

	public abstract void remove(ProjectContext context, Window window);

	public abstract void mark(
			ProjectContext context, Window window, DoubleVector start, DoubleVector end
	);

	public abstract void markStart(ProjectContext context, Window window, DoubleVector start);

	public abstract CanvasHandle getCanvas();
}
