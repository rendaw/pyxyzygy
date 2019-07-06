package com.zarbosoft.pyxyzygy.app.widgets;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.zarbosoft.pyxyzygy.app.Global.localization;

public class WidgetFormBuilder {
  GridPane gridPane = new GridPane();
  int row = 0;

  private static Label fieldLabel() {
    Label out = new Label();
    out.setMinWidth(Label.USE_PREF_SIZE);
    return out;
  }

  private static Label fieldLabel(String name) {
    Label out = fieldLabel();
    out.setText(name);
    return out;
  }

  {
    ColumnConstraints labelColumn = new ColumnConstraints();
    ColumnConstraints editColumn = new ColumnConstraints();
    editColumn.setHgrow(Priority.ALWAYS);
    gridPane.getColumnConstraints().addAll(labelColumn, editColumn);
    gridPane.setHgap(3);
    gridPane.setVgap(6);
  }

  public WidgetFormBuilder apply(Consumer<WidgetFormBuilder> cb) {
    cb.accept(this);
    return this;
  }

  public WidgetFormBuilder button(Consumer<Button> cb) {
    Button button = new Button();
    cb.accept(button);
    gridPane.add(button, 0, row++, 2, 1);
    GridPane.setHalignment(button, HPos.CENTER);
    return this;
  }

  public static class ButtonsBuilder {
    HBox box = new HBox(3);

    {
      box.setAlignment(Pos.CENTER);
    }

    public ButtonsBuilder button(Consumer<Button> cb) {
      Button button = new Button();
      cb.accept(button);
      box.getChildren().add(button);
      return this;
    }
  }

  public WidgetFormBuilder buttons(Consumer<ButtonsBuilder> cb) {
    ButtonsBuilder buttonsBuilder = new ButtonsBuilder();
    cb.accept(buttonsBuilder);
    gridPane.add(buttonsBuilder.box, 0, row++, 2, 1);
    return this;
  }

  public WidgetFormBuilder text(String name, Consumer<TextField> cb) {
    TextField widget = new TextField();
    widget.setMaxWidth(Double.MAX_VALUE);
    GridPane.setFillWidth(widget, true);
    cb.accept(widget);
    gridPane.addRow(row++, fieldLabel(name), widget);
    return this;
  }

  public WidgetFormBuilder intSpinner(
      String name, int min, int max, Consumer<Spinner<Integer>> cb) {
    Spinner<Integer> widget = new Spinner<Integer>();
    widget.setMaxWidth(Double.MAX_VALUE);
    widget.setEditable(true);
    GridPane.setFillWidth(widget, true);
    widget.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max));
    cb.accept(widget);
    gridPane.addRow(row++, fieldLabel(name), widget);
    return this;
  }

  public WidgetFormBuilder doubleSpinner(
      String name, double min, double max, double step, Consumer<Spinner<Double>> cb) {
    Spinner<Double> widget = new Spinner<Double>();
    widget.setMaxWidth(Double.MAX_VALUE);
    widget.setEditable(true);
    GridPane.setFillWidth(widget, true);
    widget.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, min, step));
    cb.accept(widget);
    gridPane.addRow(row++, fieldLabel(name), widget);
    return this;
  }

  public WidgetFormBuilder chooseDirectory(String name, Consumer<SimpleStringProperty> cb) {
    SimpleStringProperty path = new SimpleStringProperty();
    Label pathLabel = new Label();
    pathLabel.setMinWidth(0);
    pathLabel.textProperty().bind(Bindings.concat(path));
    HBox.setHgrow(pathLabel, Priority.ALWAYS);
    Button button = new Button(localization.getString("choose"));
    button.setOnAction(
        e -> {
          DirectoryChooser chooser = new DirectoryChooser();
          chooser.setTitle(name);
          File result = chooser.showDialog(button.getScene().getWindow());
          if (result != null) {
            path.set(result.toString());
          }
        });
    HBox hbox = new HBox();
    hbox.setAlignment(Pos.CENTER_RIGHT);
    hbox.setSpacing(3);
    hbox.getChildren().addAll(pathLabel, button);

    cb.accept(path);
    gridPane.addRow(row++, fieldLabel(name), hbox);
    return this;
  }

  public WidgetFormBuilder slider(String name, double min, double max, Consumer<Slider> cb) {
    Slider widget = new Slider();
    GridPane.setFillWidth(widget, true);
    widget.setMin(min);
    widget.setMax(max);
    cb.accept(widget);
    gridPane.addRow(row++, fieldLabel(name), widget);
    return this;
  }

  public WidgetFormBuilder span(Supplier<Node> supplier) {
    Node node = supplier.get();
    GridPane.setHalignment(node, HPos.CENTER);
    gridPane.add(node, 0, row++, 2, 1);
    return this;
  }

  public WidgetFormBuilder check(String name, Consumer<CheckBox> cb) {
    CheckBox widget = new CheckBox();
    cb.accept(widget);
    gridPane.addRow(row++, fieldLabel(name), widget);
    return this;
  }

  public WidgetFormBuilder custom(String name, Supplier<Node> cb) {
    Node widget = cb.get();
    GridPane.setFillWidth(widget, true);
    gridPane.addRow(row++, fieldLabel(name), widget);
    return this;
  }

  public Node build() {
    return gridPane;
  }

  public WidgetFormBuilder separator() {
    Region space = new Region();
    space.setMinHeight(4);
    gridPane.add(space, 0, row++, 2, 1);
    return this;
  }

  public WidgetFormBuilder section(String title) {
    Label s = new Label(title);
    s.getStyleClass().add("h2");
    gridPane.add(s, 0, row++, 2, 1);
    return this;
  }

  public WidgetFormBuilder twoLine(String name, Supplier<Node> cb) {
    gridPane.add(fieldLabel(name), 0, row++, 1, 1);
    return span(cb);
  }

  public <T> WidgetFormBuilder dropDown(String name, Consumer<ComboBox<T>> cb) {
    ComboBox<T> widget = new ComboBox<>();
    GridPane.setFillWidth(widget, true);
    cb.accept(widget);
    gridPane.addRow(row++, fieldLabel(name), widget);
    return this;
  }
}
