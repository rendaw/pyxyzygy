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
import com.zarbosoft.pyxyzygy.core.model.latest.GroupPositionFrame;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteImageFrame;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteImageLayer;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteTile;
import com.zarbosoft.pyxyzygy.core.model.latest.TrueColorImageFrame;
import com.zarbosoft.pyxyzygy.core.model.latest.TrueColorImageLayer;
import com.zarbosoft.pyxyzygy.core.model.latest.TrueColorTile;
import com.zarbosoft.pyxyzygy.seed.Rectangle;
import com.zarbosoft.pyxyzygy.seed.Vector;
import com.zarbosoft.rendaw.common.Assertion;

import static com.zarbosoft.pyxyzygy.app.Global.opacityMax;

public class Render {

  public static Rectangle findBounds(Context context, int frame, ProjectObject node1) {
    BoundsBuilder out = new BoundsBuilder();
    if (false) {
      throw new Assertion();
    } else if (node1 instanceof GroupLayer) {
      GroupLayer node = (GroupLayer) node1;
      for (GroupChild layer : node.children()) {
        Rectangle childBounds = findBounds(context, frame, layer);
        out.point(childBounds.corner());
        out.point(childBounds.corner().plus(childBounds.span()));
      }
    } else if (node1 instanceof GroupChild) {
      GroupChild node = (GroupChild) node1;
      GroupPositionFrame pos = GroupChildWrapper.positionFrameFinder.findFrame(node, frame).frame;
      int frame1 = GroupChildWrapper.findInnerFrame(node, frame);
      if (node.inner() != null) {
        Rectangle childBounds = findBounds(context, frame1, node.inner());
        out.point(childBounds.corner().plus(pos.offset()));
        out.point(childBounds.corner().plus(childBounds.span()).plus(pos.offset()));
      }
    } else if (node1 instanceof TrueColorImageLayer) {
      TrueColorImageLayer node = (TrueColorImageLayer) node1;
      TrueColorImageFrame frame1 =
          TrueColorImageNodeWrapper.frameFinder.findFrame(node, frame).frame;
      frame1
          .tiles()
          .forEach(
              (key, tile) -> {
                Vector corner = Vector.from1D(key);
                out.point(corner);
                out.point(corner.plus(new Vector(context.project.tileSize(), context.project.tileSize())));
              });
    } else {
      throw new Assertion();
    }
    return out.buildInt();
  }

  public static Rectangle render(
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

  public static Rectangle render(
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
    if (false) {
      throw new Assertion();
    } else if (node1 instanceof GroupLayer) {
      GroupLayer node = (GroupLayer) node1;
      for (GroupChild layer : node.children())
        render(context, layer, out, frame, crop.unshift(node.offset()), opacity);
    } else if (node1 instanceof GroupChild) {
      GroupChild node = (GroupChild) node1;
      if (node.enabled() && node.inner() != null) {
        GroupPositionFrame pos = GroupChildWrapper.positionFrameFinder.findFrame(node, frame).frame;
        int frame1 = GroupChildWrapper.findInnerFrame(node, frame);
        double useOpacity = opacity * ((double) node.opacity() / opacityMax);
        render(context, node.inner(), out, frame1, crop.unshift(pos.offset()), useOpacity);
      }
    } else if (node1 instanceof TrueColorImageLayer) {
      TrueColorImageLayer node = (TrueColorImageLayer) node1;
      render(
          context,
          out,
          node,
          TrueColorImageNodeWrapper.frameFinder.findFrame(node, frame).frame,
          crop,
          opacity);
    } else if (node1 instanceof PaletteImageLayer) {
      PaletteImageLayer node = (PaletteImageLayer) node1;
      render(
          context,
          out,
          node,
          PaletteImageNodeWrapper.frameFinder.findFrame(node, frame).frame,
          crop,
          opacity);
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
      int frame1 = GroupChildWrapper.findInnerFrame(node, frame);
      if (node.inner() != null) out = out.expand(bounds(context, node.inner(), frame1));
    } else if (node1 instanceof TrueColorImageLayer) {
      TrueColorImageLayer node = (TrueColorImageLayer) node1;
      TrueColorImageFrame frame1 =
          TrueColorImageNodeWrapper.frameFinder.findFrame(node, frame).frame;
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
