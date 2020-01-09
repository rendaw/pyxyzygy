package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.automodel.lib.History;
import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.javafxbinders.BinderRoot;
import com.zarbosoft.javafxbinders.HalfBinder;
import com.zarbosoft.javafxbinders.SimpleBinderRoot;
import com.zarbosoft.pyxyzygy.app.config.RootProjectConfig;
import com.zarbosoft.pyxyzygy.core.model.latest.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.latest.Model;
import com.zarbosoft.pyxyzygy.core.model.latest.Palette;
import com.zarbosoft.pyxyzygy.core.model.latest.Project;
import com.zarbosoft.pyxyzygy.core.model.latest.ProjectLayer;
import com.zarbosoft.pyxyzygy.seed.TrueColor;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static com.zarbosoft.rendaw.common.Common.opt;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public class Context {
  public final Model model;
  public RootProjectConfig config;
  public Namer namer = new Namer();
  public final Hotkeys hotkeys;
  public static Map<String, Image> iconCache = new HashMap<>();
  public final Project project;
  public final HalfBinder<Boolean> canUndo;
  public final HalfBinder<Boolean> canRedo;

  public Context(Model model) {
    this.model = model;
    model.objectMap.values().stream()
        .filter(o -> o instanceof ProjectLayer)
        .forEach(o -> namer.countUniqueName(((ProjectLayer) o).name()));
    model.objectMap.values().stream()
        .filter(o -> o instanceof Palette)
        .forEach(o -> namer.countUniqueName(((Palette) o).name()));
    hotkeys = new Hotkeys();
    config =
        ConfigBase.deserialize(
            new TypeInfo(RootProjectConfig.class),
            model.path.resolve("config.luxem"),
            () -> {
              RootProjectConfig config = new RootProjectConfig();
              config.trueColor.set(TrueColor.fromJfx(Color.BLACK));
              return config;
            });
    this.project = (Project) model.current.root;
    Path tilesPath = model.path.resolve("tiles");
    uncheck(() -> Files.createDirectories(tilesPath));
    project.setTileDir(tilesPath);
    canUndo =
        new HalfBinder<Boolean>() {
          @Override
          public BinderRoot addListener(Consumer<Boolean> listener) {
            Consumer<Integer> inner;
            model.addUndoSizeListener(inner = i -> listener.accept(i > 0));
            return new SimpleBinderRoot(this, inner);
          }

          @Override
          public void removeRoot(Object key) {
            model.removeUndoSizeListener((Consumer<Integer>) key);
          }

          @Override
          public Optional<Boolean> asOpt() {
            return opt(model.undoSize() != 0);
          }
        };
    canRedo =
        new HalfBinder<Boolean>() {
          @Override
          public BinderRoot addListener(Consumer<Boolean> listener) {
            Consumer<Integer> inner;
            model.addRedoSizeListener(inner = i -> listener.accept(i > 0));
            return new SimpleBinderRoot(this, inner);
          }

          @Override
          public void removeRoot(Object key) {
            model.removeRedoSizeListener((Consumer<Integer>) key);
          }

          @Override
          public Optional<Boolean> asOpt() {
            return opt(model.redoSize() != 0);
          }
        };
  }

  public void change(History.Tuple unique, Consumer<ChangeStepBuilder> consumer) {
    List<Runnable> postChange = new ArrayList<>();
    model.change(
        unique, postChange, s -> consumer.accept(new ChangeStepBuilder(model, s, postChange)));
    for (Runnable runnable : postChange) {
      runnable.run();
    }
  }

  public void markDirty() {
    model.setDirty(model);
  }
}
