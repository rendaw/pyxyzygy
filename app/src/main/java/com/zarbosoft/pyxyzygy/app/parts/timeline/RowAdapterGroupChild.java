package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.automodel.lib.Listener;
import com.zarbosoft.javafxbinders.BinderRoot;
import com.zarbosoft.javafxbinders.CustomBinding;
import com.zarbosoft.javafxbinders.HalfBinder;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.WidgetHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeWrapper;
import com.zarbosoft.pyxyzygy.core.model.latest.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupChild;
import com.zarbosoft.pyxyzygy.core.model.latest.ProjectLayer;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;

import static com.zarbosoft.javafxbinders.Helper.opt;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.icon;

public class RowAdapterGroupChild extends RowAdapter {
  private final GroupChild child;
  private final BinderRoot cleanIconBind;
  public final HalfBinder<Boolean> selected;
  SimpleObjectProperty<Image> stateImage = new SimpleObjectProperty<>(null);
  Runnable nameCleanup;
  private Listener.ScalarSet<GroupChild, ProjectLayer> innerListener;

  RowAdapterGroupChild(GroupNodeWrapper wrapper, GroupChild child) {
    this.child = child;
    this.selected = wrapper.specificChild.map(c -> opt(c == child));
    cleanIconBind =
        CustomBinding.bind(
            stateImage,
            selected.map(s -> opt(s ? icon("editing.png") : null)));
  }

  @Override
  public ObservableValue<String> getName() {
    SimpleStringProperty out = new SimpleStringProperty();
    innerListener =
        child.addInnerSetListeners(
            (target, inner) -> {
              if (nameCleanup != null) {
                nameCleanup.run();
                nameCleanup = null;
              }
              if (inner != null) {
                Listener.ScalarSet<ProjectLayer, String> nameListener =
                    inner.addNameSetListeners((target1, name) -> out.setValue(name));
                nameCleanup =
                    () -> {
                      inner.removeNameSetListeners(nameListener);
                    };
              }
            });
    return out;
  }

  @Override
  public void remove(Context context) {
    child.removeInnerSetListeners(innerListener);
    cleanIconBind.destroy();
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
    return child;
  }

  @Override
  public boolean isMain() {
    return false;
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
    Context context, Window window, ChangeStepBuilder change, int outer) {
    throw new Assertion();
  }

  @Override
  public boolean duplicateFrame(
    Context context, Window window, ChangeStepBuilder change, int outer) {
    throw new Assertion();
  }

  @Override
  public WidgetHandle createRowWidget(Context context, Window window) {
    return null;
  }

  @Override
  public int updateFrames(Context context, Window window) {
    return 0;
  }

  @Override
  public void updateFrameMarker(Context context, Window window) {}

  @Override
  public ObservableObjectValue<Image> getStateImage() {
    return stateImage;
  }
}
