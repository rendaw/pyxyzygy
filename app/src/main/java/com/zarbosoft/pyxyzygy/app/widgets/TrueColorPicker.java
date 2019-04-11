package com.zarbosoft.pyxyzygy.app.widgets;

import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;

public class TrueColorPicker extends GridPane {
	public final DoubleProperty hue = new SimpleDoubleProperty(0);
	public final DoubleProperty sat = new SimpleDoubleProperty(0);
	public final DoubleProperty bright = new SimpleDoubleProperty(0);
	public final DoubleProperty alpha = new SimpleDoubleProperty(1);

	public boolean suppressProxyListeners;
	public final SimpleObjectProperty<Color> colorProxyProperty = new SimpleObjectProperty<>();

	private final Region sliceCursor;
	private final Region alphaBarMarker;

	public TrueColorPicker() {
		ColorSwatch newColorDisplay = new ColorSwatch(1);
		newColorDisplay.colorProperty.bind(colorProxyProperty);
		newColorDisplay.disableProperty().bind(disableProperty());

		// Hue bar - select a hue for the slice
		Pane hueBar = new Pane();
		hueBar.getStyleClass().add("hue-bar");
		{
			double offset;
			Stop[] stops = new Stop[255];
			for (int x = 0; x < 255; x++) {
				offset = (double) ((1.0 / 255) * x);
				int h = (int) ((x / 255.0) * 360);
				stops[x] = new Stop(offset, Color.hsb(h, 1.0, 1.0));
			}
			hueBar.setBackground(new Background(new BackgroundFill(new LinearGradient(0f,
					0f,
					1f,
					0f,
					true,
					CycleMethod.NO_CYCLE,
					stops
			), CornerRadii.EMPTY, Insets.EMPTY)));
		}
		Region hueBarMarker = new Region();
		hueBarMarker.setId("hue-bar-cursor");
		hueBarMarker.setMouseTransparent(true);
		hueBarMarker.setCache(true);
		hueBarMarker.layoutXProperty().bind(hue.divide(360.0).multiply(hueBar.widthProperty()));
		hueBar.getChildren().setAll(hueBarMarker);

		// Alpha bar
		Pane alphaBarLayer = new Pane();
		final ChangeListener<Number> updateAlphaListener = new ChangeListener<Number>() {
			{
				changed(null, null, null);
			}

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				alphaBarLayer.setBackground(new Background(new BackgroundFill(new LinearGradient(0f,
						0f,
						0f,
						1f,
						true,
						CycleMethod.NO_CYCLE,
						new Stop(0, Color.hsb(hue.get(), sat.get(), bright.get(), 1.0)),
						new Stop(1, Color.hsb(hue.get(), sat.get(), bright.get(), 0.0))
				), CornerRadii.EMPTY, Insets.EMPTY)));
			}
		};
		hue.addListener(updateAlphaListener);
		sat.addListener(updateAlphaListener);
		bright.addListener(updateAlphaListener);
		alpha.addListener(updateAlphaListener);
		Pane alphaBar = new StackPane();
		alphaBar.getStyleClass().addAll("alpha-bar", "true-color-transparent-pattern");
		alphaBarMarker = new Region();
		alphaBarMarker.setId("alpha-bar-cursor");
		alphaBarMarker.setManaged(false);
		alphaBarMarker.setMouseTransparent(true);
		alphaBarMarker.setCache(true);
		alphaBarMarker.layoutYProperty().bind(Bindings.subtract(1.0, alpha).multiply(alphaBar.heightProperty()));
		alphaBar.getChildren().setAll(alphaBarLayer, alphaBarMarker);

		// Slice, chooses saturation and brightness
		Pane sliceLayerColor = new Pane();
		sliceLayerColor.setMouseTransparent(true);
		sliceLayerColor.backgroundProperty().bind(Bindings.createObjectBinding(() -> {
			return new Background(new BackgroundFill(Color.hsb(hue.getValue(), 1.0, 1.0),
					CornerRadii.EMPTY,
					Insets.EMPTY
			));
		}, hue));

		Pane sliceLayerWhite = new Pane();
		sliceLayerWhite.setMouseTransparent(true);
		sliceLayerWhite.getStyleClass().add("slice");
		sliceLayerWhite.setBackground(new Background(new BackgroundFill(new LinearGradient(0,
				0,
				1,
				0,
				true,
				CycleMethod.NO_CYCLE,
				new Stop(0, Color.rgb(255, 255, 255, 1)),
				new Stop(1, Color.rgb(255, 255, 255, 0))
		), CornerRadii.EMPTY, Insets.EMPTY)));

		Pane sliceLayerBlack = new Pane();
		sliceLayerBlack.setMouseTransparent(true);
		sliceLayerBlack.getStyleClass().addAll("slice");
		sliceLayerBlack.setBackground(new Background(new BackgroundFill(new LinearGradient(0,
				0,
				0,
				1,
				true,
				CycleMethod.NO_CYCLE,
				new Stop(0, Color.rgb(0, 0, 0, 0)),
				new Stop(1, Color.rgb(0, 0, 0, 1))
		), CornerRadii.EMPTY, Insets.EMPTY)));

		final StackPane sliceOpaqueLayers = new StackPane();
		sliceOpaqueLayers.opacityProperty().bind(alpha);
		sliceOpaqueLayers.getChildren().setAll(sliceLayerColor, sliceLayerWhite, sliceLayerBlack);

		Pane sliceBorder = new Pane();
		sliceBorder.setMouseTransparent(true);
		sliceBorder.getStyleClass().addAll("slice", "slice-border");

		sliceCursor = new Region();
		sliceCursor.setId("slice-cursor");
		sliceCursor.setManaged(false);
		sliceCursor.setMouseTransparent(true);
		sliceCursor.setCache(true);

		StackPane slice = new StackPane();
		slice.getStyleClass().addAll("slice", "true-color-transparent-pattern");
		VBox.setVgrow(slice, Priority.SOMETIMES);
		slice.getChildren().setAll(sliceOpaqueLayers, sliceBorder, sliceCursor);

		sliceCursor.layoutXProperty().bind(sat.multiply(slice.widthProperty()));
		sliceCursor.layoutYProperty().bind(Bindings.subtract(1.0, bright).multiply(slice.heightProperty()));

		// Assemble
		getStyleClass().add("true-color-picker");
		addRow(0, hueBar, newColorDisplay);
		addRow(1, slice, alphaBar);

		// Event handling
		disableProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue) {
				hueBar.setEffect(new ColorAdjust(0,-1,0,0));
				alphaBarLayer.setEffect(new ColorAdjust(0,-1,0,0));
				sliceLayerColor.setEffect(new ColorAdjust(0,-1,0,0));
			} else {
				hueBar.setEffect(null);
				alphaBarLayer.setEffect(null);
				sliceLayerColor.setEffect(null);
			}
		});
		EventHandler<MouseEvent> hueBarMouseHandler = event -> {
			hue.set(clamp(event.getX() / hueBar.getWidth()) * 360);
		};
		hueBar.setOnMouseDragged(hueBarMouseHandler);
		hueBar.setOnMousePressed(hueBarMouseHandler);

		EventHandler<MouseEvent> alphaBarMouseHandler = event -> {
			alpha.set(1.0 - clamp(event.getY() / alphaBar.getHeight()));
		};
		alphaBar.setOnMouseDragged(alphaBarMouseHandler);
		alphaBar.setOnMousePressed(alphaBarMouseHandler);

		EventHandler<MouseEvent> sliceMouseHandler = event -> {
			sat.set(clamp(event.getX() / slice.getWidth()));
			bright.set(1.0 - clamp(event.getY() / slice.getHeight()));
		};
		sliceOpaqueLayers.setOnMouseDragged(sliceMouseHandler);
		sliceOpaqueLayers.setOnMousePressed(sliceMouseHandler);

		colorProxyProperty.addListener((observable, oldValue, newValue) -> {
			if (suppressProxyListeners)
				return;
			suppressProxyListeners = true;
			try {
				hue.set(newValue.getHue());
				sat.set(newValue.getSaturation());
				bright.set(newValue.getBrightness());
				alpha.set(newValue.getOpacity());
			} finally {
				suppressProxyListeners = false;
			}
		});
		final ChangeListener<Number> hsbProxyListener = new ChangeListener<Number>() {
			{
				changed(null, null, null);
			}

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				if (suppressProxyListeners)
					return;
				suppressProxyListeners = true;
				try {
					Color out = Color.hsb(hue.get(), sat.get(), bright.get(), alpha.get());
					colorProxyProperty.set(out);
				} finally {
					suppressProxyListeners = false;
				}
			}
		};
		hue.addListener(hsbProxyListener);
		sat.addListener(hsbProxyListener);
		bright.addListener(hsbProxyListener);
		alpha.addListener(hsbProxyListener);
	}

	@Override
	protected void layoutChildren() {
		super.layoutChildren();
		sliceCursor.autosize();
		alphaBarMarker.autosize();
	}

	static double clamp(double value) {
		return Math.max(0, Math.min(1, value));
	}
}
