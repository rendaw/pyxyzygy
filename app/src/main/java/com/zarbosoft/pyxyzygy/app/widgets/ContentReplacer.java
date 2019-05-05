package com.zarbosoft.pyxyzygy.app.widgets;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.Region;

public abstract class ContentReplacer<T> {
	private Object key;

	public T set(Object key, T content) {
		if (key != this.key) innerClear();
		this.key = key;
		innerSet(content);
		return content;
	}

	protected abstract void innerSet(T content);
	protected abstract void innerClear();

	public void clear(Object key) {
		if (key != this.key) return;
		this.key = null;
		innerClear();
	}
}
