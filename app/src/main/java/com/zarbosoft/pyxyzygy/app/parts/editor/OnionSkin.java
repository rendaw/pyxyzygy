package com.zarbosoft.pyxyzygy.app.parts.editor;

import com.google.common.base.Objects;
import com.zarbosoft.javafxbinders.BinderRoot;
import com.zarbosoft.javafxbinders.DoubleHalfBinder;
import com.zarbosoft.javafxbinders.HalfBinder;
import com.zarbosoft.javafxbinders.PropertyHalfBinder;
import com.zarbosoft.pyxyzygy.app.BoundsBuilder;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.DoubleRectangle;
import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.EditHandle;
import com.zarbosoft.pyxyzygy.app.GUILaunch;
import com.zarbosoft.pyxyzygy.app.Render;
import com.zarbosoft.pyxyzygy.app.parts.timeline.Timeline;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.nearestneighborimageview.NearestNeighborImageView;
import com.zarbosoft.pyxyzygy.seed.Rectangle;
import com.zarbosoft.pyxyzygy.seed.TrueColor;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import static com.zarbosoft.rendaw.common.Common.opt;

public class OnionSkin {
  final ImageView widget = NearestNeighborImageView.create();
  private final EditHandle editHandle;

  // Convenience, from relations
  private final SimpleIntegerProperty frameProp;
  private final SimpleObjectProperty<DoubleRectangle> bounds;
  private final BinderRoot onCleanup;
  private final HalfBinder<Boolean> on;
  private final Group overlay;
  private final SimpleObjectProperty<TrueColor> colorProperty;

  // Own
  private boolean lastOn = false;
  private int lastFrame = -1;
  private DoubleRectangle triggerBounds = new DoubleRectangle(0, 0, 0, 0);
  private TrueColor lastColor = null;

  public OnionSkin(Context context, Timeline timeline, EditHandle editHandle, boolean previous) {
    this.editHandle = editHandle;
    frameProp = previous ? timeline.previousFrame : timeline.nextFrame;
    bounds = editHandle.getCanvas().bounds;
    on =
        new DoubleHalfBinder<>(
                new PropertyHalfBinder<>(
                    previous
                        ? editHandle.getWrapper().getConfig().onionLeft
                        : editHandle.getWrapper().getConfig().onionRight),
                new PropertyHalfBinder<>(timeline.playingProperty))
            .map((on0, playing) -> opt(on0 && !playing));
    frameProp.addListener((observable, oldValue, newValue) -> render(context));
    bounds.addListener((observable, oldValue, newValue) -> render(context));
    colorProperty =
        previous
            ? GUILaunch.profileConfig.ghostPreviousColor
            : GUILaunch.profileConfig.ghostNextColor;
    colorProperty.addListener((observable, oldValue, newValue) -> render(context));

    this.overlay = editHandle.getCanvas().overlay;
    overlay.getChildren().add(widget);

    onCleanup = on.addListener(v -> render(context));

    render(context);
  }

  public void remove() {
    onCleanup.destroy();
    overlay.getChildren().remove(widget);
  }

  private void render(Context context) {
    int frame = frameProp.get();
    boolean on = this.on.get().get();
    if (frame < 0 || bounds.get() == null || !on) {
      if (!on) lastOn = false;
      widget.setImage(null);
      return;
    }
    TrueColor color = colorProperty.get();
    if (lastOn == on
        && frame == lastFrame
        && (triggerBounds.contains(bounds.get().corner())
            && triggerBounds.contains(bounds.get().corner().plus(bounds.get().span())))
        && Objects.equal(color, lastColor)) return;
    lastOn = true;
    lastFrame = frameProp.get();
    lastColor = color;
    final DoubleVector span = bounds.get().span();
    triggerBounds =
        new BoundsBuilder()
            .point(bounds.get().corner().minus(span.multiply(0.5)))
            .point(bounds.get().corner().plus(span.multiply(1.5)))
            .build();
    final Rectangle renderBounds =
        new BoundsBuilder()
            .point(bounds.get().corner().minus(span))
            .point(bounds.get().corner().plus(span.multiply(2)))
            .build()
            .divideContains(1);
    if (renderBounds.height == 0 || renderBounds.width == 0) return;
    Image image;
    final TrueColorImage buffer = TrueColorImage.create(renderBounds.width, renderBounds.height);
    try {
      Render.render(
          context,
          editHandle.getWrapper().getValue(),
          buffer,
          frame,
          renderBounds.divideContains(1),
          1);
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
