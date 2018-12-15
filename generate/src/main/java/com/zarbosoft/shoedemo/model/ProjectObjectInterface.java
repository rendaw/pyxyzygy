package com.zarbosoft.shoedemo.model;

import com.zarbosoft.luxem.write.RawWriter;

public interface ProjectObjectInterface {
	void serialize(RawWriter writer);
	void incrementCount();
	void decrementCount();
}
