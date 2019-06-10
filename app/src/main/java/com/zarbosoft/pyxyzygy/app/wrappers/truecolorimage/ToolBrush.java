package com.zarbosoft.pyxyzygy.app.wrappers.truecolorimage;

import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.Render;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.config.TrueColorBrush;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.app.widgets.TrueColorPicker;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.BinderRoot;
import com.zarbosoft.pyxyzygy.app.widgets.binding.CustomBinding;
import com.zarbosoft.pyxyzygy.app.widgets.binding.PropertyBinder;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.BaseToolBrush;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.WrapTile;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.v0.TrueColorImageFrame;
import com.zarbosoft.pyxyzygy.seed.model.v0.Rectangle;
import com.zarbosoft.pyxyzygy.seed.model.v0.TrueColor;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

import java.util.Optional;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.pad;

public class ToolBrush extends BaseToolBrush<TrueColorImageFrame, TrueColorImage> {
  final TrueColorBrush brush;
  private final TrueColorImageEditHandle editHandle;

  public ToolBrush(
      ProjectContext context,
      Window window,
      TrueColorImageEditHandle trueColorImageEditHandle,
      TrueColorBrush brush) {
    super(window, trueColorImageEditHandle.wrapper, brush);
    this.editHandle = trueColorImageEditHandle;
    this.brush = brush;
    TrueColorPicker colorPicker = new TrueColorPicker();
    GridPane.setHalignment(colorPicker, HPos.CENTER);
    editHandle.toolProperties.set(
        this,
        pad(
            new WidgetFormBuilder()
                .text("Name", t -> t.textProperty().bindBidirectional(brush.name))
                .span(
                    () -> {
                      return colorPicker;
                    })
                .check(
                    "Use brush color",
                    widget -> {
                      widget.selectedProperty().bindBidirectional(brush.useColor);
                      widget
                          .selectedProperty()
                          .addListener(
                              new ChangeListener<Boolean>() {
                                BinderRoot pickerBindingCleanup;

                                {
                                  changed(null, null, widget.isSelected());
                                }

                                @Override
                                public void changed(
                                    ObservableValue<? extends Boolean> observable,
                                    Boolean oldValue,
                                    Boolean newValue) {
                                  SimpleObjectProperty<TrueColor> color;
                                  if (newValue) color = brush.color;
                                  else color = context.config.trueColor;

                                  if (pickerBindingCleanup != null) {
                                    pickerBindingCleanup.destroy();
                                    pickerBindingCleanup = null;
                                  }
                                  pickerBindingCleanup =
                                      CustomBinding.bindBidirectional(
                                          new PropertyBinder<>(color),
                                          new PropertyBinder<>(colorPicker.colorProxyProperty)
                                              .bimap(
                                                  c -> {
                                                    TrueColor out = new TrueColor();
                                                    out.r = (byte) (c.getRed() * 255);
                                                    out.g = (byte) (c.getGreen() * 255);
                                                    out.b = (byte) (c.getBlue() * 255);
                                                    out.a = (byte) (c.getOpacity() * 255);
                                                    return Optional.of(out);
                                                  },
                                                  c -> opt(c.toJfx())));
                                }
                              });
                    })
                .custom(
                    "Size",
                    () -> {
                      Pair<Node, SimpleObjectProperty<Integer>> brushSize =
                          HelperJFX.nonlinearSlider(10, 2000, 1, 10);
                      brushSize.second.bindBidirectional(brush.size);
                      return brushSize.first;
                    })
                .slider(
                    "Blend",
                    1,
                    1000,
                    s -> {
                      s.valueProperty().bindBidirectional(brush.blend);
                    })
                .check(
                    "Hard",
                    checkBox -> {
                      checkBox.selectedProperty().bindBidirectional(brush.hard);
                    })
                .check(
                    "Aligned",
                    checkBox -> {
                      checkBox.selectedProperty().bindBidirectional(brush.aligned);
                    })
                .build()));
  }

  private void setColor(ProjectContext context, Color color) {
    if (brush.useColor.get()) {
      brush.color.set(TrueColor.fromJfx(color));
    } else {
      context.config.trueColor.set(TrueColor.fromJfx(color));
    }
  }

  @Override
  protected void stroke(
      ProjectContext context,
      TrueColorImage canvas,
      DoubleVector start,
      double startRadius,
      DoubleVector end,
      double endRadius) {
    TrueColor color = brush.useColor.get() ? brush.color.get() : context.config.trueColor.get();
    if (brush.hard.get())
      canvas.strokeHard(
          color.r,
          color.g,
          color.b,
          color.a,
          start.x,
          start.y,
          startRadius,
          end.x,
          end.y,
          endRadius,
          brush.blend.get() / 1000.0);
    else
      canvas.strokeSoft(
          color.r,
          color.g,
          color.b,
          color.a,
          start.x,
          start.y,
          startRadius,
          end.x,
          end.y,
          endRadius,
          brush.blend.get() / 1000.0);
  }

  @Override
  public void mark(
      ProjectContext context,
      Window window,
      DoubleVector start,
      DoubleVector end,
      DoubleVector globalStart,
      DoubleVector globalEnd) {
    if (false) {
      throw new Assertion();
    } else if (window.pressed.contains(KeyCode.CONTROL) && window.pressed.contains(KeyCode.SHIFT)) {
      DoubleVector outer = Window.toGlobal(editHandle.wrapper.canvasHandle, end);
      TrueColorImage out = TrueColorImage.create(1, 1);
      Render.render(
          context,
          window.getSelectedForView().getWrapper().getValue(),
          out,
          window.getSelectedForView().frameNumber.get(),
          new Rectangle((int) Math.floor(outer.x), (int) Math.floor(outer.y), 1, 1),
          1);
      setColor(context, HelperJFX.toImage(out).getPixelReader().getColor(0, 0));
    } else if (window.pressed.contains(KeyCode.CONTROL)) {
      Vector quantizedCorner = end.divide(context.tileSize).toInt();
      WrapTile tile = wrapper.canvasHandle.wrapTiles.get(quantizedCorner.to1D());
      if (tile == null) {
        setColor(context, Color.TRANSPARENT);
      } else {
        Vector intEnd = end.toInt();
        Vector tileCorner = quantizedCorner.multiply(context.tileSize);
        setColor(
            context,
            tile.widget
                .getImage()
                .getPixelReader()
                .getColor(intEnd.x - tileCorner.x, intEnd.y - tileCorner.y));
      }
    } else super.mark(context, window, start, end, globalStart, globalEnd);
  }

  @Override
  public void remove(ProjectContext context, Window window) {
    super.remove(context, window);
    editHandle.toolProperties.clear(this);
  }
}
