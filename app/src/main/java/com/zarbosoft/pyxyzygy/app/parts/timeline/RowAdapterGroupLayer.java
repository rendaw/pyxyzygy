package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.WidgetHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeWrapper;
import com.zarbosoft.pyxyzygy.core.model.v0.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.v0.GroupLayer;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectNode;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;

import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.icon;

public class RowAdapterGroupLayer extends RowAdapter {
	private final GroupNodeWrapper wrapper;
	private final GroupLayer layer;
	SimpleObjectProperty<Image> stateImage = new SimpleObjectProperty<>(null);
	Runnable nameCleanup;
	private Listener.ScalarSet<GroupLayer, ProjectNode> innerListener;

	RowAdapterGroupLayer(GroupNodeWrapper wrapper, GroupLayer layer) {
		this.wrapper = wrapper;
		this.layer = layer;
	}

	@Override
	public ObservableValue<String> getName() {
		SimpleStringProperty out = new SimpleStringProperty();
		innerListener = layer.addInnerSetListeners((target, inner) -> {
			if (nameCleanup != null) {
				nameCleanup.run();
				nameCleanup = null;
			};
			if (inner != null) {
				Listener.ScalarSet<ProjectNode, String> nameListener =
						inner.addNameSetListeners((target1, name) -> out.setValue(name));
				nameCleanup = () -> {
					inner.removeNameSetListeners(nameListener);
				};
			}
		});
		return out;
	}

	@Override
	public void remove(ProjectContext context) {
		layer.removeInnerSetListeners(innerListener);
		if (nameCleanup != null) {
			nameCleanup.run();
		}
	}

	@Override
	public boolean frameAt(Window window, int outer) {
		throw new Assertion();
	}

	@Override
	public Object getData() {
		return layer;
	}

	@Override
	public boolean hasFrames() {
		return false;
	}

	@Override
	public boolean hasNormalFrames() {
		return false;
	}

	@Override
	public boolean createFrame(
			ProjectContext context, Window window, ChangeStepBuilder change, int outer
	) {
		throw new Assertion();
	}

	@Override
	public boolean duplicateFrame(
			ProjectContext context, Window window, ChangeStepBuilder change, int outer
	) {
		throw new Assertion();
	}

	@Override
	public WidgetHandle createRowWidget(ProjectContext context, Window window) {
		return null;
	}

	@Override
	public int updateTime(ProjectContext context, Window window) {
		return 0;
	}

	@Override
	public void updateFrameMarker(ProjectContext context, Window window) {

	}

	@Override
	public ObservableObjectValue<Image> getStateImage() {
		return stateImage;
	}

	@Override
	public void deselected() {
		treeDeselected();
	}

	@Override
	public void selected() {
		treeSelected();
	}

	public void treeDeselected() {
		wrapper.setSpecificLayer(null);
		stateImage.set(null);
	}

	public void treeSelected() {
		wrapper.setSpecificLayer(layer);
		stateImage.set(icon("cursor-move16.png"));
	}
}
