package com.zarbosoft.pyxyzygy.app.parts.editor;

import com.google.common.base.Objects;
import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.parts.timeline.Timeline;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.nearestneighborimageview.NearestNeighborImageView;
import com.zarbosoft.pyxyzygy.seed.model.v0.Rectangle;
import com.zarbosoft.pyxyzygy.seed.model.v0.TrueColor;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;

public class OnionSkin {
	final ImageView widget = NearestNeighborImageView.create();
	private final EditHandle editHandle;

	// Convenience, from relations
	private final SimpleIntegerProperty frameProp;
	private final SimpleObjectProperty<DoubleRectangle> bounds;
	private final Runnable onCleanup;
	private final CustomBinding.HalfBinder<Boolean> on;
	private final Group overlay;

	// Own
	private boolean lastOn = false;
	private int lastFrame = -1;
	private DoubleRectangle triggerBounds = new DoubleRectangle(0, 0, 0, 0);
	private TrueColor lastColor = null;

	public OnionSkin(ProjectContext context, Timeline timeline, EditHandle editHandle) {
		this.editHandle = editHandle;
		frameProp = editHandle.getCanvas().previousFrame;
		bounds = editHandle.getCanvas().bounds;
		on = new CustomBinding.DoubleHalfBinder<>(
				new CustomBinding.PropertyHalfBinder<>(editHandle.getWrapper().getConfig().onionSkin),
				new CustomBinding.PropertyHalfBinder<>(timeline.playingProperty)
		).map((on0, playing) -> opt(on0 && !playing));
		onCleanup = on.addListener(v -> render(context));
		frameProp.addListener((observable, oldValue, newValue) -> render(context));
		bounds.addListener((observable, oldValue, newValue) -> render(context));
		GUILaunch.profileConfig.onionSkinColor.addListener((observable, oldValue, newValue) -> render(context));

		this.overlay = editHandle.getCanvas().overlay;
		overlay.getChildren().add(widget);

		render(context);
	}

	public void remove() {
		onCleanup.run();
		overlay.getChildren().remove(widget);
	}

	private void render(ProjectContext context) {
		int frame = frameProp.get();
		boolean on = this.on.get().get();
		if (frame < 0 || bounds.get() == null || !on) {
			if (!on)
				lastOn = false;
			widget.setImage(null);
			return;
		}
		TrueColor color = GUILaunch.profileConfig.onionSkinColor.get();
		if (lastOn == on && frame == lastFrame && (
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
		final Rectangle renderBounds = new BoundsBuilder()
				.point(bounds.get().corner().minus(span))
				.point(bounds.get().corner().plus(span.multiply(2)))
				.build()
				.divideContains(1);
		if (renderBounds.height == 0 || renderBounds.width == 0)
			return;
		Image image;
		final TrueColorImage buffer = TrueColorImage.create(renderBounds.width, renderBounds.height);
		try {
			Render.render(context,
					editHandle.getWrapper().getValue(),
					buffer,
					frame,
					renderBounds.divideContains(1),
					1
			);
			image = HelperJFX.toImage(buffer, color);
		} finally {
			buffer.delete();
		}
		widget.setImage(image);
		widget.setOpacity(color.toJfx().getOpacity());
		widget.setX(renderBounds.x);
		widget.setY(renderBounds.y);
	}
}
