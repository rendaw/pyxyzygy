package com.zarbosoft.pyxyzygy.app.parts.editor;

import com.zarbosoft.javafxbinders.BinderRoot;
import com.zarbosoft.javafxbinders.CustomBinding;
import com.zarbosoft.javafxbinders.PropertyHalfBinder;
import com.zarbosoft.pyxyzygy.seed.Vector;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Group;
import javafx.scene.effect.BlendMode;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import static com.zarbosoft.javafxbinders.Helper.opt;

public class Origin extends Group {
  private final Line originVert = new Line();
  private final Line originHoriz = new Line();
  private final BinderRoot xRoot;
  private final BinderRoot yRoot;

  public final SimpleObjectProperty<Vector> offset = new SimpleObjectProperty<Vector>(Vector.ZERO);

  public Origin(Editor editor, double size) {
    xRoot =
        CustomBinding.bind(
            layoutXProperty(), new PropertyHalfBinder<>(offset).map(o -> opt((double) o.x)));
    yRoot =
        CustomBinding.bind(
            layoutYProperty(), new PropertyHalfBinder<>(offset).map(o -> opt((double) o.y)));
    originHoriz.setStroke(Color.GRAY);
    originVert.setStroke(Color.GRAY);
    getChildren().addAll(originHoriz, originVert);

    // SimpleDoubleProperty scale = editor.zoomFactor;
    DoubleBinding scale = Bindings.divide(1.0, editor.zoomFactor);
    DoubleBinding originHalfSpan = scale.multiply(size);
    originHoriz.startXProperty().bind(originHalfSpan.negate());
    originHoriz.endXProperty().bind(originHalfSpan);
    originVert.startYProperty().bind(originHalfSpan.negate());
    originVert.endYProperty().bind(originHalfSpan);
    originHoriz.strokeWidthProperty().bind(scale);
    originVert.strokeWidthProperty().bind(scale);
    originHoriz.startYProperty().bind(scale.multiply(0.5));
    originHoriz.endYProperty().bind(scale.multiply(0.5));
    originVert.startXProperty().bind(scale.multiply(0.5));
    originVert.endXProperty().bind(scale.multiply(0.5));

    setBlendMode(BlendMode.DIFFERENCE);
  }

  public void remove() {
    xRoot.destroy();
    yRoot.destroy();
  }
}
