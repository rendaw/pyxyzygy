package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.pyxyzygy.app.config.NodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.core.model.v0.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectLayer;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectObject;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeItem;

public abstract class Wrapper {
	public int parentIndex;
	public final SimpleObjectProperty<TreeItem<Wrapper>> tree = new SimpleObjectProperty<>();
	public final SimpleBooleanProperty tagLifted = new SimpleBooleanProperty(false);
	public CanvasHandle canvasParent;

	public abstract Wrapper getParent();

	public void setParentIndex(int index) {
		parentIndex = index;
	}

	public abstract ProjectObject getValue();

	public abstract NodeConfig getConfig();

	public abstract CanvasHandle getCanvas(
			ProjectContext context, Window window
	);

	public abstract EditHandle buildEditControls(
			ProjectContext context, Window window
	);

	public abstract void remove(ProjectContext context);

	public void delete(ProjectContext context, ChangeStepBuilder change) {
		if (getParent() != null)
			getParent().deleteChild(context, change, parentIndex);
		else
			change.project(context.project).topRemove(parentIndex, 1);
	}

	public abstract ProjectLayer separateClone(ProjectContext context);

	public abstract void deleteChild(
			ProjectContext context, ChangeStepBuilder change, int index
	);

	public void setCanvasParent(CanvasHandle canvasHandle) {
		this.canvasParent = canvasHandle;
	}

	public static enum TakesChildren {
		NONE,
		ANY
	}

	// TODO take this info to prevent calling addChildren if it wouldn't succeed, simplify that signature
	public abstract TakesChildren takesChildren();

	public static class ToolToggle extends HelperJFX.IconToggleButton {
		private final Wrapper wrapper;
		private final String value;

		public ToolToggle(Wrapper wrapper, String icon, String hint, String value) {
			super(icon, hint);
			this.wrapper = wrapper;
			this.value = value;
			selectedProperty().bind(Bindings.createBooleanBinding(() -> {
				return value.equals(this.wrapper.getConfig().tool.get());
			}, this.wrapper.getConfig().tool));
			setMaxHeight(Double.MAX_VALUE);
		}

		@Override
		public void fire() {
			wrapper.getConfig().tool.set(value);
		}
	}
}
