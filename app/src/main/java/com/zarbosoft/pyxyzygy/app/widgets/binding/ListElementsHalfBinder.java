package com.zarbosoft.pyxyzygy.app.widgets.binding;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zarbosoft.pyxyzygy.app.Misc.unopt;

public class ListElementsHalfBinder<T> implements HalfBinder<T> {
  WeakList<Consumer<T>> listeners = new WeakList<>();
  Optional<T> at = Optional.empty();
  final List<BinderRoot> rootElements;
  private boolean listenerSuppress = true;

  public <G> ListElementsHalfBinder(
      List<HalfBinder<G>> list, Function<Stream<G>, Optional<T>> function) {
    Consumer<G> listener =
        u0 -> {
          if (listenerSuppress) return;
          at =
              function.apply(
                  list.stream().map(v -> v.get()).filter(v -> v.isPresent()).map(v -> unopt(v)));
          if (at.isPresent()) new ArrayList<>(listeners).forEach(l -> l.accept(unopt(at)));
        };
    listenerSuppress = true;
    rootElements = list.stream().map(t -> t.addListener(listener)).collect(Collectors.toList());
    listenerSuppress = false;
    listener.accept(null);
  }

  @Override
  public BinderRoot addListener(Consumer<T> listener) {
    listeners.add(listener);
    if (at.isPresent()) listener.accept(unopt(at));
    return new SimpleBinderRoot(this, listener);
  }

  @Override
  public void removeRoot(Object key) {
    listeners.remove(key);
  }

  @Override
  public Optional<T> get() {
    return at;
  }
}
