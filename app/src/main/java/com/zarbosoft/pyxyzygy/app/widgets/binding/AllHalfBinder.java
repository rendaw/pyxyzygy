package com.zarbosoft.pyxyzygy.app.widgets.binding;

import com.zarbosoft.automodel.lib.WeakList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;

public class AllHalfBinder implements HalfBinder<Boolean> {
  private final WeakList<Consumer<Boolean>> listeners = new WeakList<>();
  @SuppressWarnings("unused")
  private final List<BinderRoot> sourceRoots;
  private final List<HalfBinder<Boolean>> sources;
  private boolean v = false;

  public AllHalfBinder(HalfBinder<Boolean>... sources) {
    this(Arrays.asList(sources));
  }

  public AllHalfBinder(List<HalfBinder<Boolean>> sources) {
    this.sources = sources;
    sourceRoots =
        sources.stream().map(s -> s.addListener(v -> update())).collect(Collectors.toList());
  }

  private void update() {
    v = sources.stream().allMatch(s -> s.get().orElse(false));
    new ArrayList<>(listeners).forEach(l -> l.accept(v));
  }

  @Override
  public BinderRoot addListener(Consumer<Boolean> listener) {
    this.listeners.add(listener);
    return new SimpleBinderRoot(this, listener);
  }

  @Override
  public void removeRoot(Object key) {
    this.listeners.remove(key);
  }

  @Override
  public Optional<Boolean> get() {
    return opt(v);
  }
}
