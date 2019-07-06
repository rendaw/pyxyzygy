package com.zarbosoft.automodel.task;

import com.squareup.javapoet.ClassName;
import com.zarbosoft.rendaw.common.Assertion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AutoModelVersion {
  final List<AutoObject> newObjects = new ArrayList<>();
  final String vid;
  final AutoModel model;
  final boolean latest;
  AutoObject root;
  private final Set<String> seenNames = new HashSet<>();

  public AutoModelVersion(AutoModel model, String vid, boolean latest) {
    this.vid = vid;
    this.model = model;
    this.latest = latest;
  }

  public AutoObject obj(String name) {
    if (seenNames.contains(name))
      throw Assertion.format("Model version %s contains multiple objects named %s", vid, name);
    AutoObject object = new AutoObject(this, name);
    newObjects.add(object);
    return object;
  }

  public AutoObject rootObj(String name) {
    root = obj(name);
    return root;
  }

  public ClassName name(String... parts) {
    return Helper.name(model.packag + "." + (latest ? "latest" : vid), parts);
  }
}
