package com.zarbosoft.pyxyzygy.app.widgets;

import javafx.scene.Node;

public abstract class ChildrenReplacer<T> {
	private Object key;

	public void set(Object key, T ...content) {
		this.key = key;
		innerSet(content);
	}

	protected abstract void innerSet(T ...content);
	protected abstract void innerClear();

	public void clear(Object key) {
		if (key != this.key) return;
		this.key = null;
		innerClear();
	}
}
