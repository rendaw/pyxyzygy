package com.zarbosoft.pyxyzygy.gui;

import javafx.scene.Node;

import java.util.Optional;

public abstract class EditHandle {
	public abstract void cursorMoved(ProjectContext context, DoubleVector vector);

	public abstract Wrapper getWrapper();

	public abstract Node getProperties();

	public abstract void remove(ProjectContext context);

	public abstract void mark(
			ProjectContext context, Window window, DoubleVector start, DoubleVector end
	);

	public abstract void markStart(ProjectContext context, Window window, DoubleVector start);

	public abstract CanvasHandle getCanvas();

	public abstract Optional<Integer> previousFrame(int frame);
}
