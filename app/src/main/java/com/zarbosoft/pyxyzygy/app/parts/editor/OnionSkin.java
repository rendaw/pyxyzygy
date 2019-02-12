package com.zarbosoft.pyxyzygy.app.parts.editor;

import com.zarbosoft.pyxyzygy.app.DoubleRectangle;
import com.zarbosoft.pyxyzygy.app.EditHandle;
import com.zarbosoft.pyxyzygy.app.Render;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.nearestneighborimageview.NearestNeighborImageView;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class OnionSkin {
	final ImageView widget = NearestNeighborImageView.create();
	private final EditHandle editHandle;

	public OnionSkin(ProjectContext context, EditHandle editHandle) {
		this.editHandle = editHandle;
		final SimpleBooleanProperty onionSkin = editHandle.getWrapper().getConfig().onionSkin;
		final SimpleIntegerProperty frameProp = new SimpleIntegerProperty();
		editHandle.getCanvas().frameNumber.addListener(new ChangeListener<Number>() {
			{
				changed(null, null, editHandle.getCanvas().frameNumber.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends Number> observable, Number oldValue, Number newValue
			) {
				int frame = editHandle.previousFrame(editHandle.getCanvas().frameNumber.get()).orElse(-1);
				if (frameProp.get() == frame)
					return;
				frameProp.set(frame);
			}
		});
		SimpleObjectProperty<DoubleRectangle> bounds = editHandle.getCanvas().bounds;
		widget.imageProperty().bind(Bindings.createObjectBinding(() -> {
			int frame = frameProp.get();
			if (frame < 0)
				return null;
			if (bounds.get() == null)
				return null;
			if (!onionSkin.get())
				return null;
			TrueColorImage buffer = TrueColorImage.create((int) bounds.get().width + 1, (int) bounds.get().height + 1);
			Render.render(context, editHandle.getWrapper().getValue(), buffer, frame, bounds.get().quantize(1), 1);
			Image image = HelperJFX.toImage(buffer, context.config.onionSkinColor.get());
			return image;
		}, onionSkin, frameProp, bounds));
		widget.xProperty().bind(Bindings.createDoubleBinding(() -> bounds.get().x, bounds));
		widget.yProperty().bind(Bindings.createDoubleBinding(() -> bounds.get().y, bounds));
		widget.setOpacity(0.5);
		editHandle.getCanvas().overlay.getChildren().add(widget);
	}

	public void remove() {
		editHandle.getCanvas().overlay.getChildren().remove(widget);
	}
}
