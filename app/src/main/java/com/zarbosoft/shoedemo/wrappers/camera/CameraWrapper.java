package com.zarbosoft.shoedemo.wrappers.camera;

import com.zarbosoft.shoedemo.*;
import com.zarbosoft.shoedemo.model.Camera;
import com.zarbosoft.shoedemo.model.ProjectNode;
import com.zarbosoft.shoedemo.widgets.HelperJFX;
import com.zarbosoft.shoedemo.widgets.WidgetFormBuilder;
import com.zarbosoft.shoedemo.wrappers.group.GroupNodeWrapper;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.zarbosoft.rendaw.common.Common.uncheck;
import static com.zarbosoft.shoedemo.Main.nodeFormFields;

public class CameraWrapper extends GroupNodeWrapper {
	private final Camera node;
	private final Camera.WidthSetListener widthListener;
	private final Camera.HeightSetListener heightListener;
	private final Camera.EndSetListener endListener;
	private final Camera.FrameRateSetListener framerateListener;

	Group canvas;
	Rectangle cameraBorder;

	private Spinner<Integer> propertiesWidth;
	private Spinner<Integer> propertiesHeight;
	private Spinner<Integer> propertiesEndFrame;
	private Spinner<Double> propertiesFrameRate;
	private Path renderPath;
	private com.zarbosoft.shoedemo.model.Rectangle crop;

	public CameraWrapper(ProjectContext context, Wrapper parent, int parentIndex, Camera node) {
		super(context, parent, parentIndex, node);
		this.parentIndex = parentIndex;
		this.node = node;
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
		node.removeWidthSetListeners(widthListener);
		node.removeHeightSetListeners(heightListener);
		node.removeEndSetListeners(endListener);
		node.removeFrameRateSetListeners(framerateListener);
		super.remove(context);
	}

	@Override
	public WidgetHandle buildCanvas(ProjectContext context) {
		return new WidgetHandle() {
			WidgetHandle groupHandle = CameraWrapper.super.buildCanvas(context);

			{
				canvas = new Group();
				cameraBorder = new Rectangle();
				cameraBorder.setStrokeWidth(1);
				cameraBorder.setStrokeType(StrokeType.OUTSIDE);
				cameraBorder.setFill(Color.TRANSPARENT);
				cameraBorder.setStroke(HelperJFX.c(new java.awt.Color(128, 128, 128)));
				updateCameraBorder();
				canvas.getChildren().addAll(groupHandle.getWidget(), cameraBorder);
			}

			@Override
			public Node getWidget() {
				return canvas;
			}

			@Override
			public void remove() {
				groupHandle.remove();
				canvas = null;
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
	public EditControlsHandle buildEditControls(ProjectContext context, TabPane tabPane) {
		List<Runnable> miscCleanup = new ArrayList<>();
		Tab tab = new Tab("Camera", new WidgetFormBuilder()
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
						TrueColorImage canvas = TrueColorImage.create(node.width(), node.height());
						for (int i = 0; i < node.end(); ++i) {
							if (i != 0)
								canvas.clear();
							Render.render(context,node, canvas ,i ,crop , 1.0);
							canvas.serialize(renderPath.resolve(String.format("frame%06d.png", i)).toString());
						}
					}));
				})
				.build());
		tabPane.getTabs().addAll(tab);
		return new EditControlsHandle() {
			@Override
			public Node getProperties() {
				return null;
			}

			@Override
			public void remove(ProjectContext context) {
				tabPane.getTabs().removeAll(tab);
				propertiesWidth = null;
				propertiesHeight = null;
				propertiesEndFrame = null;
				propertiesFrameRate = null;
				miscCleanup.forEach(c -> c.run());
			}
		};
	}
}
