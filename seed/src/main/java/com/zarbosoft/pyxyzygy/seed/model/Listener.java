package com.zarbosoft.pyxyzygy.seed.model;

import java.util.List;
import java.util.Map;

public class Listener {
  @FunctionalInterface
  public interface ScalarSet<T, V> {
    void accept(T target, V value);
  }

  @FunctionalInterface
  public interface ListAdd<T, V> {
    void accept(T target, int at, List<V> value);
  }

  @FunctionalInterface
  public interface ListRemove<T> {
    void accept(T target, int at, int count);
  }

  @FunctionalInterface
  public interface Clear<T> {
    void accept(T target);
  }

  @FunctionalInterface
  public interface ListMoveTo<T> {
    void accept(T target, int source, int count, int dest);
  }

  @FunctionalInterface
  public interface SetAdd<T, V> {
    void accept(T target, List<V> value);
  }

  @FunctionalInterface
  public interface SetRemove<T, V> {
    void accept(T target, List<V> value);
  }

  @FunctionalInterface
  public interface MapPutAll<T, K, V> {
    void accept(T target, Map<K, V> put, List<K> remove);
  }
}
