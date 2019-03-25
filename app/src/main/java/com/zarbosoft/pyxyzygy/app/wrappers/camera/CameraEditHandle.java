package com.zarbosoft.pyxyzygy.app.wrappers.camera;

import com.squareup.gifencoder.Color;
import com.squareup.gifencoder.GifEncoder;
import com.squareup.gifencoder.Image;
import com.squareup.gifencoder.ImageOptions;
import com.zarbosoft.pyxyzygy.app.CustomBinding;
import com.zarbosoft.pyxyzygy.app.DoubleVector;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.config.CameraNodeConfig;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeEditHandle;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.group.ToolMove;
import com.zarbosoft.pyxyzygy.core.model.v0.Camera;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.model.Picture;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.zarbosoft.pyxyzygy.app.Misc.nodeFormFields;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.pad;
import static com.zarbosoft.rendaw.common.Common.uncheck;
import static org.jcodec.common.model.ColorSpace.RGB;

public class CameraEditHandle extends GroupNodeEditHandle {
	public CameraEditHandle(
			ProjectContext context, Window window, CameraWrapper wrapper
	) {
		super(context, window,wrapper);
		window.layerTabContent.set(this,
				pad(new WidgetFormBuilder()
						.apply(b -> cleanup.add(nodeFormFields(context, b, wrapper)))
						.intSpinner("Width", 1, 99999, s -> {
							cleanup.add(CustomBinding.bindBidirectional(
									new CustomBinding.PropertyBinder<Integer>(wrapper.width.asObject()),
									new CustomBinding.PropertyBinder<Integer>(s.getValueFactory().valueProperty())
							));
							s
									.focusedProperty()
									.addListener((observable, oldValue, newValue) -> context.history.finishChange());
						})
						.intSpinner("Height", 1, 99999, s -> {
							cleanup.add(CustomBinding.bindBidirectional(
									new CustomBinding.PropertyBinder<Integer>(wrapper.height.asObject() ),
									new CustomBinding.PropertyBinder<Integer>(s.getValueFactory().valueProperty() )
							));
							s
									.focusedProperty()
									.addListener((observable, oldValue, newValue) -> context.history.finishChange());
						})
						.intSpinner("End frame", 1, 99999, s -> {
							Listener.ScalarSet<Camera, Integer> listener =
									wrapper.node.addEndSetListeners((target, value) -> s
											.getValueFactory()
											.setValue(value));
							cleanup.add(() -> wrapper.node.removeEndSetListeners(listener));
							s
									.getValueFactory()
									.valueProperty()
									.addListener((observable, oldValue, newValue) -> context.history.change(c -> c
											.camera(wrapper.node)
											.endSet(newValue)));
							s
									.focusedProperty()
									.addListener((observable, oldValue, newValue) -> context.history.finishChange());
						})
						.doubleSpinner("Framerate", 1, 999, 1, s -> {
							Listener.ScalarSet<Camera, Integer> listener =
									wrapper.node.addFrameRateSetListeners((target, value) -> s
											.getValueFactory()
											.setValue(value / 10.0));
							cleanup.add(() -> wrapper.node.removeFrameRateSetListeners(listener));
							s
									.getValueFactory()
									.valueProperty()
									.addListener((observable, oldValue, newValue) -> context.history.change(c -> c
											.camera(wrapper.node)
											.frameRateSet((int) (newValue * 10.0))));
							s
									.focusedProperty()
									.addListener((observable, oldValue, newValue) -> context.history.finishChange());
						}).<CameraNodeConfig.RenderMode>dropDown("Render type", d -> {
							Supplier<ListCell<CameraNodeConfig.RenderMode>> cellSupplier = () -> new ListCell<>() {
								@Override
								protected void updateItem(
										CameraNodeConfig.RenderMode item, boolean empty
								) {
									if (item == null) {
										setText(null);
									} else {
										setText(item.human());
									}
									super.updateItem(item, empty);
								}
							};
							d.setButtonCell(cellSupplier.get());
							d.setCellFactory((ListView<CameraNodeConfig.RenderMode> param) -> cellSupplier.get());
							d.getItems().addAll(CameraNodeConfig.RenderMode.PNG,
									CameraNodeConfig.RenderMode.GIF,
									CameraNodeConfig.RenderMode.WEBM,
									CameraNodeConfig.RenderMode.PNG_SEQUENCE
							);
							d.getSelectionModel().select(wrapper.config.renderMode);
							d
									.getSelectionModel()
									.selectedItemProperty()
									.addListener((observable, oldValue, newValue) -> wrapper.config.renderMode =
											newValue);
						}).chooseDirectory("Render path", s -> {
							s.setValue(wrapper.config.renderDir);
							s.addListener((observable, oldValue, newValue) -> wrapper.config.renderDir = newValue);
						}).text("Render name", t -> {
							t.setText(wrapper.config.renderName);
							t
									.textProperty()
									.addListener((observable, oldValue, newValue) -> wrapper.config.renderName =
											newValue);
						}).button(button -> {
							button.setText("Render");
							button.setOnAction(e -> uncheck(() -> {
								Path dir = Paths.get(wrapper.config.renderDir);
								Files.createDirectories(dir);
								switch (wrapper.config.renderMode) {
									case PNG:
										wrapper.render(context,
												window,
												(i, canvas) -> canvas.serialize(dir
														.resolve(String.format("%s.png", wrapper.config.renderName))
														.toString()),
												wrapper.canvasHandle.frameNumber.get(),
												wrapper.canvasHandle.frameNumber.get() + 1
										);
										break;
									case WEBM: {
										SequenceEncoder encoder = SequenceEncoder.createSequenceEncoder(dir
												.resolve(String.format("%s.webm", wrapper.config.renderName))
												.toFile(), (int) (wrapper.node.frameRate() / 10.0));
										Picture rgbPic =
												Picture.create(wrapper.node.width(), wrapper.node.height(), RGB);
										byte[] rgb = rgbPic.getPlaneData(0);
										wrapper.render(context, window, (i, canvas) -> {
											byte[] bgra = canvas.data();
											for (int y = 0; y < wrapper.node.height(); ++y) {
												for (int x = 0; x < wrapper.node.width(); ++x) {
													int inBase = (y * wrapper.node.width() + x) * 4;
													int outBase = (y * wrapper.node.width() + x) * 3;
													int b = Byte.toUnsignedInt(bgra[inBase + 0]);
													int g = Byte.toUnsignedInt(bgra[inBase + 1]);
													int r = Byte.toUnsignedInt(bgra[inBase + 2]);
													int a = Byte.toUnsignedInt(bgra[inBase + 3]);
													rgb[outBase] = (byte) (
															(r * a + 0xFF * (0xFF - a)) / 0xFF - 128
													);
													rgb[outBase + 1] = (byte) (
															(g * a + 0xFF * (0xFF - a)) / 0xFF - 128
													);
													rgb[outBase + 2] = (byte) (
															(b * a + 0xFF * (0xFF - a)) / 0xFF - 128
													);
												}
											}
											uncheck(() -> encoder.encodeNativeFrame(rgbPic));
										});
										encoder.finish();
										break;
									}
									case GIF:
										try (
												OutputStream stream = Files.newOutputStream(dir.resolve(String.format("%s.gif",
														wrapper.config.renderName
												)))
										) {
											GifEncoder gifEncoder = new GifEncoder(stream,
													wrapper.node.width(),
													wrapper.node.height(),
													0
											);
											Method addImage = GifEncoder.class.getDeclaredMethod("addImage",
													Image.class,
													ImageOptions.class
											);
											addImage.setAccessible(true);

											ImageOptions options = new ImageOptions();
											options.setDelay(TimeUnit.MICROSECONDS.convert(1, TimeUnit.SECONDS) /
													wrapper.node.frameRate(), TimeUnit.MICROSECONDS);

											Color[][] rgb =
													new Color[wrapper.node.height()][wrapper.node
															.width()];
											wrapper.render(context, window, (i, canvas) -> {
												byte[] bgra = canvas.data();
												for (int y = 0; y < wrapper.node.height(); ++y) {
													for (int x = 0; x < wrapper.node.width(); ++x) {
														int base = (y * wrapper.node.width() + x) * 4;
														double b = Byte.toUnsignedInt(bgra[base]) / 255.0;
														double g = Byte.toUnsignedInt(bgra[base + 1]) / 255.0;
														double r = Byte.toUnsignedInt(bgra[base + 2]) / 255.0;
														double a = Byte.toUnsignedInt(bgra[base + 3]) / 255.0;
														rgb[y][x] = new Color(r * a + (1.0 - a),
																g * a + (1.0 - a),
																b * a + (1.0 - a)
														);
													}
												}
												uncheck(() -> addImage.invoke(gifEncoder,
														Image.fromColors(rgb),
														options
												));
											});
											gifEncoder.finishEncoding();
										}
										break;
									case PNG_SEQUENCE: {
										wrapper.render(context,
												window,
												(i, canvas) -> canvas.serialize(dir
														.resolve(String.format("%s%06d.png",
																wrapper.config.renderName,
																i
														))
														.toString())
										);
									}
									break;
								}
							}));
						}).build())
		);
	}

	@Override
	protected ToolMove createToolMove(Window window, GroupNodeWrapper wrapper1) {
		CameraWrapper wrapper = (CameraWrapper) wrapper1;
		return new ToolMove(window, wrapper) {
			boolean adjustHorizontal;
			boolean adjustPositive;
			int startValue;

			@Override
			public void markStart(ProjectContext context, Window window, DoubleVector start) {
				if (wrapper.adjustViewport) {
					double xyRatio = (double) wrapper.node.width() / wrapper.node.height();
					adjustHorizontal = Math.abs(start.x) >= Math.abs(start.y * xyRatio);
					if (adjustHorizontal) {
						adjustPositive = start.x >= 0;
						startValue = wrapper.node.width();
					} else {
						adjustPositive = start.y >= 0;
						startValue = wrapper.node.height();
					}
					markStart = start;
				} else
					super.markStart(context, window, start);
			}

			@Override
			public void mark(ProjectContext context, Window window, DoubleVector start, DoubleVector end) {
				if (wrapper.adjustViewport) {
					double negative = adjustPositive ? 2 : -2;
					if (adjustHorizontal) {
						wrapper.width.set((int) (startValue + (end.x - markStart.x) * negative));
					} else {
						wrapper.height.set((int) (startValue + (end.y - markStart.y) * negative));
					}
				}
			}
		};
	}
}
