package com.zarbosoft.pyxyzygy.app.wrappers.baseimage;

import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.nearestneighborimageview.NearestNeighborImageView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public abstract class WrapTile<T> {

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

  public abstract Image getImage(ProjectContext context, T tile);
}
