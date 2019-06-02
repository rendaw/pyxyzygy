package com.zarbosoft.pyxyzygy.app.wrappers.camera;

import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.config.CameraNodeConfig;
import com.zarbosoft.pyxyzygy.app.config.GroupNodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.binding.BinderRoot;
import com.zarbosoft.pyxyzygy.app.widgets.binding.CustomBinding;
import com.zarbosoft.pyxyzygy.app.widgets.binding.PropertyBinder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.ScalarBinder;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeCanvasHandle;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeWrapper;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.v0.Camera;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectLayer;
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

public class CameraWrapper extends GroupNodeWrapper {
	public final Camera node;
	public final SimpleIntegerProperty width = new SimpleIntegerProperty(0);
	public final SimpleIntegerProperty height = new SimpleIntegerProperty(0);
	private final BinderRoot cleanupWidth;
	private final BinderRoot cleanupHeight;
	public boolean adjustViewport = false;
	private ProjectContext.Tuple actionWidthChange = new ProjectContext.Tuple(this, "width");
	private ProjectContext.Tuple actionHeightChange = new ProjectContext.Tuple(this, "height");

	public CameraNodeConfig config;

	public CameraWrapper(ProjectContext context, Wrapper parent, int parentIndex, Camera node) {
		super(context, parent, parentIndex, node);
		this.node = node;
		cleanupWidth = CustomBinding.bindBidirectional(new ScalarBinder<Integer>(node,
						"width",
						v -> context.change(actionWidthChange, c -> c.camera(node).widthSet(v))
				),
				new PropertyBinder<>(width.asObject())
		);
		cleanupHeight = CustomBinding.bindBidirectional(new ScalarBinder<Integer>(node,
						"height",
						v -> context.change(actionHeightChange, c -> c.camera(node).heightSet(v))
				),
				new PropertyBinder<>(height.asObject())
		);
	}

	@Override
	protected GroupNodeConfig initConfig(ProjectContext context, long id) {
		return this.config =
				(CameraNodeConfig) context.config.nodes.computeIfAbsent(id, id1 -> new CameraNodeConfig(context));
	}

	@Override
	public void remove(ProjectContext context) {
		cleanupWidth.destroy();
		cleanupHeight.destroy();
		super.remove(context);
	}

	@Override
	public CanvasHandle buildCanvas(
			ProjectContext context, Window window, CanvasHandle parent
	) {
		if (canvasHandle == null) {
			class CameraCanvasHandle extends GroupNodeCanvasHandle {
				Rectangle cameraBorder;

				public CameraCanvasHandle(
						ProjectContext context, Window window, GroupNodeWrapper wrapper
				) {
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
				public void remove(ProjectContext context, Wrapper excludeSubtree) {
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
	public ProjectLayer separateClone(ProjectContext context) {
		Camera clone = Camera.create(context);
		cloneSet(context, clone);
		clone.initialOffsetSet(context, node.offset());
		clone.initialWidthSet(context, node.width());
		clone.initialHeightSet(context, node.height());
		clone.initialFrameRateSet(context, node.frameRate());
		clone.initialFrameStartSet(context, node.frameStart());
		clone.initialFrameLengthSet(context, node.frameLength());
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
		Window.DialogBuilder builder = window.dialog("Rendering");
		ProgressBar progress = new ProgressBar();
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
				Platform.runLater(() -> builder.close());
			}
		}).start();
		builder.addContent(progress).addAction(ButtonType.CANCEL, true, () -> {
			cancel.set(true);
			return true;
		}).go();
	}

}
