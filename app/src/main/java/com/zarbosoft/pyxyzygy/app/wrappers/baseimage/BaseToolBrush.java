package com.zarbosoft.pyxyzygy.app.wrappers.baseimage;

import com.zarbosoft.pyxyzygy.app.BoundsBuilder;
import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.Tool;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.config.BaseBrush;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.CircleCursor;
import com.zarbosoft.pyxyzygy.app.widgets.TrueColorPicker;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.seed.model.v0.Rectangle;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.geometry.HPos;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;

import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.pad;

public abstract class BaseToolBrush<F, L> extends Tool {
	private final BaseBrush brush;
	public final BaseImageNodeWrapper<?,F,?,L> wrapper;
	private DoubleVector lastEnd;
	private ImageCursor cursor = null;

	public BaseToolBrush(
			ProjectContext context, Window window, BaseImageNodeWrapper<?,F,?,L> wrapper, BaseBrush brush
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
			strokeInner(context, lastEnd, end);
		} else
			strokeInner(context, start, end);
	}

	private void strokeInner(ProjectContext context, DoubleVector start, DoubleVector end) {
		final double startRadius = brush.size.get() / 20.0;
		final double endRadius = brush.size.get() / 20.0;
		wrapper.modify(context,new BoundsBuilder()
				.circle(start, startRadius)
				.circle(end, endRadius)
				.build(),(image, offset) -> {
			stroke(context, image, start.minus(offset), startRadius, end.minus(offset), endRadius);
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
}
