package com.zarbosoft.pyxyzygy.app.widgets.binding;

import java.lang.ref.WeakReference;
import java.util.*;

public class WeakList<T> implements Collection<T> {
  private List<WeakReference<T>> list = new ArrayList<>();

  @Override
  public int size() {
    return list.size();
  }

  @Override
  public boolean isEmpty() {
    return list.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return stream().anyMatch(v -> v == o);
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<T>() {
      Iterator<WeakReference<T>> iter = list.iterator();
      Boolean hasNext = null;
      T v;

      private void advance() {
        if (hasNext != null) return;
        while (iter.hasNext()) {
          WeakReference<T> vRef = iter.next();
          v = vRef.get();
          if (v == null) {
            iter.remove();
            continue;
          }
          hasNext = true;
          return;
        }
        hasNext = false;
      }

      @Override
      public boolean hasNext() {
        advance();
        return hasNext;
      }

      @Override
      public T next() {
        advance();
        if (hasNext == false) throw new NoSuchElementException();
        hasNext = null;
        T out = v;
        v = null;
        return out;
      }

      @Override
      public void remove() {
        iter.remove();
        hasNext = null;
        v = null;
      }
    };
  }

  @Override
  public Object[] toArray() {
    ArrayList<T> temp = new ArrayList<>();
    for (Iterator<T> iter = iterator(); iter.hasNext(); ) {
      temp.add(iter.next());
    }
    return temp.toArray();
  }

  @Override
  public <T1> T1[] toArray(T1[] t1s) {
    ArrayList<T> temp = new ArrayList<>();
    for (Iterator<T> iter = iterator(); iter.hasNext(); ) {
      temp.add(iter.next());
    }
    return temp.toArray(t1s);
  }

  @Override
  public boolean add(T t) {
    WeakReference<T> tRef = new WeakReference<>(t);
    list.add(tRef);
    return true;
  }

  @Override
  public boolean remove(Object o) {
    for (Iterator<T> iter = iterator(); iter.hasNext(); ) {
      T v = iter.next();
      if (v == o) {
        iter.remove();
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean containsAll(Collection<?> collection) {
    HashSet set = new HashSet<>(collection);
    for (T v : this) set.remove(v);
    return set.isEmpty();
  }

  @Override
  public boolean addAll(Collection<? extends T> collection) {
    for (T v : collection) add(v);
    return true;
  }

  @Override
  public boolean removeAll(Collection<?> collection) {
    HashSet set = new HashSet<>(collection);
    boolean changed = false;
    for (Iterator<T> iter = iterator(); iter.hasNext(); ) {
      T v = iter.next();
      if (set.contains(v)) {
        iter.remove();
        changed = true;
      }
    }
    return changed;
  }

  @Override
  public boolean retainAll(Collection<?> collection) {
    HashSet set = new HashSet<>(collection);
    boolean changed = false;
    for (Iterator<T> iter = iterator(); iter.hasNext(); ) {
      T v = iter.next();
      if (!set.contains(v)) {
        iter.remove();
        changed = true;
      }
    }
    return changed;
  }

  @Override
  public void clear() {
    list.clear();
  }
}
