package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.automodel.lib.ProjectObject;
import com.zarbosoft.pyxyzygy.app.config.NodeConfig;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.core.model.latest.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.latest.ProjectLayer;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeItem;

public abstract class Wrapper {
  public int parentIndex;
  public final SimpleObjectProperty<TreeItem<Wrapper>> tree = new SimpleObjectProperty<>();
  public final SimpleBooleanProperty tagLifted = new SimpleBooleanProperty(false);

  public abstract Wrapper getParent();

  public void setParentIndex(int index) {
    parentIndex = index;
  }

  public abstract ProjectObject getValue();

  public abstract NodeConfig getConfig();

  public abstract CanvasHandle buildCanvas(Context context, Window window, CanvasHandle parent);

  public abstract EditHandle buildEditControls(Context context, Window window);

  public abstract void remove(Context context);

  public void delete(Context context, ChangeStepBuilder change) {
    if (getParent() != null) getParent().deleteChild(context, change, parentIndex);
    else change.project(context.project).topRemove(parentIndex, 1);
  }

  public abstract ProjectLayer separateClone(Context context);

  public abstract void deleteChild(Context context, ChangeStepBuilder change, int index);

  public static enum TakesChildren {
    NONE,
    ANY
  }

  // TODO take this info to prevent calling addChildren if it wouldn't succeed, simplify that
  // signature
  public abstract TakesChildren takesChildren();

  public static class ToolToggle extends HelperJFX.IconToggleButton {
    private final Wrapper wrapper;
    private final String value;

    public ToolToggle(Wrapper wrapper, String icon, String hint, String value) {
      super(icon, hint);
      this.wrapper = wrapper;
      this.value = value;
      selectedProperty()
          .bind(
              Bindings.createBooleanBinding(
                  () -> {
                    return value.equals(this.wrapper.getConfig().tool.get());
                  },
                  this.wrapper.getConfig().tool));
      setMaxHeight(Double.MAX_VALUE);
    }

    @Override
    public void fire() {
      wrapper.getConfig().tool.set(value);
    }
  }
}
