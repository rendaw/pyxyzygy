package com.zarbosoft.pyxyzygy.app.wrappers;

import com.zarbosoft.automodel.lib.Listener;
import com.zarbosoft.javafxbinders.BinderRoot;
import com.zarbosoft.javafxbinders.HalfBinder;
import com.zarbosoft.javafxbinders.SimpleBinderRoot;
import com.zarbosoft.pyxyzygy.core.PaletteColors;
import com.zarbosoft.pyxyzygy.core.model.latest.Palette;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteColor;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteImageLayer;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteSeparator;
import com.zarbosoft.pyxyzygy.seed.TrueColor;
import com.zarbosoft.rendaw.common.Assertion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import static com.zarbosoft.rendaw.common.Common.opt;

public class PaletteWrapper {
  private static WeakHashMap<Palette, PaletteWrapper> wrapperLookup = new WeakHashMap<>();
  public Instant updatedAt;
  public PaletteColors colors;
  public List<Runnable> cleanup = new ArrayList<>();
  public List<PaletteImageLayer> users = new ArrayList<>();
  public List<Runnable> listeners = new ArrayList<>();
  public HalfBinder<PaletteWrapper> selfBinder =
      new HalfBinder<PaletteWrapper>() {
        @Override
        public BinderRoot addListener(Consumer<PaletteWrapper> listener) {
          Runnable listener1 = () -> listener.accept(PaletteWrapper.this);
          listeners.add(listener1);
          listener1.run();
          return new SimpleBinderRoot(this, listener1);
        }

        @Override
        public void removeRoot(Object key) {
          listeners.remove(key);
        }

        @Override
        public Optional<PaletteWrapper> get() {
          return opt(PaletteWrapper.this);
        }
      };
  @SuppressWarnings("unused")
  private Runnable mirrorRoot;

  public static void addPaletteUser(PaletteImageLayer image) {
    getPaletteWrapper(image.palette()).users.add(image);
  }

  public static PaletteWrapper getPaletteWrapper(Palette palette) {
    return wrapperLookup.computeIfAbsent(
        palette,
        k -> {
          PaletteWrapper out = new PaletteWrapper();
          out.colors = new PaletteColors();
          out.updatedAt = Instant.now();
          out.mirrorRoot = palette.mirrorEntries(
              out.cleanup,
              v -> {
                out.updatedAt = Instant.now();
                if (v instanceof PaletteColor) {
                  final Listener.ScalarSet<PaletteColor, TrueColor> colorChangeListener =
                      (target, value) -> {
                        out.updatedAt = Instant.now();
                        out.colors.set(
                            ((PaletteColor) v).index(), value.r, value.g, value.b, value.a);
                        out.listeners.forEach(l -> l.run());
                      };
                  ((PaletteColor) v).addColorSetListeners(colorChangeListener);
                  return () -> {
                    out.updatedAt = Instant.now();
                    out.colors.set(
                        ((PaletteColor) v).index(), (byte) 0, (byte) 0, (byte) 0, (byte) 0);
                    ((PaletteColor) v).removeColorSetListeners(colorChangeListener);
                  };
                } else if (v instanceof PaletteSeparator) {
                  return () -> {};
                } else throw new Assertion();
              },
              r -> {
                out.updatedAt = Instant.now();
                r.run();
              },
              i -> {
                out.listeners.forEach(l -> l.run());
              });
          return out;
        });
  }

  public static PaletteColors getPaletteColors(Palette palette) {
    return getPaletteWrapper(palette).colors;
  }
}
