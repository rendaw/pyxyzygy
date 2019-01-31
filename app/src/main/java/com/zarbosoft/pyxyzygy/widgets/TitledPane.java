package com.zarbosoft.pyxyzygy.widgets;

import javafx.scene.Node;

public class TitledPane extends javafx.scene.control.TitledPane {
	{
		setAnimated(false);
	}

	public TitledPane(String title, Node node) {
		super(title, node);
	}
}
