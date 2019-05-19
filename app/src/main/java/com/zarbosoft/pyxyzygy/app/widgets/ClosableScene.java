package com.zarbosoft.pyxyzygy.app.widgets;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;

public class ClosableScene extends Scene {

	public ClosableScene(Parent root) {
		super(root);
	}

	public ClosableScene(Parent root, double width, double height) {
		super(root, width, height);
	}

	public void close() {

	}
}
