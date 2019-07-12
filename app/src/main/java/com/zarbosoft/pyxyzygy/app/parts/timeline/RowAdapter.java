package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.WidgetHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.core.model.latest.ChangeStepBuilder;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;

public abstract class RowAdapter extends TreeItem<RowAdapter> {
  public RowAdapter() {
    this.setValue(this);
    this.setExpanded(true);
  }

  @Override
  public String toString() {
    return String.format("(Row adapter for %s)", getData());
  }

  public abstract ObservableValue<String> getName();

  public abstract boolean hasFrames();

  public abstract boolean hasNormalFrames();

  public abstract boolean createFrame(
    Context context, Window window, ChangeStepBuilder change, int outer);

  public abstract ObservableObjectValue<Image> getStateImage();

  public abstract boolean duplicateFrame(
    Context context, Window window, ChangeStepBuilder change, int outer);

  public abstract WidgetHandle createRowWidget(Context context, Window window);

  /**
   * @param context
   * @param window
   * @return Maximum frame in this row
   */
  public abstract int updateTime(Context context, Window window);

  public abstract void updateFrameMarker(Context context, Window window);

  public abstract void remove(Context context);

  public abstract boolean frameAt(Window window, int outer);

  /**
   * Only called if hasFrames (unique data for describing a change)
   *
   * @return
   */
  public abstract Object getData();

  /**
   * Currently means row is selected and should be used to decide previous/next frame
   * @return
   */
  public abstract boolean isMain();
}
