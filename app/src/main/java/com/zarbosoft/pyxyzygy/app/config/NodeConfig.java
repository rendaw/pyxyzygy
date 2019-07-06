package com.zarbosoft.pyxyzygy.app.config;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.DoubleVector;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.Arrays;
import java.util.List;

@Configuration
public abstract class NodeConfig {
  @Configuration public final SimpleIntegerProperty frame = new SimpleIntegerProperty(0);

  @Configuration
  public final SimpleBooleanProperty flipHorizontal = new SimpleBooleanProperty(false);

  @Configuration public final SimpleBooleanProperty flipVertical = new SimpleBooleanProperty(false);

  @Configuration public final SimpleIntegerProperty zoom = new SimpleIntegerProperty(0);

  @Configuration
  public final SimpleObjectProperty<DoubleVector> scroll =
      new SimpleObjectProperty<>(new DoubleVector(0, 0));

  @Configuration public final SimpleBooleanProperty onionLeft = new SimpleBooleanProperty(false);

  @Configuration public final SimpleBooleanProperty onionRight = new SimpleBooleanProperty(false);

  @Configuration(optional = true)
  public final SimpleObjectProperty<Integer> previewStart = new SimpleObjectProperty<>(0);

  @Configuration(optional = true)
  public final SimpleObjectProperty<Integer> previewLength = new SimpleObjectProperty<>(10);

  @Configuration(optional = true)
  public final SimpleObjectProperty<Integer> previewRate = new SimpleObjectProperty<>(10);

  public NodeConfig(Context context) {
    zoom.set(context.config.defaultZoom);
  }

  public NodeConfig() {}

  public final SimpleBooleanProperty selectedSomewhere = new SimpleBooleanProperty(false);

  public static final String TOOL_MOVE = "move";

  @Configuration public final SimpleStringProperty tool = new SimpleStringProperty(TOOL_MOVE);

  /**
   * When selecting the node and the view is not an ancestor, default to this view (last view when
   * the node was selected).
   */
  @Configuration(optional = true)
  public List<Integer> viewPath = Arrays.asList(0);
}
