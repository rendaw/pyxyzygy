package com.zarbosoft.pyxyzygy.app;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.interface1.Configuration;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

public class Hotkeys {
  public static enum Scope {
    GLOBAL {
      @Override
      public String toString() {
        return "global";
      }
    },
    CANVAS {
      @Override
      public String toString() {
        return "canvas";
      }
    },
    TIMELINE {
      @Override
      public String toString() {
        return "timeline";
      }
    },
    STRUCTURE {
      @Override
      public String toString() {
        return "structure";
      }
    }
  }

  @Configuration(name = "hotkey")
  public static class Hotkey {
    KeyCode key;
    boolean ctrl;
    boolean alt;
    boolean shift;

    public static Hotkey create(KeyCode code, boolean ctrl, boolean alt, boolean shift) {
      Hotkey out = new Hotkey();
      out.key = code;
      out.ctrl = ctrl;
      out.alt = alt;
      out.shift = shift;
      return out;
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      if (ctrl) builder.append("ctrl+");
      if (alt) builder.append("alt+");
      if (shift) builder.append("shift+");
      builder.append(key.getName());
      return builder.toString();
    }
  }

  public abstract static class Action {
    final Scope scope;
    final String name;
    final String description;
    final SimpleObjectProperty<Hotkey> key = new SimpleObjectProperty<>();

    public Action(Scope scope, String name, String description, Hotkey defaultKey) {
      this.scope = scope;
      this.name = name;
      this.description = description;
      this.key.set(defaultKey);
    }

    public abstract void run(Context context, Window window);

    public String id() {
      return String.format("%s:%s", scope, name);
    }
  }

  ObservableList<Action> actions = FXCollections.observableArrayList();

  public void register(Action action) {
    Hotkey found = GUILaunch.profileConfig.hotkeys.get(action.id());
    if (found != null) action.key.set(found);
    actions.add(action);
  }

  public void unregister(Action action) {
    actions.remove(action);
  }

  public boolean event(Context context, Window window, Scope scope, KeyEvent e) {
    for (Action action : ImmutableList.copyOf(actions)) {
      if (action.scope != scope) continue;
      if (action.key.get().key != e.getCode()) continue;
      if (action.key.get().alt != e.isAltDown()) continue;
      if (action.key.get().ctrl != e.isControlDown()) continue;
      if (action.key.get().shift != e.isShiftDown()) continue;
      action.run(context, window);
      return true;
    }
    return false;
  }
}
