package com.zarbosoft.pyxyzygy.app.wrappers.baseimage;

import com.zarbosoft.automodel.lib.ProjectObject;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.nearestneighborimageview.NearestNeighborImageView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public abstract class WrapTile {

  public final ImageView widget;

  public WrapTile(int x, int y) {
    widget = NearestNeighborImageView.create();
    widget.setMouseTransparent(true);
    widget.setLayoutX(x);
    widget.setLayoutY(y);
    widget.setManaged(false);
  }

  public void update(Image image) {
    widget.setImage(image);
  }

  public abstract Image getImage(Context context, ProjectObject tile);
}
