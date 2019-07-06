package com.zarbosoft.pyxyzygy.app.wrappers.camera;

import com.google.common.collect.ImmutableList;
import com.squareup.gifencoder.Color;
import com.squareup.gifencoder.GifEncoder;
import com.squareup.gifencoder.Image;
import com.squareup.gifencoder.ImageOptions;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.Tool;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.Wrapper;
import com.zarbosoft.pyxyzygy.app.config.CameraNodeConfig;
import com.zarbosoft.pyxyzygy.app.config.GroupNodeConfig;
import com.zarbosoft.pyxyzygy.app.widgets.TitledPane;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.CustomBinding;
import com.zarbosoft.pyxyzygy.app.widgets.binding.PropertyBinder;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeEditHandle;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeWrapper;
import com.zarbosoft.pyxyzygy.core.model.latest.Camera;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.model.Picture;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.zarbosoft.pyxyzygy.app.Global.localization;
import static com.zarbosoft.pyxyzygy.app.Misc.nodeFormFields;
import static com.zarbosoft.pyxyzygy.app.Misc.separateFormField;
import static com.zarbosoft.pyxyzygy.app.config.CameraNodeConfig.RenderMode.PNG_SEQUENCE;
import static com.zarbosoft.pyxyzygy.app.wrappers.camera.CameraWrapper.getActualFrameRate;
import static com.zarbosoft.pyxyzygy.app.wrappers.camera.CameraWrapper.getActualFrameTimeMs;
import static com.zarbosoft.rendaw.common.Common.uncheck;
import static org.jcodec.common.model.ColorSpace.RGB;

public class CameraEditHandle extends GroupNodeEditHandle {
  public CameraEditHandle(Context context, Window window, GroupNodeWrapper wrapper) {
    super(context, window, wrapper);
  }

  @Override
  public VBox buildTab(Context context, Window window, TitledPane toolProps) {
    CameraWrapper wrapper = (CameraWrapper) this.wrapper;
    VBox tabBox = new VBox();
    tabBox
        .getChildren()
        .addAll(
            new TitledPane(
                localization.getString("layer"),
                new WidgetFormBuilder()
                    .apply(b -> cleanup.add(nodeFormFields(context, b, wrapper)))
                    .apply(b -> separateFormField(context, b, wrapper))
                    .build()),
            new TitledPane(
                localization.getString("camera"),
                new WidgetFormBuilder()
                    .intSpinner(
                        localization.getString("width"),
                        1,
                        99999,
                        s -> {
                          cleanup2.add(
                              CustomBinding.bindBidirectional(
                                  new PropertyBinder<Integer>(wrapper.width.asObject()),
                                  new PropertyBinder<Integer>(
                                      s.getValueFactory().valueProperty())));
                        })
                    .intSpinner(
                        localization.getString("height"),
                        1,
                        99999,
                        s -> {
                          cleanup2.add(
                              CustomBinding.bindBidirectional(
                                  new PropertyBinder<Integer>(wrapper.height.asObject()),
                                  new PropertyBinder<Integer>(
                                      s.getValueFactory().valueProperty())));
                        })
                    .build()),
            new TitledPane(
                localization.getString("render"),
                new WidgetFormBuilder()
                    .<CameraNodeConfig.RenderMode>dropDown(
                        localization.getString("render.type"),
                        d -> {
                          Supplier<ListCell<CameraNodeConfig.RenderMode>> cellSupplier =
                              () ->
                                  new ListCell<>() {
                                    @Override
                                    protected void updateItem(
                                        CameraNodeConfig.RenderMode item, boolean empty) {
                                      if (item == null) {
                                        setText(null);
                                      } else {
                                        setText(item.human());
                                      }
                                      super.updateItem(item, empty);
                                    }
                                  };
                          d.setButtonCell(cellSupplier.get());
                          d.setCellFactory(
                              (ListView<CameraNodeConfig.RenderMode> param) -> cellSupplier.get());
                          d.getItems()
                              .addAll(
                                  CameraNodeConfig.RenderMode.PNG,
                                  CameraNodeConfig.RenderMode.GIF,
                                  CameraNodeConfig.RenderMode.WEBM,
                                  PNG_SEQUENCE);
                          d.getSelectionModel().select(wrapper.config.renderMode);
                          d.getSelectionModel()
                              .selectedItemProperty()
                              .addListener(
                                  (observable, oldValue, newValue) ->
                                      wrapper.config.renderMode = newValue);
                        })
                    .chooseDirectory(
                        localization.getString("render.path"),
                        s -> {
                          s.setValue(wrapper.config.renderDir);
                          s.addListener(
                              (observable, oldValue, newValue) ->
                                  wrapper.config.renderDir = newValue);
                        })
                    .text(
                        localization.getString("render.name"),
                        t -> {
                          t.setText(wrapper.config.renderName);
                          t.textProperty()
                              .addListener(
                                  (observable, oldValue, newValue) ->
                                      wrapper.config.renderName = newValue);
                        })
                    .intSpinner(
                        localization.getString("render.scale"),
                        1,
                        50,
                        s -> {
                          s.getValueFactory()
                              .valueProperty()
                              .bindBidirectional(wrapper.config.renderScale.asObject());
                        })
                    .button(
                        button -> {
                          button.setText(localization.getString("render"));
                          button.setOnAction(
                              e ->
                                  uncheck(
                                      () -> {
                                        render(context, window, wrapper);
                                      }));
                        })
                    .build()),
            toolProps);
    return tabBox;
  }

  private static void render(Context context, Window window, CameraWrapper wrapper)
      throws IOException, NoSuchMethodException {
    Camera node = (Camera) wrapper.node;
    Path dir = Paths.get(wrapper.config.renderDir);
    Files.createDirectories(dir);
    int scale = wrapper.config.renderScale.get();
    switch (wrapper.config.renderMode) {
      case PNG:
        wrapper.render(
            context,
            window,
            continuation -> {
              continuation.run(
                  (i, canvas) ->
                      canvas.serialize(
                          dir.resolve(String.format("%s.png", wrapper.config.renderName))
                              .toString()));
            },
            wrapper.canvasHandle.frameNumber.get(),
            wrapper.canvasHandle.frameNumber.get() + 1,
            scale);
        break;
      case WEBM:
        wrapper.render(
            context,
            window,
            continuation -> {
              SequenceEncoder encoder =
                  SequenceEncoder.createSequenceEncoder(
                      dir.resolve(String.format("%s.webm", wrapper.config.renderName)).toFile(),
                      (int) getActualFrameRate(node));
              Picture rgbPic = Picture.create(node.width() * scale, node.height() * scale, RGB);
              byte[] rgb = rgbPic.getPlaneData(0);
              continuation.run(
                  (i, canvas) -> {
                    byte[] bgra = canvas.data();
                    for (int y = 0; y < canvas.getHeight(); ++y) {
                      for (int x = 0; x < canvas.getWidth(); ++x) {
                        int inBase = (y * canvas.getWidth() + x) * 4;
                        int outBase = (y * canvas.getWidth() + x) * 3;
                        int b = Byte.toUnsignedInt(bgra[inBase + 0]);
                        int g = Byte.toUnsignedInt(bgra[inBase + 1]);
                        int r = Byte.toUnsignedInt(bgra[inBase + 2]);
                        int a = Byte.toUnsignedInt(bgra[inBase + 3]);
                        rgb[outBase] = (byte) ((r * a + 0xFF * (0xFF - a)) / 0xFF - 128);
                        rgb[outBase + 1] = (byte) ((g * a + 0xFF * (0xFF - a)) / 0xFF - 128);
                        rgb[outBase + 2] = (byte) ((b * a + 0xFF * (0xFF - a)) / 0xFF - 128);
                      }
                    }
                    uncheck(() -> encoder.encodeNativeFrame(rgbPic));
                  });
              encoder.finish();
            },
            scale);
        break;
      case GIF:
        wrapper.render(
            context,
            window,
            continuation -> {
              try (OutputStream stream =
                  Files.newOutputStream(
                      dir.resolve(String.format("%s.gif", wrapper.config.renderName)))) {
                GifEncoder gifEncoder =
                    new GifEncoder(stream, node.width() * scale, node.height() * scale, 0);
                Method addImage =
                    GifEncoder.class.getDeclaredMethod("addImage", Image.class, ImageOptions.class);
                addImage.setAccessible(true);

                ImageOptions options = new ImageOptions();
                options.setDelay(
                    (long)
                        (TimeUnit.MICROSECONDS.convert(1, TimeUnit.MILLISECONDS)
                            * getActualFrameTimeMs(node)),
                    TimeUnit.MICROSECONDS);

                Color[][] rgb = new Color[node.height() * scale][node.width() * scale];
                continuation.run(
                    (i, canvas) -> {
                      byte[] bgra = canvas.data();
                      for (int y = 0; y < canvas.getHeight(); ++y) {
                        for (int x = 0; x < canvas.getWidth(); ++x) {
                          int base = (y * canvas.getWidth() + x) * 4;
                          double b = Byte.toUnsignedInt(bgra[base]) / 255.0;
                          double g = Byte.toUnsignedInt(bgra[base + 1]) / 255.0;
                          double r = Byte.toUnsignedInt(bgra[base + 2]) / 255.0;
                          double a = Byte.toUnsignedInt(bgra[base + 3]) / 255.0;
                          rgb[y][x] =
                              new Color(r * a + (1.0 - a), g * a + (1.0 - a), b * a + (1.0 - a));
                        }
                      }
                      uncheck(() -> addImage.invoke(gifEncoder, Image.fromColors(rgb), options));
                    });
                gifEncoder.finishEncoding();
              }
            },
            scale);
        break;
      case PNG_SEQUENCE:
        {
          wrapper.render(
              context,
              window,
              continuation -> {
                continuation.run(
                    (i, canvas) ->
                        canvas.serialize(
                            dir.resolve(String.format("%s%06d.png", wrapper.config.renderName, i))
                                .toString()));
              },
              scale);
        }
        break;
    }
  }

  @Override
  protected List<Node> createToolButtons() {
    return ImmutableList.of(
        new Wrapper.ToolToggle(
            wrapper,
            "cursor-move16.png",
            localization.getString("move.layer"),
            GroupNodeConfig.TOOL_MOVE),
        new Wrapper.ToolToggle(
            wrapper,
            "resize.png",
            localization.getString("resize.viewport"),
            CameraNodeConfig.TOOL_VIEWPORT),
        new Wrapper.ToolToggle(
            wrapper, "stamper16.png", localization.getString("stamp"), GroupNodeConfig.TOOL_STAMP));
  }

  @Override
  protected Tool createTool(Context context, Window window, String newValue) {
    if (CameraNodeConfig.TOOL_VIEWPORT.equals(newValue)) {
      return new ToolViewport((CameraWrapper) wrapper);
    } else return super.createTool(context, window, newValue);
  }

  @Override
  public void remove(Context context, Window window) {
    super.remove(context, window);
    cleanup2.forEach(c -> c.destroy());
  }
}
