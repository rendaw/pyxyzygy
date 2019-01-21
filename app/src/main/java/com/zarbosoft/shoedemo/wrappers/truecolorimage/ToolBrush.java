package com.zarbosoft.shoedemo.wrappers.truecolorimage;

import com.zarbosoft.rendaw.common.Pair;
import com.zarbosoft.shoedemo.*;
import com.zarbosoft.shoedemo.config.TrueColor;
import com.zarbosoft.shoedemo.config.TrueColorBrush;
import com.zarbosoft.shoedemo.model.Rectangle;
import com.zarbosoft.shoedemo.model.Tile;
import com.zarbosoft.shoedemo.widgets.TrueColorPicker;
import com.zarbosoft.shoedemo.wrappers.group.Tool;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

import java.util.Optional;

public class ToolBrush extends Tool {
	final TrueColorImageNodeWrapper trueColorImageNodeWrapper;
	final TrueColorBrush brush;
	private final Node properties;

	public ToolBrush(ProjectContext context,
			TrueColorImageNodeWrapper trueColorImageNodeWrapper, TrueColorBrush brush
	) {
		this.trueColorImageNodeWrapper = trueColorImageNodeWrapper;
		this.brush = brush;

		TrueColorPicker colorPicker = new TrueColorPicker();
		GridPane.setHalignment(colorPicker, HPos.CENTER);
		properties =  new WidgetFormBuilder()
				.text("Name", t -> t.textProperty().bindBidirectional(brush.name))
				.span(() -> {
					return colorPicker;
				})
				.check("Use brush color", widget -> {
					widget.selectedProperty().bindBidirectional(brush.useColor);
					widget.selectedProperty().addListener(new ChangeListener<Boolean>() {
						Runnable pickerBindingCleanup;

						{
							changed(null, null, widget.isSelected());
						}

						@Override
						public void changed(
								ObservableValue<? extends Boolean> observable,
								Boolean oldValue,
								Boolean newValue
						) {
							SimpleObjectProperty<TrueColor> color;
							if (newValue)
								color = brush.color;
							else
								color = context.config.trueColor;

							if (pickerBindingCleanup != null) {
								pickerBindingCleanup.run();
								pickerBindingCleanup = null;
							}
							pickerBindingCleanup = CustomBinding.<TrueColor, Color>bindBidirectional(color,
									colorPicker.colorProxyProperty,
									c -> Optional.of(c.toJfx()),
									c -> {
										TrueColor out = new TrueColor();
										out.r = (byte) (c.getRed() * 255);
										out.g = (byte) (c.getGreen() * 255);
										out.b = (byte) (c.getBlue() * 255);
										out.a = (byte) (c.getOpacity() * 255);
										return Optional.of(out);
									}
							);
						}
					});
				})
				.custom("Size", () -> {
					Pair<Node, SimpleIntegerProperty> brushSize = HelperJFX.nonlinerSlider(10, 2000, 1, 10);
					brushSize.second.bindBidirectional(brush.size);
					return brushSize.first;
				})
				.slider("Blend", 1, 1000, s -> {
					s.valueProperty().bindBidirectional(brush.blend);
				})
				.build();
	}

	@Override
	public void markStart(ProjectContext context, DoubleVector start) {

	}

	@Override
	public void mark(ProjectContext context, DoubleVector start, DoubleVector end) {
		TrueColor color = brush.useColor.get() ? brush.color.get() : context.config.trueColor.get();

		final double startRadius = brush.size.get() / 20.0;
		final double endRadius = brush.size.get() / 20.0;

		// Get frame-local coordinates
		System.out.format("stroke start %s %s to %s %s rad %s %s\n",
				start.x,
				start.y,
				end.x,
				end.y,
				startRadius,
				endRadius
		);

		// Calculate mark bounds
		Rectangle bounds = new BoundsBuilder()
				.circle(start, startRadius)
				.circle(end, endRadius)
				.quantize(context.tileSize)
				.buildInt();
		System.out.format("stroke bounds: %s\n", bounds);

		// Copy tiles to canvas
		TrueColorImage canvas = TrueColorImage.create(bounds.width, bounds.height);
		Rectangle tileBounds = trueColorImageNodeWrapper.render(context, canvas, trueColorImageNodeWrapper.frame, bounds, 1);

		// Do the stroke
		System.out.format("stroke %s %s to %s %s, c %s %s %s %s\n",
				start.x,
				start.y,
				end.x,
				end.y,
				color.r,
				color.g,
				color.b,
				color.a
		);
		canvas.stroke(color.r,
				color.g,
				color.b,
				color.a,
				start.x - bounds.x,
				start.y - bounds.y,
				startRadius,
				end.x - bounds.x,
				end.y - bounds.y,
				endRadius,
				brush.blend.get() / 1000.0
		);

		// Replace tiles in frame
		for (int x = 0; x < tileBounds.width; ++x) {
			for (int y = 0; y < tileBounds.height; ++y) {
				final int x0 = x;
				final int y0 = y;
				System.out.format("\tcopy %s %s: %s %s by %s %s\n",
						x0,
						y0,
						x0 * context.tileSize,
						y0 * context.tileSize,
						context.tileSize,
						context.tileSize
				);
				TrueColorImage shot =
						canvas.copy(x0 * context.tileSize, y0 * context.tileSize, context.tileSize, context.tileSize);
				context.history.change(c -> c
						.trueColorImageFrame(trueColorImageNodeWrapper.frame)
						.tilesPut(tileBounds.corner().plus(x0, y0).to1D(), Tile.create(context, shot)));
			}
		}
	}

	@Override
	public Node getProperties() {
		return properties;
	}

	@Override
	public void remove(ProjectContext context) {

	}
}
