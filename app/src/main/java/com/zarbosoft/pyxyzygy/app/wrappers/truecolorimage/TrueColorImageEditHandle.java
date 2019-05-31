package com.zarbosoft.pyxyzygy.app.wrappers.truecolorimage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.config.TrueColorBrush;
import com.zarbosoft.pyxyzygy.app.config.TrueColorImageNodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.ContentReplacer;
import com.zarbosoft.pyxyzygy.app.widgets.TitledPane;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.IndirectHalfBinder;
import com.zarbosoft.pyxyzygy.app.wrappers.ToolMove;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.BrushButton;
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
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.zarbosoft.pyxyzygy.app.Global.pasteHotkey;
import static com.zarbosoft.pyxyzygy.app.Misc.opt;
import static com.zarbosoft.pyxyzygy.app.Misc.separateFormField;
import static com.zarbosoft.pyxyzygy.app.config.NodeConfig.TOOL_MOVE;
import static com.zarbosoft.pyxyzygy.app.config.TrueColorImageNodeConfig.TOOL_FRAME_MOVE;
import static com.zarbosoft.pyxyzygy.app.config.TrueColorImageNodeConfig.TOOL_SELECT;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.pad;
import static com.zarbosoft.rendaw.common.Common.enumerate;

public class TrueColorImageEditHandle extends EditHandle {
	final TrueColorImageNodeWrapper wrapper;
	private final TitledPane toolPane;
	List<Runnable> cleanup = new ArrayList<>();

	private final Runnable brushesCleanup;
	Group overlay;
	private final Hotkeys.Action[] actions;
	Tool tool = null;
	ContentReplacer<Node> toolProperties = new ContentReplacer<Node>() {

		@Override
		protected void innerSet(String title, Node content) {
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

	private void setBrush(int brush) {
		wrapper.config.lastBrush = wrapper.config.brush.get();
		wrapper.config.brush.set(brush);
	}

	public TrueColorImageEditHandle(
			ProjectContext context, Window window, final TrueColorImageNodeWrapper wrapper
	) {
		this.wrapper = wrapper;

		positiveZoom.bind(wrapper.canvasHandle.zoom);

		actions = Streams.concat(Stream.of(new Hotkeys.Action(Hotkeys.Scope.CANVAS, "paste", "Paste", pasteHotkey) {
			@Override
			public void run(ProjectContext context, Window window) {
				wrapper.config.tool.set(TOOL_SELECT);
				((ToolSelect) tool).paste(context, window);
			}
		}, new Hotkeys.Action(Hotkeys.Scope.CANVAS,
				"last-brush",
				"Last brush",
				Hotkeys.Hotkey.create(KeyCode.SPACE, false, false, false)
		) {
			@Override
			public void run(ProjectContext context, Window window) {
				if (wrapper.config.tool.get() == TrueColorImageNodeConfig.TOOL_BRUSH) {
					if (wrapper.config.lastBrush < 0 ||
							wrapper.config.lastBrush >= GUILaunch.profileConfig.trueColorBrushes.size())
						return;
					setBrush(wrapper.config.lastBrush);
				} else {
					wrapper.config.tool.set(TrueColorImageNodeConfig.TOOL_BRUSH);
				}
			}
		}, new Hotkeys.Action(Hotkeys.Scope.CANVAS,
				"select",
				"Select",
				Hotkeys.Hotkey.create(KeyCode.S, false, false, false)
		) {
			@Override
			public void run(ProjectContext context, Window window) {
				wrapper.config.tool.set(TOOL_SELECT);
			}
		}, new Hotkeys.Action(Hotkeys.Scope.CANVAS,
				"move",
				"Move layer",
				Hotkeys.Hotkey.create(KeyCode.M, false, false, false)
		) {
			@Override
			public void run(ProjectContext context, Window window) {
				wrapper.config.tool.set(TrueColorImageNodeConfig.TOOL_MOVE);
			}
		}, new Hotkeys.Action(Hotkeys.Scope.CANVAS,
				"move-frame",
				"Move frame contents",
				Hotkeys.Hotkey.create(KeyCode.F, false, false, false)
		) {
			@Override
			public void run(ProjectContext context, Window window) {
				wrapper.config.tool.set(TOOL_FRAME_MOVE);
			}
		}), enumerate(Stream.of(KeyCode.DIGIT1,
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
				if (p.first >= GUILaunch.profileConfig.trueColorBrushes.size())
					return;
				setBrush(p.first);
				wrapper.config.tool.set(TrueColorImageNodeConfig.TOOL_BRUSH);
			}
		})).toArray(Hotkeys.Action[]::new);
		for (Hotkeys.Action action : actions)
			context.hotkeys.register(action);

		// Overlay
		overlay = new Group();
		wrapper.canvasHandle.overlay.getChildren().add(overlay);

		Wrapper.ToolToggle move = new Wrapper.ToolToggle(wrapper, "cursor-move16.png", "Move layer", TOOL_MOVE);
		Wrapper.ToolToggle select = new Wrapper.ToolToggle(wrapper, "select.png", "Select", TOOL_SELECT);

		window.timeline.toolBoxContents.set(this,
				ImmutableList.of(new Wrapper.ToolToggle(wrapper,
						"cursor-frame-move.png",
						"Move frame contents",
						TOOL_FRAME_MOVE
				))
		);

		// Brushes
		MenuItem menuNew = new MenuItem("New");
		menuNew.setOnAction(e -> {
			TrueColorBrush brush = new TrueColorBrush();
			brush.name.set(context.namer.uniqueName("New brush"));
			brush.useColor.set(true);
			brush.color.set(TrueColor.rgba(0, 0, 0, 255));
			brush.blend.set(1000);
			brush.size.set(20);
			GUILaunch.profileConfig.trueColorBrushes.add(brush);
			if (GUILaunch.profileConfig.trueColorBrushes.size() == 1) {
				setBrush(0);
			}
		});
		MenuItem menuDelete = new MenuItem("Delete");
		BooleanBinding brushSelected =
				Bindings.createBooleanBinding(() -> wrapper.config.tool.get() == TrueColorImageNodeConfig.TOOL_BRUSH &&
								Range
										.closedOpen(0, GUILaunch.profileConfig.trueColorBrushes.size())
										.contains(wrapper.config.brush.get()),
						GUILaunch.profileConfig.trueColorBrushes,
						wrapper.config.tool,
						wrapper.config.brush
				);
		menuDelete.disableProperty().bind(brushSelected);
		menuDelete.setOnAction(e -> {
			int index = GUILaunch.profileConfig.trueColorBrushes.indexOf(wrapper.config.brush.get());
			GUILaunch.profileConfig.trueColorBrushes.remove(index);
			if (GUILaunch.profileConfig.trueColorBrushes.isEmpty()) {
				setBrush(0);
			} else {
				setBrush(Math.max(0, index - 1));
			}
		});
		MenuItem menuLeft = new MenuItem("Move left");
		menuLeft.disableProperty().bind(brushSelected);
		menuLeft.setOnAction(e -> {
			int index = wrapper.config.brush.get();
			TrueColorBrush brush = GUILaunch.profileConfig.trueColorBrushes.get(index);
			if (index == 0)
				return;
			GUILaunch.profileConfig.trueColorBrushes.remove(index);
			GUILaunch.profileConfig.trueColorBrushes.add(index - 1, brush);
		});
		MenuItem menuRight = new MenuItem("Move right");
		menuRight.disableProperty().bind(brushSelected);
		menuRight.setOnAction(e -> {
			int index = GUILaunch.profileConfig.trueColorBrushes.indexOf(wrapper.config.brush.get());
			TrueColorBrush brush = GUILaunch.profileConfig.trueColorBrushes.get(index);
			if (index == GUILaunch.profileConfig.trueColorBrushes.size() - 1)
				return;
			GUILaunch.profileConfig.trueColorBrushes.remove(index);
			GUILaunch.profileConfig.trueColorBrushes.add(index + 1, brush);
		});

		window.menuChildren.set(this, ImmutableList.of(menuNew, menuDelete, menuLeft, menuRight));

		HBox brushesBox = new HBox();
		brushesBox.setSpacing(3);
		brushesBox.setAlignment(Pos.CENTER_LEFT);
		brushesBox.setFillHeight(true);
		brushesCleanup = Misc.mirror(GUILaunch.profileConfig.trueColorBrushes, brushesBox.getChildren(), b -> {
			return new BrushButton(b.size,
					new IndirectHalfBinder<TrueColor>(b.useColor,
							(Boolean u) -> opt(u ? b.color : context.config.trueColor)
					).map(c -> opt(c.toJfx())),
					wrapper.brushBinder.map(b1 -> opt(b1 == b))
			) {
				@Override
				public void selectBrush() {
					wrapper.config.tool.set(TrueColorImageNodeConfig.TOOL_BRUSH);
					wrapper.config.brush.set(GUILaunch.profileConfig.trueColorBrushes.indexOf(b));
				}
			};
		}, Misc.noopConsumer(), Misc.noopConsumer());

		window.toolBarChildren.set(this, ImmutableList.of(move, select, brushesBox));

		// Tab
		VBox tabBox = new VBox();
		tabBox.getChildren().addAll(new TitledPane("Layer",
				new WidgetFormBuilder()
						.apply(b -> cleanup.add(Misc.nodeFormFields(context, b, wrapper)))
						.apply(b -> separateFormField(context, b, wrapper))
						.build()
		), toolPane = new TitledPane("Tool", null));
		window.layerTabContent.set(this, pad(tabBox));

		wrapper.config.tool.addListener(new ChangeListener<String>() {

			private ChangeListener<Number> brushListener;
			private ListChangeListener<TrueColorBrush> brushesListener;

			{
				changed(null, null, wrapper.config.tool.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends String> observable, String oldValue, String newValue
			) {
				if (brushListener != null) {
					wrapper.config.brush.removeListener(brushListener);
					brushListener = null;
				}
				if (brushesListener != null) {
					GUILaunch.profileConfig.trueColorBrushes.removeListener(brushesListener);
					brushesListener = null;
				}
				if (TrueColorImageNodeConfig.TOOL_MOVE.equals(newValue)) {
					setTool(context, window, () -> {
						return new ToolMove(window, wrapper);
					});
				} else if (TOOL_FRAME_MOVE.equals(newValue)) {
					setTool(context, window, () -> {
						return new ToolFrameMove(window, wrapper);
					});
				} else if (TOOL_SELECT.equals(newValue)) {
					setTool(context, window, () -> {
						ToolSelect out = new ToolSelect(TrueColorImageEditHandle.this);
						out.setState(context, out.new StateCreate(context, window));
						return out;
					});
				} else if (TrueColorImageNodeConfig.TOOL_BRUSH.equals(newValue)) {
					Runnable update = new Runnable() {
						TrueColorBrush lastBrush;

						@Override
						public void run() {
							int i = wrapper.config.brush.get();
							if (!Range.closedOpen(0, GUILaunch.profileConfig.trueColorBrushes.size()).contains(i))
								return;
							TrueColorBrush brush = GUILaunch.profileConfig.trueColorBrushes.get(i);
							setTool(context,
									window,
									() -> new ToolBrush(context, window, TrueColorImageEditHandle.this, brush)
							);
						}
					};
					wrapper.config.brush.addListener((observable1, oldValue1, newValue1) -> update.run());
					GUILaunch.profileConfig.trueColorBrushes.addListener(brushesListener = c -> update.run());
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
		cleanup.forEach(Runnable::run);
		for (Hotkeys.Action action : actions)
			context.hotkeys.unregister(action);
		window.menuChildren.clear(this);
		window.toolBarChildren.clear(this);
		window.timeline.toolBoxContents.clear(this);
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

	private Vector offset() {
		return wrapper.node.offset().plus(wrapper.canvasHandle.frame.offset());
	}

	@Override
	public void markStart(ProjectContext context, Window window, DoubleVector start) {
		if (tool == null)
			return;
		tool.markStart(context, window, Window.toLocal(wrapper.canvasHandle, start).minus(offset()), start);
	}

	@Override
	public CanvasHandle getCanvas() {
		return wrapper.canvasHandle;
	}

	@Override
	public void mark(ProjectContext context, Window window, DoubleVector start, DoubleVector end) {
		if (tool == null)
			return;
		Vector offset = offset();
		tool.mark(context,
				window,
				Window.toLocal(wrapper.canvasHandle, start).minus(offset),
				Window.toLocal(wrapper.canvasHandle, end).minus(offset),
				start,
				end
		);
	}
}
