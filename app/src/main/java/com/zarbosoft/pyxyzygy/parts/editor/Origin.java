package com.zarbosoft.pyxyzygy.parts.editor;

import com.zarbosoft.pyxyzygy.Launch;
import com.zarbosoft.pyxyzygy.Window;
import com.zarbosoft.pyxyzygy.Wrapper;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeType;

public class Origin extends Group {
	private final Line originVert = new Line();
	private final Line originHoriz = new Line();

	public Origin(Window window) {
		originHoriz.setFill(Color.BLACK);
		originVert.setFill(Color.BLACK);
		originHoriz.setOpacity(0.5);
		originVert.setOpacity(0.5);
		originHoriz.visibleProperty().bind(Launch.config.showOrigin);
		originVert.visibleProperty().bind(Launch.config.showOrigin);
		getChildren().addAll(originHoriz, originVert);
		setBlendMode(BlendMode.MULTIPLY);
		window.selectedForEdit.addListener(new ChangeListener<Wrapper.EditHandle>() {
			{
				changed(null, null, window.selectedForEdit.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends Wrapper.EditHandle> observable,
					Wrapper.EditHandle oldValue,
					Wrapper.EditHandle newValue
			) {
				if (oldValue != null) {
					oldValue.getCanvas().overlay.getChildren().remove(Origin.this);
				}

				if (newValue != null) {
					DoubleBinding scale = Bindings.createDoubleBinding(() -> 1.0/Editor.computeViewTransform(newValue.getWrapper()).x,
									newValue.getWrapper().getConfig().zoom);
					DoubleBinding originHalfSpan =scale.multiply(20.0);
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
					newValue.getCanvas().overlay.getChildren().addAll(Origin.this);
				}
			}
		});
	}
}
