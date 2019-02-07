package com.zarbosoft.pyxyzygy.gui.parts.editor;

import com.zarbosoft.pyxyzygy.*;
import com.zarbosoft.pyxyzygy.widgets.HelperJFX;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.ImageView;

public class OnionSkin {
	final ImageView widget = HelperJFX.nearestNeighborImageView();
	private final EditHandle editHandle;
	private int frame = -1;

	public OnionSkin(ProjectContext context, EditHandle editHandle) {
		this.editHandle = editHandle;
		editHandle.getCanvas().overlay.getChildren().add(widget);
		widget.visibleProperty().bind(editHandle.getWrapper().getConfig().onionSkin);
		widget.setOpacity(0.5);
		editHandle.getCanvas().frameNumber.addListener(new ChangeListener<Number>() {
			{
				changed(null, null, editHandle.getCanvas().frameNumber.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends Number> observable1, Number oldValue1, Number frame
			) {
				editHandle.previousFrame(frame.intValue()).filter(f -> f != OnionSkin.this.frame).ifPresent(f -> {
					OnionSkin.this.frame = f;
					render(context);
				});
			}
		});
		editHandle.getCanvas().bounds.addListener((observable, oldValue, newValue) -> render(context));
		editHandle.getWrapper().getConfig().onionSkin.addListener((observable, oldValue, newValue) -> render(context));
	}

	public void remove() {
		editHandle.getCanvas().overlay.getChildren().remove(widget);
	}

	public void render(ProjectContext context) {
		if (!editHandle.getWrapper().getConfig().onionSkin.get())
			return;
		if (editHandle.getCanvas().bounds.get() == null)
			return;
		final DoubleRectangle bounds = editHandle.getCanvas().bounds.get();
		TrueColorImage buffer = TrueColorImage.create((int) bounds.width + 1, (int) bounds.height + 1);
		Render.render(context, editHandle.getWrapper().getValue(), buffer, frame, bounds.quantize(1), 1);
		widget.setImage(HelperJFX.toImage(buffer));
		widget.setX((int) bounds.x);
		widget.setY((int) bounds.y);
	}
}
