package com.zarbosoft.pyxyzygy.app.wrappers.camera;

import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.config.CameraNodeConfig;
import com.zarbosoft.pyxyzygy.app.config.GroupNodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeCanvasHandle;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeWrapper;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.v0.Camera;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectNode;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

public class CameraWrapper extends GroupNodeWrapper {
	public final Camera node;
	public final SimpleIntegerProperty width = new SimpleIntegerProperty(0);
	public final SimpleIntegerProperty height = new SimpleIntegerProperty(0);
	private final Runnable cleanupWidth;
	private final Runnable cleanupHeight;
	public boolean adjustViewport = false;
	private ProjectContext.Tuple actionWidthChange = new ProjectContext.Tuple(this, "width");
	private ProjectContext.Tuple actionHeightChange = new ProjectContext.Tuple(this,"height");

	public CameraNodeConfig config;

	public CameraWrapper(ProjectContext context, Wrapper parent, int parentIndex, Camera node) {
		super(context, parent, parentIndex, node);
		this.node = node;
		cleanupWidth = CustomBinding.bindBidirectional(new CustomBinding.ScalarBinder<Integer>(
				node,
				"width",
				v -> context.change(actionWidthChange, c -> c.camera(node).widthSet(v))
		), new CustomBinding.PropertyBinder<>(width.asObject()));
		cleanupHeight = CustomBinding.bindBidirectional(new CustomBinding.ScalarBinder<Integer>(
				node,
				"height",
				v -> context.change(actionHeightChange, c -> c.camera(node).heightSet(v))
		), new CustomBinding.PropertyBinder<>(height.asObject()));
	}

	@Override
	protected GroupNodeConfig initConfig(ProjectContext context, long id) {
		return this.config =
				(CameraNodeConfig) context.config.nodes.computeIfAbsent(id, id1 -> new CameraNodeConfig(context));
	}

	@Override
	public void remove(ProjectContext context) {
		cleanupWidth.run();
		cleanupHeight.run();
		super.remove(context);
	}

	@Override
	public CanvasHandle buildCanvas(
			ProjectContext context, Window window, CanvasHandle parent
	) {
		return new GroupNodeCanvasHandle(context, window,parent, this) {
			Rectangle cameraBorder;
			CanvasHandle groupHandle = CameraWrapper.super.buildCanvas(context, window, parent);

			{
				cameraBorder = new Rectangle();
				cameraBorder.strokeWidthProperty().bind(Bindings.divide(1.0, window.editor.zoomFactor));
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
		clone.initialFrameStartSet(context,node.frameStart());
		clone.initialFrameLengthSet(context,node.frameLength());
		return clone;
	}

	@Override
	public EditHandle buildEditControls(
			ProjectContext context, Window window
	) {
		return new CameraEditHandle(context, window, this);
	}

	public void render(
			ProjectContext context, Window window, BiConsumer<Integer, TrueColorImage> frameConsumer, int scale
	) {
		render(context, window, frameConsumer, node.frameStart(), node.frameStart() + node.frameLength(), scale);
	}

	public void render(
			ProjectContext context,
			Window window,
			BiConsumer<Integer, TrueColorImage> frameConsumer,
			int start,
			int end,
			int scale
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
								new com.zarbosoft.pyxyzygy.seed.model.v0.Rectangle(-width.get() / 2,
										-height.get() / 2,
										width.get(),
										height.get()
								),
								1.0
						);
					} finally {
						context.lock.readLock().unlock();
					}
					if (scale > 1)
						canvas = canvas.scale(scale);
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
