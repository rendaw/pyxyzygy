package com.zarbosoft.shoedemo.model;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.luxem.Luxem;
import com.zarbosoft.luxem.write.RawWriter;

@Configuration
public abstract class Change {
	public abstract void apply(ChangeStep changeStep);
	public abstract void serialize(RawWriter writer);
}
