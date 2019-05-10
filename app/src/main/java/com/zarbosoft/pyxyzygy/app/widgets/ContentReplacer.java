package com.zarbosoft.pyxyzygy.app.widgets;

public abstract class ContentReplacer<T> {
	private Object key;

	public T set(Object key, T content) {
		return set(key, null, content);
	}

	public T set(Object key, String title, T content) {
		if (key != this.key)
			innerClear();
		this.key = key;
		innerSet(title, content);
		return content;
	}

	protected abstract void innerSet(String title, T content);

	protected abstract void innerClear();

	public void clear(Object key) {
		if (key != this.key)
			return;
		this.key = null;
		innerClear();
	}
}
