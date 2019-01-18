package com.zarbosoft.shoedemo.widgets;

import com.zarbosoft.shoedemo.Main;
import com.zarbosoft.shoedemo.config.TrueColor;
import com.zarbosoft.shoedemo.config.TrueColorBrush;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class BrushButton extends ToggleButton {
	public BrushButton(TrueColorBrush b) {
		getStyleClass().add("brush-button");

		TrueColorSwatch swatch = new TrueColorSwatch();

		Label label = new Label();
		label.textProperty().bind(b.size.divide(10.0).asString("%.1f"));
		label.setAlignment(Pos.CENTER);

		StackPane stack = new StackPane();
		stack.getChildren().addAll(swatch, label);

		setGraphic(stack);

		selectedProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue)
				return;
			Main.config.trueColorBrush.set(Main.config.trueColorBrushes.indexOf(b));
		});

		b.useColor.addListener(new ChangeListener<Boolean>() {
			{
				changed(null, null, b.useColor.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue
			) {
				SimpleObjectProperty<TrueColor> property = newValue ? b.color : Main.config.trueColor;
				swatch.colorProperty.bind(Bindings.createObjectBinding(() -> property.get().toJfx(),
						property
				));
				label.textFillProperty().bind(Bindings.createObjectBinding(() -> {
					Color c = property.get().toJfx();
					double darkness = (1.0 - c.getBrightness()) * c.getOpacity();
					return darkness > 0.5 ? Color.WHITE : Color.BLACK;
				}, property));
			}
		});
	}
}
