package com.zarbosoft.automodel.lib;

import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.rendaw.common.Assertion;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ImmediateIDMapState extends StackReader.State {
  private final Function<String, Object> convertKeys;
  protected final Map data = new HashMap<>();
  private final DeserializeContext context;
  private String key;

  public ImmediateIDMapState(DeserializeContext context, Function<String, Object> convertKeys) {
    this.context = context;
    this.convertKeys = convertKeys;
  }

  @Override
  public void key(String value) {
    key = value;
  }

  @Override
  public void value(Object value) {
    data.put(convertKeys.apply(key), context.getObject(Long.parseLong((String) value)));
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
