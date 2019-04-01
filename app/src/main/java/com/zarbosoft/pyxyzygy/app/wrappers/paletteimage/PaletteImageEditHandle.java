package com.zarbosoft.pyxyzygy.app.wrappers.paletteimage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.config.PaletteBrush;
import com.zarbosoft.pyxyzygy.app.config.PaletteImageNodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.*;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.BrushButton;
import com.zarbosoft.pyxyzygy.core.model.v0.Palette;
import com.zarbosoft.pyxyzygy.core.model.v0.PaletteColor;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectObject;
import com.zarbosoft.pyxyzygy.seed.model.v0.TrueColor;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.zarbosoft.pyxyzygy.app.Global.pasteHotkey;
import static com.zarbosoft.pyxyzygy.app.Misc.opt;
import static com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext.uniqueName;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.icon;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.pad;
import static com.zarbosoft.rendaw.common.Common.enumerate;

public class PaletteImageEditHandle extends EditHandle {
	final PaletteImageNodeWrapper wrapper;
	private final TitledPane toolPane;
	private Runnable colorPickerDisableCleanup;
	private Runnable paletteMoveDownCleanup;
	private Runnable paletteMoveUpCleanup;
	private Runnable paletteRemoveCleanup;
	private Runnable paletteAddCleanup;
	private Runnable paletteTilesCleanup;
	private Runnable colorPickerCleanup;
	List<Runnable> cleanup = new ArrayList<>();

	private final Runnable brushesCleanup;
	Group overlay;
	private final Hotkeys.Action[] actions;
	Tool tool = null;
	ContentReplacer<Node> toolProperties = new ContentReplacer<Node>() {

		@Override
		protected void innerSet(Node content) {
			toolPane.setContent(content);
		}

		@Override
		protected void innerClear() {
			toolPane.setContent(null);
		}
	};

	public final SimpleDoubleProperty mouseX = new SimpleDoubleProperty(0);
	public final SimpleDoubleProperty mouseY = new SimpleDoubleProperty(0);
	public final SimpleIntegerProperty positiveZoom = new SimpleIntegerProperty(1);

	public final Map<ProjectObject, ColorTile> tiles = new HashMap<>();

	class ColorTile extends ColorSwatch implements Garb {
		public int index;
		public final PaletteColor color;
		private final Runnable cleanupBorder;
		private final Runnable cleanupColor;

		{
			getStyleClass().add("large");
		}

		ColorTile(PaletteColor color) {
			super(2);
			this.color = color;
			this.cleanupColor = new CustomBinding.ScalarHalfBinder<TrueColor>(color::addColorSetListeners,
					color::removeColorSetListeners
			).addListener(c0 -> {
				Color c = c0.toJfx();
				colorProperty.set(c);
			});
			cleanupBorder = HelperJFX.bindStyle(this,
					"selected",
					wrapper.paletteSelectionBinder.map(o -> Optional.of(o != null && o.id() == color.id()))
			);
			addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
				wrapper.paletteSelOffsetBinder.set(index);
			});
			tiles.put(color, this);
		}

		@Override
		public void destroy(ProjectContext context, Window window) {
			cleanupBorder.run();
			cleanupColor.run();
			tiles.remove(color);
		}
	}

	private void setBrush(int brush) {
		wrapper.config.lastBrush = wrapper.config.brush.get();
		wrapper.config.brush.set(brush);
	}

	public PaletteImageEditHandle(
			ProjectContext context, Window window, final PaletteImageNodeWrapper wrapper
	) {
		this.wrapper = wrapper;

		positiveZoom.bind(wrapper.canvasHandle.zoom);

		actions = Streams.concat(Stream.of(new Hotkeys.Action(Hotkeys.Scope.CANVAS, "paste", "Paste", pasteHotkey) {
											   @Override
											   public void run(ProjectContext context, Window window) {
												   wrapper.config.tool.set(PaletteImageNodeConfig.Tool.SELECT);
												   ((ToolSelect) tool).paste(context, window);
											   }
										   },
				new Hotkeys.Action(Hotkeys.Scope.CANVAS,
						"last-brush",
						"Last brush",
						Hotkeys.Hotkey.create(KeyCode.SPACE, false, false, false)
				) {
					@Override
					public void run(ProjectContext context, Window window) {
						if (wrapper.config.tool.get() == PaletteImageNodeConfig.Tool.BRUSH) {
							if (wrapper.config.lastBrush < 0 ||
									wrapper.config.lastBrush >= GUILaunch.config.paletteBrushes.size())
								return;
							setBrush(wrapper.config.lastBrush);
						} else {
							wrapper.config.tool.set(PaletteImageNodeConfig.Tool.BRUSH);
						}
					}
				},
				new Hotkeys.Action(Hotkeys.Scope.CANVAS,
						"select",
						"Select",
						Hotkeys.Hotkey.create(KeyCode.S, false, false, false)
				) {
					@Override
					public void run(ProjectContext context, Window window) {
						wrapper.config.tool.set(PaletteImageNodeConfig.Tool.SELECT);
					}
				},
				new Hotkeys.Action(Hotkeys.Scope.CANVAS,
						"move",
						"Move",
						Hotkeys.Hotkey.create(KeyCode.M, false, false, false)
				) {
					@Override
					public void run(ProjectContext context, Window window) {
						wrapper.config.tool.set(PaletteImageNodeConfig.Tool.MOVE);
					}
				}
		), enumerate(Stream.of(KeyCode.DIGIT1,
				KeyCode.DIGIT2,
				KeyCode.DIGIT3,
				KeyCode.DIGIT4,
				KeyCode.DIGIT5,
				KeyCode.DIGIT6,
				KeyCode.DIGIT7,
				KeyCode.DIGIT8,
				KeyCode.DIGIT9,
				KeyCode.DIGIT0
		)).map(p -> new Hotkeys.Action(Hotkeys.Scope.CANVAS,
				String.format("brush-%s", p.first + 1),
				String.format("Brush %s", p.first + 1),
				Hotkeys.Hotkey.create(p.second, false, false, false)
		) {
			@Override
			public void run(ProjectContext context, Window window) {
				if (p.first >= GUILaunch.config.paletteBrushes.size())
					return;
				setBrush(p.first);
				wrapper.config.tool.set(PaletteImageNodeConfig.Tool.BRUSH);
			}
		})).toArray(Hotkeys.Action[]::new);
		for (Hotkeys.Action action : actions)
			context.hotkeys.register(action);

		// Overlay
		overlay = new Group();
		wrapper.canvasHandle.overlay.getChildren().add(overlay);

		// Move
		ToggleButton move = new ToggleButton(null, new ImageView(icon("cursor-move16.png"))) {
			@Override
			public void fire() {
				if (isSelected())
					return;
				wrapper.config.tool.set(PaletteImageNodeConfig.Tool.MOVE);
			}
		};
		move.setMaxHeight(Double.MAX_VALUE);
		move.selectedProperty().bind(wrapper.config.tool.isEqualTo(PaletteImageNodeConfig.Tool.MOVE));

		// Select
		ToggleButton select = new ToggleButton(null, new ImageView(icon("select.png"))) {
			@Override
			public void fire() {
				if (isSelected())
					return;
				wrapper.config.tool.set(PaletteImageNodeConfig.Tool.SELECT);
			}
		};
		select.setMaxHeight(Double.MAX_VALUE);
		select.selectedProperty().bind(wrapper.config.tool.isEqualTo(PaletteImageNodeConfig.Tool.SELECT));

		// Brushes
		MenuItem menuNew = new MenuItem("New");
		menuNew.setOnAction(e -> {
			PaletteBrush brush = new PaletteBrush();
			brush.name.set(uniqueName("New brush"));
			brush.size.set(20);
			GUILaunch.config.paletteBrushes.add(brush);
			if (GUILaunch.config.paletteBrushes.size() == 1) {
				setBrush(0);
			}
		});
		MenuItem menuDelete = new MenuItem("Delete");
		BooleanBinding brushSelected =
				Bindings.createBooleanBinding(() -> wrapper.config.tool.get() == PaletteImageNodeConfig.Tool.BRUSH &&
								Range
										.closedOpen(0, GUILaunch.config.paletteBrushes.size())
										.contains(wrapper.config.brush.get()),
						GUILaunch.config.paletteBrushes,
						wrapper.config.tool,
						wrapper.config.brush
				);
		menuDelete.disableProperty().bind(brushSelected);
		menuDelete.setOnAction(e -> {
			int index = GUILaunch.config.paletteBrushes.indexOf(wrapper.config.brush.get());
			GUILaunch.config.paletteBrushes.remove(index);
			if (GUILaunch.config.paletteBrushes.isEmpty()) {
				setBrush(0);
			} else {
				setBrush(Math.max(0, index - 1));
			}
		});
		MenuItem menuLeft = new MenuItem("Move left");
		menuLeft.disableProperty().bind(brushSelected);
		menuLeft.setOnAction(e -> {
			int index = wrapper.config.brush.get();
			PaletteBrush brush = GUILaunch.config.paletteBrushes.get(index);
			if (index == 0)
				return;
			GUILaunch.config.paletteBrushes.remove(index);
			GUILaunch.config.paletteBrushes.add(index - 1, brush);
		});
		MenuItem menuRight = new MenuItem("Move right");
		menuRight.disableProperty().bind(brushSelected);
		menuRight.setOnAction(e -> {
			int index = GUILaunch.config.paletteBrushes.indexOf(wrapper.config.brush.get());
			PaletteBrush brush = GUILaunch.config.paletteBrushes.get(index);
			if (index == GUILaunch.config.paletteBrushes.size() - 1)
				return;
			GUILaunch.config.paletteBrushes.remove(index);
			GUILaunch.config.paletteBrushes.add(index + 1, brush);
		});
		window.menuChildren.set(this, ImmutableList.of(menuNew, menuDelete, menuLeft, menuRight));

		HBox brushesBox = new HBox();
		brushesBox.setSpacing(3);
		brushesBox.setAlignment(Pos.CENTER_LEFT);
		brushesCleanup = Misc.mirror(GUILaunch.config.paletteBrushes,
				brushesBox.getChildren(),
				b -> new BrushButton(b.size,
						new CustomBinding.DoubleIndirectHalfBinder<Integer, List<ProjectObject>, TrueColor>(new CustomBinding.IndirectHalfBinder<>(
								b.useColor,
								(Boolean u) -> opt(u ? b.paletteOffset : wrapper.config.paletteOffset)
						),
								new CustomBinding.ListHalfBinder<>(wrapper.node.palette(), "entries"),
								(Integer i, List<ProjectObject> l) -> {
									if (i >= l.size())
										return opt(new CustomBinding.ConstHalfBinder(null));
									ProjectObject o = l.get(i);
									if (o instanceof PaletteColor) {
										return opt(new CustomBinding.ScalarHalfBinder<TrueColor>(o, "color"));
									} else
										throw new Assertion();
								}
						).map(t -> opt(t == null ? null : t.toJfx())),
						wrapper.brushBinder.map(b1 -> opt(b1 == b))
				) {
					@Override
					public void selectBrush() {
						wrapper.config.tool.set(PaletteImageNodeConfig.Tool.BRUSH);
						wrapper.config.brush.set(GUILaunch.config.paletteBrushes.indexOf(b));
					}
				},
				button -> ((BrushButton) button).destroy(context, window),
				Misc.noopConsumer()
		);

		window.toolBarChildren.set(this, ImmutableList.of(move, select, brushesBox));

		// Tab
		Palette palette = wrapper.node.palette();

		TilePane colors = new TilePane();
		HBox.setHgrow(colors, Priority.ALWAYS);
		colors.setHgap(2);
		colors.setVgap(2);
		paletteTilesCleanup = palette.mirrorEntries(colors.getChildren(),
				new Function<ProjectObject, Node>() { /* Keep as class - lambda form causes bytebuddy to go berserk */
					@Override
					public Node apply(ProjectObject e) {
						ColorTile tile;
						if (e instanceof PaletteColor) {
							tile = new ColorTile((PaletteColor) e);
						} else
							throw new Assertion();
						return tile;
					}
				},
				r0 -> {
					ColorTile r = (ColorTile) r0;
					r.destroy(context, window);
				},
				start -> {
					for (int i = start; i < colors.getChildren().size(); ++i)
						((ColorTile) colors.getChildren().get(i)).index = i;
				}
		);

		VBox tabBox = new VBox();
		tabBox.getChildren().addAll(new TitledPane("Layer",
				new WidgetFormBuilder().apply(b -> cleanup.add(Misc.nodeFormFields(context, b, wrapper))).build()
		), new TitledPane("Palette", new WidgetFormBuilder().text("Name", t -> {
			cleanup.add(CustomBinding.bindBidirectional(new CustomBinding.ScalarBinder<>(wrapper.node.palette()::addNameSetListeners,
					wrapper.node.palette()::removeNameSetListeners,
					v -> context.change(new ProjectContext.Tuple(wrapper, "palette_name"),
							c -> c.palette(wrapper.node.palette()).nameSet(v)
					)
			), new CustomBinding.PropertyBinder<>(t.textProperty())));
		}).span(() -> {
			TrueColorPicker colorPicker = new TrueColorPicker();
			colorPickerDisableCleanup =
					CustomBinding.bind(colorPicker.disableProperty(), wrapper.paletteSelOffsetBinder.map(i -> opt(i == null || i == 0)));
			GridPane.setHalignment(colorPicker, HPos.CENTER);
			colorPickerCleanup =
					CustomBinding.bindBidirectional(new CustomBinding.IndirectBinder<TrueColor>(wrapper.paletteSelectionBinder,
									e -> {
										if (e == null)
											return opt(null);
										if (e instanceof PaletteColor) {
											return opt(new CustomBinding.ScalarBinder<TrueColor>(e,
													"color",
													v -> context.change(new ProjectContext.Tuple(e, "color"),
															c -> c.paletteColor((PaletteColor) e).colorSet(v)
													)
											));
										} else
											throw new Assertion();
									}
							),
							new CustomBinding.PropertyBinder<Color>(colorPicker.colorProxyProperty).<TrueColor>bimap(c -> Optional
									.of(TrueColor.fromJfx(c)), c -> c.toJfx())
					);
			return colorPicker;
		}).span(() -> {
			Button add = HelperJFX.button("plus.png", "New color");
			Button remove = HelperJFX.button("minus.png", "Delete");
			Button moveUp = HelperJFX.button("arrow-up.png", "Move back");
			Button moveDown = HelperJFX.button("arrow-down.png", "Move next");

			VBox tools = new VBox();
			tools.setSpacing(3);
			tools.getChildren().addAll(add, remove, moveUp, moveDown);

			HBox layout = new HBox();
			layout.setSpacing(5);
			layout.getChildren().addAll(tools, colors);

			paletteAddCleanup =
					CustomBinding.bind(add.disableProperty(), wrapper.paletteSelectionBinder.map(p -> opt(p == null)));
			add.setOnAction(_e -> {
				PaletteColor selectedColor = (PaletteColor) wrapper.paletteSelectionBinder.get().get();
				PaletteColor newColor = PaletteColor.create(context);
				newColor.initialColorSet(context, selectedColor.color());
				int id = palette.nextId();
				newColor.initialIndexSet(context, id);
				context.change(null, c -> {
					c.palette(palette).nextIdSet(id + 1);
					c.palette(palette).entriesAdd(palette.entries().indexOf(selectedColor) + 1, newColor);
				});
				wrapper.paletteSelOffsetBinder.set(tiles.get(newColor).index);
			});
			paletteRemoveCleanup =
					CustomBinding.bind(remove.disableProperty(), wrapper.paletteSelOffsetBinder.map(i -> opt(i == null || i <= 0)));
			remove.setOnAction(_e -> {
				int index = wrapper.paletteSelOffsetBinder.get().get();
				if (index < 0)
					throw new Assertion();
				context.change(null, c -> c.palette(palette).entriesRemove(index, 1));
			});
			paletteMoveUpCleanup =
					CustomBinding.bind(moveUp.disableProperty(), wrapper.paletteSelOffsetBinder.map(i -> opt(i == null || i <= 1)));
			moveUp.setOnAction(e -> {
				int index = wrapper.paletteSelOffsetBinder.get().get();
				if (index < 0)
					throw new Assertion();
				int newOffset = index - 1;
				context.change(null, c -> c.palette(palette).entriesMoveTo(index, 1, newOffset));
				wrapper.paletteSelOffsetBinder.set(newOffset);
			});
			paletteMoveDownCleanup = CustomBinding.bind(moveDown.disableProperty(),
					wrapper.paletteSelOffsetBinder.map(i -> opt(i == null || i < 1 || i >= colors.getChildren().size() - 1))
			);
			moveDown.setOnAction(e -> {
				int index = wrapper.paletteSelOffsetBinder.get().get();
				if (index < 0)
					throw new Assertion();
				int newOffset = index + 1;
				context.change(null, c -> c.palette(palette).entriesMoveTo(index, 1, newOffset));
				wrapper.paletteSelOffsetBinder.set(newOffset);
			});

			return layout;
		}).build()), toolPane = new TitledPane("Tool", null));
		window.layerTabContent.set(this, pad(tabBox));

		wrapper.config.tool.addListener(new ChangeListener<PaletteImageNodeConfig.Tool>() {

			private ChangeListener<Number> brushListener;
			private ListChangeListener<PaletteBrush> brushesListener;

			{
				changed(null, null, wrapper.config.tool.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends PaletteImageNodeConfig.Tool> observable,
					PaletteImageNodeConfig.Tool oldValue,
					PaletteImageNodeConfig.Tool newValue
			) {
				if (brushListener != null) {
					wrapper.config.brush.removeListener(brushListener);
					brushListener = null;
				}
				if (brushesListener != null) {
					GUILaunch.config.paletteBrushes.removeListener(brushesListener);
					brushesListener = null;
				}
				if (newValue == PaletteImageNodeConfig.Tool.MOVE) {
					setTool(context, window, () -> {
						return new ToolMove(window, wrapper);
					});
				} else if (newValue == PaletteImageNodeConfig.Tool.SELECT) {
					setTool(context, window, () -> {
						ToolSelect out = new ToolSelect(PaletteImageEditHandle.this);
						out.setState(context, out.new StateCreate(context, window));
						return out;
					});
				} else if (newValue == PaletteImageNodeConfig.Tool.BRUSH) {
					Runnable update = new Runnable() {
						PaletteBrush lastBrush;

						@Override
						public void run() {
							int i = wrapper.config.brush.get();
							if (!Range.closedOpen(0, GUILaunch.config.paletteBrushes.size()).contains(i))
								return;
							PaletteBrush brush = GUILaunch.config.paletteBrushes.get(i);
							setTool(context, window, () -> new ToolBrush(window, PaletteImageEditHandle.this, brush));
						}
					};
					wrapper.config.brush.addListener((observable1, oldValue1, newValue1) -> update.run());
					GUILaunch.config.paletteBrushes.addListener(brushesListener = c -> update.run());
					update.run();
				} else {
					throw new Assertion();
				}
			}
		});

		cleanup.add(() -> {
			window.layerTabContent.clear(this);
		});
	}

	private void setTool(ProjectContext context, Window window, Supplier<Tool> newTool) {
		if (tool != null) {
			tool.remove(context, window);
			tool = null;
		}
		tool = newTool.get();
	}

	@Override
	public void remove(ProjectContext context, Window window) {
		if (tool != null) {
			tool.remove(context, window);
			tool = null;
		}
		if (wrapper.canvasHandle != null)
			wrapper.canvasHandle.overlay.getChildren().remove(overlay);
		brushesCleanup.run();
		paletteTilesCleanup.run();
		paletteAddCleanup.run();
		paletteRemoveCleanup.run();
		paletteMoveUpCleanup.run();
		paletteMoveDownCleanup.run();
		colorPickerCleanup.run();
		colorPickerDisableCleanup.run();
		cleanup.forEach(Runnable::run);
		for (Hotkeys.Action action : actions)
			context.hotkeys.unregister(action);
		window.menuChildren.clear(this);
		window.toolBarChildren.clear(this);
	}

	@Override
	public void cursorMoved(ProjectContext context, Window window, DoubleVector vector) {
		vector = Window.toLocal(wrapper.canvasHandle, vector);
		mouseX.set(vector.x);
		mouseY.set(vector.y);
	}

	@Override
	public Wrapper getWrapper() {
		return wrapper;
	}

	@Override
	public void markStart(ProjectContext context, Window window, DoubleVector start) {
		if (tool == null)
			return;
		Vector offset = wrapper.canvasHandle.frame.offset();
		start = Window.toLocal(wrapper.canvasHandle, start).minus(offset);
		tool.markStart(context, window, start);
	}

	@Override
	public CanvasHandle getCanvas() {
		return wrapper.canvasHandle;
	}

	@Override
	public void mark(ProjectContext context, Window window, DoubleVector start, DoubleVector end) {
		if (tool == null)
			return;
		Vector offset = wrapper.canvasHandle.frame.offset();
		start = Window.toLocal(wrapper.canvasHandle, start).minus(offset);
		end = Window.toLocal(wrapper.canvasHandle, end).minus(offset);
		tool.mark(context, window, start, end);
	}
}
