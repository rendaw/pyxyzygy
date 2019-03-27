package com.zarbosoft.pyxyzygy.app.widgets;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Region;

public abstract class ContentReplacer {
	private Object key;

	public void set(Object key, Node content) {
		if (key != this.key) innerClear();
		this.key = key;
		innerSet(content);
	}

	protected abstract void innerSet(Node content);
	protected abstract void innerClear();

	public void clear(Object key) {
		if (key != this.key) return;
		this.key = null;
		innerClear();
	}
}
