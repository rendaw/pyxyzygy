package com.zarbosoft.automodel.lib;

public interface DeserializeContext {
  public abstract <T> T getObject(Long key);
}
