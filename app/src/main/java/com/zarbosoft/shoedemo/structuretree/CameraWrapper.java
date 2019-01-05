package com.zarbosoft.shoedemo.structuretree;

import com.zarbosoft.shoedemo.*;
import com.zarbosoft.shoedemo.model.Camera;
import com.zarbosoft.shoedemo.model.ProjectNode;
import com.zarbosoft.shoedemo.model.ProjectObject;
import com.zarbosoft.shoedemo.model.Vector;
import javafx.beans.value.ChangeListener;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.shape.Rectangle;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static com.zarbosoft.rendaw.common.Common.uncheck;
import static com.zarbosoft.shoedemo.Main.uniqueName;
import static com.zarbosoft.shoedemo.Main.uniqueName1;
import static com.zarbosoft.shoedemo.structuretree.ImageNodeWrapper.snapshot;

public class CameraWrapper extends Wrapper {
	private final ProjectContext context;
	private final Wrapper parent;
	private final Camera node;
	private final ProjectNode.NameSetListener nameListener;
	private final ChangeListener<TreeItem<Wrapper>> childTreeListener;
	private final Camera.InnerSetListener innerSetListener;
	private final Camera.WidthSetListener widthListener;
	private final Camera.HeightSetListener heightListener;
	private final Camera.EndSetListener endListener;
	private final Camera.FrameRateSetListener framerateListener;
	private final Camera.OffsetSetListener offsetListener;
	private Wrapper child;

	Group canvas;
	Group canvasChildren;
	Rectangle cameraBorder;
	private DoubleRectangle baseViewBounds;
	private DoubleRectangle viewBounds;

	private TextField propertiesName;
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
		this.nameListener = node.addNameSetListeners((target, value) -> {
			if (propertiesName != null)
				propertiesName.setText(value);
		});
		this.childTreeListener = (observable, oldValue, newValue) -> {
			tree.get().getChildren().setAll(newValue);
		};
		this.innerSetListener = node.addInnerSetListeners((target, value) -> {
			if (child != null) {
				if (canvas != null) {
					canvas.getChildren().remove(child.getCanvas());
					child.tree.removeListener(childTreeListener);
					child.destroyCanvas();
				}
				child.remove(context);
			}
			tree.get().getChildren().clear();
			if (value != null) {
				child = Main.createNode(context, this, 0, value);
				child.tree.addListener(childTreeListener);
				tree.get().getChildren().add(child.tree.getValue());
				if (canvas != null) {
					canvas.getChildren().add(0, child.buildCanvas(context, viewBounds));
				}
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
		node.removeNameSetListeners(nameListener);
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
			child.scroll(context, viewBounds, newViewBounds);
		}
		viewBounds = newViewBounds;
	}

	@Override
	public Node buildCanvas(ProjectContext context, DoubleRectangle bounds) {
		this.baseViewBounds = bounds;
		this.viewBounds = bounds.plus(node.offset());
		canvas = new Group();
		canvasChildren = new Group();
		updateOffset();
		cameraBorder = new Rectangle();
		updateCameraBorder();
		if (child != null)
			canvasChildren.getChildren().addAll(child.buildCanvas(context, bounds), cameraBorder);
		canvas.getChildren().addAll(canvasChildren, cameraBorder);
		return canvas;
	}

	@Override
	public Node getCanvas() {
		return canvas;
	}

	@Override
	public void destroyCanvas() {
		canvas = null;
		canvasChildren = null;
		cameraBorder = null;
	}

	@Override
	public void markStart(ProjectContext context, DoubleVector start) {
		this.markStart = start;
		this.markStartOffset = node.offset();
	}

	@Override
	public void mark(ProjectContext context, DoubleVector start, DoubleVector end) {
		context.change.camera(node).offsetSet(end.minus(markStart).plus(markStartOffset).toInt());
	}

	@Override
	public boolean addChildren(ProjectContext context, int at, List<ProjectNode> child) {
		if (child.size() > 1) return false;
		context.change.camera(node).innerSet(child.get(0));
		return true;
	}

	@Override
	public void delete(ProjectContext context) {
		if (parent != null)
			parent.removeChild(context, parentIndex);
		else
			this.context.change.project(this.context.project).topRemove(parentIndex, 1);
	}

	@Override
	public ProjectNode separateClone(ProjectContext context) {
		Camera clone = Camera.create(this.context);
		clone.initialNameSet(context, uniqueName1(node.name()));
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
		if (child == null)
			return;
		child.render(gc, frame, crop.plus(node.offset()));
	}

	@Override
	public void removeChild(ProjectContext context, int index) {
		context.change.camera(node).innerSet(null);
	}

	@Override
	public TakesChildren takesChildren() {
		return TakesChildren.ONE;
	}

	@Override
	public void setFrame(ProjectContext context, int frameNumber) {
		if (child == null)
			return;
		child.setFrame(context, frameNumber);
	}

	@Override
	public Node createProperties(ProjectContext context) {
		return new WidgetFormBuilder().text("Name", t -> {
			propertiesName = t;
			t.setText(node.name());
			t
					.textProperty()
					.addListener((observable, oldValue, newValue) -> this.context.change
							.projectNode(node)
							.nameSet(newValue));
		}).intSpinner("Crop width", 1, 99999, s -> {
			propertiesWidth = s;
			s.getValueFactory().setValue(node.width());
		}).intSpinner("Crop height", 1, 99999, s -> {
			propertiesHeight = s;
			s.getValueFactory().setValue(node.height());
		}).intSpinner("End frame", 1, 99999, s -> {
			propertiesEndFrame = s;
			s.getValueFactory().setValue(node.end());
		}).doubleSpinner("Framerate", 1, 999, 1, s -> {
			propertiesFrameRate = s;
			s.getValueFactory().setValue(node.frameRate() / 10.0);
		}).chooseDirector("Render path", s -> {
			if (renderPath != null)
				s.setValue(renderPath.toString());
			s.addListener((observable, oldValue, newValue) -> renderPath = Paths.get(newValue));
		}).button(b -> {
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
					child.render(gc, i, crop);
					ImageIO.write(SwingFXUtils.fromFXImage(snapshot(canvas), null),
							"PNG",
							renderPath.resolve(String.format("frame%06d.png", i)).toFile()
					);
				}
			}));
		}).build();
	}

	@Override
	public void destroyProperties() {
		propertiesName = null;
		propertiesWidth = null;
		propertiesHeight = null;
		propertiesEndFrame = null;
		propertiesFrameRate = null;
	}
}
