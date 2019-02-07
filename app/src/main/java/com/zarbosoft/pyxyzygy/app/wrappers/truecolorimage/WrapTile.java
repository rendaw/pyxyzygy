package com.zarbosoft.pyxyzygy.app.wrappers.truecolorimage;

import com.zarbosoft.pyxyzygy.app.ProjectContext;
import com.zarbosoft.pyxyzygy.app.model.Tile;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.nearestneighborimageview.NearestNeighborImageView;
import javafx.scene.image.ImageView;

import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.toImage;

public class WrapTile {

	final ImageView widget;

	WrapTile(ProjectContext context, Tile tile, int x, int y) {
		widget = NearestNeighborImageView.create();
		widget.setMouseTransparent(true);
		widget.setLayoutX(x);
		widget.setLayoutY(y);
		widget.setManaged(false);
		update(context, tile);
	}

	public void update(ProjectContext context, Tile tile) {
		TrueColorImage image = tile.getData(context);
		widget.setImage(toImage(image));
	}
}
