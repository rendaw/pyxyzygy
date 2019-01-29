package com.zarbosoft.internal.shoedemo_seed.model;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.luxem.write.RawWriter;

import java.util.function.Consumer;

@Configuration
public abstract class Change {
	public abstract void delete(ProjectContextBase project);
	public abstract void apply(ProjectContextBase project, ChangeStep changeStep);
	public abstract void serialize(RawWriter writer);
	public abstract void debugRefCounts(Consumer<ProjectObjectInterface> increment);
	public abstract boolean merge(ProjectContextBase context, Change other);
}