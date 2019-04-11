package com.zarbosoft.pyxyzygy.app.wrappers.baseimage;

import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.config.BaseBrush;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.CircleCursor;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.scene.ImageCursor;
import javafx.scene.input.KeyCode;

public abstract class BaseToolBrush<F, L> extends Tool {
	private final BaseBrush brush;
	public final BaseImageNodeWrapper<?, F, ?, L> wrapper;
	private DoubleVector lastEnd;
	private ImageCursor cursor = null;

	public BaseToolBrush(
			Window window, BaseImageNodeWrapper<?, F, ?, L> wrapper, BaseBrush brush
	) {
		this.wrapper = wrapper;
		this.brush = brush;
		brush.size.addListener((observable, oldValue, newValue) -> updateCursor(window));
		window.editor.zoomFactor.addListener((observable, oldValue, newValue) -> updateCursor(window));
		updateCursor(window);
	}

	@Override
	public void markStart(ProjectContext context, Window window, DoubleVector start) {

	}

	@Override
	public void mark(ProjectContext context, Window window, DoubleVector start, DoubleVector end) {
		if (false) {
			throw new Assertion();
		} else if (window.pressed.contains(KeyCode.SHIFT)) {
			if (lastEnd == null)
				lastEnd = end;
			strokeInner(context, null, lastEnd, end);
		} else
			strokeInner(context, new ProjectContext.Tuple(brush, "stroke"), start, end);
	}

	private void strokeInner(
			ProjectContext context,
			ProjectContext.Tuple changeUnique,
			DoubleVector start,
			DoubleVector end
	) {
		final double startRadius = brush.size.get() / 20.0;
		final double endRadius = brush.size.get() / 20.0;
		final DoubleRectangle bounds = new BoundsBuilder().circle(start, startRadius).circle(end, endRadius).build();
		context.change(changeUnique, c -> {
			wrapper.modify(context, c, bounds, (image, corner) -> {
				stroke(context, image, start.minus(corner), startRadius, end.minus(corner), endRadius);
			});
		});
		lastEnd = end;
	}

	protected abstract void stroke(
			ProjectContext context, L canvas, DoubleVector start, double startRadius, DoubleVector end, double endRadius
	);

	private void updateCursor(Window window) {
		double zoom = window.editor.zoomFactor.get();
		window.editor.outerCanvas.setCursor(cursor = CircleCursor.create(brush.sizeInPixels() * zoom));
	}

	@Override
	public void remove(ProjectContext context, Window window) {
		if (window.editor.outerCanvas.getCursor() == cursor)
			window.editor.outerCanvas.setCursor(null);
	}

	@Override
	public void cursorMoved(ProjectContext context, Window window, DoubleVector position) {

	}
}
