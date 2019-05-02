package com.zarbosoft.pyxyzygy.app.parts.editor;

import com.google.common.base.Objects;
import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.mynative;
import com.zarbosoft.pyxyzygy.nearestneighborimageview.NearestNeighborImageView;
import com.zarbosoft.pyxyzygy.seed.model.v0.TrueColor;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.image.ImageView;

public class OnionSkin {
	final ImageView widget = NearestNeighborImageView.create();
	private final EditHandle editHandle;

	// Convenience, from relations
	private final SimpleIntegerProperty frameProp;
	private final SimpleBooleanProperty onionSkin;
	private final SimpleObjectProperty<DoubleRectangle> bounds;

	// Own
	private boolean lastOn = false;
	private int lastFrame = -1;
	private DoubleRectangle triggerBounds = new DoubleRectangle(0, 0, 0, 0);
	private TrueColor lastColor = null;

	public OnionSkin(ProjectContext context, EditHandle editHandle) {
		this.editHandle = editHandle;
		onionSkin = editHandle.getWrapper().getConfig().onionSkin;
		frameProp = editHandle.getCanvas().previousFrame;
		bounds = editHandle.getCanvas().bounds;
		onionSkin.addListener((observable, oldValue, newValue) -> render(context));
		frameProp.addListener((observable, oldValue, newValue) -> render(context));
		bounds.addListener((observable, oldValue, newValue) -> render(context));
		GUILaunch.profileConfig.onionSkinColor.addListener((observable, oldValue, newValue) -> render(context));

		editHandle.getCanvas().overlay.getChildren().add(widget);

		render(context);
	}

	public void remove() {
		editHandle.getCanvas().overlay.getChildren().remove(widget);
	}

	private void render(ProjectContext context) {
		int frame = frameProp.get();
		if (frame < 0 || bounds.get() == null || !onionSkin.get()) {
			if (!onionSkin.get())
				lastOn = false;
			widget.setImage(null);
			return;
		}
		TrueColor color = GUILaunch.profileConfig.onionSkinColor.get();
		if (lastOn == onionSkin.get() && frame == lastFrame && (
				triggerBounds.contains(bounds.get().corner()) &&
						triggerBounds.contains(bounds.get().corner().plus(bounds.get().span()))
		) && Objects.equal(color, lastColor))
			return;
		lastOn = true;
		lastFrame = frameProp.get();
		lastColor = color;
		final DoubleVector span = bounds.get().span();
		triggerBounds = new BoundsBuilder()
				.point(bounds.get().corner().minus(span.multiply(0.5)))
				.point(bounds.get().corner().plus(span.multiply(1.5)))
				.build();
		final DoubleRectangle renderBounds = new BoundsBuilder()
				.point(bounds.get().corner().minus(span))
				.point(bounds.get().corner().plus(span.multiply(2)))
				.build();
		final TrueColorImage buffer =
				TrueColorImage.create((int) renderBounds.width + 1, (int) renderBounds.height + 1);
		Render.render(context, editHandle.getWrapper().getValue(), buffer, frame, renderBounds.divideContains(1), 1);
		widget.setImage(HelperJFX.toImage(buffer, color));
		buffer.delete();
		widget.setOpacity(color.toJfx().getOpacity());
		widget.setX(renderBounds.x);
		widget.setY(renderBounds.y);
	}
}
