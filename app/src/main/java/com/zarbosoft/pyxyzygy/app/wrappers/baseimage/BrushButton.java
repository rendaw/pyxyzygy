package com.zarbosoft.pyxyzygy.app.wrappers.baseimage;

import com.zarbosoft.pyxyzygy.app.CustomBinding;
import com.zarbosoft.pyxyzygy.app.Garb;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.ColorSwatch;
import javafx.beans.property.Property;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;

public abstract class BrushButton extends ToggleButton implements Garb {
	private final Runnable cleanupSelected;
	private final Runnable cleanupColor;

	public BrushButton(
			Property<Integer> size, CustomBinding.HalfBinder<Color> color, CustomBinding.HalfBinder<Boolean> selected
	) {
		getStyleClass().add("brush-button");

		ColorSwatch swatch = new ColorSwatch(1);

		Label label = new Label();
		CustomBinding.bind(label.textProperty(),
				new CustomBinding.PropertyHalfBinder<>(size).map(v -> opt(String.format("%.1f", v / 10.0))));
		label.setAlignment(Pos.CENTER);

		StackPane stack = new StackPane();
		stack.getChildren().addAll(swatch, label);

		setGraphic(stack);

		cleanupSelected = selected.addListener(b -> selectedProperty().setValue(b));

		cleanupColor = color.addListener(c -> {
			swatch.colorProperty.set(c);
				double darkness = c == null ? 0 : (1.0 - c.getBrightness()) * c.getOpacity();
				label.setTextFill(darkness > 0.5 ? Color.WHITE : Color.BLACK);
		});
	}

	@Override
	public void fire() {
		if (isSelected())
			return;
		selectBrush();
	}

	public abstract void selectBrush();

	@Override
	public void destroy(
			ProjectContext context, Window window
	) {
		cleanupSelected.run();
		cleanupColor.run();
	}
}
