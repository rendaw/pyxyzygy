package com.zarbosoft.pyxyzygy.wrappers.truecolorimage;

import com.zarbosoft.pyxyzygy.ProjectContext;
import com.zarbosoft.pyxyzygy.TrueColorImage;
import com.zarbosoft.pyxyzygy.model.Tile;
import javafx.scene.image.ImageView;

import static com.zarbosoft.pyxyzygy.widgets.HelperJFX.toImage;

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
