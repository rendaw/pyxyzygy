package com.zarbosoft.pyxyzygy.app.wrappers.truecolorimage;

import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.config.TrueColor;
import com.zarbosoft.pyxyzygy.app.config.TrueColorBrush;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.app.widgets.TrueColorPicker;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.seed.model.v0.Rectangle;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeType;

import java.util.Optional;

import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.pad;

public class ToolBrush extends Tool {
	final TrueColorBrush brush;
	private final TrueColorImageEditHandle editHandle;
	private final javafx.scene.shape.Rectangle cursor = new javafx.scene.shape.Rectangle();
	private DoubleVector lastEnd;

	public ToolBrush(
			ProjectContext context, TrueColorImageEditHandle trueColorImageEditHandle, TrueColorBrush brush
	) {
		this.editHandle = trueColorImageEditHandle;
		this.brush = brush;

		DoubleBinding sizeBinding = Bindings.createDoubleBinding(() -> brush.sizeInPixels(), brush.size);
		cursor.widthProperty().bind(sizeBinding);
		cursor.heightProperty().bind(sizeBinding);
		cursor.setStroke(Color.BLACK);
		cursor.strokeWidthProperty().bind(Bindings.divide(1.0, editHandle.positiveZoom));
		cursor.setStrokeType(StrokeType.OUTSIDE);
		cursor.setFill(Color.TRANSPARENT);
		cursor
				.visibleProperty()
				.bind(Bindings.createBooleanBinding(() -> brush.aligned.get() &&
								brush.hard.get() &&
								editHandle.positiveZoom.get() > 1,
						editHandle.positiveZoom,
						brush.hard,
						brush.aligned
				));
		cursor.setOpacity(0.5);
		cursor.layoutXProperty().bind(Bindings.createDoubleBinding(() -> {
			return Math.floor(editHandle.mouseX.get()) - brush.sizeInPixels() / 2.0 + 0.5;
		}, editHandle.mouseX, brush.size, brush.aligned));
		cursor.layoutYProperty().bind(Bindings.createDoubleBinding(() -> {
			return Math.floor(editHandle.mouseY.get()) - brush.sizeInPixels() / 2.0 + 0.5;
		}, editHandle.mouseY, brush.size, brush.aligned));
		this.editHandle.overlay.getChildren().add(cursor);

		TrueColorPicker colorPicker = new TrueColorPicker();
		GridPane.setHalignment(colorPicker, HPos.CENTER);
		trueColorImageEditHandle.paintTab.setContent(pad(new WidgetFormBuilder()
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
								ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue
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
				.check("Hard", checkBox -> {
					checkBox.selectedProperty().bindBidirectional(brush.hard);
				})
				.check("Aligned", checkBox -> {
					checkBox.selectedProperty().bindBidirectional(brush.aligned);
				})
				.build()));
	}

	@Override
	public void markStart(ProjectContext context, Window window, DoubleVector start) {

	}

	private void setColor(ProjectContext context, Color color) {
		if (brush.useColor.get()) {
			brush.color.set(TrueColor.fromJfx(color));
		} else {
			context.config.trueColor.set(TrueColor.fromJfx(color));
		}
	}

	private void stroke(ProjectContext context, DoubleVector start, DoubleVector end) {
		TrueColor color = brush.useColor.get() ? brush.color.get() : context.config.trueColor.get();

		final double startRadius = brush.size.get() / 20.0;
		final double endRadius = brush.size.get() / 20.0;

		// Calculate mark bounds
		Rectangle bounds = new BoundsBuilder()
				.circle(start, startRadius)
				.circle(end, endRadius)
				.quantize(context.tileSize)
				.buildInt();

		// Copy tiles to canvas
		TrueColorImage canvas = TrueColorImage.create(bounds.width, bounds.height);
		Rectangle tileBounds = editHandle.wrapper.canvasHandle.render(context, canvas, bounds);

		// Do the stroke
		if (brush.hard.get())
			canvas.strokeHard(color.r,
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
		else
			canvas.strokeSoft(color.r,
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
		editHandle.wrapper.canvasHandle.drop(context, tileBounds, canvas);

		lastEnd = end;
	}

	@Override
	public void mark(ProjectContext context, Window window, DoubleVector start, DoubleVector end) {
		if (false) {
			throw new Assertion();
		} else if (window.pressed.contains(KeyCode.CONTROL) && window.pressed.contains(KeyCode.SHIFT)) {
			DoubleVector outer = Window.toGlobal(editHandle.wrapper.canvasHandle, end);
			TrueColorImage out = TrueColorImage.create(1, 1);
			Render.render(context,
					window.selectedForView.get().getWrapper().getValue(),
					out,
					window.selectedForView.get().frameNumber.get(),
					new Rectangle((int) Math.floor(outer.x), (int) Math.floor(outer.y), 1, 1),
					1
			);
			setColor(context, HelperJFX.toImage(out).getPixelReader().getColor(0, 0));
		} else if (window.pressed.contains(KeyCode.SHIFT)) {
			if (lastEnd == null)
				lastEnd = end;
			stroke(context, lastEnd, end);
		} else if (window.pressed.contains(KeyCode.CONTROL)) {
			Vector quantizedCorner = end.divide(context.tileSize).toInt();
			WrapTile tile = editHandle.wrapper.canvasHandle.wrapTiles.get(quantizedCorner.to1D());
			if (tile == null) {
				setColor(context, Color.TRANSPARENT);
			} else {
				Vector intEnd = end.toInt();
				Vector tileCorner = quantizedCorner.multiply(context.tileSize);
				setColor(context,
						tile.widget
								.getImage()
								.getPixelReader()
								.getColor(intEnd.x - tileCorner.x, intEnd.y - tileCorner.y)
				);
			}
		} else
			stroke(context, start, end);
	}

	@Override
	public void remove(ProjectContext context) {
		editHandle.overlay.getChildren().removeAll(cursor);
	}
}
