package com.zarbosoft.pyxyzygy.app.wrappers.truecolorimage;

import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.config.TrueColor;
import com.zarbosoft.pyxyzygy.app.config.TrueColorBrush;
import com.zarbosoft.pyxyzygy.app.config.TrueColorImageNodeConfig;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.app.Tool;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.zarbosoft.pyxyzygy.app.ProjectContext.uniqueName;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.icon;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.pad;
import static com.zarbosoft.rendaw.common.Common.enumerate;

public class TrueColorImageEditHandle extends EditHandle {
	final TrueColorImageNodeWrapper wrapper;
	List<Runnable> cleanup = new ArrayList<>();

	private final Runnable brushesCleanup;
	private HBox box;
	Group overlay;
	final Tab paintTab;
	private final Hotkeys.Action[] actions;
	Tool tool = null;

	public final SimpleDoubleProperty mouseX = new SimpleDoubleProperty(0);
	public final SimpleDoubleProperty mouseY = new SimpleDoubleProperty(0);
	public final SimpleIntegerProperty positiveZoom = new SimpleIntegerProperty(1);

	private void setBrush(int brush) {
		wrapper.config.lastBrush = wrapper.config.brush.get();
		wrapper.config.brush.set(brush);
	}

	public TrueColorImageEditHandle(
			ProjectContext context, final TrueColorImageNodeWrapper wrapper, TabPane tabPane
	) {
		this.wrapper = wrapper;

		positiveZoom.bind(wrapper.canvasHandle.zoom);

		actions = Streams.concat(Stream.of(new Hotkeys.Action(Hotkeys.Scope.CANVAS,
				"last-brush",
				"Last brush",
				Hotkeys.Hotkey.create(KeyCode.SPACE, false, false, false)
		) {
			@Override
			public void run(ProjectContext context) {
				if (wrapper.config.tool.get() == TrueColorImageNodeConfig.Tool.BRUSH) {
					if (wrapper.config.lastBrush < 0 ||
							wrapper.config.lastBrush >= GUILaunch.config.trueColorBrushes.size())
						return;
					setBrush(wrapper.config.lastBrush);
				} else {
					wrapper.config.tool.set(TrueColorImageNodeConfig.Tool.BRUSH);
				}
			}
		}, new Hotkeys.Action(Hotkeys.Scope.CANVAS,
				"select",
				"Select",
				Hotkeys.Hotkey.create(KeyCode.S, false, false, false)
		) {
			@Override
			public void run(ProjectContext context) {
				wrapper.config.tool.set(TrueColorImageNodeConfig.Tool.SELECT);
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
			public void run(ProjectContext context) {
				if (p.first >= GUILaunch.config.trueColorBrushes.size())
					return;
				setBrush(p.first);
				wrapper.config.tool.set(TrueColorImageNodeConfig.Tool.BRUSH);
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
				wrapper.config.tool.set(TrueColorImageNodeConfig.Tool.SELECT);
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
			TrueColorBrush brush = new TrueColorBrush();
			brush.name.set(uniqueName("New brush"));
			brush.useColor.set(true);
			brush.color.set(TrueColor.rgba(0, 0, 0, 255));
			brush.blend.set(1000);
			brush.size.set(20);
			GUILaunch.config.trueColorBrushes.add(brush);
			if (GUILaunch.config.trueColorBrushes.size() == 1) {
				setBrush(0);
			}
		});
		MenuItem menuDelete = new MenuItem("Delete");
		BooleanBinding brushSelected = Bindings.createBooleanBinding(
				() -> wrapper.config.tool.get() == TrueColorImageNodeConfig.Tool.BRUSH &&
						Range.closedOpen(0, GUILaunch.config.trueColorBrushes.size()).contains(wrapper.config.brush.get()),
				GUILaunch.config.trueColorBrushes,
				wrapper.config.tool,
				wrapper.config.brush
		);
		menuDelete.disableProperty().bind(brushSelected);
		menuDelete.setOnAction(e -> {
			int index = GUILaunch.config.trueColorBrushes.indexOf(wrapper.config.brush.get());
			GUILaunch.config.trueColorBrushes.remove(index);
			if (GUILaunch.config.trueColorBrushes.isEmpty()) {
				setBrush(0);
			} else {
				setBrush(Math.max(0, index - 1));
			}
		});
		MenuItem menuLeft = new MenuItem("Move left");
		menuLeft.disableProperty().bind(brushSelected);
		menuLeft.setOnAction(e -> {
			int index = wrapper.config.brush.get();
			TrueColorBrush brush = GUILaunch.config.trueColorBrushes.get(index);
			if (index == 0)
				return;
			GUILaunch.config.trueColorBrushes.remove(index);
			GUILaunch.config.trueColorBrushes.add(index - 1, brush);
		});
		MenuItem menuRight = new MenuItem("Move right");
		menuRight.disableProperty().bind(brushSelected);
		menuRight.setOnAction(e -> {
			int index = GUILaunch.config.trueColorBrushes.indexOf(wrapper.config.brush.get());
			TrueColorBrush brush = GUILaunch.config.trueColorBrushes.get(index);
			if (index == GUILaunch.config.trueColorBrushes.size() - 1)
				return;
			GUILaunch.config.trueColorBrushes.remove(index);
			GUILaunch.config.trueColorBrushes.add(index + 1, brush);
		});

		MenuButton menuButton = new MenuButton(null, new ImageView(icon("menu.png")));
		menuButton.getItems().addAll(menuNew, menuDelete, menuLeft, menuRight);

		Region menuSpring = new Region();
		menuSpring.setMinWidth(1);
		HBox.setHgrow(menuSpring, Priority.ALWAYS);

		ToolBar menuToolbar = new ToolBar();
		menuToolbar.setMaxHeight(Double.MAX_VALUE);
		menuToolbar.getItems().addAll(menuSpring, menuButton);

		box = new HBox();
		box.setFillHeight(true);
		box.getChildren().addAll(selectToolbar, brushToolbar, menuToolbar);

		brushesCleanup = Misc.mirror(GUILaunch.config.trueColorBrushes,
				brushToolbar.getItems(),
				b -> new BrushButton(context, wrapper, b),
				Misc.noopConsumer(),
				Misc.noopConsumer()
		);

		// Tab
		Tab generalTab = new Tab("Image");
		generalTab.setContent(pad(new WidgetFormBuilder()
				.apply(b -> cleanup.add(Misc.nodeFormFields(context, b, wrapper)))
				.build()));

		paintTab = new Tab("Paint");
		wrapper.config.tool.addListener(new ChangeListener<TrueColorImageNodeConfig.Tool>() {

			private ChangeListener<Number> brushListener;
			private ListChangeListener<TrueColorBrush> brushesListener;

			{
				changed(null, null, wrapper.config.tool.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends TrueColorImageNodeConfig.Tool> observable,
					TrueColorImageNodeConfig.Tool oldValue,
					TrueColorImageNodeConfig.Tool newValue
			) {
				if (brushListener != null) {
					wrapper.config.brush.removeListener(brushListener);
					brushListener = null;
				}
				if (brushesListener != null) {
					GUILaunch.config.trueColorBrushes.removeListener(brushesListener);
					brushesListener = null;
				}
				if (newValue == TrueColorImageNodeConfig.Tool.SELECT) {
					setTool(context, () -> new ToolSelect(context, TrueColorImageEditHandle.this));
				} else if (newValue == TrueColorImageNodeConfig.Tool.BRUSH) {
					Runnable update = new Runnable() {
						TrueColorBrush lastBrush;

						@Override
						public void run() {
							int i = wrapper.config.brush.get();
							if (!Range.closedOpen(0, GUILaunch.config.trueColorBrushes.size()).contains(i))
								return;
							TrueColorBrush brush = GUILaunch.config.trueColorBrushes.get(i);
							setTool(context, () -> new ToolBrush(context, TrueColorImageEditHandle.this, brush));
						}
					};
					wrapper.config.brush.addListener((observable1, oldValue1, newValue1) -> update.run());
					GUILaunch.config.trueColorBrushes.addListener(brushesListener = c -> update.run());
					update.run();
				} else {
					throw new Assertion();
				}
			}
		});

		tabPane.getTabs().addAll(generalTab, paintTab);
		cleanup.add(() -> tabPane.getTabs().removeAll(generalTab, paintTab));
	}

	private void setTool(ProjectContext context, Supplier<Tool> newTool) {
		if (tool != null) {
			tool.remove(context);
			paintTab.setContent(null);
			tool = null;
		}
		tool = newTool.get();
	}

	@Override
	public Node getProperties() {
		return box;
	}

	@Override
	public void remove(ProjectContext context) {
		if (tool != null) {
			tool.remove(context);
			tool = null;
		}
		if (wrapper.canvasHandle != null)
			wrapper.canvasHandle.overlay.getChildren().remove(overlay);
		brushesCleanup.run();
		cleanup.forEach(Runnable::run);
		for (Hotkeys.Action action : actions)
			context.hotkeys.register(action);
	}

	@Override
	public void cursorMoved(ProjectContext context, DoubleVector vector) {
		vector = Window.toLocal(wrapper.canvasHandle,vector);
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
		tool.markStart(context, window,start);
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
		tool.mark(context, window,start, end);
	}

	@Override
	public Optional<Integer> previousFrame(int frame) {
		if (wrapper.node.framesLength() == 1) return Optional.empty();
		int p = wrapper.findFrame(wrapper.node,frame).at - 1;
		if (p == 0) p = wrapper.node.framesLength() - 1;
		return Optional.of(p);
	}
}
