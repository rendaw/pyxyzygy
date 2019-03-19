package com.zarbosoft.pyxyzygy.app.wrappers.paletteimage;

import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.config.PaletteBrush;
import com.zarbosoft.pyxyzygy.app.config.PaletteImageNodeConfig;
import com.zarbosoft.pyxyzygy.app.config.TrueColorImageNodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.ContentReplacer;
import com.zarbosoft.pyxyzygy.app.widgets.TrueColorPicker;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.BrushButton;
import com.zarbosoft.pyxyzygy.core.model.v0.PaletteColor;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectObject;
import com.zarbosoft.pyxyzygy.seed.model.v0.TrueColor;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.zarbosoft.pyxyzygy.app.Global.pasteHotkey;
import static com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext.uniqueName;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.icon;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.pad;
import static com.zarbosoft.rendaw.common.Common.enumerate;

public class PaletteImageEditHandle extends EditHandle {
	final PaletteImageNodeWrapper wrapper;
	List<Runnable> cleanup = new ArrayList<>();

	private final Runnable brushesCleanup;
	private HBox toolbarBox;
	Group overlay;
	private final Hotkeys.Action[] actions;
	Tool tool = null;
	ContentReplacer toolProperties = new ContentReplacer();

	public final SimpleDoubleProperty mouseX = new SimpleDoubleProperty(0);
	public final SimpleDoubleProperty mouseY = new SimpleDoubleProperty(0);
	public final SimpleIntegerProperty positiveZoom = new SimpleIntegerProperty(1);

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
															   wrapper.config.lastBrush >=
																	   GUILaunch.config.paletteBrushes.size())
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

		// Select
		ToggleButton select = new ToggleButton(null, new ImageView(icon("select.png"))) {
			@Override
			public void fire() {
				if (isSelected())
					return;
				wrapper.config.tool.set(PaletteImageNodeConfig.Tool.SELECT);
			}
		};
		select.selectedProperty().bind(wrapper.config.tool.isEqualTo(TrueColorImageNodeConfig.Tool.SELECT));
		ToolBar selectToolbar = new ToolBar();
		selectToolbar.setMaxHeight(Double.MAX_VALUE);
		selectToolbar.getItems().addAll(select);

		// Brushes
		ToolBar brushToolbar = new ToolBar();
		brushToolbar.setMaxHeight(Double.MAX_VALUE);
		HBox.setHgrow(brushToolbar, Priority.ALWAYS);

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

		MenuButton menuButton = new MenuButton(null, new ImageView(icon("menu.png")));
		menuButton.getItems().addAll(menuNew, menuDelete, menuLeft, menuRight);

		Region menuSpring = new Region();
		menuSpring.setMinWidth(1);
		HBox.setHgrow(menuSpring, Priority.ALWAYS);

		ToolBar menuToolbar = new ToolBar();
		menuToolbar.setMaxHeight(Double.MAX_VALUE);
		menuToolbar.getItems().addAll(menuSpring, menuButton);

		toolbarBox = new HBox();
		toolbarBox.setFillHeight(true);
		toolbarBox.getChildren().addAll(selectToolbar, brushToolbar, menuToolbar);
		brushesCleanup =
				Misc.mirror(GUILaunch.config.paletteBrushes, brushToolbar.getItems(), b -> new BrushButton(b.size,
						new CustomBinding.DoubleIndirectHalfBinder<Integer, List<ProjectObject>, TrueColor>(
								new CustomBinding.IndirectHalfBinder<Integer>(
										b.useColor,
										(Boolean u) -> Optional.of(u ? b.index : wrapper.config.index)
								),
								new CustomBinding.ListHalfBinder<ProjectObject>(wrapper.node.palette(), "Entries"),
								(Integer i, List<ProjectObject> l) -> {
									ProjectObject o = l.get(i);
									if (o instanceof PaletteColor) {
										return Optional.of(new CustomBinding.ScalarHalfBinder<TrueColor>(o, "color"));
									} else
										throw new Assertion();
								}
						).map(t -> Optional.of(t.toJfx())),
						Bindings.createBooleanBinding(() -> wrapper.config.tool.get() ==
										PaletteImageNodeConfig.Tool.BRUSH &&
										wrapper.config.brush.get() == GUILaunch.config.paletteBrushes.indexOf(b),
								wrapper.config.tool,
								wrapper.config.brush,
								GUILaunch.config.paletteBrushes
						)
				) {
					@Override
					public void selectBrush() {
						wrapper.config.tool.set(PaletteImageNodeConfig.Tool.BRUSH);
						wrapper.config.brush.set(GUILaunch.config.trueColorBrushes.indexOf(b));
					}
				}, Misc.noopConsumer(), Misc.noopConsumer());

		// Tab
		TrueColorPicker color = new TrueColorPicker();
		VBox tabBox = new VBox();
		tabBox.getChildren().addAll(new Label("Layer"),
				new WidgetFormBuilder()
						.apply(b -> cleanup.add(Misc.nodeFormFields(context, b, wrapper)))
						.text("Palette name", t -> {
							cleanup.add(CustomBinding.bindBidirectionalMultiple(new CustomBinding.ScalarBinder<>(wrapper.node
									.palette()::addNameSetListeners,
									wrapper.node.palette()::removeNameSetListeners,
									v -> context.history.change(c -> c.palette(wrapper.node.palette()).nameSet(v))
							), new CustomBinding.PropertyBinder<>(t.textProperty())));
						})
						.span(() -> color)
						.build(),
				new Label("Tool"),
				toolProperties
		);
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
				if (newValue == PaletteImageNodeConfig.Tool.SELECT) {
					setTool(context, window, () -> new ToolSelect(context, window, PaletteImageEditHandle.this));
				} else if (newValue == PaletteImageNodeConfig.Tool.BRUSH) {
					Runnable update = new Runnable() {
						PaletteBrush lastBrush;

						@Override
						public void run() {
							int i = wrapper.config.brush.get();
							if (!Range.closedOpen(0, GUILaunch.config.paletteBrushes.size()).contains(i))
								return;
							PaletteBrush brush = GUILaunch.config.paletteBrushes.get(i);
							setTool(context,
									window,
									() -> new ToolBrush(context, window, PaletteImageEditHandle.this, brush)
							);
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
	public Node getProperties() {
		return toolbarBox;
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
		cleanup.forEach(Runnable::run);
		for (Hotkeys.Action action : actions)
			context.hotkeys.unregister(action);
	}

	@Override
	public void cursorMoved(ProjectContext context, DoubleVector vector) {
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
		start = Window.toLocal(wrapper.canvasHandle, start);
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
		start = Window.toLocal(wrapper.canvasHandle, start);
		end = Window.toLocal(wrapper.canvasHandle, end);
		tool.mark(context, window, start, end);
	}
}
