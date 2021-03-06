package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.automodel.lib.History;
import com.zarbosoft.javafxbinders.BinderRoot;
import com.zarbosoft.javafxbinders.CustomBinding;
import com.zarbosoft.javafxbinders.PropertyBinder;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.WidgetHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.widgets.binders.ScalarBinder;
import com.zarbosoft.pyxyzygy.core.model.latest.Camera;
import com.zarbosoft.pyxyzygy.core.model.latest.ChangeStepBuilder;
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
  public boolean createFrame(Context context, Window window, ChangeStepBuilder change, int outer) {
    return false;
  }

  @Override
  public ObservableObjectValue<Image> getStateImage() {
    return Timeline.emptyStateImage;
  }

  @Override
  public boolean duplicateFrame(
      Context context, Window window, ChangeStepBuilder change, int outer) {
    return false;
  }

  @Override
  public WidgetHandle createRowWidget(Context context, Window window) {
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
                            new History.Tuple(node, "framestart"),
                            c -> c.camera(node).frameStartSet(v))),
                new PropertyBinder<>(widget.start.asObject()));
        cleanupLength =
            CustomBinding.bindBidirectional(
                new ScalarBinder<Integer>(
                    node,
                    "frameLength",
                    v ->
                        context.change(
                            new History.Tuple(node, "framelength"),
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
  public int updateFrames(Context context, Window window) {
    return 0;
  }

  @Override
  public void updateFrameMarker(Context context, Window window) {
    if (widget != null) widget.updateFrameMarker(window);
  }

  @Override
  public void remove(Context context) {}

  @Override
  public boolean frameAt(Window window, int outer) {
    return false;
  }

  @Override
  public Object getData() {
    return node;
  }

  @Override
  public boolean isMain() {
    return false;
  }
}
