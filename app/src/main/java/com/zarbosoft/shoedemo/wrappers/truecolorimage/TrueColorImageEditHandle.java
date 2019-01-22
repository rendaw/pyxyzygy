package com.zarbosoft.shoedemo.wrappers.truecolorimage;

import com.google.common.collect.Range;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.shoedemo.Main;
import com.zarbosoft.shoedemo.ProjectContext;
import com.zarbosoft.shoedemo.WidgetFormBuilder;
import com.zarbosoft.shoedemo.Wrapper;
import com.zarbosoft.shoedemo.config.TrueColor;
import com.zarbosoft.shoedemo.config.TrueColorBrush;
import com.zarbosoft.shoedemo.config.TrueColorImageNodeConfig;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;

import static com.zarbosoft.shoedemo.HelperJFX.icon;
import static com.zarbosoft.shoedemo.HelperJFX.pad;
import static com.zarbosoft.shoedemo.Main.*;
import static com.zarbosoft.shoedemo.ProjectContext.uniqueName;

public class TrueColorImageEditHandle extends Wrapper.EditControlsHandle {
	final TrueColorImageNodeWrapper wrapper;
	List<Runnable> cleanup = new ArrayList<>();

	private final Runnable brushesCleanup;
	private HBox box;
	Group overlay;
	final Tab paintTab;

	public TrueColorImageEditHandle(
			ProjectContext context, final TrueColorImageNodeWrapper wrapper, TabPane tabPane
	) {
		this.wrapper = wrapper;
		// Overlay
		overlay = new Group();
		wrapper.canvasHandle.outerDraw.getChildren().add(overlay);

		// Select
		ToggleButton select = new ToggleButton(null, new ImageView(icon("select.svg"))) {
			@Override
			public void fire() {
				if (isSelected())
					return;
				wrapper.config.tool.set(TrueColorImageNodeConfig.Tool.SELECT);
			}
		};
		select
				.selectedProperty()
				.bind(wrapper.config.tool.isEqualTo(TrueColorImageNodeConfig.Tool.SELECT));
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
				wrapper.config.brush.set(0);
			}
		});
		MenuItem menuDelete = new MenuItem("Delete");
		BooleanBinding brushSelected =
				Bindings.createBooleanBinding(() -> wrapper.config.tool.get() ==
								TrueColorImageNodeConfig.Tool.BRUSH &&
								Range
										.closedOpen(0, Main.config.trueColorBrushes.size())
										.contains(wrapper.config.brush.get()),
						Main.config.trueColorBrushes,
						wrapper.config.tool,
						wrapper.config.brush
				);
		menuDelete.disableProperty().bind(brushSelected);
		menuDelete.setOnAction(e -> {
			int index = Main.config.trueColorBrushes.indexOf(wrapper.config.brush.get());
			Main.config.trueColorBrushes.remove(index);
			if (Main.config.trueColorBrushes.isEmpty()) {
				wrapper.config.brush.set(-1);
			} else {
				wrapper.config.brush.set(Math.max(0, index - 1));
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

		MenuButton menuButton = new MenuButton(null, new ImageView(icon("menu.svg")));
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

		brushesCleanup = mirror(Main.config.trueColorBrushes,
				brushToolbar.getItems(),
				b -> new BrushButton(context, wrapper, b),
				noopConsumer(),
				noopConsumer()
		);

		// Tab
		Tab generalTab = new Tab("Image");
		generalTab.setContent(pad(new WidgetFormBuilder()
				.apply(b -> cleanup.add(nodeFormFields(context, b, wrapper.node)))
				.build()));

		paintTab = new Tab("Paint");
		wrapper.config.tool.addListener(new ChangeListener<TrueColorImageNodeConfig.Tool>() {

			private ChangeListener<Number> brushListener;
			private ListChangeListener<TrueColorBrush> brushesListener;

			{
				changed(null,null ,wrapper.config.tool.get() );
			}

			@Override
			public void changed(
					ObservableValue<? extends TrueColorImageNodeConfig.Tool> observable,
					TrueColorImageNodeConfig.Tool oldValue,
					TrueColorImageNodeConfig.Tool newValue
			) {
				paintTab.setContent(null);
				if (wrapper.tool != null) {
					wrapper.tool.remove(context);
					wrapper.tool = null;
				}
				if (brushListener != null) {
					wrapper.config.brush.removeListener(brushListener);
					brushListener = null;
				}
				if (brushesListener != null) {
					Main.config.trueColorBrushes.removeListener(brushesListener);
					brushesListener = null;
				}
				if (newValue == TrueColorImageNodeConfig.Tool.SELECT) {
					wrapper.tool = new ToolSelect(context,TrueColorImageEditHandle.this);
				} else if (newValue == TrueColorImageNodeConfig.Tool.BRUSH) {
					Runnable update = new Runnable() {
						TrueColorBrush lastBrush;

						@Override
						public void run() {
							int i = wrapper.config.brush.get();
							if (!Range.closedOpen(0, Main.config.trueColorBrushes.size()).contains(i))
								return;
							TrueColorBrush brush = Main.config.trueColorBrushes.get(i);
							wrapper.tool = new ToolBrush(context, TrueColorImageEditHandle.this, brush);
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

	@Override
	public Node getProperties() {
		return box;
	}

	@Override
	public void remove(ProjectContext context) {
		brushesCleanup.run();
		cleanup.forEach(Runnable::run);
	}
}
