package com.zarbosoft.shoedemo.parts.timeline;

import com.zarbosoft.shoedemo.ProjectContext;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;

import static javafx.scene.paint.Color.BLACK;
import static javafx.scene.paint.Color.PURPLE;

public class FrameWidget extends Canvas {
	double zoom;

	final RowFramesWidget row;
	int index;
	RowAdapterFrame frame;

	int absStart;
	int absEnd;
	int minLength;

	public FrameWidget(ProjectContext context, RowFramesWidget row) {
		this.row = row;
		addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
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
			((FrameWidget)this.row.frames.get(index - 1)).frame.setLength(context, length);
		});
		addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
			context.history.finishChange();
		});
		setWidth(Timeline.baseSize);
		setHeight(Timeline.baseSize);
		deselect();
	}

	public void select() {
		GraphicsContext gc = getGraphicsContext2D();
		gc.clearRect(0, 0, getWidth(), getHeight());
		gc.setFill(PURPLE);
		gc.fillOval(2, 2, Timeline.baseSize - 4, Timeline.baseSize - 4);
	}

	public void deselect() {
		GraphicsContext gc = getGraphicsContext2D();
		gc.clearRect(0, 0, getWidth(), getHeight());
		gc.setFill(BLACK);
		gc.strokeOval(2, 2, Timeline.baseSize - 4, Timeline.baseSize - 4);
	}

	/**
	 * The limits are because when working under a group the frame might be part of a loop and dragging it outside the
	 * loop would cause it to disappear (in the current context).
	 * Prevent dragging out of loops.  Otherwise no rightward limits.
	 * @param index
	 * @param frame
	 * @param absStart  Farthest left frame can be dragged
	 * @param absEnd    Farthest right frame can be dragged or -1
	 * @param minLength When dragged all the way to the left, what length does the frame's preceding frame get
	 * @param offset    Where to draw the frame relative to absStart
	 */
	public void set(double zoom, int index, RowAdapterFrame frame, int absStart, int absEnd, int minLength, int offset) {
		this.zoom = zoom;
		this.index = index;
		this.frame = frame;
		this.absStart = absStart;
		this.absEnd = absEnd;
		this.minLength = minLength;
		System.out.format("pos %s\n",(absStart + offset) * zoom);
		setLayoutX((absStart + offset) * zoom);
	}
}
