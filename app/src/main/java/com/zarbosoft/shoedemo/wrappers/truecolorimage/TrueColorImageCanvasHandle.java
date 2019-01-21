package com.zarbosoft.shoedemo.wrappers.truecolorimage;

import com.zarbosoft.shoedemo.DoubleRectangle;
import com.zarbosoft.shoedemo.ProjectContext;
import com.zarbosoft.shoedemo.WidgetHandle;
import com.zarbosoft.shoedemo.model.*;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.transform.Scale;

import java.util.HashMap;
import java.util.Map;

import static com.zarbosoft.shoedemo.Main.opacityMax;

public class TrueColorImageCanvasHandle extends WidgetHandle {
	private final ProjectNode.OpacitySetListener opacityListener;
	private TrueColorImageNodeWrapper trueColorImageNodeWrapper;
	Group outerDraw;
	Group draw;
	private TrueColorImageFrame.TilesPutAllListener tilesPutListener;
	Map<Long, WrapTile> wrapTiles = new HashMap<>();

	public TrueColorImageCanvasHandle(
			ProjectContext context, TrueColorImageNodeWrapper trueColorImageNodeWrapper
	) {
		this.trueColorImageNodeWrapper = trueColorImageNodeWrapper;
		draw = new Group();
		this.opacityListener = trueColorImageNodeWrapper.node.addOpacitySetListeners((target, value) -> {
			draw.setOpacity((double) value / opacityMax);
		});
		draw
				.getTransforms()
				.setAll(new Scale(1.0 / trueColorImageNodeWrapper.zoom, 1.0 / trueColorImageNodeWrapper.zoom));

		outerDraw = new Group();
		outerDraw.getChildren().add(draw);

		attachTiles(context);
	}

	@Override
	public Node getWidget() {
		return outerDraw;
	}

	@Override
	public void remove() {
		trueColorImageNodeWrapper.node.removeOpacitySetListeners(opacityListener);
		detachTiles();
		trueColorImageNodeWrapper.canvasHandle = null;
	}

	public void updateViewport(
			ProjectContext context, DoubleRectangle oldBounds1, DoubleRectangle newBounds1, boolean zoomChanged
	) {
		Rectangle oldBounds = oldBounds1.scale(3).quantize(context.tileSize);
		Rectangle newBounds = newBounds1.scale(3).quantize(context.tileSize);
		System.out.format("image scroll 2; use bounds %s\n", newBounds);

		if (zoomChanged) {
			draw.getChildren().clear();
			wrapTiles.clear();
			draw
					.getTransforms()
					.setAll(new Scale(1.0 / trueColorImageNodeWrapper.zoom, 1.0 / trueColorImageNodeWrapper.zoom));
		} else {
			// Remove tiles outside view bounds
			for (int x = 0; x < oldBounds.width; ++x) {
				for (int y = 0; y < oldBounds.height; ++y) {
					if (newBounds.contains(x, y))
						continue;
					long key = oldBounds.corner().to1D();
					draw.getChildren().remove(wrapTiles.get(key));
				}
			}
		}

		// Add missing tiles in bounds
		for (int x = 0; x < newBounds.width; ++x) {
			for (int y = 0; y < newBounds.height; ++y) {
				Vector useIndexes = newBounds.corner().plus(x, y);
				long key = useIndexes.to1D();
				if (wrapTiles.containsKey(key)) {
					continue;
				}
				Tile tile = (Tile) trueColorImageNodeWrapper.frame.tilesGet(key);
				if (tile == null) {
					continue;
				}
				WrapTile wrapTile = new WrapTile(
						context,
						tile,
						trueColorImageNodeWrapper.zoom,
						useIndexes.x * context.tileSize,
						useIndexes.y * context.tileSize
				);
				wrapTiles.put(key, wrapTile);
				draw.getChildren().add(wrapTile.widget);
			}
		}
	}

	public void updateFrame(ProjectContext context) {
		detachTiles();
		attachTiles(context);
	}

	public void attachTiles(ProjectContext context) {
		trueColorImageNodeWrapper.frame.addTilesPutAllListeners(tilesPutListener = (target, put, remove) -> {
			for (Long key : remove) {
				WrapTile old = wrapTiles.remove(key);
				if (old != null)
					draw.getChildren().remove(old.widget);
			}
			Rectangle checkBounds = trueColorImageNodeWrapper.bounds.scale(3).quantize(context.tileSize);
			System.out.format("attach tiles: %s = q %s\n", trueColorImageNodeWrapper.bounds, checkBounds);
			for (Map.Entry<Long, TileBase> entry : put.entrySet()) {
				long key = entry.getKey();
				Vector indexes = Vector.from1D(key);
				if (!checkBounds.contains(indexes.x, indexes.y)) {
					continue;
				}
				Tile value = (Tile) entry.getValue();
				WrapTile old = wrapTiles.get(key);
				if (old != null) {
					old.update(context, value);
				} else {
					WrapTile wrap = new WrapTile(
							context,
							value,
							trueColorImageNodeWrapper.zoom,
							indexes.x * context.tileSize,
							indexes.y * context.tileSize
					);
					wrapTiles.put(key, wrap);
					draw.getChildren().add(wrap.widget);
				}
			}
		});
	}

	public void detachTiles() {
		trueColorImageNodeWrapper.frame.removeTilesPutAllListeners(tilesPutListener);
		tilesPutListener = null;
		draw.getChildren().clear();
		wrapTiles.clear();
	}

}
