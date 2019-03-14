package com.zarbosoft.pyxyzygy.app.wrappers.baseimage;

import com.zarbosoft.pyxyzygy.app.widgets.ColorSwatch;
import javafx.beans.binding.IntegerExpression;
import javafx.beans.value.*;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public abstract class BrushButton extends ToggleButton {
	public BrushButton(
			IntegerExpression size, ObservableObjectValue<Color> color, ObservableBooleanValue selected
	) {
		getStyleClass().add("brush-button");

		ColorSwatch swatch = new ColorSwatch();

		Label label = new Label();
		label.textProperty().bind(size.divide(10.0).asString("%.1f"));
		label.setAlignment(Pos.CENTER);

		StackPane stack = new StackPane();
		stack.getChildren().addAll(swatch, label);

		setGraphic(stack);

		selectedProperty().bind(selected);

		color.addListener(new ChangeListener<Color>() {
			{
				changed(null,null,color.get());
			}
			@Override
			public void changed(ObservableValue<? extends Color> observable, Color oldValue, Color newValue) {
				swatch.colorProperty.set(newValue);
				double darkness = (1.0 - newValue.getBrightness()) * newValue.getOpacity();
				label.setTextFill(darkness > 0.5 ? Color.WHITE : Color.BLACK);
			}
		});
	}

	@Override
	public void fire() {
		if (isSelected())
			return;
		selectBrush();
	}

	public abstract void selectBrush();
}
