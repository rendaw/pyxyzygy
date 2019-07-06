package com.zarbosoft.pyxyzygy.app.widgets.binding;

import com.zarbosoft.automodel.lib.WeakList;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.zarbosoft.pyxyzygy.app.Misc.unopt;

public class MapBinder<T, U> implements HalfBinder<U>, Consumer<T> {
  private final Function<T, Optional<U>> forward;
  private final BinderRoot root; // GC root
  Optional<U> last = Optional.empty();
  WeakList<Consumer<U>> listeners = new WeakList<>();

  public MapBinder(HalfBinder parent, Function<T, Optional<U>> forward) {
    this.forward = forward;
    root = parent.addListener(this);
  }

  @Override
  public BinderRoot addListener(Consumer<U> listener) {
    listeners.add(listener);
    if (last.isPresent()) listener.accept(unopt(last));
    return new SimpleBinderRoot(this, listener);
  }

  @Override
  public void removeRoot(Object key) {
    listeners.remove(key);
  }

  @Override
  public Optional<U> get() {
    return last;
  }

  @Override
  public void accept(T t) {
    Optional<U> v = forward.apply(t);
    if (!v.isPresent()) return;
    last = v;
    listeners.forEach(l -> l.accept(unopt(v)));
  }
}
