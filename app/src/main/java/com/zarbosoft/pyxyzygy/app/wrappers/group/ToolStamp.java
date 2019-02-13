package com.zarbosoft.pyxyzygy.app.wrappers.group;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.modelmirror.*;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.v0.*;
import com.zarbosoft.pyxyzygy.seed.model.v0.Rectangle;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Line;
import javafx.scene.transform.Scale;

import java.util.HashMap;
import java.util.Map;

import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.pad;

public class ToolStamp extends Tool {
	private final MirrorProject mirror;
	private final GroupNodeEditHandle editHandle;
	private ProjectNode stampSource;
	private final SimpleObjectProperty<Rectangle> stampOverlayBounds =
			new SimpleObjectProperty<>(new Rectangle(0, 0, 0, 0));
	private final GroupNodeWrapper wrapper;
	private final Group overlayGroup;

	ToolStamp(GroupNodeWrapper wrapper, ProjectContext context, GroupNodeEditHandle editHandle) {
		this.editHandle = editHandle;
		final TreeView<ObjectMirror> tree = new TreeView<>();
		tree.setCellFactory(param -> new TreeCell<ObjectMirror>() {
			Runnable cleanup;

			@Override
			protected void updateItem(ObjectMirror item, boolean empty) {
				if (cleanup != null) {
					cleanup.run();
				}
				if (item == null) {
					setText("");
				} else {
					final ProjectNode.NameSetListener nameListener = (target, value) -> {
						setText(value);
					};
					((ProjectNode) item.getValue()).addNameSetListeners(nameListener);
					cleanup = () -> {
						((ProjectNode) item.getValue()).removeNameSetListeners(nameListener);
					};
				}
				super.updateItem(item, empty);
			}
		});
		Map<Long, ObjectMirror> lookup = new HashMap<>();
		mirror = new MirrorProject(context, new ObjectMirror.Context() {
			@Override
			public ObjectMirror create(
					ProjectContext context, ObjectMirror parent, ProjectObject object
			) {
				ObjectMirror out;
				if (false) {
					throw new Assertion();
				} else if (object instanceof GroupNode) {
					out = new MirrorGroupNode(context, this, parent, (GroupNode) object);
				} else if (object instanceof GroupLayer) {
					out = new MirrorGroupLayer(context, this, parent, (GroupLayer) object);
				} else if (object instanceof TrueColorImageNode) {
					out = new MirrorTrueColorImageNode(parent, (TrueColorImageNode) object);
				} else
					throw new Assertion();
				lookup.put(object.id(), out);
				return out;
			}
		}, null, context.project);
		tree.setRoot(mirror.tree.get());
		tree.setShowRoot(false);
		ObjectMirror found = lookup.get(wrapper.config.stampSource.get());
		if (found != null)
			tree.getSelectionModel().select(found.tree.get());
		ImageView stampOverlayImage = new ImageView();
		stampOverlayImage.setOpacity(0.5);
		overlayGroup = new Group();
		wrapper.canvasHandle.positiveZoom.addListener((observable, oldValue, newValue) -> overlayGroup
				.getTransforms()
				.setAll(new Scale(1.0 / wrapper.canvasHandle.positiveZoom.get(),
						1.0 / wrapper.canvasHandle.positiveZoom.get()
				)));
		overlayGroup
				.layoutXProperty()
				.bind(Bindings.createDoubleBinding(() -> Math.floor(editHandle.mouseX.get()), editHandle.mouseX));
		overlayGroup
				.layoutYProperty()
				.bind(Bindings.createDoubleBinding(() -> Math.floor(editHandle.mouseY.get()), editHandle.mouseY));
		Line overlayMarker = new Line(0, 0, 10, 10);
		overlayGroup.getChildren().addAll(stampOverlayImage, overlayMarker);
		editHandle.overlay.getChildren().add(overlayGroup);
		Runnable updateStampImage = () -> {
			stampOverlayImage.setImage(null);
			TreeItem<ObjectMirror> item = tree.getSelectionModel().getSelectedItem();
			if (item == null)
				return;
			wrapper.config.stampSource.set(item.getValue().getValue().id());
			stampSource = (ProjectNode) item.getValue().getValue();
			stampOverlayBounds.set(Render.bounds(context, stampSource, 0));
			if (stampOverlayBounds.get().width == 0 || stampOverlayBounds.get().height == 0)
				return;
			TrueColorImage gc = TrueColorImage.create(stampOverlayBounds.get().width, stampOverlayBounds.get().height);
			Render.render(context, stampSource, gc, 0, stampOverlayBounds.get(), 1);
			stampOverlayImage.setImage(HelperJFX.toImage(gc));
			stampOverlayImage.setLayoutX((double) stampOverlayBounds.get().x * wrapper.canvasHandle.positiveZoom.get());
			stampOverlayImage.setLayoutY((double) stampOverlayBounds.get().y * wrapper.canvasHandle.positiveZoom.get());
		};
		tree
				.getSelectionModel()
				.selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> updateStampImage.run());
		wrapper.canvasHandle.positiveZoom.addListener((observable, oldValue, newValue) -> updateStampImage.run());
		editHandle.toolTab.setContent(pad(new WidgetFormBuilder().span(() -> {
			return tree;
		}).build()));
		this.wrapper = wrapper;
	}

	@Override
	public void markStart(ProjectContext context, Window window, DoubleVector start) {
		if (stampSource == null)
			return;
		GroupLayer layer = GroupLayer.create(context);
		layer.initialInnerSet(context, stampSource);
		GroupPositionFrame positionFrame = GroupPositionFrame.create(context);
		positionFrame.initialLengthSet(context, -1);
		positionFrame.initialOffsetSet(
				context,
				new Vector((int) Math.floor(editHandle.mouseX.get()), (int) Math.floor(editHandle.mouseY.get()))
		);
		layer.initialPositionFramesAdd(context, ImmutableList.of(positionFrame));
		GroupTimeFrame timeFrame = GroupTimeFrame.create(context);
		timeFrame.initialLengthSet(context, -1);
		timeFrame.initialInnerOffsetSet(context, 0);
		timeFrame.initialInnerLoopSet(context, 0);
		layer.initialTimeFramesAdd(context, ImmutableList.of(timeFrame));
		context.history.change(c -> c.groupNode(wrapper.node).layersAdd(layer));
	}

	@Override
	public void mark(ProjectContext context, Window window, DoubleVector start, DoubleVector end) {

	}

	@Override
	public void remove(ProjectContext context, Window window) {
		editHandle.overlay.getChildren().remove(overlayGroup);
		mirror.remove(context);
	}
}
