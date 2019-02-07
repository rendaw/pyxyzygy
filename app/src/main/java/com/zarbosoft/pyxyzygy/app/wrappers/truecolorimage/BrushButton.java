package com.zarbosoft.pyxyzygy.app.wrappers.truecolorimage;

import com.zarbosoft.pyxyzygy.app.GUILaunch;
import com.zarbosoft.pyxyzygy.app.ProjectContext;
import com.zarbosoft.pyxyzygy.app.config.TrueColor;
import com.zarbosoft.pyxyzygy.app.config.TrueColorBrush;
import com.zarbosoft.pyxyzygy.app.config.TrueColorImageNodeConfig;
import com.zarbosoft.pyxyzygy.app.widgets.TrueColorSwatch;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class BrushButton extends ToggleButton {
	private final TrueColorBrush brush;
	private final TrueColorImageNodeWrapper trueColorImageNodeWrapper;

	public BrushButton(
			ProjectContext context, TrueColorImageNodeWrapper trueColorImageNodeWrapper, TrueColorBrush b
	) {
		this.trueColorImageNodeWrapper = trueColorImageNodeWrapper;
		this.brush = b;
		getStyleClass().add("brush-button");

		TrueColorSwatch swatch = new TrueColorSwatch();

		Label label = new Label();
		label.textProperty().bind(b.size.divide(10.0).asString("%.1f"));
		label.setAlignment(Pos.CENTER);

		StackPane stack = new StackPane();
		stack.getChildren().addAll(swatch, label);

		setGraphic(stack);

		selectedProperty().bind(Bindings.createBooleanBinding(
				() -> trueColorImageNodeWrapper.config.tool.get() == TrueColorImageNodeConfig.Tool.BRUSH &&
						trueColorImageNodeWrapper.config.brush.get() == GUILaunch.config.trueColorBrushes.indexOf(b),
				trueColorImageNodeWrapper.config.tool,
				trueColorImageNodeWrapper.config.brush,
				GUILaunch.config.trueColorBrushes
		));

		b.useColor.addListener(new ChangeListener<Boolean>() {
			{
				changed(null, null, b.useColor.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue
			) {
				SimpleObjectProperty<TrueColor> property = newValue ? b.color : context.config.trueColor;
				swatch.colorProperty.bind(Bindings.createObjectBinding(() -> property.get().toJfx(), property));
				label.textFillProperty().bind(Bindings.createObjectBinding(() -> {
					Color c = property.get().toJfx();
					double darkness = (1.0 - c.getBrightness()) * c.getOpacity();
					return darkness > 0.5 ? Color.WHITE : Color.BLACK;
				}, property));
			}
		});
	}

	@Override
	public void fire() {
		if (isSelected())
			return;
		trueColorImageNodeWrapper.config.tool.set(TrueColorImageNodeConfig.Tool.BRUSH);
		trueColorImageNodeWrapper.config.brush.set(GUILaunch.config.trueColorBrushes.indexOf(brush));
	}
}
