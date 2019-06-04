package com.zarbosoft.pyxyzygy.seed.deserialize;

import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.rendaw.common.Assertion;

import java.util.HashMap;
import java.util.Map;

public abstract class GeneralMapState extends StackReader.State {
  protected final Map data = new HashMap<>();
  protected String key;
  protected String type;

  @Override
  public void key(String value) {
    key = value;
  }

  @Override
  public void type(final String value) {
    this.type = value;
  }

  @Override
  public Object get() {
    return data;
  }

  @Override
  public StackReader.State array() {
    throw new Assertion();
  }

  @Override
  public StackReader.State record() {
    throw new Assertion();
  }
}
