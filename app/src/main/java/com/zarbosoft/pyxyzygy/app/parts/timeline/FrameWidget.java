package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.c;

public class FrameWidget extends Pane {
	private final static double sizePercent = 0.7;
	private final static double sizePercentComp = (1.0 - sizePercent) * 0.5;
	final Rectangle rectangle;
	double zoom;

	final RowFramesWidget row;
	int index;
	RowAdapterFrame frame;

	int absStart;
	int absEnd;
	int minLength;
	int at;

	public FrameWidget(ProjectContext context, RowFramesWidget row) {
		this.row = row;

		setWidth(Timeline.baseSize);
		setHeight(Timeline.baseSize);
		setMouseTransparent(false);

		rectangle = new Rectangle(0, Timeline.baseSize * sizePercent);
		rectangle.setStroke(c(new java.awt.Color(0, 0, 0)));
		rectangle.setFill(Color.TRANSPARENT);
		rectangle.setMouseTransparent(true);
		rectangle.setLayoutY(Timeline.baseSize * sizePercentComp);
		rectangle.setArcWidth(Timeline.baseSize * 0.4);
		rectangle.setArcHeight(rectangle.getArcWidth());
		getChildren().add(rectangle);

		addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
			context.history.finishChange();
			row.timeline.select(this);
		});
		addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
			if (index == 0)
				return;
			double x = (e.getSceneX() - row.getLocalToSceneTransform().transform(0, 0).getX());
			int frame = (int) (x / zoom);
			if (absEnd != -1)
				frame = Math.min(frame, absEnd - 1);
			frame = Math.max(frame, absStart);
			int length = minLength + frame - absStart;
			((FrameWidget) this.row.frames.get(index - 1)).frame.setLength(context, length);
		});
		addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
			context.history.finishChange();
		});
		deselect();
	}

	public void select() {
		rectangle.setFill(c(new java.awt.Color(52, 52, 52)));
	}

	public void deselect() {
		rectangle.setFill(Color.TRANSPARENT);
	}

	/**
	 * The limits are because when working under a group the frame might be part of a loop and dragging it outside the
	 * loop would cause it to disappear (in the current context).
	 * Prevent dragging out of loops.  Otherwise no rightward limits.
	 *
	 * @param index
	 * @param frame
	 * @param absStart  Farthest left frame can be dragged
	 * @param absEnd    Farthest right frame can be dragged or -1
	 * @param minLength When dragged all the way to the left, what length does the frame's preceding frame get
	 * @param offset    Where to draw the frame relative to absStart
	 */
	public void set(
			double zoom,
			int index,
			RowAdapterFrame frame,
			int absStart,
			int absEnd,
			int minLength,
			int offset
	) {
		this.zoom = zoom;
		rectangle.setWidth(zoom * sizePercent);
		rectangle.setLayoutX(zoom * sizePercentComp);
		this.index = index;
		this.frame = frame;
		this.absStart = absStart;
		this.absEnd = absEnd;
		this.minLength = minLength;
		at = absStart + offset;
		setLayoutX(at * zoom);
	}
}
