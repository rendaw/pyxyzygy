package com.zarbosoft.shoedemo;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.HPos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.function.Consumer;

public class WidgetFormBuilder {
	GridPane gridPane = new GridPane();
	int row = 0;

	public WidgetFormBuilder button(Consumer<Button> cb) {
		Button button = new Button();
		cb.accept(button);
		gridPane.add(button, 0, row++, 2, 1);
		GridPane.setHalignment(button, HPos.CENTER);
		return this;
	}

	public WidgetFormBuilder text(String name, Consumer<TextField> cb) {
		TextField widget = new TextField();
		cb.accept(widget);
		gridPane.addRow(row++, new Label(name), widget);
		return this;
	}

	public WidgetFormBuilder intSpinner(String name, int min, int max, Consumer<Spinner<Integer>> cb) {
		Spinner<Integer> widget = new Spinner<Integer>();
		widget.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max));
		cb.accept(widget);
		gridPane.addRow(row++, new Label(name), widget);
		return this;
	}

	public WidgetFormBuilder doubleSpinner(
			String name, double min, double max, double step, Consumer<Spinner<Double>> cb
	) {
		Spinner<Double> widget = new Spinner<Double>();
		widget.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, min, step));
		cb.accept(widget);
		gridPane.addRow(row++, new Label(name), widget);
		return this;
	}

	public WidgetFormBuilder chooseDirector(String name, Consumer<SimpleStringProperty> cb) {
		SimpleStringProperty path = new SimpleStringProperty();
		Button button = new Button("Choose...");
		button.setOnAction(e -> {
			DirectoryChooser chooser = new DirectoryChooser();
			chooser.setTitle(name);
			File result = chooser.showDialog(button.getScene().getWindow());
			if (result != null) {
				path.set(result.toString());
			}
		});
		Label label = new Label();
		label.textProperty().bind(Bindings.concat(name + ": ", path));
		cb.accept(path);
		gridPane.addRow(row++, label, button);
		return this;
	}

	public Node build() {
		return gridPane;
	}
}
