package com.zarbosoft.pyxyzygy.app.wrappers.camera;

import com.google.common.collect.ImmutableList;
import com.squareup.gifencoder.Color;
import com.squareup.gifencoder.GifEncoder;
import com.squareup.gifencoder.Image;
import com.squareup.gifencoder.ImageOptions;
import com.zarbosoft.pyxyzygy.app.Tool;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.Wrapper;
import com.zarbosoft.pyxyzygy.app.config.CameraNodeConfig;
import com.zarbosoft.pyxyzygy.app.config.GroupNodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.TitledPane;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.BinderRoot;
import com.zarbosoft.pyxyzygy.app.widgets.binding.CustomBinding;
import com.zarbosoft.pyxyzygy.app.widgets.binding.PropertyBinder;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeEditHandle;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeWrapper;
import javafx.scene.Node;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.model.Picture;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.zarbosoft.pyxyzygy.app.Misc.nodeFormFields;
import static com.zarbosoft.pyxyzygy.app.Misc.separateFormField;
import static com.zarbosoft.pyxyzygy.app.config.CameraNodeConfig.RenderMode.PNG_SEQUENCE;
import static com.zarbosoft.rendaw.common.Common.uncheck;
import static org.jcodec.common.model.ColorSpace.RGB;

public class CameraEditHandle extends GroupNodeEditHandle {
  protected List<BinderRoot> cleanup2 = new ArrayList<>();

  public CameraEditHandle(ProjectContext context, Window window, GroupNodeWrapper wrapper) {
    super(context, window, wrapper);
  }

  @Override
  public VBox buildTab(ProjectContext context, Window window, TitledPane toolProps) {
    CameraWrapper wrapper = (CameraWrapper) this.wrapper;
    VBox tabBox = new VBox();
    tabBox
        .getChildren()
        .addAll(
            new TitledPane(
                "Layer",
                new WidgetFormBuilder()
                    .apply(b -> cleanup.add(nodeFormFields(context, b, wrapper)))
                    .apply(b -> separateFormField(context, b, wrapper))
                    .build()),
            new TitledPane(
                "Camera",
                new WidgetFormBuilder()
                    .intSpinner(
                        "Width",
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
                        "Height",
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
                "Render",
                new WidgetFormBuilder()
                    .<CameraNodeConfig.RenderMode>dropDown(
                        "Render type",
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
                        "Render path",
                        s -> {
                          s.setValue(wrapper.config.renderDir);
                          s.addListener(
                              (observable, oldValue, newValue) ->
                                  wrapper.config.renderDir = newValue);
                        })
                    .text(
                        "Render name",
                        t -> {
                          t.setText(wrapper.config.renderName);
                          t.textProperty()
                              .addListener(
                                  (observable, oldValue, newValue) ->
                                      wrapper.config.renderName = newValue);
                        })
                    .intSpinner(
                        "Render scale",
                        1,
                        50,
                        s -> {
                          s.getValueFactory()
                              .valueProperty()
                              .bindBidirectional(wrapper.config.renderScale.asObject());
                        })
                    .button(
                        button -> {
                          button.setText("Render");
                          button.setOnAction(
                              e ->
                                  uncheck(
                                      () -> {
                                        Path dir = Paths.get(wrapper.config.renderDir);
                                        Files.createDirectories(dir);
                                        int scale = wrapper.config.renderScale.get();
                                        switch (wrapper.config.renderMode) {
                                          case PNG:
                                            wrapper.render(
                                                context,
                                                window,
                                                (i, canvas) ->
                                                    canvas.serialize(
                                                        dir.resolve(
                                                                String.format(
                                                                    "%s.png",
                                                                    wrapper.config.renderName))
                                                            .toString()),
                                                wrapper.canvasHandle.frameNumber.get(),
                                                wrapper.canvasHandle.frameNumber.get() + 1,
                                                scale);
                                            break;
                                          case WEBM:
                                            {
                                              SequenceEncoder encoder =
                                                  SequenceEncoder.createSequenceEncoder(
                                                      dir.resolve(
                                                              String.format(
                                                                  "%s.webm",
                                                                  wrapper.config.renderName))
                                                          .toFile(),
                                                      (int) (wrapper.node.frameRate() / 10.0));
                                              Picture rgbPic =
                                                  Picture.create(
                                                      wrapper.node.width() * scale,
                                                      wrapper.node.height() * scale,
                                                      RGB);
                                              byte[] rgb = rgbPic.getPlaneData(0);
                                              wrapper.render(
                                                  context,
                                                  window,
                                                  (i, canvas) -> {
                                                    byte[] bgra = canvas.data();
                                                    for (int y = 0;
                                                        y < wrapper.node.height();
                                                        ++y) {
                                                      for (int x = 0;
                                                          x < wrapper.node.width();
                                                          ++x) {
                                                        int inBase =
                                                            (y * wrapper.node.width() + x) * 4;
                                                        int outBase =
                                                            (y * wrapper.node.width() + x) * 3;
                                                        int b =
                                                            Byte.toUnsignedInt(bgra[inBase + 0]);
                                                        int g =
                                                            Byte.toUnsignedInt(bgra[inBase + 1]);
                                                        int r =
                                                            Byte.toUnsignedInt(bgra[inBase + 2]);
                                                        int a =
                                                            Byte.toUnsignedInt(bgra[inBase + 3]);
                                                        rgb[outBase] =
                                                            (byte)
                                                                ((r * a + 0xFF * (0xFF - a)) / 0xFF
                                                                    - 128);
                                                        rgb[outBase + 1] =
                                                            (byte)
                                                                ((g * a + 0xFF * (0xFF - a)) / 0xFF
                                                                    - 128);
                                                        rgb[outBase + 2] =
                                                            (byte)
                                                                ((b * a + 0xFF * (0xFF - a)) / 0xFF
                                                                    - 128);
                                                      }
                                                    }
                                                    uncheck(
                                                        () -> encoder.encodeNativeFrame(rgbPic));
                                                  },
                                                  scale);
                                              encoder.finish();
                                              break;
                                            }
                                          case GIF:
                                            try (OutputStream stream =
                                                Files.newOutputStream(
                                                    dir.resolve(
                                                        String.format(
                                                            "%s.gif",
                                                            wrapper.config.renderName)))) {
                                              GifEncoder gifEncoder =
                                                  new GifEncoder(
                                                      stream,
                                                      wrapper.node.width(),
                                                      wrapper.node.height(),
                                                      0);
                                              Method addImage =
                                                  GifEncoder.class.getDeclaredMethod(
                                                      "addImage", Image.class, ImageOptions.class);
                                              addImage.setAccessible(true);

                                              ImageOptions options = new ImageOptions();
                                              options.setDelay(
                                                  TimeUnit.MICROSECONDS.convert(1, TimeUnit.SECONDS)
                                                      / wrapper.node.frameRate(),
                                                  TimeUnit.MICROSECONDS);

                                              Color[][] rgb =
                                                  new Color[wrapper.node.height() * scale]
                                                      [wrapper.node.width() * scale];
                                              wrapper.render(
                                                  context,
                                                  window,
                                                  (i, canvas) -> {
                                                    byte[] bgra = canvas.data();
                                                    for (int y = 0;
                                                        y < wrapper.node.height();
                                                        ++y) {
                                                      for (int x = 0;
                                                          x < wrapper.node.width();
                                                          ++x) {
                                                        int base =
                                                            (y * wrapper.node.width() + x) * 4;
                                                        double b =
                                                            Byte.toUnsignedInt(bgra[base]) / 255.0;
                                                        double g =
                                                            Byte.toUnsignedInt(bgra[base + 1])
                                                                / 255.0;
                                                        double r =
                                                            Byte.toUnsignedInt(bgra[base + 2])
                                                                / 255.0;
                                                        double a =
                                                            Byte.toUnsignedInt(bgra[base + 3])
                                                                / 255.0;
                                                        rgb[y][x] =
                                                            new Color(
                                                                r * a + (1.0 - a),
                                                                g * a + (1.0 - a),
                                                                b * a + (1.0 - a));
                                                      }
                                                    }
                                                    uncheck(
                                                        () ->
                                                            addImage.invoke(
                                                                gifEncoder,
                                                                Image.fromColors(rgb),
                                                                options));
                                                  },
                                                  scale);
                                              gifEncoder.finishEncoding();
                                            }
                                            break;
                                          case PNG_SEQUENCE:
                                            {
                                              wrapper.render(
                                                  context,
                                                  window,
                                                  (i, canvas) ->
                                                      canvas.serialize(
                                                          dir.resolve(
                                                                  String.format(
                                                                      "%s%06d.png",
                                                                      wrapper.config.renderName, i))
                                                              .toString()),
                                                  scale);
                                            }
                                            break;
                                        }
                                      }));
                        })
                    .build()),
            toolProps);
    return tabBox;
  }

  @Override
  protected List<Node> createToolButtons() {
    return ImmutableList.of(
        new Wrapper.ToolToggle(
            wrapper, "cursor-move16.png", "Move layer", GroupNodeConfig.TOOL_MOVE),
        new Wrapper.ToolToggle(
            wrapper, "resize.png", "Resize viewport", CameraNodeConfig.TOOL_VIEWPORT),
        new Wrapper.ToolToggle(wrapper, "stamper16.png", "Stamp", GroupNodeConfig.TOOL_STAMP));
  }

  @Override
  protected Tool createTool(ProjectContext context, Window window, String newValue) {
    if (CameraNodeConfig.TOOL_VIEWPORT.equals(newValue)) {
      return new ToolViewport((CameraWrapper) wrapper);
    } else return super.createTool(context, window, newValue);
  }

  @Override
  public void remove(ProjectContext context, Window window) {
    super.remove(context, window);
    cleanup2.forEach(c -> c.destroy());
  }
}
