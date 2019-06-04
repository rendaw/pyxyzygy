package com.zarbosoft.pyxyzygy.app.config;

import com.zarbosoft.interface1.Configuration;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

@Configuration
public class BaseBrush {
  @Configuration public SimpleObjectProperty<Integer> size = new SimpleObjectProperty<>(0);

  public double sizeInPixels() {
    return size.get() / 10.0;
  }

  @Configuration public SimpleStringProperty name = new SimpleStringProperty();
}
