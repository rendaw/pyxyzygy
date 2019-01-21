package com.zarbosoft.shoedemo.wrappers.truecolorimage;

import com.zarbosoft.shoedemo.ProjectContext;
import com.zarbosoft.shoedemo.TrueColorImage;
import com.zarbosoft.shoedemo.model.Tile;
import javafx.scene.image.ImageView;

import static com.zarbosoft.shoedemo.HelperJFX.toImage;

public class WrapTile {
	final ImageView widget;
	private final int zoom;

	WrapTile(ProjectContext context, Tile tile, int zoom, int x, int y) {
		this.zoom = zoom;
		widget = new ImageView();
		widget.setMouseTransparent(true);
		widget.setLayoutX(x * zoom);
		widget.setLayoutY(y * zoom);
		widget.setManaged(false);
		update(context, tile);
	}

	public void update(ProjectContext context, Tile tile) {
		TrueColorImage image = tile.getData(context);
		widget.setImage(toImage(image, zoom));
	}
}
