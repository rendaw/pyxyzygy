package com.zarbosoft.pyxyzygy.wrappers.truecolorimage;

import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import com.zarbosoft.pyxyzygy.*;
import com.zarbosoft.pyxyzygy.config.TrueColor;
import com.zarbosoft.pyxyzygy.config.TrueColorBrush;
import com.zarbosoft.pyxyzygy.config.TrueColorImageNodeConfig;
import com.zarbosoft.pyxyzygy.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.wrappers.group.Tool;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleDoubleProperty;
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
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.zarbosoft.pyxyzygy.ProjectContext.uniqueName;
import static com.zarbosoft.pyxyzygy.widgets.HelperJFX.icon;
import static com.zarbosoft.pyxyzygy.widgets.HelperJFX.pad;
import static com.zarbosoft.rendaw.common.Common.enumerate;

public class TrueColorImageEditHandle extends Wrapper.EditHandle {
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

	private void setBrush(int brush) {
		wrapper.config.lastBrush = wrapper.config.brush.get();
		wrapper.config.brush.set(brush);
	}

	public TrueColorImageEditHandle(
			ProjectContext context, final TrueColorImageNodeWrapper wrapper, TabPane tabPane
	) {
		this.wrapper = wrapper;

		actions = Streams.concat(Stream.of(new Hotkeys.Action(Hotkeys.Scope.EDITOR,
												   "last-brush",
												   "Last brush",
												   Hotkeys.Hotkey.create(KeyCode.SPACE, false, false, false)
										   ) {
											   @Override
											   public void run(ProjectContext context) {
												   if (wrapper.config.tool.get() == TrueColorImageNodeConfig.Tool.BRUSH) {
													   if (wrapper.config.lastBrush < 0 || wrapper.config.lastBrush >= Main.config.trueColorBrushes.size())
														   return;
													   setBrush(wrapper.config.lastBrush);
												   } else {
													   wrapper.config.tool.set(TrueColorImageNodeConfig.Tool.BRUSH);
												   }
											   }
										   },
										   new Hotkeys.Action(Hotkeys.Scope.EDITOR,
												   "select",
												   "Select",
												   Hotkeys.Hotkey.create(KeyCode.S, false, false, false)
										   ) {
											   @Override
											   public void run(ProjectContext context) {
												   wrapper.config.tool.set(TrueColorImageNodeConfig.Tool.SELECT);
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
		)).map(p -> new Hotkeys.Action(Hotkeys.Scope.EDITOR,
				String.format("brush-%s", p.first + 1),
				String.format("Brush %s", p.first + 1),
				Hotkeys.Hotkey.create(p.second, false, false, false)
		) {
			@Override
			public void run(ProjectContext context) {
				if (p.first >= Main.config.trueColorBrushes.size())
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
			Main.config.trueColorBrushes.add(brush);
			if (Main.config.trueColorBrushes.size() == 1) {
				setBrush(0);
			}
		});
		MenuItem menuDelete = new MenuItem("Delete");
		BooleanBinding brushSelected = Bindings.createBooleanBinding(
				() -> wrapper.config.tool.get() == TrueColorImageNodeConfig.Tool.BRUSH &&
						Range.closedOpen(0, Main.config.trueColorBrushes.size()).contains(wrapper.config.brush.get()),
				Main.config.trueColorBrushes,
				wrapper.config.tool,
				wrapper.config.brush
		);
		menuDelete.disableProperty().bind(brushSelected);
		menuDelete.setOnAction(e -> {
			int index = Main.config.trueColorBrushes.indexOf(wrapper.config.brush.get());
			Main.config.trueColorBrushes.remove(index);
			if (Main.config.trueColorBrushes.isEmpty()) {
				setBrush(0);
			} else {
				setBrush(Math.max(0, index - 1));
			}
		});
		MenuItem menuLeft = new MenuItem("Move left");
		menuLeft.disableProperty().bind(brushSelected);
		menuLeft.setOnAction(e -> {
			int index = wrapper.config.brush.get();
			TrueColorBrush brush = Main.config.trueColorBrushes.get(index);
			if (index == 0)
				return;
			Main.config.trueColorBrushes.remove(index);
			Main.config.trueColorBrushes.add(index - 1, brush);
		});
		MenuItem menuRight = new MenuItem("Move right");
		menuRight.disableProperty().bind(brushSelected);
		menuRight.setOnAction(e -> {
			int index = Main.config.trueColorBrushes.indexOf(wrapper.config.brush.get());
			TrueColorBrush brush = Main.config.trueColorBrushes.get(index);
			if (index == Main.config.trueColorBrushes.size() - 1)
				return;
			Main.config.trueColorBrushes.remove(index);
			Main.config.trueColorBrushes.add(index + 1, brush);
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

		brushesCleanup = Misc.mirror(Main.config.trueColorBrushes,
				brushToolbar.getItems(),
				b -> new BrushButton(context, wrapper, b),
				Misc.noopConsumer(),
				Misc.noopConsumer()
		);

		// Tab
		Tab generalTab = new Tab("Image");
		generalTab.setContent(pad(new WidgetFormBuilder()
				.apply(b -> cleanup.add(Misc.nodeFormFields(context, b, wrapper.node)))
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
					Main.config.trueColorBrushes.removeListener(brushesListener);
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
							if (!Range.closedOpen(0, Main.config.trueColorBrushes.size()).contains(i))
								return;
							TrueColorBrush brush = Main.config.trueColorBrushes.get(i);
							setTool(context, () -> new ToolBrush(context, TrueColorImageEditHandle.this, brush));
						}
					};
					wrapper.config.brush.addListener((observable1, oldValue1, newValue1) -> update.run());
					Main.config.trueColorBrushes.addListener(brushesListener = c -> update.run());
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
		mouseX.set(vector.x);
		mouseY.set(vector.y);
	}

	@Override
	public Wrapper getWrapper() {
		return wrapper;
	}

	@Override
	public void markStart(ProjectContext context, DoubleVector start) {
		if (tool == null)
			return;
		start = Window.toLocal(wrapper.canvasHandle, start);
		tool.markStart(context, start);
	}

	@Override
	public void mark(ProjectContext context, DoubleVector start, DoubleVector end) {
		if (tool == null)
			return;
		start = Window.toLocal(wrapper.canvasHandle, start);
		end = Window.toLocal(wrapper.canvasHandle, end);
		tool.mark(context, start, end);
	}

}
