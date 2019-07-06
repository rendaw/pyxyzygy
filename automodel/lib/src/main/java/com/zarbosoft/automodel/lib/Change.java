package com.zarbosoft.automodel.lib;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.luxem.write.RawWriter;

import java.util.function.Consumer;

@Configuration
public abstract class Change {
  public abstract void delete(ModelBase context);

  public abstract void apply(ModelBase context, ChangeStep changeStep);

  public abstract void serialize(RawWriter writer);

  public abstract void debugRefCounts(Consumer<ProjectObject> increment);

  public abstract boolean merge(ModelBase context, Change other);
}
