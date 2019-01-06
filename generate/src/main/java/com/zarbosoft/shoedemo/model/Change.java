package com.zarbosoft.shoedemo.model;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.internal.shoedemo_generate.premodel.ProjectObject;
import com.zarbosoft.luxem.write.RawWriter;

import java.util.function.Consumer;

@Configuration
public abstract class Change {
	public abstract void delete(ProjectContextBase project);
	public abstract void apply(ProjectContextBase project, ChangeStep changeStep);
	public abstract void serialize(RawWriter writer);
	public abstract void debugRefCounts(Consumer<ProjectObjectInterface> increment);
}
