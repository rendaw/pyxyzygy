package com.zarbosoft.pyxyzygy.app.wrappers.truecolorimage;

import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.BaseToolSelect;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.latest.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.latest.TrueColorImageFrame;
import com.zarbosoft.pyxyzygy.seed.Rectangle;
import com.zarbosoft.pyxyzygy.seed.Vector;
import com.zarbosoft.rendaw.common.Pair;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;

import javax.imageio.ImageIO;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.zarbosoft.pyxyzygy.app.Global.nameSymbol;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public class ToolSelect extends BaseToolSelect<TrueColorImageFrame, TrueColorImage> {
  public final TrueColorImageEditHandle editHandle;

  public ToolSelect(TrueColorImageEditHandle editHandle) {
    super(editHandle.wrapper, editHandle.wrapper.canvasHandle.zoom);
    this.editHandle = editHandle;
  }

  @Override
  protected void copy(ClipboardContent content, TrueColorImage lifted) {
    content.putImage(HelperJFX.toImage(lifted));
  }

  @Override
  protected Image toImage(TrueColorImage buffer) {
    return HelperJFX.toImage(buffer);
  }

  @Override
  protected void propertiesSet(Node node) {
    editHandle.toolProperties.set(this, node);
  }

  @Override
  protected void propertiesClear() {
    editHandle.toolProperties.clear(this);
  }

  @Override
  protected void overlayAdd(Node... nodes) {
    editHandle.overlay.getChildren().addAll(nodes);
  }

  @Override
  protected void overlayRemove(Node... nodes) {
    editHandle.overlay.getChildren().removeAll(nodes);
  }

  @Override
  public void clear(Context context, ChangeStepBuilder change, Rectangle bounds) {
    editHandle.wrapper.canvasHandle.clear(context, change, bounds);
  }

  @Override
  protected Vector negViewCenter(Window window) {
    return editHandle
        .wrapper
        .canvasHandle
        .toInner(window.getSelectedForView().getWrapper().getConfig().scroll.get())
        .toInt();
  }

  @Override
  protected Pair<TrueColorImage, Vector> uncopy(Context context) {
    Image image0 = Clipboard.getSystemClipboard().getImage();
    if (image0 == null) return null;
    return uncheck(
        () -> {
          Path temp = Files.createTempFile(nameSymbol + "-paste", ".png");
          temp.toFile().deleteOnExit();
          try (OutputStream dest = Files.newOutputStream(temp)) {
            ImageIO.write(SwingFXUtils.fromFXImage(image0, null), "PNG", dest);
            dest.flush();
            TrueColorImage image = TrueColorImage.deserialize(temp.toString());
            return new Pair<>(image, new Vector(image.getWidth(), image.getHeight()));
          }
        });
  }
}
