package com.zarbosoft.shoedemo.wrappers.group;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.internal.shoedemo_seed.model.Rectangle;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.shoedemo.*;
import com.zarbosoft.shoedemo.model.*;
import com.zarbosoft.shoedemo.modelmirror.*;
import com.zarbosoft.shoedemo.widgets.HelperJFX;
import com.zarbosoft.shoedemo.widgets.WidgetFormBuilder;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.shape.Line;
import javafx.scene.transform.Scale;

import java.util.HashMap;
import java.util.Map;

public class ToolStamp extends Tool {
	private final Node node;
	private final MirrorProject mirror;
	private final GroupNodeEditHandle editHandle;
	private ProjectNode stampSource;
	private final SimpleObjectProperty<Rectangle> stampOverlayBounds =
			new SimpleObjectProperty<>(new Rectangle(0, 0, 0, 0));
	private GroupNodeWrapper groupNodeWrapper;
	private final Group overlayGroup;

	ToolStamp(GroupNodeWrapper groupNodeWrapper, ProjectContext context, GroupNodeEditHandle editHandle) {
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
		ObjectMirror found = lookup.get(groupNodeWrapper.config.stampSource.get());
		if (found != null)
			tree.getSelectionModel().select(found.tree.get());
		ImageView stampOverlayImage = new ImageView();
		stampOverlayImage.setOpacity(0.5);
		overlayGroup = new Group();
		groupNodeWrapper.positiveZoom.addListener((observable, oldValue, newValue) -> overlayGroup
				.getTransforms()
				.setAll(new Scale(
						1.0 / groupNodeWrapper.positiveZoom.get(),
						1.0 / groupNodeWrapper.positiveZoom.get()
				)));
		overlayGroup
				.layoutXProperty()
				.bind(Bindings.createDoubleBinding(() -> (double) groupNodeWrapper.mousePosition.get().x,
						groupNodeWrapper.mousePosition
				));
		overlayGroup
				.layoutYProperty()
				.bind(Bindings.createDoubleBinding(() -> (double) groupNodeWrapper.mousePosition.get().y,
						groupNodeWrapper.mousePosition
				));
		Line overlayMarker = new Line(0, 0, 10, 10);
		overlayGroup.getChildren().addAll(stampOverlayImage, overlayMarker);
		editHandle.overlay.getChildren().add(overlayGroup);
		Runnable updateStampImage = () -> {
			stampOverlayImage.setImage(null);
			TreeItem<ObjectMirror> item = tree.getSelectionModel().getSelectedItem();
			if (item == null)
				return;
			groupNodeWrapper.config.stampSource.set(item.getValue().getValue().id());
			stampSource = (ProjectNode) item.getValue().getValue();
			stampOverlayBounds.set(Render.bounds(context, stampSource, 0));
			if (stampOverlayBounds.get().width == 0 || stampOverlayBounds.get().height == 0)
				return;
			TrueColorImage gc = TrueColorImage.create(stampOverlayBounds.get().width, stampOverlayBounds.get().height);
			Render.render(context, stampSource, gc, 0, stampOverlayBounds.get(), 1);
			stampOverlayImage.setImage(HelperJFX.toImage(gc, groupNodeWrapper.positiveZoom.get()));
			stampOverlayImage.setLayoutX((double) stampOverlayBounds.get().x * groupNodeWrapper.positiveZoom.get());
			stampOverlayImage.setLayoutY((double) stampOverlayBounds.get().y * groupNodeWrapper.positiveZoom.get());
		};
		tree
				.getSelectionModel()
				.selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> updateStampImage.run());
		groupNodeWrapper.positiveZoom.addListener((observable, oldValue, newValue) -> updateStampImage.run());
		node = new WidgetFormBuilder().span(() -> {
			return tree;
		}).build();
		this.groupNodeWrapper = groupNodeWrapper;
	}

	@Override
	public void markStart(ProjectContext context, DoubleVector start) {
		if (stampSource == null)
			return;
		GroupLayer layer = GroupLayer.create(context);
		layer.initialInnerSet(context, stampSource);
		GroupPositionFrame positionFrame = GroupPositionFrame.create(context);
		positionFrame.initialLengthSet(context, -1);
		positionFrame.initialOffsetSet(context, groupNodeWrapper.mousePosition.get());
		layer.initialPositionFramesAdd(context, ImmutableList.of(positionFrame));
		GroupTimeFrame timeFrame = GroupTimeFrame.create(context);
		timeFrame.initialLengthSet(context, -1);
		timeFrame.initialInnerOffsetSet(context, 0);
		timeFrame.initialInnerLoopSet(context, 0);
		layer.initialTimeFramesAdd(context, ImmutableList.of(timeFrame));
		context.history.change(c -> c.groupNode(groupNodeWrapper.node).layersAdd(layer));
	}

	@Override
	public void mark(ProjectContext context, DoubleVector start, DoubleVector end) {

	}

	@Override
	public Node getProperties() {
		return node;
	}

	@Override
	public void remove(ProjectContext context) {
		editHandle.overlay.getChildren().remove(overlayGroup);
		mirror.remove(context);
	}
}
