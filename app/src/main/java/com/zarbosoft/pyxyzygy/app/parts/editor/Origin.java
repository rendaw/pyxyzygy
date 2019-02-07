package com.zarbosoft.pyxyzygy.app.parts.editor;

import com.zarbosoft.pyxyzygy.app.CanvasHandle;
import com.zarbosoft.pyxyzygy.app.EditHandle;
import com.zarbosoft.pyxyzygy.app.GUILaunch;
import com.zarbosoft.pyxyzygy.app.Window;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Group;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public class Origin extends Group {
	private final Line originVert = new Line();
	private final Line originHoriz = new Line();

	public Origin(Window window) {
		originHoriz.setFill(Color.BLACK);
		originVert.setFill(Color.BLACK);
		originHoriz.setOpacity(0.5);
		originVert.setOpacity(0.5);
		originHoriz.visibleProperty().bind(GUILaunch.config.showOrigin);
		originVert.visibleProperty().bind(GUILaunch.config.showOrigin);
		getChildren().addAll(originHoriz, originVert);
		setBlendMode(BlendMode.MULTIPLY);
		window.selectedForView.addListener(new ChangeListener<CanvasHandle>() {
			{
				changed(null, null, window.selectedForView.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends CanvasHandle> observable, CanvasHandle oldValue, CanvasHandle newValue
			) {
				DoubleBinding scale = Bindings.createDoubleBinding(
						() -> 1.0 / Editor.computeViewTransform(newValue.getWrapper()).x,
						newValue.getWrapper().getConfig().zoom
				);
				DoubleBinding originHalfSpan = scale.multiply(20.0);
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
			}
		});
		window.selectedForEdit.addListener(new ChangeListener<EditHandle>() {
			{
				changed(null, null, window.selectedForEdit.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends EditHandle> observable, EditHandle oldValue, EditHandle newValue
			) {
				if (oldValue != null) {
					oldValue.getCanvas().overlay.getChildren().remove(Origin.this);
				}

				if (newValue != null) {
					newValue.getCanvas().overlay.getChildren().addAll(Origin.this);
				}
			}
		});
	}
}
