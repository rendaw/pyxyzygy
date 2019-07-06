package com.zarbosoft.automodel.task;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;

public class Poetry {
  public static class PoetryPair {
    final ClassName name;
    final TypeSpec.Builder builder;

    public PoetryPair(ClassName name, TypeSpec.Builder builder) {
      this.name = name;
      this.builder = builder;
    }
  }
}
