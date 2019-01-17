package com.zarbosoft.shoedemo;

import com.zarbosoft.rendaw.common.Pair;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.text.DecimalFormat;
import java.util.Optional;
import java.util.function.Function;

public class HelperJFX {
	public static Node pad(Node node) {
		VBox out = new VBox();
		out.setPadding(new Insets(3));
		out.getChildren().add(node);
		return out;
	}

	public static Color c(java.awt.Color source) {
		return Color.rgb(source.getRed(), source.getGreen(), source.getBlue());
	}

	public static Pair<Node, SimpleIntegerProperty> nonlinerSlider(int min, int max, int precision, int divide) {
		Slider slider = new Slider();
		slider.setMin(0);
		slider.setMax(1);
		HBox.setHgrow(slider, Priority.ALWAYS);

		TextField text = new TextField();
		text.setMinWidth(50);
		text.setPrefWidth(50);
		HBox.setHgrow(text, Priority.NEVER);

		HBox out = new HBox();
		out.setSpacing(3);
		out.setAlignment(Pos.CENTER_LEFT);
		out.getChildren().addAll(text, slider);

		double range = max - min;
		Function<Double, Integer> fromNonlinear = v -> (int) (Math.pow(v, 2) * range + min);
		Function<Integer, Double> toNonlinear = v -> Math.pow((v - min) / range, 0.5);
		SimpleIntegerProperty value = new SimpleIntegerProperty();

		CustomBinding.bindBidirectional(
				value,
				slider.valueProperty(),
				v -> Optional.of(toNonlinear.apply(v.intValue())),
				v -> Optional.of(fromNonlinear.apply(v.doubleValue()))
		);
		DecimalFormat textFormat = new DecimalFormat();
		textFormat.setMaximumFractionDigits(precision);
		CustomBinding.bindBidirectional(value,
				text.textProperty(),
				v -> Optional.of(textFormat.format((double)v.intValue() / divide)),
				v -> {
					try {
						return Optional.of((int)(Double.parseDouble(v) * divide));
					} catch (NumberFormatException e) {
						return Optional.empty();
					}
				});

		return new Pair<>(out, value);
	}
}
