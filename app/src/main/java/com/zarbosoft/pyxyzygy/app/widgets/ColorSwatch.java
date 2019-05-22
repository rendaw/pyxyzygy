package com.zarbosoft.pyxyzygy.app.widgets;

import com.zarbosoft.pyxyzygy.app.widgets.binding.*;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.Optional;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;

public class ColorSwatch extends StackPane {
	public final double gapScaler;
	public final SimpleObjectProperty<Color> colorProperty = new SimpleObjectProperty<>();

	private Node createClip() {
		Rectangle clip = new Rectangle();
		HalfBinder<CornerRadii> radiiBinder =
				new DoubleHalfBinder<>(backgroundProperty(), borderProperty()).map((bg, border) -> {
					if (bg != null)
						return opt(bg.getFills().get(0).getRadii());
					if (border != null)
						if (border.getStrokes().isEmpty())
							return opt(CornerRadii.EMPTY);
						else
							return opt(border.getStrokes().get(0).getRadii());
					return opt(CornerRadii.EMPTY);
				});
		HalfBinder<Double> borderWidthBinder =
				new PropertyHalfBinder<>(borderProperty()).map(b0 -> Optional.of(Optional
						.ofNullable(b0)
						.flatMap(b -> b.getStrokes().isEmpty() ? Optional.empty() : Optional.of(b.getStrokes().get(0)))
						.map(b -> b.getWidths().getTop())
						.orElse(0.)));
		new DoubleIndirectHalfBinder<>(borderWidthBinder, radiiBinder, (w, r) -> {
			clip.setArcWidth(r.getTopLeftHorizontalRadius() * 2 - w * gapScaler);
			clip.setArcHeight(r.getTopLeftVerticalRadius() * 2 - w * gapScaler);
			return Optional.empty();
		});
		new DoubleIndirectHalfBinder<>(
				borderWidthBinder,
				layoutBoundsProperty(),
				(borderWidth, bounds) -> {
					clip.setWidth(bounds.getWidth() - borderWidth * 2.0 * gapScaler);
					clip.setHeight(bounds.getHeight() - borderWidth * 2.0 * gapScaler);
					clip.setLayoutX(bounds.getWidth() * 0.5 - clip.getWidth() * 0.5);
					clip.setLayoutY(bounds.getHeight() * 0.5 - clip.getHeight() * 0.5);
					return Optional.<Void>empty();
				}
		);
		return clip;
	}

	public ColorSwatch(double gapScaler) {
		this.gapScaler = gapScaler;
		getStyleClass().addAll("color-swatch");

		Pane bgLayer = new Pane();
		bgLayer.getStyleClass().add("true-color-transparent-pattern");
		bgLayer.setClip(createClip());
		bgLayer.visibleProperty().bind(colorProperty.isNotNull());

		final Pane colorLayer = new Pane();
		colorProperty.addListener(new ChangeListener<Color>() {
			{
				changed(null, null, colorProperty.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends Color> observable, Color oldValue, Color newValue
			) {
				if (newValue == null) return;
				colorLayer.setBackground(new Background(new BackgroundFill(newValue, CornerRadii.EMPTY, Insets.EMPTY)));
			}
		});
		colorLayer.setClip(createClip());
		colorLayer.visibleProperty().bind(colorProperty.isNotNull());

		getChildren().addAll(bgLayer, colorLayer);

		disableProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				colorLayer.setEffect(new ColorAdjust(0,-1,0,0));
			}else{
				colorLayer.setEffect(null);
			}
		});
		layoutBoundsProperty().addListener(new ChangeListener<Bounds>() {
			{
				changed(null, null, layoutBoundsProperty().getValue());
			}

			@Override
			public void changed(ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) {
				bgLayer.setMinWidth(newValue.getWidth());
				bgLayer.setMinHeight(newValue.getHeight());
				colorLayer.setMinWidth(newValue.getWidth());
				colorLayer.setMinHeight(newValue.getHeight());
			}
		});
	}
}
