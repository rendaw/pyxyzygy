package com.zarbosoft.pyxyzygy.app.wrappers.camera;

import com.squareup.gifencoder.GifEncoder;
import com.squareup.gifencoder.Image;
import com.squareup.gifencoder.ImageOptions;
import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.config.CameraNodeConfig;
import com.zarbosoft.pyxyzygy.app.config.GroupNodeConfig;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeCanvasHandle;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeEditHandle;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeWrapper;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.Camera;
import com.zarbosoft.pyxyzygy.core.model.ProjectNode;
import javafx.application.Platform;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.jcodec.api.SequenceEncoder;
import org.jcodec.common.model.Picture;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static com.zarbosoft.pyxyzygy.app.Misc.nodeFormFields;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.pad;
import static com.zarbosoft.rendaw.common.Common.uncheck;
import static org.jcodec.common.model.ColorSpace.RGB;

public class CameraWrapper extends GroupNodeWrapper {
	private final Camera node;
	private final Camera.WidthSetListener widthListener;
	private final Camera.HeightSetListener heightListener;
	private final SimpleIntegerProperty width = new SimpleIntegerProperty(0);
	private final SimpleIntegerProperty height = new SimpleIntegerProperty(0);

	private CameraNodeConfig config;

	public CameraWrapper(ProjectContext context, Wrapper parent, int parentIndex, Camera node) {
		super(context, parent, parentIndex, node);
		this.parentIndex = parentIndex;
		this.node = node;
		this.widthListener = node.addWidthSetListeners((target, value) -> {
			width.set(value);
		});
		width.addListener((observable, oldValue, newValue) -> context.history.change(c -> c
				.camera(node)
				.widthSet(newValue.intValue())));
		this.heightListener = node.addHeightSetListeners((target, value) -> {
			height.set(value);
		});
		height.addListener((observable, oldValue, newValue) -> context.history.change(c -> c
				.camera(node)
				.heightSet(newValue.intValue())));
	}

	@Override
	protected GroupNodeConfig initConfig(ProjectContext context, long id) {
		return this.config = (CameraNodeConfig) context.config.nodes.computeIfAbsent(id, id1 -> new CameraNodeConfig());
	}

	@Override
	public void remove(ProjectContext context) {
		node.removeWidthSetListeners(widthListener);
		node.removeHeightSetListeners(heightListener);
		super.remove(context);
	}

	@Override
	public CanvasHandle buildCanvas(ProjectContext context, CanvasHandle parent) {
		return new GroupNodeCanvasHandle(context, parent, this) {
			Rectangle cameraBorder;
			CanvasHandle groupHandle = CameraWrapper.super.buildCanvas(context, parent);

			{
				cameraBorder = new Rectangle();
				cameraBorder.setStrokeWidth(1);
				cameraBorder.setStrokeType(StrokeType.OUTSIDE);
				cameraBorder.setFill(Color.TRANSPARENT);
				cameraBorder.setStroke(HelperJFX.c(new java.awt.Color(128, 128, 128)));
				cameraBorder.widthProperty().bind(width);
				cameraBorder.heightProperty().bind(height);
				cameraBorder.layoutXProperty().bind(width.divide(2).negate());
				cameraBorder.layoutYProperty().bind(height.divide(2).negate());
				inner.getChildren().addAll(groupHandle.getWidget(), cameraBorder);
			}

			@Override
			public void remove(ProjectContext context) {
				groupHandle.remove(context);
				cameraBorder = null;
			}
		};
	}

	@Override
	public ProjectNode separateClone(ProjectContext context) {
		Camera clone = Camera.create(context);
		cloneSet(context, clone);
		clone.initialWidthSet(context, node.width());
		clone.initialHeightSet(context, node.height());
		clone.initialFrameRateSet(context, node.frameRate());
		clone.initialEndSet(context, node.end());
		return clone;
	}

	@Override
	public EditHandle buildEditControls(
			ProjectContext context, Window window, TabPane tabPane
	) {
		return new GroupNodeEditHandle(this, context, tabPane) {
			@Override
			public Tab buildNodeTab(ProjectContext context) {
				return new Tab("Crop",
						pad(new WidgetFormBuilder()
								.apply(b -> cleanup.add(nodeFormFields(context, b, CameraWrapper.this)))
								.intSpinner("Crop width", 1, 99999, s -> {
									cleanup.add(CustomBinding.bindBidirectionalMultiple(new CustomBinding.Binder<Integer>(width,
													() -> Optional.of(width.get()),
													width::set
											),
											new CustomBinding.Binder<Integer>(s.getValueFactory().valueProperty(),
													() -> Optional.of(s.getValueFactory().valueProperty().get()),
													s.getValueFactory().valueProperty()::set
											)
									));
									s
											.focusedProperty()
											.addListener((observable, oldValue, newValue) -> context.history.finishChange());
									s.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
										context.history.finishChange();
									});
								})
								.intSpinner("Crop height", 1, 99999, s -> {
									cleanup.add(CustomBinding.bindBidirectionalMultiple(new CustomBinding.Binder<Integer>(height,
													() -> Optional.of(height.get()),
													height::set
											),
											new CustomBinding.Binder<Integer>(s.getValueFactory().valueProperty(),
													() -> Optional.of(s.getValueFactory().valueProperty().get()),
													s.getValueFactory().valueProperty()::set
											)
									));
									s
											.focusedProperty()
											.addListener((observable, oldValue, newValue) -> context.history.finishChange());
									s.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
										context.history.finishChange();
									});
								})
								.intSpinner("End frame", 1, 99999, s -> {
									Camera.EndSetListener listener = node.addEndSetListeners((target, value) -> s
											.getValueFactory()
											.setValue(value));
									cleanup.add(() -> node.removeEndSetListeners(listener));
									s
											.getValueFactory()
											.valueProperty()
											.addListener((observable, oldValue, newValue) -> context.history.change(c -> c
													.camera(node)
													.endSet(newValue)));
									s
											.focusedProperty()
											.addListener((observable, oldValue, newValue) -> context.history.finishChange());
									s.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
										context.history.finishChange();
									});
								})
								.doubleSpinner("Framerate", 1, 999, 1, s -> {
									Camera.FrameRateSetListener listener =
											node.addFrameRateSetListeners((target, value) -> s
													.getValueFactory()
													.setValue(value / 10.0));
									cleanup.add(() -> node.removeFrameRateSetListeners(listener));
									s
											.getValueFactory()
											.valueProperty()
											.addListener((observable, oldValue, newValue) -> context.history.change(c -> c
													.camera(node)
													.frameRateSet((int) (newValue * 10.0))));
									s
											.focusedProperty()
											.addListener((observable, oldValue, newValue) -> context.history.finishChange());
									s.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
										context.history.finishChange();
									});
								}).<CameraNodeConfig.RenderMode>dropDown("Render type", d -> {
									Supplier<ListCell<CameraNodeConfig.RenderMode>> cellSupplier =
											() -> new ListCell<>() {
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
									d.getSelectionModel().select(config.renderMode);
									d
											.getSelectionModel()
											.selectedItemProperty()
											.addListener((observable, oldValue, newValue) -> config.renderMode =
													newValue);
								}).chooseDirectory("Render path", s -> {
									s.setValue(config.renderDir);
									s.addListener((observable, oldValue, newValue) -> config.renderDir = newValue);
								}).text("Render name", t -> {
									t.setText(config.renderName);
									t
											.textProperty()
											.addListener((observable, oldValue, newValue) -> config.renderName =
													newValue);
								}).button(button -> {
									button.setText("Render");
									button.setOnAction(e -> uncheck(() -> {
										Path dir = Paths.get(config.renderDir);
										Files.createDirectories(dir);
										switch (config.renderMode) {
											case PNG:
												render(context,
														window,
														(i, canvas) -> canvas.serialize(dir
																.resolve(String.format("%s.png", config.renderName))
																.toString()),
														canvasHandle.frameNumber.get(),
														canvasHandle.frameNumber.get() + 1
												);
												break;
											case WEBM: {
												SequenceEncoder encoder = SequenceEncoder.createSequenceEncoder(dir
														.resolve(String.format("%s.webm", config.renderName))
														.toFile(), (int) (node.frameRate() / 10.0));
												Picture rgbPic = Picture.create(node.width(), node.height(), RGB);
												byte[] rgb = rgbPic.getPlaneData(0);
												render(context, window, (i, canvas) -> {
													byte[] bgra = canvas.data();
													for (int y = 0; y < node.height(); ++y) {
														for (int x = 0; x < node.width(); ++x) {
															int inBase = (y * node.width() + x) * 4;
															int outBase = (y * node.width() + x) * 3;
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
																config.renderName
														)))
												) {
													GifEncoder gifEncoder =
															new GifEncoder(stream, node.width(), node.height(), 0);
													Method addImage = GifEncoder.class.getDeclaredMethod("addImage",
															Image.class,
															ImageOptions.class
													);
													addImage.setAccessible(true);

													ImageOptions options = new ImageOptions();
													options.setDelay(
															TimeUnit.MICROSECONDS.convert(1, TimeUnit.SECONDS) /
																	node.frameRate(),
															TimeUnit.MICROSECONDS
													);

													com.squareup.gifencoder.Color[][] rgb =
															new com.squareup.gifencoder.Color[node.height()][node.width()];
													render(context, window, (i, canvas) -> {
														byte[] bgra = canvas.data();
														for (int y = 0; y < node.height(); ++y) {
															for (int x = 0; x < node.width(); ++x) {
																int base = (y * node.width() + x) * 4;
																double b = Byte.toUnsignedInt(bgra[base]) / 255.0;
																double g = Byte.toUnsignedInt(bgra[base + 1]) / 255.0;
																double r = Byte.toUnsignedInt(bgra[base + 2]) / 255.0;
																double a = Byte.toUnsignedInt(bgra[base + 3]) / 255.0;
																rgb[y][x] = new com.squareup.gifencoder.Color(r * a +
																		(1.0 - a),
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
												render(context,
														window,
														(i, canvas) -> canvas.serialize(dir
																.resolve(String.format("%s%06d.png",
																		config.renderName,
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
		};
	}

	public void render(
			ProjectContext context, Window window, BiConsumer<Integer, TrueColorImage> frameConsumer
	) {
		render(context, window, frameConsumer, 0, node.end());
	}

	public void render(
			ProjectContext context, Window window, BiConsumer<Integer, TrueColorImage> frameConsumer, int start, int end
	) {
		ProgressBar progress = new ProgressBar();
		progress.setPadding(new Insets(3));
		Stage dialog = new Stage();
		dialog.setTitle("Rendering");
		dialog.initOwner(window.stage);
		dialog.initModality(Modality.APPLICATION_MODAL);
		dialog.setScene(new Scene(progress));
		AtomicBoolean cancel = new AtomicBoolean(false);
		new Thread(() -> {
			try {
				TrueColorImage canvas = TrueColorImage.create(node.width(), node.height());
				for (int i = start; i < end; ++i) {
					if (cancel.get())
						return;
					if (i != start)
						canvas.clear();
					context.lock.readLock().lock();
					try {
						Render.render(context,
								node,
								canvas,
								i,
								new com.zarbosoft.pyxyzygy.seed.model.Rectangle(-width.get() / 2,
										-height.get() / 2,
										width.get(),
										height.get()
								),
								1.0
						);
					} finally {
						context.lock.readLock().unlock();
					}
					frameConsumer.accept(i, canvas);
					final double percent = ((double) (i - start)) / (end - start);
					Platform.runLater(() -> progress.setProgress(percent));
				}
			} finally {
				Platform.runLater(() -> dialog.hide());
			}
		}).start();
		dialog.setOnCloseRequest(e -> {
			cancel.set(true);
			e.consume();
		});
		dialog.showAndWait();
	}
}
