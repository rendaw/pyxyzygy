package com.zarbosoft.automodel.lib;

import com.zarbosoft.luxem.read.StackReader.ArrayState;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ImmediateIDListState extends ArrayState {
  final List<Long> temp = new ArrayList<>();
  private final DeserializeContext context;

  public ImmediateIDListState(DeserializeContext context) {
    this.context = context;
  }

  @Override
  public void value(Object value) {
    data.add(context.getObject(Long.parseLong((String) value)));
  }
}
