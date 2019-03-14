package com.zarbosoft.pyxyzygy.app.wrappers.paletteimage;

import com.zarbosoft.pyxyzygy.app.CustomBinding;
import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.config.PaletteBrush;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.ColorSwatch;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.app.widgets.TrueColorPicker;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.BaseToolBrush;
import com.zarbosoft.pyxyzygy.core.PaletteColors;
import com.zarbosoft.pyxyzygy.core.PaletteImage;
import com.zarbosoft.pyxyzygy.core.model.v0.*;
import com.zarbosoft.pyxyzygy.seed.model.v0.TrueColor;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.ChainComparator;
import com.zarbosoft.rendaw.common.Pair;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeType;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.zarbosoft.rendaw.common.Common.iterable;

public class ToolBrush extends BaseToolBrush<PaletteImageFrame, PaletteImage> {
	final PaletteBrush brush;
	private final PaletteImageEditHandle editHandle;
	private final javafx.scene.shape.Rectangle alignedCursor = new javafx.scene.shape.Rectangle();
	private final PaletteColors nativePalette;

	public ToolBrush(
			ProjectContext context, Window window, PaletteImageEditHandle paletteImageEditHandle, PaletteBrush brush
	) {
		super(context, window, paletteImageEditHandle.wrapper, brush);
		this.editHandle = paletteImageEditHandle;
		this.brush = brush;
		this.nativePalette = context.getPaletteColors(paletteImageEditHandle.wrapper.node.palette());

		DoubleBinding sizeBinding = Bindings.createDoubleBinding(() -> brush.sizeInPixels(), brush.size);
		alignedCursor.widthProperty().bind(sizeBinding);
		alignedCursor.heightProperty().bind(sizeBinding);
		alignedCursor.setStroke(Color.BLACK);
		alignedCursor.strokeWidthProperty().bind(Bindings.divide(1.0, editHandle.positiveZoom));
		alignedCursor.setStrokeType(StrokeType.OUTSIDE);
		alignedCursor.setFill(Color.TRANSPARENT);
		alignedCursor
				.visibleProperty()
				.bind(Bindings.createBooleanBinding(() -> editHandle.positiveZoom.get() > 1, editHandle.positiveZoom));
		alignedCursor.setOpacity(0.5);
		alignedCursor.layoutXProperty().bind(Bindings.createDoubleBinding(() -> {
			return Math.floor(editHandle.mouseX.get()) - brush.sizeInPixels() / 2.0 + 0.5;
		}, editHandle.mouseX, brush.size));
		alignedCursor.layoutYProperty().bind(Bindings.createDoubleBinding(() -> {
			return Math.floor(editHandle.mouseY.get()) - brush.sizeInPixels() / 2.0 + 0.5;
		}, editHandle.mouseY, brush.size));
		this.editHandle.overlay.getChildren().add(alignedCursor);

		editHandle.toolProperties.set(this,
				new WidgetFormBuilder().text("Name", t -> t.textProperty().bindBidirectional(brush.name)).span(() -> {
					TrueColorPicker colorPicker = new TrueColorPicker();
					GridPane.setHalignment(colorPicker, HPos.CENTER);
					CustomBinding.bindBidirectionalMultiple(new CustomBinding.IndirectBinder<TrueColor>(new CustomBinding.DoubleIndirectHalfBinder<Integer, List<PaletteColor>, PaletteColor>(new CustomBinding.IndirectHalfBinder<SimpleIntegerProperty>(
									brush.useColor,
									b -> Optional.of((Boolean) b ?
											brush.index :
											editHandle.wrapper.config.index)
							),
									new CustomBinding.ListHalfBinder<PaletteColor>(editHandle.wrapper.node.palette(),
											"Entries"
									),
									(index, entries) -> {
										return entries.stream().filter(e -> e.index() == index).findFirst();
									}
							),
									e -> Optional.of(((PaletteColor) e).color()),
									(b, v) -> context.history.change(c -> c.paletteColor((PaletteColor) b).colorSet(v))
							),
							new CustomBinding.PropertyBinder<Color>(colorPicker.colorProxyProperty).<TrueColor>convert(c -> TrueColor
									.fromJfx(c), c -> c.toJfx())
					);
					return colorPicker;
				}).custom("Size", () -> {
					Pair<Node, SimpleIntegerProperty> brushSize = HelperJFX.nonlinearSlider(10, 2000, 1, 10);
					brushSize.second.bindBidirectional(brush.size);
					return brushSize.first;
				}).span(() -> {
					Palette palette = editHandle.wrapper.node.palette();
					SimpleObjectProperty<ColorSwatch> selected = new SimpleObjectProperty<>();
					Button add = HelperJFX.button("plus.png", "New color");
					Button remove = HelperJFX.button("minus.png", "Delete");
					Button moveUp = HelperJFX.button("arrow-up.png", "Move back");
					Button moveDown = HelperJFX.button("arrow-down.png", "Move next");

					VBox tools = new VBox();
					tools.getChildren().addAll(add, remove, moveUp, moveDown);

					TilePane colors = new TilePane();
					class ColorTile extends ColorSwatch {
						Runnable cleanup;
						public final PaletteColor color;

						{
							addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
								mySelect();
							});
						}

						ColorTile(PaletteColor color) {
							this.color = color;
						}

						public void mySelect() {
							if (selected.get() != this) {
								((ColorTile) selected.get()).myDeselect();
							}
							selected.set(this);
							borderProperty().set(new Border(new BorderStroke(Color.BLACK,
									BorderStrokeStyle.SOLID,
									CornerRadii.EMPTY,
									new BorderWidths(2)
							)));
							if (brush.useColor.get()) {
								brush.index.set(color.index());
							} else {
								editHandle.wrapper.config.index.set(color.index());
							}
						}

						public void myDeselect() {
							borderProperty().set(null);
						}
					}
					palette.mirrorEntries(colors.getChildren(), e -> {
						ColorTile tile;
						if (e instanceof PaletteColor) {
							tile = new ColorTile((PaletteColor) e);
							tile.cleanup =
									new CustomBinding.ScalarHalfBinder<TrueColor>(((PaletteColor) e)::addColorSetListeners,
											((PaletteColor) e)::removeColorSetListeners
									).addListener(o -> o.ifPresent(v -> tile.colorProperty.set(v.toJfx())));
						} else
							throw new Assertion();
						tile.setUserData(e);
						return tile;
					}, r0 -> {
						ColorTile r = (ColorTile) r0;
						if (r.cleanup != null)
							r.cleanup.run();
					}, i -> {
						if (i == 0)
							((ColorTile) colors.getChildren().get(0)).mySelect();
					});

					HBox layout = new HBox();
					layout.getChildren().addAll(tools, colors);

					add.disableProperty().bind(Bindings.isNull(selected));
					add.setOnAction(_e -> {
						PaletteColor selectedColor = ((ColorTile) selected.get()).color;
						PaletteColor newColor = PaletteColor.create(context);
						newColor.initialColorSet(context, selectedColor.color());
						int expect = 0;
						for (ProjectObject c : iterable(palette.entries().stream().flatMap(e -> {
							if (e instanceof PaletteColor)
								return Stream.of((PaletteColor) e);
							throw new Assertion();
						}).sorted(new ChainComparator<PaletteColor>().lesserFirst(e -> e.index()).build()))) {
							if (c instanceof PaletteColor) {
								if (((PaletteColor) c).index() != expect) {
									break;
								}
							} else
								throw new Assertion();
						}
						newColor.initialIndexSet(context, expect);
						context.history.change(c -> c
								.palette(palette)
								.entriesAdd(palette.entries().indexOf(selectedColor) + 1, newColor));
					});
					IntegerBinding selectedIndex = Bindings.createIntegerBinding(() -> {
						return colors.getChildren().indexOf(selected.get());
					}, selected);
					remove.disableProperty().bind(selectedIndex.greaterThanOrEqualTo(1));
					remove.setOnAction(_e -> {
						int index = colors.getChildren().indexOf(selected.get());
						if (index < 0)
							throw new Assertion();
						context.history.change(c -> c.palette(palette).entriesRemove(index, 1));
						if (index < colors.getChildren().size())
							selected.set((ColorSwatch) colors.getChildren().get(index));
						else
							selected.set((ColorSwatch) colors.getChildren().get(index - 1));
					});
					moveUp.disableProperty().bind(selectedIndex.greaterThanOrEqualTo(2));
					moveUp.setOnAction(e -> {
						int index = colors.getChildren().indexOf(selected.get());
						if (index < 0)
							throw new Assertion();
						context.history.change(c -> c.palette(palette).entriesMoveTo(index, 1, index - 1));
						selected.set((ColorSwatch) colors.getChildren().get(index - 1));
					});
					moveDown
							.disableProperty()
							.bind(selectedIndex
									.greaterThanOrEqualTo(1)
									.and(selectedIndex.lessThan(Bindings.size(colors.getChildren()).subtract(1))));
					moveDown.setOnAction(e -> {
						int index = colors.getChildren().indexOf(selected.get());
						if (index < 0)
							throw new Assertion();
						context.history.change(c -> c.palette(palette).entriesMoveTo(index, 1, index - 1));
						selected.set((ColorSwatch) colors.getChildren().get(index - 1));
					});

					return layout;
				}).build()
		);
	}

	@Override
	public void markStart(ProjectContext context, Window window, DoubleVector start) {

	}

	private void setColor(ProjectContext context, int index) {
		if (brush.useColor.get()) {
			brush.index.set(index);
		} else {
			editHandle.wrapper.config.index.set(index);
		}
	}

	@Override
	protected void stroke(
			ProjectContext context,
			PaletteImage canvas,
			DoubleVector start,
			double startRadius,
			DoubleVector end,
			double endRadius
	) {
		canvas.stroke((byte) editHandle.wrapper.config.index.get(),
				start.x,
				start.y,
				startRadius,
				end.x,
				end.y,
				endRadius
		);
	}

	@Override
	public void mark(ProjectContext context, Window window, DoubleVector start, DoubleVector end) {
		if (false) {
			throw new Assertion();
		} else if (window.pressed.contains(KeyCode.CONTROL)) {
			Vector quantizedCorner = end.divide(context.tileSize).toInt();
			PaletteTileBase tile = editHandle.wrapper.canvasHandle.frame.tilesGet(quantizedCorner.to1D());
			if (tile == null) {
				setColor(context, 0);
			} else {
				Vector intEnd = end.toInt();
				Vector tileCorner = quantizedCorner.multiply(context.tileSize);
				setColor(context, tile.getPixel(intEnd.x - tileCorner.x, intEnd.y - tileCorner.y));
			}
		} else
			super.mark(context, window, start, end);
	}

	@Override
	public void remove(ProjectContext context, Window window) {
		editHandle.overlay.getChildren().removeAll(alignedCursor);
		super.remove(context, window);
	}
}
