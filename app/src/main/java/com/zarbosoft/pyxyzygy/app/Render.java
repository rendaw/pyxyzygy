package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.automodel.lib.ProjectObject;
import com.zarbosoft.pyxyzygy.app.wrappers.PaletteWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupChildWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.paletteimage.PaletteImageNodeWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.truecolorimage.TrueColorImageNodeWrapper;
import com.zarbosoft.pyxyzygy.core.PaletteColors;
import com.zarbosoft.pyxyzygy.core.PaletteImage;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupChild;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupLayer;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteImageFrame;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteImageLayer;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteTile;
import com.zarbosoft.pyxyzygy.core.model.latest.TrueColorImageFrame;
import com.zarbosoft.pyxyzygy.core.model.latest.TrueColorImageLayer;
import com.zarbosoft.pyxyzygy.core.model.latest.TrueColorTile;
import com.zarbosoft.pyxyzygy.seed.Rectangle;
import com.zarbosoft.pyxyzygy.seed.Vector;
import com.zarbosoft.rendaw.common.Assertion;

import java.util.Optional;

import static com.zarbosoft.pyxyzygy.app.Global.NO_INNER;
import static com.zarbosoft.pyxyzygy.app.Global.opacityMax;

public class Render {
  public static Rectangle renderTrueColorImageLayer(
      Context context,
      TrueColorImage gc,
      TrueColorImageLayer node,
      TrueColorImageFrame frame,
      Rectangle crop,
      double opacity) {
    crop = crop.unshift(node.offset()).unshift(frame.offset());
    Rectangle tileBounds = crop.divideContains(context.project.tileSize());
    for (int x = 0; x < tileBounds.width; ++x) {
      for (int y = 0; y < tileBounds.height; ++y) {
        TrueColorTile tile = (TrueColorTile) frame.tilesGet(tileBounds.corner().plus(x, y).to1D());
        if (tile == null) continue;
        final int renderX = (x + tileBounds.x) * context.project.tileSize() - crop.x;
        final int renderY = (y + tileBounds.y) * context.project.tileSize() - crop.y;
        TrueColorImage data = TrueColorTileHelp.getData(context, tile);
        gc.compose(data, renderX, renderY, (float) opacity);
      }
    }
    return tileBounds;
  }

  public static Rectangle renderPaletteImageLayer(
      Context context,
      TrueColorImage gc,
      PaletteImageLayer node,
      PaletteImageFrame frame,
      Rectangle crop,
      double opacity) {
    crop = crop.unshift(node.offset()).unshift(frame.offset());
    PaletteColors colors = PaletteWrapper.getPaletteColors(node.palette());
    Rectangle tileBounds = crop.divideContains(context.project.tileSize());
    for (int x = 0; x < tileBounds.width; ++x) {
      for (int y = 0; y < tileBounds.height; ++y) {
        PaletteTile tile = (PaletteTile) frame.tilesGet(tileBounds.corner().plus(x, y).to1D());
        if (tile == null) continue;
        final int renderX = (x + tileBounds.x) * context.project.tileSize() - crop.x;
        final int renderY = (y + tileBounds.y) * context.project.tileSize() - crop.y;
        PaletteImage data = PaletteTileHelp.getData(context, tile);
        gc.compose(data, colors, renderX, renderY, (float) opacity);
      }
    }
    return tileBounds;
  }

  /**
   * @param context
   * @param node1 subtree to render
   * @param out for canvas with w/h = crop w/h
   * @param frame frame to render
   * @param crop viewport of render
   * @param opacity opacity of this subtree
   */
  public static void render(
      Context context,
      ProjectObject node1,
      TrueColorImage out,
      int frame,
      Rectangle crop,
      double opacity) {
    if (frame == NO_INNER) return;
    if (false) {
      throw new Assertion();
    } else if (node1 instanceof GroupLayer) {
      GroupLayer node = (GroupLayer) node1;
      for (GroupChild layer : node.children())
        render(context, layer, out, frame, crop.unshift(node.offset()), opacity);
    } else if (node1 instanceof GroupChild) {
      GroupChild node = (GroupChild) node1;
      if (node.enabled() && node.inner() != null) {
        Vector offset =
            Optional.ofNullable(GroupChildWrapper.positionFrameFinder.findFrame(node, frame).frame)
                .map(f -> f.offset())
                .orElse(Vector.ZERO);
        int frame1 = GroupChildWrapper.toInnerTime(node, frame);
        double useOpacity = opacity * ((double) node.opacity() / opacityMax);
        render(context, node.inner(), out, frame1, crop.unshift(offset), useOpacity);
      }
    } else if (node1 instanceof TrueColorImageLayer) {
      TrueColorImageLayer node = (TrueColorImageLayer) node1;
      TrueColorImageFrame frame1 =
          TrueColorImageNodeWrapper.frameFinder.findFrame(node, frame).frame;
      if (frame1 != null) {
        renderTrueColorImageLayer(context, out, node, frame1, crop, opacity);
      }
    } else if (node1 instanceof PaletteImageLayer) {
      PaletteImageLayer node = (PaletteImageLayer) node1;
      PaletteImageFrame frame1 = PaletteImageNodeWrapper.frameFinder.findFrame(node, frame).frame;
      if (frame1 != null) {
        renderPaletteImageLayer(context, out, node, frame1, crop, opacity);
      }
    } else {
      throw new Assertion();
    }
  }

  public static Rectangle bounds(Context context, ProjectObject node1, int frame) {
    Rectangle out = new Rectangle(0, 0, 0, 0);
    if (false) {
      throw new Assertion();
    } else if (node1 instanceof GroupLayer) {
      GroupLayer node = (GroupLayer) node1;
      for (GroupChild layer : node.children())
        out = out.expand(bounds(context, layer, frame)).shift(node.offset());
    } else if (node1 instanceof GroupChild) {
      GroupChild node = (GroupChild) node1;
      Vector offset =
          Optional.ofNullable(GroupChildWrapper.positionFrameFinder.findFrame(node, frame).frame)
              .map(f -> f.offset())
              .orElse(Vector.ZERO);
      int frame1 = GroupChildWrapper.toInnerTime(node, frame);
      if (node.inner() != null)
        out = out.expand(bounds(context, node.inner(), frame1).shift(offset));
    } else if (node1 instanceof TrueColorImageLayer) {
      TrueColorImageLayer node = (TrueColorImageLayer) node1;
      TrueColorImageFrame frame1 =
          TrueColorImageNodeWrapper.frameFinder.findFrame(node, frame).frame;
      if (frame1 != null)
        for (Long address : frame1.tiles().keySet()) {
          Vector v = Vector.from1D(address).multiply(context.project.tileSize());
          out =
              out.expand(
                  new Rectangle(v.x, v.y, context.project.tileSize(), context.project.tileSize())
                      .shift(node.offset())
                      .shift(frame1.offset()));
        }
    } else if (node1 instanceof PaletteImageLayer) {
      PaletteImageLayer node = (PaletteImageLayer) node1;
      PaletteImageFrame frame1 = PaletteImageNodeWrapper.frameFinder.findFrame(node, frame).frame;
      if (frame1 != null)
        for (Long address : frame1.tiles().keySet()) {
          Vector v = Vector.from1D(address).multiply(context.project.tileSize());
          out =
              out.expand(
                  new Rectangle(v.x, v.y, context.project.tileSize(), context.project.tileSize())
                      .shift(node.offset())
                      .shift(frame1.offset()));
        }
    } else {
      throw new Assertion();
    }
    return out;
  }
}
