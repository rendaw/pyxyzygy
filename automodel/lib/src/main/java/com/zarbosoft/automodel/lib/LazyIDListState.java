package com.zarbosoft.automodel.lib;

import com.zarbosoft.luxem.read.StackReader.ArrayState;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LazyIDListState extends ArrayState {
  final List<Long> temp = new ArrayList<>();
  ModelVersionDeserializer.Finisher finisher =
      new ModelVersionDeserializer.Finisher() {
        @Override
        public void finish(ModelVersionDeserializer context) {
          data.addAll(
              temp.stream()
                  .map(e -> context.objectMap.get(e))
                  .collect(Collectors.toCollection(ArrayList::new)));
        }
      };

  public LazyIDListState(ModelVersionDeserializer context) {
    context.finishers.add(finisher);
  }

  @Override
  public void value(Object value) {
    temp.add(Long.parseLong((String) value));
  }
}
