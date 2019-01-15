package com.zarbosoft.shoedemo.structuretree;

import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;
import com.zarbosoft.shoedemo.*;
import com.zarbosoft.shoedemo.config.TrueColor;
import com.zarbosoft.shoedemo.config.TrueColorBrush;
import com.zarbosoft.shoedemo.model.*;
import com.zarbosoft.shoedemo.model.Vector;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.zarbosoft.shoedemo.HelperJFX.pad;
import static com.zarbosoft.shoedemo.Main.*;
import static com.zarbosoft.shoedemo.ProjectContext.uniqueName;
import static com.zarbosoft.shoedemo.ProjectContext.uniqueName1;
import static com.zarbosoft.shoedemo.Window.icon;

public class TrueColorImageNodeWrapper extends Wrapper {
	private final ProjectContext context;
	private final TrueColorImageNode node;
	private final TrueColorImageNode.FramesAddListener framesAddListener;
	private final TrueColorImageNode.FramesRemoveListener framesRemoveListener;
	private final TrueColorImageNode.FramesMoveToListener framesMoveListener;
	private TrueColorImageFrame frame;
	private final Wrapper parent;
	DoubleRectangle bounds;
	private int frameNumber;
	Map<Long, WrapTile> wrapTiles = new HashMap<>();
	Pane draw;
	private TrueColorImageFrame.TilesPutAllListener tilesPutListener;

	class WrapTile {
		private final ImageView widget;

		WrapTile(Tile tile, int x, int y) {
			widget = new ImageView();
			widget.setMouseTransparent(true);
			widget.setLayoutX(x);
			widget.setLayoutY(y);
			update(tile);
		}

		public void update(Tile tile) {
			widget.setImage(new Image(new ByteArrayInputStream(tile.getData(context).data())));
		}
	}

	public TrueColorImageNodeWrapper(ProjectContext context, Wrapper parent, int parentIndex, TrueColorImageNode node) {
		this.context = context;
		this.node = node;
		this.parent = parent;
		this.parentIndex = parentIndex;
		tree.set(new TreeItem<>(this));
		this.framesAddListener = node.addFramesAddListeners((target, at, value) -> setFrame(context, frameNumber));
		this.framesRemoveListener =
				node.addFramesRemoveListeners((target, at, count) -> setFrame(context, frameNumber));
		this.framesMoveListener =
				node.addFramesMoveToListeners((target, source, count, dest) -> setFrame(context, frameNumber));
	}

	@Override
	public Wrapper getParent() {
		return parent;
	}

	@Override
	public DoubleVector toInner(DoubleVector vector) {
		return vector;
	}

	@Override
	public ProjectObject getValue() {
		return node;
	}

	@Override
	public void scroll(ProjectContext context, DoubleRectangle oldBounds1, DoubleRectangle newBounds1) {
		this.bounds = newBounds1;
		System.out.format("image scroll; b %s\n", bounds);
		if (draw == null)
			return;
		Rectangle oldBounds = oldBounds1.scale(3).descaleIntOuter(context.tileSize);
		Rectangle newBounds = bounds.scale(3).descaleIntOuter(context.tileSize);

		// Remove tiles outside view bounds
		for (int x = 0; x < oldBounds.width; ++x) {
			for (int y = 0; y < oldBounds.height; ++y) {
				if (newBounds.contains(x, y))
					continue;
				long key = oldBounds.corner().to1D();
				draw.getChildren().remove(wrapTiles.get(key));
			}
		}

		// Add missing tiles in bounds
		for (int x = 0; x < newBounds.width; ++x) {
			for (int y = 0; y < newBounds.height; ++y) {
				Vector useIndexes = newBounds.corner().plus(x, y);
				long key = useIndexes.to1D();
				if (wrapTiles.containsKey(key)) {
					continue;
				}
				Tile tile = (Tile) frame.tilesGet(key);
				if (tile == null) {
					continue;
				}
				WrapTile wrapTile =
						new WrapTile(tile, useIndexes.x * context.tileSize, useIndexes.y * context.tileSize);
				wrapTiles.put(key, wrapTile);
				draw.getChildren().add(wrapTile.widget);
			}
		}
	}

	public void attachTiles() {
		frame.addTilesPutAllListeners(tilesPutListener = (target, put, remove) -> {
			for (Long key : remove) {
				WrapTile old = wrapTiles.remove(key);
				if (old != null)
					draw.getChildren().remove(old.widget);
			}
			Rectangle checkBounds = bounds.scale(3).descaleIntOuter(context.tileSize);
			for (Map.Entry<Long, TileBase> entry : put.entrySet()) {
				long key = entry.getKey();
				Vector indexes = Vector.from1D(key);
				if (!checkBounds.contains(indexes.x, indexes.y)) {
					continue;
				}
				Tile value = (Tile) entry.getValue();
				WrapTile old = wrapTiles.get(key);
				if (old != null) {
					old.update(value);
				} else {
					WrapTile wrap = new WrapTile(value, indexes.x * context.tileSize, indexes.y * context.tileSize);
					wrapTiles.put(key, wrap);
					draw.getChildren().add(wrap.widget);
				}
			}
		});
	}

	public void detachTiles() {
		frame.removeTilesPutAllListeners(tilesPutListener);
		tilesPutListener = null;
		draw.getChildren().clear();
		wrapTiles.clear();
	}

	@Override
	public WidgetHandle buildCanvas(ProjectContext context, DoubleRectangle bounds) {
		this.bounds = bounds;
		return new WidgetHandle() {
			private final ProjectNode.OpacitySetListener opacityListener;

			{
				draw = new Pane();
				this.opacityListener = node.addOpacitySetListeners((target, value) -> {
					draw.setOpacity((double) value / opacityMax);
				});
				attachTiles();
			}

			@Override
			public Node getWidget() {
				return draw;
			}

			@Override
			public void remove() {
				node.removeOpacitySetListeners(opacityListener);
				detachTiles();
				draw = null;
			}
		};
	}

	@Override
	public WidgetHandle buildCanvasProperties(ProjectContext context) {
		return new WidgetHandle() {
			private final Runnable brushesCleanup;
			private HBox box;

			{
				ToolBar brushes = new ToolBar();
				brushes.setMinWidth(0);

				MenuItem menuNew = new MenuItem("New");
				menuNew.setOnAction(e -> {
					TrueColorBrush brush = new TrueColorBrush();
					brush.name.set(uniqueName("New brush"));
					brush.useColor.set(true);
					brush.color.set(TrueColor.rgba(0, 0, 0, 255));
					brush.blend.set(1);
					brush.size.set(1);
					context.trueColorBrushes.add(brush);
				});
				MenuItem menuDelete = new MenuItem("Delete");
				menuDelete.setOnAction(e -> {
					int index = context.trueColorBrushes.indexOf(context.trueColorBrush.get());
					if (index == -1) return;
					context.trueColorBrushes.remove(index);
					if (context.trueColorBrushes.isEmpty()) {
						context.trueColorBrush.set(null);
					} else {
						context.trueColorBrush.set(context.trueColorBrushes.get(Math.max(0, index - 1)));
					}
					context.trueColorBrushes.add(index - 1, context.trueColorBrush.get());
				});
				MenuItem menuLeft = new MenuItem("Move left");
				menuLeft.setOnAction(e -> {
					int index = context.trueColorBrushes.indexOf(context.trueColorBrush.get());
					if (index == -1) return;
					if (index == 0)
						return;
					context.trueColorBrushes.remove(index);
					context.trueColorBrushes.add(index - 1, context.trueColorBrush.get());
				});
				MenuItem menuRight = new MenuItem("Move right");
				menuRight.setOnAction(e -> {
					int index = context.trueColorBrushes.indexOf(context.trueColorBrush.get());
					if (index == -1) return;
					if (index == context.trueColorBrushes.size() - 1)
						return;
					context.trueColorBrushes.remove(index);
					context.trueColorBrushes.add(index + 1, context.trueColorBrush.get());
				});

				MenuButton menuButton = new MenuButton(null, new ImageView(icon("menu.svg")));
				menuButton.getItems().addAll(menuNew, menuDelete, menuLeft, menuRight);

				Region menuSpring = new Region();
				menuSpring.setMinWidth(1);
				HBox.setHgrow(menuSpring,Priority.ALWAYS );

				ToolBar menu = new ToolBar();
				menu.getItems().addAll(menuSpring, menuButton);
				HBox.setHgrow(menu,Priority.ALWAYS );

				brushes.prefHeightProperty().bind(menu.heightProperty());

				box = new HBox();
				box.setFillHeight(true);
				box.getChildren().addAll(menu);

				brushesCleanup = mirror(context.trueColorBrushes, brushes.getItems(), b -> {
					TrueColorSwatch swatch = new TrueColorSwatch();

					Label label = new Label();
					label.textProperty().bind(b.size.divide(10.0).asString("%.1f"));
					label.setAlignment(Pos.CENTER);

					StackPane stack = new StackPane();
					stack.getChildren().addAll(swatch, label);

					ToggleButton out = new ToggleButton();
					out.setGraphic(stack);

					out.selectedProperty().addListener((observable, oldValue, newValue) -> {
						if (!newValue)
							return;
						context.trueColorBrush.set(b);
					});

					b.useColor.addListener(new ChangeListener<Boolean>() {
						{
							changed(null, null, b.useColor.get());
						}

						@Override
						public void changed(
								ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue
						) {
							SimpleObjectProperty<TrueColor> property = newValue ? b.color : context.trueColor;
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
					if (brushes.getItems().isEmpty() && box.getChildren().size() == 1) {
						box.getChildren().add(0,brushes);
						HBox.setHgrow(brushes,Priority.ALWAYS );
						HBox.setHgrow(menu,Priority.NEVER);
						menuSpring.prefHeightProperty().bind(out.heightProperty());
						menuSpring.minHeightProperty().bind(out.heightProperty());
						menu.layout();
					}
					return out;
				}, noopConsumer(), noopConsumer());
			}

			@Override
			public Node getWidget() {
				return box;
			}

			@Override
			public void remove() {
				brushesCleanup.run();
			}
		};
	}

	@Override
	public void markStart(ProjectContext context, DoubleVector start) {
		// nop
	}

	@Override
	public void mark(ProjectContext context, DoubleVector start, DoubleVector end) {
		TrueColorBrush brush = context.trueColorBrush.get();
		if (brush == null) return;
		TrueColor color = brush.useColor.get() ? brush.color.get() : context.trueColor.get();

		// Get frame-local coordinates
		System.out.format("stroke start %s %s to %s %s\n", start.x, start.y, end.x, end.y);
		start = Window.toLocal(this, start);
		end = Window.toLocal(this, end);

		// Calculate mark bounds
		final double radius = 2;
		Rectangle bounds =
				new BoundsBuilder().circle(start, radius).circle(end, radius).quantize(context.tileSize).buildInt();

		// Copy tiles to canvas
		TrueColorImage canvas = TrueColorImage.create(bounds.width, bounds.height);
		Rectangle tileBounds = render(canvas, frame, bounds, 1);

		// Do the stroke
		System.out.format("stroke %s %s to %s %s\n", start.x, start.y, end.x, end.y);
		canvas.stroke(color.r,
				color.g,
				color.b,
				color.a,
				start.x - bounds.x,
				start.y - bounds.y,
				brush.size.get() / 20.0,
				end.x - bounds.x,
				end.y - bounds.y,
				brush.size.get() / 20.0,
				brush.blend.get() / 1000.0
		);

		// Replace tiles in frame
		for (int x = 0; x < tileBounds.width; ++x) {
			for (int y = 0; y < tileBounds.height; ++y) {
				final int x0 = x;
				final int y0 = y;
				TrueColorImage shot =
						canvas.copy(x0 * context.tileSize, y0 * context.tileSize, context.tileSize, context.tileSize);
				context.history.change(c -> c
						.trueColorImageFrame(frame)
						.tilesPut(tileBounds.corner().plus(x0, y0).to1D(), Tile.create(context, shot)));
			}
		}
	}

	@Override
	public boolean addChildren(ProjectContext context, int at, List<ProjectNode> child) {
		return false;
	}

	@Override
	public void delete(ProjectContext context) {
		if (parent != null)
			parent.removeChild(context, parentIndex);
		else
			this.context.history.change(c -> c.project(this.context.project).topRemove(parentIndex, 1));
	}

	@Override
	public ProjectNode separateClone(ProjectContext context) {
		TrueColorImageNode clone = TrueColorImageNode.create(this.context);
		clone.initialNameSet(context, uniqueName1(node.name()));
		clone.initialOpacitySet(context, node.opacity());
		clone.initialFramesAdd(context, node.frames().stream().map(frame -> {
			TrueColorImageFrame newFrame = TrueColorImageFrame.create(this.context);
			newFrame.initialOffsetSet(context, frame.offset());
			newFrame.initialLengthSet(context, frame.length());
			newFrame.initialTilesPutAll(context, frame.tiles());
			return newFrame;
		}).collect(Collectors.toList()));
		return clone;
	}

	public Rectangle render(TrueColorImage gc, TrueColorImageFrame frame, Rectangle crop, double opacity) {
		Rectangle tileBounds = crop.quantize(context.tileSize);
		for (int x = 0; x < tileBounds.width; ++x) {
			for (int y = 0; y < tileBounds.height; ++y) {
				Tile tile = (Tile) frame.tilesGet(tileBounds.corner().plus(x, y).to1D());
				if (tile == null)
					continue;
				final int renderX = (x + tileBounds.x) * context.tileSize - crop.x;
				final int renderY = (y + tileBounds.y) * context.tileSize - crop.y;
				gc.compose(tile.getData(context), renderX, renderY, (float) opacity);
			}
		}
		return tileBounds;
	}

	private TrueColorImageFrame findFrame(int frameNumber) {
		return findFrame(node, frameNumber).frame;
	}

	public static class FrameResult {
		public final TrueColorImageFrame frame;
		public final int at;
		public final int frameIndex;

		public FrameResult(TrueColorImageFrame frame, int at, int frameIndex) {
			this.frame = frame;
			this.at = at;
			this.frameIndex = frameIndex;
		}
	}

	public static FrameResult findFrame(TrueColorImageNode node, int frame) {
		int at = 0;
		for (int i = 0; i < node.framesLength(); ++i) {
			TrueColorImageFrame pos = node.frames().get(i);
			if ((i == node.framesLength() - 1) || (frame >= at && (pos.length() == -1 || frame < at + pos.length()))) {
				return new FrameResult(pos, at, i);
			}
			at += pos.length();
		}
		throw new Assertion();
	}

	@Override
	public void setFrame(ProjectContext context, int frameNumber) {
		this.frameNumber = frameNumber;
		TrueColorImageFrame found = findFrame(frameNumber);
		System.out.format("set frame %s: %s vs %s\n", frameNumber, frame, found);
		if (frame != found) {
			if (draw != null) {
				detachTiles();
			}
			frame = found;
			if (draw != null) {
				attachTiles();
			}
		}
	}

	@Override
	public void remove(ProjectContext context) {
		node.removeFramesAddListeners(framesAddListener);
		node.removeFramesRemoveListeners(framesRemoveListener);
		node.removeFramesMoveToListeners(framesMoveListener);
	}

	@Override
	public WidgetHandle createProperties(ProjectContext context) {
		List<Runnable> cleanup = new ArrayList<>();
		return new WidgetHandle() {
			private final TabPane tabs;

			{
				Tab generalTab = new Tab("General");
				generalTab.setContent(pad(new WidgetFormBuilder()
						.apply(b -> cleanup.add(nodeFormFields(context, b, node)))
						.build()));

				Tab paintTab = new Tab("Paint");
				context.trueColorBrush.addListener(new ChangeListener<TrueColorBrush>() {
					{
						changed(null, null, context.trueColorBrush.get());
					}

					@Override
					public void changed(
							ObservableValue<? extends TrueColorBrush> observable,
							TrueColorBrush oldValue,
							TrueColorBrush newBrush
					) {
						if (newBrush == null) {
							paintTab.setContent(null);
							return;
						}

						TrueColorPicker colorPicker = new TrueColorPicker();
						GridPane.setHalignment(colorPicker, HPos.CENTER );
						paintTab.setContent(pad(new WidgetFormBuilder()
								.text("Name", t -> t.textProperty().bindBidirectional(newBrush.name))
								.span(() -> {
									return colorPicker;
								})
								.check("Use brush color", widget -> {
									widget.selectedProperty().bindBidirectional(newBrush.useColor);
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
												color = newBrush.color;
											else
												color = context.trueColor;

											if (pickerBindingCleanup != null) {
												pickerBindingCleanup.run();
												pickerBindingCleanup = null;
											}
											pickerBindingCleanup = CustomBinding.<TrueColor, Color>bindBidirectional(
													color,
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
								.custom("Radius", () -> {
									Pair<Node, SimpleIntegerProperty> brushSize = HelperJFX.nonlinerSlider(1, 200, 10);
									brushSize.second.bindBidirectional(newBrush.size);
									return brushSize.first;
								})
								.slider("Blend", 1, 1000, s -> {
									s.valueProperty().bindBidirectional(newBrush.blend);
								})
								.build()));
					}
				});

				tabs = new TabPane();
				tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
				tabs.getTabs().addAll(generalTab, paintTab);
			}

			@Override
			public Node getWidget() {
				return tabs;
			}

			@Override
			public void remove() {
				cleanup.forEach(c -> c.run());
			}
		};
	}

	@Override
	public void render(TrueColorImage out, int frame, Rectangle crop, double opacity) {
		render(out, findFrame(frame), crop, opacity * ((double) node.opacity() / opacityMax));
	}

	@Override
	public void removeChild(ProjectContext context, int index) {
		throw new Assertion();
	}

	@Override
	public TakesChildren takesChildren() {
		return TakesChildren.NONE;
	}
}
