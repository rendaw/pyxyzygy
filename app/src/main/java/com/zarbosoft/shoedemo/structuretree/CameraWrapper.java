package com.zarbosoft.shoedemo.structuretree;

import com.zarbosoft.shoedemo.*;
import com.zarbosoft.shoedemo.model.Camera;
import com.zarbosoft.shoedemo.model.ProjectNode;
import com.zarbosoft.shoedemo.model.ProjectObject;
import com.zarbosoft.shoedemo.model.Vector;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Spinner;
import javafx.scene.control.TreeItem;
import javafx.scene.shape.Rectangle;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.zarbosoft.rendaw.common.Common.uncheck;
import static com.zarbosoft.shoedemo.Main.nodeFormFields;
import static com.zarbosoft.shoedemo.Main.opacityMax;
import static com.zarbosoft.shoedemo.ProjectContext.uniqueName1;
import static com.zarbosoft.shoedemo.structuretree.ImageNodeWrapper.snapshot;

public class CameraWrapper extends Wrapper {
	private final ProjectContext context;
	private final Wrapper parent;
	private final Camera node;
	private final ProjectNode.OpacitySetListener opacityListener;
	private final ChangeListener<TreeItem<Wrapper>> childTreeListener;
	private final Camera.InnerSetListener innerSetListener;
	private final Camera.WidthSetListener widthListener;
	private final Camera.HeightSetListener heightListener;
	private final Camera.EndSetListener endListener;
	private final Camera.FrameRateSetListener framerateListener;
	private final Camera.OffsetSetListener offsetListener;
	private final SimpleObjectProperty<Wrapper> child = new SimpleObjectProperty<>();

	Group canvas;
	Group canvasChildren;
	Rectangle cameraBorder;
	private DoubleRectangle baseViewBounds;
	private DoubleRectangle viewBounds;

	private Spinner<Integer> propertiesWidth;
	private Spinner<Integer> propertiesHeight;
	private Spinner<Integer> propertiesEndFrame;
	private Spinner<Double> propertiesFrameRate;
	private Path renderPath;
	private com.zarbosoft.shoedemo.model.Rectangle crop;
	private DoubleVector markStart;
	private Vector markStartOffset;

	public CameraWrapper(ProjectContext context, Wrapper parent, int parentIndex, Camera node) {
		this.context = context;
		this.parent = parent;
		this.parentIndex = parentIndex;
		this.node = node;
		tree.set(new TreeItem<>(this));
		this.opacityListener = node.addOpacitySetListeners((target, value) -> {
			if (canvas != null) {
				canvasChildren.setOpacity((double)value / opacityMax);
			}
		});
		this.childTreeListener = (observable, oldValue, newValue) -> {
			tree.get().getChildren().setAll(newValue);
		};
		this.innerSetListener = node.addInnerSetListeners((target, value) -> {
			if (child.get() != null) {
				child.get().tree.removeListener(childTreeListener);
				child.get().remove(context);
			}
			tree.get().getChildren().clear();
			if (value != null) {
				child .set(Window.createNode(context, this, 0, value));
				child.get().tree.addListener(childTreeListener);
				tree.get().getChildren().add(child.get().tree.getValue());
			}
		});
		this.widthListener = node.addWidthSetListeners((target, value) -> {
			updateCameraBorder();
			if (propertiesWidth != null)
				propertiesWidth.getValueFactory().setValue(value);
		});
		this.heightListener = node.addHeightSetListeners((target, value) -> {
			updateCameraBorder();
			if (propertiesHeight != null)
				propertiesHeight.getValueFactory().setValue(value);
		});
		this.endListener = node.addEndSetListeners((target, value) -> {
			if (propertiesEndFrame != null)
				propertiesEndFrame.getValueFactory().setValue(value);
		});
		this.framerateListener = node.addFrameRateSetListeners((target, value) -> {
			if (propertiesFrameRate != null)
				propertiesFrameRate.getValueFactory().setValue((double) (value * 10));
		});
		this.offsetListener = node.addOffsetSetListeners((target, value) -> {
			updateOffset();
		});
	}

	private void updateOffset() {
		DoubleRectangle newViewBounds = baseViewBounds.plus(node.offset());
		scroll(context, viewBounds, baseViewBounds.plus(node.offset()));
		viewBounds = newViewBounds;
		if (canvasChildren == null)
			return;
		canvasChildren.setLayoutX(node.offset().x);
		canvasChildren.setLayoutY(node.offset().y);
	}

	private void updateCameraBorder() {
		if (cameraBorder == null)
			return;
		this.crop = new com.zarbosoft.shoedemo.model.Rectangle(-node.width() / 2,
				-node.height() / 2,
				node.width(),
				node.height()
		);
		cameraBorder.setWidth(crop.width);
		cameraBorder.setLayoutX(crop.x);
		cameraBorder.setHeight(crop.height);
		cameraBorder.setLayoutY(crop.y);
	}

	@Override
	public void remove(ProjectContext context) {
		node.removeOpacitySetListeners(opacityListener);
		node.removeInnerSetListeners(innerSetListener);
		node.removeWidthSetListeners(widthListener);
		node.removeHeightSetListeners(heightListener);
		node.removeEndSetListeners(endListener);
		node.removeFrameRateSetListeners(framerateListener);
		node.removeOffsetSetListeners(offsetListener);
	}

	@Override
	public Wrapper getParent() {
		return parent;
	}

	@Override
	public DoubleVector toInner(DoubleVector vector) {
		return vector.minus(node.offset());
	}

	@Override
	public ProjectObject getValue() {
		return node;
	}

	@Override
	public void scroll(
			ProjectContext context, DoubleRectangle oldBounds, DoubleRectangle newBounds
	) {
		this.baseViewBounds = newBounds;
		DoubleRectangle newViewBounds = baseViewBounds.plus(node.offset());
		if (child != null) {
			child.get().scroll(context, viewBounds, newViewBounds);
		}
		viewBounds = newViewBounds;
	}

	@Override
	public WidgetHandle buildCanvas(ProjectContext context, DoubleRectangle bounds) {
		return new WidgetHandle() {
			private final ChangeListener<? super Wrapper> childListener;
			WidgetHandle childCanvas;

			{
				baseViewBounds = bounds;
				viewBounds = bounds.plus(node.offset());
				canvas = new Group();
				canvasChildren = new Group();
				cameraBorder = new Rectangle();
				updateOffset();
				updateCameraBorder();
				canvas.getChildren().addAll(canvasChildren, cameraBorder);
				child.addListener(this.childListener = (observable, oldValue, newValue) -> {
					if (oldValue != null) {
						canvasChildren.getChildren().clear();
						childCanvas.remove();
						childCanvas = null;
					}
					if (newValue != null) {
						childCanvas = newValue.buildCanvas(context, viewBounds);
						canvasChildren.getChildren().add(0, childCanvas.getWidget());
					}
				});
				childListener.changed(null,null ,child.get() );
			}
			@Override
			public Node getWidget() {
				return canvas;
			}

			@Override
			public void remove() {
				child.removeListener(childListener);
				canvas = null;
				canvasChildren = null;
				cameraBorder = null;
			}
		};
	}

	@Override
	public void markStart(ProjectContext context, DoubleVector start) {
		this.markStart = start;
		this.markStartOffset = node.offset();
	}

	@Override
	public void mark(ProjectContext context, DoubleVector start, DoubleVector end) {
		context.history.change(c -> c.camera(node).offsetSet(end.minus(markStart).plus(markStartOffset).toInt()));
	}

	@Override
	public boolean addChildren(ProjectContext context, int at, List<ProjectNode> child) {
		if (child.size() > 1)
			return false;
		context.history.change(c -> c.camera(node).innerSet(child.get(0)));
		return true;
	}

	@Override
	public void delete(ProjectContext context) {
		if (parent != null)
			parent.removeChild(context, parentIndex);
		else
			this.context.history.change(c -> c.project(this.context.project).topRemove(parentIndex, 1));
	}

	@Override
	public ProjectNode separateClone(ProjectContext context) {
		Camera clone = Camera.create(this.context);
		clone.initialNameSet(context, uniqueName1(node.name()));
		clone.initialOpacitySet(context, node.opacity());
		clone.initialWidthSet(context, node.width());
		clone.initialHeightSet(context, node.height());
		clone.initialFrameRateSet(context, node.frameRate());
		clone.initialEndSet(context, node.end());
		clone.initialInnerSet(context, node.inner());
		clone.initialOffsetSet(context, node.offset());
		return clone;
	}

	@Override
	public void render(GraphicsContext gc, int frame, com.zarbosoft.shoedemo.model.Rectangle crop) {
		if (child.get() == null)
			return;
		child.get().render(gc, frame, crop.plus(node.offset()));
	}

	@Override
	public void removeChild(ProjectContext context, int index) {
		context.history.change(c -> c.camera(node).innerSet(null));
	}

	@Override
	public TakesChildren takesChildren() {
		return TakesChildren.ONE;
	}

	@Override
	public void setFrame(ProjectContext context, int frameNumber) {
		if (child.get() == null)
			return;
		child.get().setFrame(context, frameNumber);
	}

	@Override
	public WidgetHandle createProperties(ProjectContext context) {
		List<Runnable> miscCleanup = new ArrayList<>();
		Node widget = new WidgetFormBuilder()
				.apply(b -> miscCleanup.add(nodeFormFields(context, b, node)))
				.intSpinner("Crop width", 1, 99999, s -> {
					s.getValueFactory().setValue(node.width());
					propertiesWidth = s;
				})
				.intSpinner("Crop height", 1, 99999, s -> {
					s.getValueFactory().setValue(node.height());
					propertiesHeight = s;
				})
				.intSpinner("End frame", 1, 99999, s -> {
					s.getValueFactory().setValue(node.end());
					propertiesEndFrame = s;
				})
				.doubleSpinner("Framerate", 1, 999, 1, s -> {
					s.getValueFactory().setValue(node.frameRate() / 10.0);
					propertiesFrameRate = s;
				})
				.chooseDirectory("Render path", s -> {
					if (renderPath != null)
						s.setValue(renderPath.toString());
					s.addListener((observable, oldValue, newValue) -> renderPath = Paths.get(newValue));
				})
				.button(b -> {
					b.setText("Render");
					b.setOnAction(e -> uncheck(() -> {
						if (renderPath == null)
							return;
						Files.createDirectories(renderPath);
						Canvas canvas = new Canvas();
						canvas.setWidth(node.width());
						canvas.setHeight(node.height());
						GraphicsContext gc = canvas.getGraphicsContext2D();
						for (int i = 0; i < node.end(); ++i) {
							gc.clearRect(0, 0, node.width(), node.height());
							child.get().render(gc, i, crop);
							ImageIO.write(SwingFXUtils.fromFXImage(snapshot(canvas), null),
									"PNG",
									renderPath.resolve(String.format("frame%06d.png", i)).toFile()
							);
						}
					}));
				})
				.build();
		return new WidgetHandle() {
			@Override
			public Node getWidget() {
				return widget;
			}

			@Override
			public void remove() {
				propertiesWidth = null;
				propertiesHeight = null;
				propertiesEndFrame = null;
				propertiesFrameRate = null;
				miscCleanup.forEach(c -> c.run());
			}
		};
	}
}
