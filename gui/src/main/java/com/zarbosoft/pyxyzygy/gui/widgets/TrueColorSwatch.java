package com.zarbosoft.pyxyzygy.gui.widgets;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class TrueColorSwatch extends StackPane {
	public final SimpleObjectProperty<Color> colorProperty = new SimpleObjectProperty<>();

	public TrueColorSwatch() {
		getStyleClass().addAll("color-swatch", "true-color-transparent-pattern");
		final Pane colorLayer = new Pane();
		colorProperty.addListener(new ChangeListener<Color>() {
			{
				changed(null, null, colorProperty.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends Color> observable, Color oldValue, Color newValue
			) {
				colorLayer.setBackground(new Background(new BackgroundFill(newValue, CornerRadii.EMPTY, Insets.EMPTY)));
			}
		});
		getChildren().addAll(colorLayer);
	}
}
