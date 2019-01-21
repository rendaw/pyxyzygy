package com.zarbosoft.shoedemo;

import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.shoedemo.model.*;
import com.zarbosoft.shoedemo.wrappers.group.GroupLayerWrapper;
import com.zarbosoft.shoedemo.wrappers.truecolorimage.TrueColorImageNodeWrapper;

import static com.zarbosoft.shoedemo.Main.opacityMax;

public class Render {

	public static Rectangle render(
			ProjectContext context, TrueColorImage gc, TrueColorImageFrame frame, Rectangle crop, double opacity
	) {
		Rectangle tileBounds = crop.quantize(context.tileSize);
		//System.out.format("render tb %s\n", tileBounds);
		for (int x = 0; x < tileBounds.width; ++x) {
			for (int y = 0; y < tileBounds.height; ++y) {
				Tile tile = (Tile) frame.tilesGet(tileBounds.corner().plus(x, y).to1D());
				if (tile == null)
					continue;
				final int renderX = (x + tileBounds.x) * context.tileSize - crop.x;
				final int renderY = (y + tileBounds.y) * context.tileSize - crop.y;
				//System.out.format("composing at %s %s op %s\n", renderX, renderY, opacity);
				System.out.flush();
				TrueColorImage data = tile.getData(context);
				gc.compose(data, renderX, renderY, (float) opacity);
			}
		}
		return tileBounds;
	}

	/**
	 * @param context
	 * @param node1   subtree to render
	 * @param gc      for canvas with w/h = crop w/h
	 * @param frame   frame to render
	 * @param crop    viewport of render
	 * @param opacity opacity of this subtree
	 */
	public static void render(
			ProjectContext context, ProjectObject node1, TrueColorImage out, int frame, Rectangle crop, double opacity
	) {
		if (false) {
			throw new Assertion();
		} else if (node1 instanceof GroupNode) {
			GroupNode node = (GroupNode) node1;
			double useOpacity = opacity * ((double) node.opacity() / opacityMax);
			for (GroupLayer layer : node.layers())
				render(context, layer, out, frame, crop, useOpacity);
		} else if (node1 instanceof GroupLayer) {
			GroupLayer node = (GroupLayer) node1;
			GroupPositionFrame pos = GroupLayerWrapper.findPosition(node, frame).frame;
			int frame1 = GroupLayerWrapper.findInnerFrame(node, frame);
			if (node.inner() != null)
				render(context, node.inner(), out, frame1, crop.plus(pos.offset()), opacity);
		} else if (node1 instanceof TrueColorImageNode) {
			TrueColorImageNode node = (TrueColorImageNode) node1;
			render(
					context,
					out,
					TrueColorImageNodeWrapper.findFrame(node, frame).frame,
					crop,
					opacity * ((double) node.opacity() / opacityMax)
			);
		} else {
			throw new Assertion();
		}
	}

	public static Rectangle bounds(ProjectContext context, ProjectObject node1, int frame) {
		Rectangle out = new Rectangle(0, 0, 0, 0);
		if (false) {
			throw new Assertion();
		} else if (node1 instanceof GroupNode) {
			GroupNode node = (GroupNode) node1;
			for (GroupLayer layer : node.layers())
				out = out.expand(bounds(context, layer, frame));
		} else if (node1 instanceof GroupLayer) {
			GroupLayer node = (GroupLayer) node1;
			int frame1 = GroupLayerWrapper.findInnerFrame(node, frame);
			if (node.inner() != null)
				out = out.expand(bounds(context, node.inner(), frame1));
		} else if (node1 instanceof TrueColorImageNode) {
			TrueColorImageNode node = (TrueColorImageNode) node1;
			TrueColorImageFrame frame1 = TrueColorImageNodeWrapper.findFrame(node, frame).frame;
			for (Long address : frame1.tiles().keySet()) {
				Vector v = Vector.from1D(address).multiply(context.tileSize);
				out = out.expand(new Rectangle(v.x, v.y, context.tileSize, context.tileSize));
			}
		} else {
			throw new Assertion();
		}
		return out;
	}
}
