package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.automodel.lib.Listener;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.WidgetHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.wrappers.FrameFinder;
import com.zarbosoft.pyxyzygy.app.wrappers.paletteimage.PaletteImageNodeWrapper;
import com.zarbosoft.pyxyzygy.core.model.latest.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteImageFrame;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteImageLayer;
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

class RowAdapterPaletteImageNode extends BaseFrameRowAdapter<PaletteImageLayer, PaletteImageFrame> {
  private final PaletteImageLayer node;

  public RowAdapterPaletteImageNode(Timeline timeline, PaletteImageLayer node) {
    super(timeline);
    this.node = node;
  }

  @Override
  protected void frameClear(ChangeStepBuilder change, PaletteImageFrame paletteImageFrame) {
    change.paletteImageFrame(paletteImageFrame).tilesClear();
  }

  @Override
  protected int frameCount() {
    return node.framesLength();
  }

  @Override
  protected void removeFrame(ChangeStepBuilder change, int at, int count) {
    change.paletteImageLayer(node).framesRemove(at, count);
  }

  @Override
  protected void moveFramesTo(ChangeStepBuilder change, int source, int count, int dest) {
    change.paletteImageLayer(node).framesMoveTo(source, count, dest);
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
      private final Listener.ScalarSet<PaletteImageLayer, Integer> prelengthCleanup;
      private VBox layout;
      private final Runnable framesCleanup;
      private final List<Runnable> frameCleanup = new ArrayList<>();

      {
        layout = new VBox();
        row = Optional.of(new RowFramesWidget(window, timeline, RowAdapterPaletteImageNode.this));
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
                  Listener.ScalarSet<PaletteImageFrame, Integer> lengthListener =
                      f.addLengthSetListeners(
                          (target, value) -> {
                            updateFrames(context, window);
                          });
                  return () -> {
                    f.removeLengthSetListeners(lengthListener);
                  };
                },
                c -> c.run(),
                (at, end) -> {
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
  protected PaletteImageFrame innerCreateFrame(Context context, PaletteImageFrame previousFrame) {
    PaletteImageFrame out = PaletteImageFrame.create(context.model);
    out.initialOffsetSet(context.model, Vector.ZERO);
    return out;
  }

  @Override
  protected void addFrame(ChangeStepBuilder change, int at, PaletteImageFrame frame) {
    change.paletteImageLayer(node).framesAdd(at, frame);
  }

  @Override
  protected void setFrameLength(ChangeStepBuilder change, PaletteImageFrame frame, int length) {
    change.paletteImageFrame(frame).lengthSet(length);
  }

  @Override
  protected void setPrelength(ChangeStepBuilder change, PaletteImageLayer node, int length) {
    change.paletteImageLayer(node).prelengthSet(length);
  }

  @Override
  protected int getPrelength(PaletteImageLayer node) {
    return node.prelength();
  }

  @Override
  protected void setFrameInitialLength(Context context, PaletteImageFrame frame, int length) {
    frame.initialLengthSet(context.model, length);
  }

  @Override
  protected int getFrameLength(PaletteImageFrame frame) {
    return frame.length();
  }

  @Override
  public FrameFinder<PaletteImageLayer, PaletteImageFrame> getFrameFinder() {
    return PaletteImageNodeWrapper.frameFinder;
  }

  @Override
  protected PaletteImageFrame innerDuplicateFrame(Context context, PaletteImageFrame source) {
    PaletteImageFrame created = PaletteImageFrame.create(context.model);
    created.initialOffsetSet(context.model, source.offset());
    created.initialTilesPutAll(context.model, source.tiles());
    return created;
  }

  @Override
  protected PaletteImageLayer getNode() {
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
