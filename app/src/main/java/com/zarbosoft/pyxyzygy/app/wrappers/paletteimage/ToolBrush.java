package com.zarbosoft.pyxyzygy.app.wrappers.paletteimage;

import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.config.PaletteBrush;
import com.zarbosoft.pyxyzygy.app.model.v0.PaletteTile;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.BaseToolBrush;
import com.zarbosoft.pyxyzygy.core.PaletteImage;
import com.zarbosoft.pyxyzygy.core.model.v0.PaletteColor;
import com.zarbosoft.pyxyzygy.core.model.v0.PaletteImageFrame;
import com.zarbosoft.pyxyzygy.core.model.v0.PaletteSeparator;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectObject;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Common;
import com.zarbosoft.rendaw.common.Pair;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.effect.BlendMode;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeType;

import static com.zarbosoft.pyxyzygy.app.Misc.unopt;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.pad;

public class ToolBrush extends BaseToolBrush<PaletteImageFrame, PaletteImage> {
  final PaletteBrush brush;
  private final PaletteImageEditHandle editHandle;
  private final javafx.scene.shape.Rectangle alignedCursor = new javafx.scene.shape.Rectangle();

  public ToolBrush(
      Window window, PaletteImageEditHandle paletteImageEditHandle, PaletteBrush brush) {
    super(window, paletteImageEditHandle.wrapper, brush);
    this.editHandle = paletteImageEditHandle;
    this.brush = brush;

    DoubleBinding sizeBinding =
        Bindings.createDoubleBinding(() -> brush.sizeInPixels(), brush.size);
    alignedCursor.widthProperty().bind(sizeBinding);
    alignedCursor.heightProperty().bind(sizeBinding);
    alignedCursor.setBlendMode(BlendMode.DIFFERENCE);
    alignedCursor.setStroke(Color.GRAY);
    alignedCursor.strokeWidthProperty().bind(Bindings.divide(1.0, editHandle.positiveZoom));
    alignedCursor.setStrokeType(StrokeType.OUTSIDE);
    alignedCursor.setFill(Color.TRANSPARENT);
    alignedCursor
        .visibleProperty()
        .bind(
            Bindings.createBooleanBinding(
                () -> editHandle.positiveZoom.get() > 1, editHandle.positiveZoom));
    alignedCursor.setOpacity(0.8);
    alignedCursor
        .layoutXProperty()
        .bind(
            Bindings.createDoubleBinding(
                () -> {
                  return Math.floor(editHandle.mouseX.get()) - brush.sizeInPixels() / 2.0 + 0.5;
                },
                editHandle.mouseX,
                brush.size));
    alignedCursor
        .layoutYProperty()
        .bind(
            Bindings.createDoubleBinding(
                () -> {
                  return Math.floor(editHandle.mouseY.get()) - brush.sizeInPixels() / 2.0 + 0.5;
                },
                editHandle.mouseY,
                brush.size));
    this.editHandle.overlay.getChildren().add(alignedCursor);

    editHandle.toolProperties.set(
        this,
        pad(
            new WidgetFormBuilder()
                .text("Name", t -> t.textProperty().bindBidirectional(brush.name))
                .custom(
                    "Size",
                    () -> {
                      Pair<Node, SimpleObjectProperty<Integer>> brushSize =
                          HelperJFX.nonlinearSlider(10, 2000, 1, 10);
                      brushSize.second.bindBidirectional(brush.size);
                      return brushSize.first;
                    })
                .check(
                    "Use brush color",
                    widget -> {
                      widget.selectedProperty().bindBidirectional(brush.useColor);
                    })
                .build()));
  }

  @Override
  public void markStart(
      ProjectContext context, Window window, DoubleVector start, DoubleVector globalStart) {}

  private void setColor(int index) {
    editHandle.wrapper.node.palette().entries().stream()
        .map(new Common.Enumerator<>())
        .filter(
            pair -> {
              if (pair.second instanceof PaletteColor) {
                return ((PaletteColor) pair.second).index() == index;
              } else if (pair.second instanceof PaletteSeparator) {
                return false;
              } else throw new Assertion();
            })
        .findFirst()
        .ifPresent(
            pair -> {
              if (brush.useColor.get()) {
                brush.paletteOffset.set(pair.first);
              } else {
                editHandle.wrapper.config.paletteOffset.set(pair.first);
              }
            });
  }

  @Override
  protected void stroke(
      ProjectContext context,
      PaletteImage canvas,
      DoubleVector start,
      double startRadius,
      DoubleVector end,
      double endRadius) {
    ProjectObject paletteSelection = unopt(editHandle.wrapper.paletteSelectionBinder.get());
    if (paletteSelection == null || !(paletteSelection instanceof PaletteColor)) return;
    canvas.stroke(
        ((PaletteColor) paletteSelection).index(),
        start.x,
        start.y,
        startRadius,
        end.x,
        end.y,
        endRadius);
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
    } else if (window.pressed.contains(KeyCode.CONTROL)) {
      Vector quantizedCorner = end.divide(context.tileSize).toInt();
      PaletteTile tile =
          (PaletteTile) editHandle.wrapper.canvasHandle.frame.tilesGet(quantizedCorner.to1D());
      if (tile == null) {
        setColor(0);
      } else {
        Vector intEnd = end.toInt();
        Vector tileCorner = quantizedCorner.multiply(context.tileSize);
        setColor(tile.getData(context).getPixel(intEnd.x - tileCorner.x, intEnd.y - tileCorner.y));
      }
    } else super.mark(context, window, start, end, globalStart, globalEnd);
  }

  @Override
  public void remove(ProjectContext context, Window window) {
    editHandle.overlay.getChildren().removeAll(alignedCursor);
    editHandle.toolProperties.clear(this);
    super.remove(context, window);
  }
}