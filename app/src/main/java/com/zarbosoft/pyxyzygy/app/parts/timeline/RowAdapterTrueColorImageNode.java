package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.automodel.lib.Listener;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.WidgetHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.wrappers.FrameFinder;
import com.zarbosoft.pyxyzygy.app.wrappers.truecolorimage.TrueColorImageNodeWrapper;
import com.zarbosoft.pyxyzygy.core.model.latest.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.latest.TrueColorImageFrame;
import com.zarbosoft.pyxyzygy.core.model.latest.TrueColorImageLayer;
import com.zarbosoft.pyxyzygy.seed.Vector;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableObjectValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.zarbosoft.pyxyzygy.app.Global.localization;

class RowAdapterTrueColorImageNode
    extends BaseFrameRowAdapter<TrueColorImageLayer, TrueColorImageFrame> {
  private final TrueColorImageLayer node;

  public RowAdapterTrueColorImageNode(Timeline timeline, TrueColorImageLayer node) {
    super(timeline);
    this.node = node;
  }

  @Override
  public void remove(Context context) {}

  @Override
  public ObservableValue<String> getName() {
    return new SimpleStringProperty(localization.getString("frames"));
  }

  @Override
  public WidgetHandle createRowWidget(Context context, Window window) {
    return new WidgetHandle() {
      private VBox layout;
      private final Runnable framesCleanup;
      private final Listener.ScalarSet<TrueColorImageLayer, Integer> prelengthCleanup;
      private final List<Runnable> frameCleanup = new ArrayList<>();

      {
        layout = new VBox();
        row = Optional.of(new RowFramesWidget(window, timeline, RowAdapterTrueColorImageNode.this));
        layout.getChildren().add(row.get());

        prelengthCleanup =
            node.addPrelengthSetListeners(
                ((target, value) -> {
                  updateFrames(context, window);
                }));
        framesCleanup =
            node.mirrorFrames(
                frameCleanup,
                f -> {
                  Listener.ScalarSet<TrueColorImageFrame, Integer> lengthListener =
                      f.addLengthSetListeners(
                          (target, value) -> {
                            updateFrames(context, window);
                          });
                  return () -> {
                    f.removeLengthSetListeners(lengthListener);
                  };
                },
                c -> c.run(),
                at -> {
                  updateFrames(context, window);
                });
      }

      @Override
      public Node getWidget() {
        return layout;
      }

      @Override
      public void remove() {
        framesCleanup.run();
        node.removePrelengthSetListeners(prelengthCleanup);
        frameCleanup.forEach(c -> c.run());
      }
    };
  }

  @Override
  protected TrueColorImageFrame innerCreateFrame(
      Context context, TrueColorImageFrame previousFrame) {
    TrueColorImageFrame out = TrueColorImageFrame.create(context.model);
    out.initialOffsetSet(context.model, Vector.ZERO);
    return out;
  }

  @Override
  protected void addFrame(ChangeStepBuilder change, int at, TrueColorImageFrame frame) {
    change.trueColorImageLayer(node).framesAdd(at, frame);
  }

  @Override
  protected void setFrameLength(ChangeStepBuilder change, TrueColorImageFrame frame, int length) {
    change.trueColorImageFrame(frame).lengthSet(length);
  }

  @Override
  protected void setPrelength(ChangeStepBuilder change, TrueColorImageLayer node, int length) {
    change.trueColorImageLayer(node).prelengthSet(length);
  }

  @Override
  protected int getPrelength(TrueColorImageLayer node) {
    return node.prelength();
  }

  @Override
  protected void setFrameInitialLength(Context context, TrueColorImageFrame frame, int length) {
    frame.initialLengthSet(context.model, length);
  }

  @Override
  protected int getFrameLength(TrueColorImageFrame frame) {
    return frame.length();
  }

  @Override
  protected void frameClear(ChangeStepBuilder change, TrueColorImageFrame trueColorImageFrame) {
    change.trueColorImageFrame(trueColorImageFrame).tilesClear();
  }

  @Override
  protected int frameCount() {
    return node.framesLength();
  }

  @Override
  protected void removeFrame(ChangeStepBuilder change, int at, int count) {
    change.trueColorImageLayer(node).framesRemove(at, count);
  }

  @Override
  protected void moveFramesTo(ChangeStepBuilder change, int source, int count, int dest) {
    change.trueColorImageLayer(node).framesMoveTo(source, count, dest);
  }

  @Override
  public FrameFinder<TrueColorImageLayer, TrueColorImageFrame> getFrameFinder() {
    return TrueColorImageNodeWrapper.frameFinder;
  }

  @Override
  protected TrueColorImageFrame innerDuplicateFrame(Context context, TrueColorImageFrame source) {
    TrueColorImageFrame created = TrueColorImageFrame.create(context.model);
    created.initialOffsetSet(context.model, source.offset());
    created.initialTilesPutAll(context.model, source.tiles());
    return created;
  }

  @Override
  protected TrueColorImageLayer getNode() {
    return node;
  }

  @Override
  public ObservableObjectValue<Image> getStateImage() {
    return Timeline.emptyStateImage;
  }

  @Override
  public boolean isMain() {
    return true;
  }
}
