package com.zarbosoft.pyxyzygy.app.widgets;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public class ContentReplacer extends StackPane {
	private Object key;

	public void set(Object key, Node content) {
		this.key = key;
		getChildren().setAll(content);
	}

	public void clear(Object key) {
		if (key != this.key) return;
		this.key = null;
		getChildren().clear();
	}
}
