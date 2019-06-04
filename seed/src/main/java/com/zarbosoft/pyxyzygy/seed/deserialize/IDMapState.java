package com.zarbosoft.pyxyzygy.seed.deserialize;

import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IDMapState extends StackReader.State {
  private final Function<String, Object> convertKeys;
  protected final Map data = new HashMap<>();
  private String key;
  private final List<Pair<String, Long>> temp = new ArrayList<>();
  ModelDeserializationContext.Finisher finisher =
      new ModelDeserializationContext.Finisher() {
        @Override
        public void finish(ModelDeserializationContext context) {
          data.putAll(
              temp.stream()
                  .collect(
                      Collectors.toMap(
                          e -> convertKeys.apply(e.first),
                          e -> context.objectMap.get(e.second),
                          (a, b) -> a,
                          HashMap::new)));
        }
      };

  public IDMapState(ModelDeserializationContext context, Function<String, Object> convertKeys) {
    this.convertKeys = convertKeys;
    context.finishers.add(finisher);
  }

  @Override
  public void key(String value) {
    key = value;
  }

  @Override
  public void value(Object value) {
    temp.add(new Pair<>(key, Long.parseLong((String) value)));
  }

  @Override
  public void type(String value) {
    throw new Assertion();
  }

  @Override
  public Object get() {
    return data;
  }
}
