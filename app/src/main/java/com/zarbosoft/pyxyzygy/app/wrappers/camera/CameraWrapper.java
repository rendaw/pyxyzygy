package com.zarbosoft.pyxyzygy.app.wrappers.camera;

import com.zarbosoft.automodel.lib.History;
import com.zarbosoft.pyxyzygy.app.CanvasHandle;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.EditHandle;
import com.zarbosoft.pyxyzygy.app.Render;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.Wrapper;
import com.zarbosoft.pyxyzygy.app.config.CameraNodeConfig;
import com.zarbosoft.pyxyzygy.app.config.GroupNodeConfig;
import com.zarbosoft.pyxyzygy.app.widgets.binding.BinderRoot;
import com.zarbosoft.pyxyzygy.app.widgets.binding.CustomBinding;
import com.zarbosoft.pyxyzygy.app.widgets.binding.PropertyBinder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.ScalarBinder;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeCanvasHandle;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeWrapper;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.latest.Camera;
import com.zarbosoft.pyxyzygy.core.model.latest.ProjectLayer;
import com.zarbosoft.rendaw.common.Common;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import static com.zarbosoft.pyxyzygy.app.Global.localization;

public class CameraWrapper extends GroupNodeWrapper {
  public final SimpleIntegerProperty width = new SimpleIntegerProperty(0);
  public final SimpleIntegerProperty height = new SimpleIntegerProperty(0);
  private final BinderRoot cleanupWidth;
  private final BinderRoot cleanupHeight;
  public boolean adjustViewport = false;
  private History.Tuple actionWidthChange = new History.Tuple(this, "width");
  private History.Tuple actionHeightChange = new History.Tuple(this, "height");

  public CameraNodeConfig config;

  public CameraWrapper(Context context, Wrapper parent, int parentIndex, Camera node) {
    super(context, parent, parentIndex, node);
    cleanupWidth =
        CustomBinding.bindBidirectional(
            new ScalarBinder<Integer>(
                node,
                "width",
                v -> context.change(actionWidthChange, c -> c.camera(node).widthSet(v))),
            new PropertyBinder<>(width.asObject()));
    cleanupHeight =
        CustomBinding.bindBidirectional(
            new ScalarBinder<Integer>(
                node,
                "height",
                v -> context.change(actionHeightChange, c -> c.camera(node).heightSet(v))),
            new PropertyBinder<>(height.asObject()));
  }

  @Override
  protected GroupNodeConfig initConfig(Context context, long id) {
    return this.config =
        (CameraNodeConfig)
            context.config.nodes.computeIfAbsent(id, id1 -> new CameraNodeConfig(context));
  }

  @Override
  public void remove(Context context) {
    cleanupWidth.destroy();
    cleanupHeight.destroy();
    super.remove(context);
  }

  @Override
  public CanvasHandle buildCanvas(Context context, Window window, CanvasHandle parent) {
    if (canvasHandle == null) {
      class CameraCanvasHandle extends GroupNodeCanvasHandle {
        Rectangle cameraBorder;

        public CameraCanvasHandle(Context context, Window window, GroupNodeWrapper wrapper) {
          super(context, window, wrapper);
          cameraBorder = new Rectangle();
          cameraBorder.strokeWidthProperty().bind(Bindings.divide(1.0, window.editor.zoomFactor));
          cameraBorder.setOpacity(0.8);
          cameraBorder.setBlendMode(BlendMode.DIFFERENCE);
          cameraBorder.setStrokeType(StrokeType.OUTSIDE);
          cameraBorder.setFill(Color.TRANSPARENT);
          cameraBorder.setStroke(Color.GRAY);
          cameraBorder.widthProperty().bind(width);
          cameraBorder.heightProperty().bind(height);
          cameraBorder.layoutXProperty().bind(width.divide(2).negate());
          cameraBorder.layoutYProperty().bind(height.divide(2).negate());
          overlay.getChildren().addAll(cameraBorder);
        }

        @Override
        public void remove(Context context, Wrapper excludeSubtree) {
          cameraBorder = null;
          super.remove(context, excludeSubtree);
        }
      }
      canvasHandle = new CameraCanvasHandle(context, window, this);
    }
    canvasHandle.setParent(parent);
    return canvasHandle;
  }

  @Override
  public ProjectLayer separateClone(Context context) {
    Camera clone = Camera.create(context.model);
    cloneSet(context, clone);
    Camera node = (Camera) this.node;
    clone.initialOffsetSet(context.model, node.offset());
    clone.initialWidthSet(context.model, node.width());
    clone.initialHeightSet(context.model, node.height());
    clone.initialFrameRateSet(context.model, node.frameRate());
    clone.initialFrameStartSet(context.model, node.frameStart());
    clone.initialFrameLengthSet(context.model, node.frameLength());
    return clone;
  }

  @Override
  public EditHandle buildEditControls(Context context, Window window) {
    return new CameraEditHandle(context, window, this);
  }

  public void render(
    Context context,
    Window window,
    Common.UncheckedConsumer<Common.UncheckedConsumer<BiConsumer<Integer, TrueColorImage>>>
          thread,
    int scale) {
    Camera node = (Camera) this.node;
    render(
        context, window, thread, node.frameStart(), node.frameStart() + node.frameLength(), scale);
  }

  public void render(
    Context context,
    Window window,
    Common.UncheckedConsumer<Common.UncheckedConsumer<BiConsumer<Integer, TrueColorImage>>>
          thread,
    int start,
    int end,
    int scale) {
    Window.DialogBuilder builder = window.dialog(localization.getString("rendering"));
    ProgressBar progress = new ProgressBar();
    AtomicBoolean cancel = new AtomicBoolean(false);
    new Thread(
            () -> {
              try {
                thread.run(
                    frameConsumer -> {
                      Camera node = (Camera) this.node;
                      TrueColorImage canvas = TrueColorImage.create(node.width(), node.height());
                      for (int i = start; i < end; ++i) {
                        if (cancel.get()) return;
                        if (i != start) canvas.clear();
                        context.model.lock.readLock().lock();
                        try {
                          Render.render(
                              context,
                              node,
                              canvas,
                              i,
                              new com.zarbosoft.pyxyzygy.seed.Rectangle(
                                  -width.get() / 2, -height.get() / 2, width.get(), height.get()),
                              1.0);
                        } finally {
                          context.model.lock.readLock().unlock();
                        }
                        frameConsumer.accept(i, scale == 1 ? canvas : canvas.scale(scale));
                        final double percent = ((double) (i - start)) / (end - start);
                        Platform.runLater(() -> progress.setProgress(percent));
                      }
                    });
              } catch (Exception e) {
                Platform.runLater(
                    () ->
                        window.error(
                            e,
                            localization.getString("error.while.rendering"),
                            localization.getString("encountered.an.error.while.rendering")));
              } finally {
                Platform.runLater(() -> builder.close());
              }
            })
        .start();
    builder
        .addContent(progress)
        .addAction(
            ButtonType.CANCEL,
            true,
            () -> {
              cancel.set(true);
              return true;
            })
        .go();
  }

  public static double getActualFrameRate(Camera camera) {
    return camera.frameRate();
  }

  public static double getActualFrameTimeMs(Camera camera) {
    return 1000.0 / camera.frameRate();
  }
}
