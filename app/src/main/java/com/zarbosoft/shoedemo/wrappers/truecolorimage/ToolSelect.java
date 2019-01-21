package com.zarbosoft.shoedemo.wrappers.truecolorimage;

import com.zarbosoft.shoedemo.BoundsBuilder;
import com.zarbosoft.shoedemo.CustomBinding;
import com.zarbosoft.shoedemo.DoubleVector;
import com.zarbosoft.shoedemo.ProjectContext;
import com.zarbosoft.shoedemo.model.Rectangle;
import com.zarbosoft.shoedemo.wrappers.group.Tool;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;
import javafx.scene.paint.Color;

public class ToolSelect extends Tool {
	final TrueColorImageEditHandle trueColorImageEditHandle;
	private DoubleVector markStart;

	abstract class State {

		public abstract void markStart(ProjectContext context, DoubleVector start);

		public abstract void mark(ProjectContext context, DoubleVector start, DoubleVector end);

		public abstract void remove(ProjectContext context);
	}

	/*
	abstract class Interactive {
		public abstract boolean contains(Vector vector);

		public abstract void mark(ProjectContext context, DoubleVector start, DoubleVector end);
	}

	class HGrabber extends Interactive {

	}

	class VGrabber extends Interactive {

	}
	*/

	class StateCreate extends State {
		javafx.scene.shape.Rectangle rectangle = new javafx.scene.shape.Rectangle();

		SimpleIntegerProperty x1 = new SimpleIntegerProperty();
		SimpleIntegerProperty x2 = new SimpleIntegerProperty();
		SimpleIntegerProperty y1 = new SimpleIntegerProperty();
		SimpleIntegerProperty y2 = new SimpleIntegerProperty();
		/*
		HGrabber left = new HGrabber();
		HGrabber right = new HGrabber();
		VGrabber top = new VGrabber();
		VGrabber bottom = new VGrabber();

		Interactive mark;
		*/

		StateCreate() {
			rectangle.setStroke(Color.GRAY);
			rectangle.setFill(Color.TRANSPARENT);
			rectangle.getStrokeDashArray().setAll(5.0, 5.0);
			rectangle.layoutXProperty().bind(Bindings.min(x1, x2));
			rectangle.layoutYProperty().bind(Bindings.min(y1, y2));
			rectangle.widthProperty().bind(CustomBinding.bindAbs(Bindings.subtract(x1, x2)));
			rectangle.heightProperty().bind(CustomBinding.bindAbs(Bindings.subtract(y1, y2)));
			rectangle.visibleProperty().bind(Bindings.and(Bindings.notEqual(x1, x2), Bindings.notEqual(y1, y2)));
			trueColorImageEditHandle.overlay.getChildren().add(rectangle);
		}

		@Override
		public void markStart(ProjectContext context, DoubleVector start) {
			/*
			if (selection.contains(start.toInt())) {
				setState(context, new StateMove());
			} else if (left.contains(start.toInt())) {
				mark = left;
			} else if (right.contains(start.toInt())) {
				mark = right;
			} else if (top.contains(start.toInt())) {
				mark = top;
			} else if (bottom.contains(start.toInt())) {
				mark = bottom;
			} else {
				mark = null;
			}
			*/
		}

		@Override
		public void mark(ProjectContext context, DoubleVector start, DoubleVector end) {
			Rectangle newBounds = new BoundsBuilder().circle(markStart, 0).circle(end, 0).buildInt();
			x1.set(newBounds.x);
			x2.set(newBounds.x + newBounds.width);
			y1.set(newBounds.y);
			y2.set(newBounds.y + newBounds.height);
		}

		@Override
		public void remove(ProjectContext context) {
			trueColorImageEditHandle.overlay.getChildren().remove(rectangle);
		}
	}

	State state;

	ToolSelect(TrueColorImageEditHandle trueColorImageEditHandle) {
		this.trueColorImageEditHandle = trueColorImageEditHandle;
		state = new StateCreate();
	}

	void setState(ProjectContext context, State newState) {
		state.remove(context);
		state = newState;
	}

	@Override
	public void markStart(ProjectContext context, DoubleVector start) {
		this.markStart = start;
		state.markStart(context, start);
	}

	@Override
	public void mark(ProjectContext context, DoubleVector start, DoubleVector end) {
		state.mark(context, start, end);
	}

	@Override
	public Node getProperties() {
		return null;
	}

	@Override
	public void remove(ProjectContext context) {
		state.remove(context);
	}
}
