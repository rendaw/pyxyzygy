package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.javafxbinders.BinderRoot;
import com.zarbosoft.javafxbinders.CustomBinding;
import com.zarbosoft.javafxbinders.PropertyBinder;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.WidgetHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.Wrapper;
import com.zarbosoft.pyxyzygy.app.config.NodeConfig;
import com.zarbosoft.pyxyzygy.core.model.latest.ChangeStepBuilder;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.image.Image;

import static com.zarbosoft.pyxyzygy.app.Global.localization;

public class RowAdapterPreview extends RowAdapter {
  private final NodeConfig config;
  private final Wrapper data;
  RowTimeRangeWidget widget = null;
  private final Timeline timeline;

  public RowAdapterPreview(Timeline timeline, Wrapper edit) {
    this.timeline = timeline;
    this.config = edit.getConfig();
    this.data = edit;
  }

  @Override
  public ObservableValue<String> getName() {
    return new SimpleStringProperty(localization.getString("preview"));
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
    Context context, Window window, ChangeStepBuilder change, int outer) {
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
      private final BinderRoot startCleanup;
      private final BinderRoot lengthCleanup;

      {
        widget = new RowTimeRangeWidget(timeline);
        startCleanup =
            CustomBinding.bindBidirectional(
                new PropertyBinder<>(config.previewStart),
                new PropertyBinder<>(widget.start.asObject()));
        lengthCleanup =
            CustomBinding.bindBidirectional(
                new PropertyBinder<>(config.previewLength),
                new PropertyBinder<>(widget.length.asObject()));
      }

      @Override
      public Node getWidget() {
        return widget.base;
      }

      @Override
      public void remove() {
        startCleanup.destroy();
        lengthCleanup.destroy();
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
    return data;
  }

  @Override
  public boolean isMain() {
    return false;
  }
}
