package com.zarbosoft.pyxyzygy.app.parts.editor;

import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.widgets.binding.BinderRoot;
import com.zarbosoft.pyxyzygy.app.widgets.binding.CustomBinding;
import com.zarbosoft.pyxyzygy.app.widgets.binding.PropertyHalfBinder;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;

public class Origin extends Group {
	private final Line originVert = new Line();
	private final Line originHoriz = new Line();
	private final BinderRoot selectedForEditCleanup;
	private Group overlay;

	public final SimpleObjectProperty<Vector> offset = new SimpleObjectProperty<Vector>(Vector.ZERO);

	public Origin(Window window, Editor editor, double size) {
		CustomBinding.bind(layoutXProperty(),new PropertyHalfBinder<>(offset).map(o -> opt((double)o.x)));
		CustomBinding.bind(layoutYProperty(),new PropertyHalfBinder<>(offset).map(o -> opt((double)o.y)));
		originHoriz.setStroke(Color.GRAY);
		originVert.setStroke(Color.GRAY);
		getChildren().addAll(originHoriz, originVert);

		//SimpleDoubleProperty scale = editor.zoomFactor;
		DoubleBinding scale = Bindings.divide(1.0, editor.zoomFactor);
		DoubleBinding originHalfSpan = scale.multiply(size);
		originHoriz.startXProperty().bind(originHalfSpan.negate());
		originHoriz.endXProperty().bind(originHalfSpan);
		originVert.startYProperty().bind(originHalfSpan.negate());
		originVert.endYProperty().bind(originHalfSpan);
		originHoriz.strokeWidthProperty().bind(scale);
		originVert.strokeWidthProperty().bind(scale);
		originHoriz.startYProperty().bind(scale.multiply(0.5));
		originHoriz.endYProperty().bind(scale.multiply(0.5));
		originVert.startXProperty().bind(scale.multiply(0.5));
		originVert.endXProperty().bind(scale.multiply(0.5));

		setBlendMode(BlendMode.DIFFERENCE);

		this.selectedForEditCleanup = window.selectedForEditOriginBinder.addListener(newValue -> {
			if (overlay != null) {
				overlay.getChildren().remove(Origin.this);
				overlay = null;
			}

			if (newValue != null) {
				overlay = newValue.getCanvas().overlay;
				overlay.getChildren().addAll(Origin.this);
			}
		});
	}

	public void remove() {
		overlay.getChildren().remove(Origin.this);
		overlay = null;
		selectedForEditCleanup.destroy();
	}
}
