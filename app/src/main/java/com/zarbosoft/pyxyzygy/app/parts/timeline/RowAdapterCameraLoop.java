package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.WidgetHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.binding.BinderRoot;
import com.zarbosoft.pyxyzygy.app.widgets.binding.CustomBinding;
import com.zarbosoft.pyxyzygy.app.widgets.binding.PropertyBinder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.ScalarBinder;
import com.zarbosoft.pyxyzygy.core.model.v0.Camera;
import com.zarbosoft.pyxyzygy.core.model.v0.ChangeStepBuilder;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.image.Image;

import static com.zarbosoft.pyxyzygy.app.Global.localization;

public class RowAdapterCameraLoop extends RowAdapter {
  private final Camera node;
  RowTimeRangeWidget widget = null;
  private final Timeline timeline;

  public RowAdapterCameraLoop(Timeline timeline, Camera node) {
    this.timeline = timeline;
    this.node = node;
  }

  @Override
  public ObservableValue<String> getName() {
    return new SimpleStringProperty(localization.getString("time.cut"));
  }

  @Override
  public boolean hasFrames() {
    return true;
  }

  @Override
  public boolean hasNormalFrames() {
    return false;
  }

  @Override
  public boolean createFrame(
      ProjectContext context, Window window, ChangeStepBuilder change, int outer) {
    return false;
  }

  @Override
  public ObservableObjectValue<Image> getStateImage() {
    return Timeline.emptyStateImage;
  }

  @Override
  public boolean duplicateFrame(
      ProjectContext context, Window window, ChangeStepBuilder change, int outer) {
    return false;
  }

  @Override
  public WidgetHandle createRowWidget(ProjectContext context, Window window) {
    return new WidgetHandle() {
      private final BinderRoot cleanupStart;
      private final BinderRoot cleanupLength;

      {
        widget = new RowTimeRangeWidget(timeline);
        cleanupStart =
            CustomBinding.bindBidirectional(
                new ScalarBinder<Integer>(
                    node,
                    "frameStart",
                    v ->
                        context.change(
                            new ProjectContext.Tuple(node, "framestart"),
                            c -> c.camera(node).frameStartSet(v))),
                new PropertyBinder<>(widget.start.asObject()));
        cleanupLength =
            CustomBinding.bindBidirectional(
                new ScalarBinder<Integer>(
                    node,
                    "frameLength",
                    v ->
                        context.change(
                            new ProjectContext.Tuple(node, "framelength"),
                            c -> c.camera(node).frameLengthSet(v))),
                new PropertyBinder<>(widget.length.asObject()));
      }

      @Override
      public Node getWidget() {
        return widget.base;
      }

      @Override
      public void remove() {
        cleanupStart.destroy();
        cleanupLength.destroy();
      }
    };
  }

  @Override
  public int updateTime(ProjectContext context, Window window) {
    return 0;
  }

  @Override
  public void updateFrameMarker(ProjectContext context, Window window) {
    if (widget != null) widget.updateFrameMarker(window);
  }

  @Override
  public void remove(ProjectContext context) {}

  @Override
  public boolean frameAt(Window window, int outer) {
    return false;
  }

  @Override
  public Object getData() {
    return node;
  }
}
