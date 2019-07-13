package com.zarbosoft.pyxyzygy.app.parts.structure;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.zarbosoft.automodel.lib.History;
import com.zarbosoft.automodel.lib.Listener;
import com.zarbosoft.automodel.lib.ProjectObject;
import com.zarbosoft.pyxyzygy.app.CanvasHandle;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.EditHandle;
import com.zarbosoft.pyxyzygy.app.GUILaunch;
import com.zarbosoft.pyxyzygy.app.Global;
import com.zarbosoft.pyxyzygy.app.Hotkeys;
import com.zarbosoft.pyxyzygy.app.TrueColorTileHelp;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.Wrapper;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.app.widgets.binding.BinderRoot;
import com.zarbosoft.pyxyzygy.app.widgets.binding.CustomBinding;
import com.zarbosoft.pyxyzygy.app.widgets.binding.DoubleHalfBinder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.HalfBinder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.IndirectBinder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.IndirectHalfBinder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.PropertyBinder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.PropertyHalfBinder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.ScalarBinder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.ScalarHalfBinder;
import com.zarbosoft.pyxyzygy.app.wrappers.PaletteWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupChildWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeWrapper;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.latest.Camera;
import com.zarbosoft.pyxyzygy.core.model.latest.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupChild;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupLayer;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupPositionFrame;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupTimeFrame;
import com.zarbosoft.pyxyzygy.core.model.latest.Palette;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteColor;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteImageFrame;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteImageLayer;
import com.zarbosoft.pyxyzygy.core.model.latest.Project;
import com.zarbosoft.pyxyzygy.core.model.latest.ProjectLayer;
import com.zarbosoft.pyxyzygy.core.model.latest.TrueColorImageFrame;
import com.zarbosoft.pyxyzygy.core.model.latest.TrueColorImageLayer;
import com.zarbosoft.pyxyzygy.seed.Rectangle;
import com.zarbosoft.pyxyzygy.seed.TrueColor;
import com.zarbosoft.pyxyzygy.seed.Vector;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zarbosoft.automodel.lib.Logger.logger;
import static com.zarbosoft.pyxyzygy.app.Global.localization;
import static com.zarbosoft.pyxyzygy.app.Global.opacityMax;
import static com.zarbosoft.pyxyzygy.app.Misc.moveTo;
import static com.zarbosoft.pyxyzygy.app.Misc.opt;
import static com.zarbosoft.pyxyzygy.app.Misc.separate;
import static com.zarbosoft.pyxyzygy.app.Wrapper.TakesChildren.NONE;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.bindStyle;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.icon;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.pad;
import static com.zarbosoft.rendaw.common.Common.last;
import static com.zarbosoft.rendaw.common.Common.sublist;

public class Structure {
  private final Context context;
  private final Window window;
  private final boolean main;

  @SuppressWarnings("unused")
  private final BinderRoot opacityAllDisableRoot;

  @SuppressWarnings("unused")
  private final BinderRoot opacityEnabledGraphicRoot;

  @SuppressWarnings("unused")
  private final BinderRoot opacityEnabledRoot;

  @SuppressWarnings("unused")
  private final BinderRoot opacityOpacityRoot;

  VBox layout = new VBox();
  public TreeView<Wrapper> tree;
  ToolBar toolbar;
  ObservableSet<Wrapper> taggedLifted = FXCollections.observableSet();
  Hotkeys.Action[] actions =
      new Hotkeys.Action[] {
        new Hotkeys.Action(
            Hotkeys.Scope.STRUCTURE,
            "clear-lift",
            localization.getString("clear.lifting"),
            Hotkeys.Hotkey.create(KeyCode.ESCAPE, false, false, false)) {
          @Override
          public void run(Context context, Window window) {
            clearTagLifted();
          }
        },
        new Hotkeys.Action(
            Hotkeys.Scope.STRUCTURE, "lift", localization.getString("lift"), Global.cutHotkey) {
          @Override
          public void run(Context context, Window window) {
            lift();
          }
        },
        new Hotkeys.Action(
            Hotkeys.Scope.STRUCTURE,
            "place-auto",
            localization.getString("paste.linked"),
            Global.pasteHotkey) {
          @Override
          public void run(Context context, Window window) {
            context.change(
                null,
                c -> {
                  placeAuto(c);
                });
          }
        },
        new Hotkeys.Action(
            Hotkeys.Scope.STRUCTURE,
            "duplicate",
            localization.getString("unlinked.duplicate"),
            Hotkeys.Hotkey.create(KeyCode.D, true, false, false)) {
          @Override
          public void run(Context context, Window window) {
            context.change(
                null,
                c -> {
                  duplicate(c);
                });
          }
        },
        new Hotkeys.Action(
            Hotkeys.Scope.STRUCTURE,
            "link",
            localization.getString("linked.duplicate"),
            Hotkeys.Hotkey.create(KeyCode.L, true, false, false)) {
          @Override
          public void run(Context context, Window window) {
            context.change(
                null,
                c -> {
                  link(c);
                });
          }
        },
        new Hotkeys.Action(
            Hotkeys.Scope.STRUCTURE,
            "delete",
            localization.getString("delete"),
            Hotkeys.Hotkey.create(KeyCode.DELETE, false, false, false)) {
          @Override
          public void run(Context context, Window window) {
            context.change(
                null,
                c -> {
                  delete(context, c);
                });
          }
        }
      };
  boolean postInit = false;
  public boolean suppressSelect = false;

  @SuppressWarnings("unused")
  private Listener.ListAdd<Project, ProjectLayer> topAddRoot;

  @SuppressWarnings("unused")
  private Listener.ListRemove<Project> topRemoveRoot;

  @SuppressWarnings("unused")
  private Listener.ListMoveTo<Project> topMoveToRoot;

  public static Stream<Integer> getPath(TreeItem<Wrapper> leaf) {
    if (leaf.getParent() == null) return Stream.empty();
    else
      return Stream.concat(
          getPath(leaf.getParent()), Stream.of(leaf.getParent().getChildren().indexOf(leaf)));
  }

  public void treeItemAdded(TreeItem<Wrapper> item) {
    if (!postInit) return;
    if (!suppressSelect) {
      tree.getSelectionModel().clearSelection();
      Platform.runLater(
          () -> {
            // Layer child initialization happens after tree node mirroring
            // Thus canvas may not be created yet
            tree.getSelectionModel().select(item);
          });
    }
  }

  public void treeItemRemoved(TreeItem<Wrapper> item) {
    Wrapper wrapper = item.getValue();
    if (wrapper != null) {
      if (wrapper.tagLifted.get()) taggedLifted.remove(wrapper);
    }
  }

  private void prepareTreeItem(TreeItem<Wrapper> item) {
    item.setExpanded(true);
    item.getChildren().forEach(c -> prepareTreeItem(c));
    item.getChildren()
        .addListener(
            (ListChangeListener<? super TreeItem<Wrapper>>)
                c -> {
                  while (c.next()) {
                    if (!c.getAddedSubList().isEmpty()) {
                      c.getAddedSubList()
                          .forEach(
                              a -> {
                                prepareTreeItem(a);
                                treeItemAdded(a);
                              });
                    }
                    c.getRemoved().forEach(r -> treeItemRemoved(r));
                  }
                });
  }

  public Structure(Context context, Window window, boolean main) {
    this.main = main;
    this.context = context;
    this.window = window;

    for (Hotkeys.Action action : actions) context.hotkeys.register(action);

    tree = new TreeView();
    tree.getStyleClass().addAll("part-structure");
    tree.setShowRoot(false);
    tree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    tree.setMinHeight(0);
    tree.addEventFilter(
        KeyEvent.KEY_PRESSED,
        e -> {
          if (context.hotkeys.event(context, window, Hotkeys.Scope.STRUCTURE, e)) e.consume();
        });
    tree.getSelectionModel()
        .getSelectedItems()
        .addListener(
            (ListChangeListener<TreeItem<Wrapper>>)
                c -> {
                  while (c.next()) {
                    List<? extends TreeItem<Wrapper>> added = c.getAddedSubList();
                    if (added.isEmpty()) return;
                    TreeItem<Wrapper> first = added.get(0);
                    if (first.getValue() == null) return;
                    window.selectForEdit(context, first.getValue());
                    return;
                  }
                });
    tree.addEventFilter(
        MouseEvent.MOUSE_PRESSED,
        e -> {
          if (e.getClickCount() >= 2) e.consume();
        });
    tree.setCellFactory(
        (TreeView<Wrapper> param) -> {
          return new TreeCell<Wrapper>() {
            private BinderRoot viewingCleanup;
            private BinderRoot viewingStyleCleanup;
            final ImageView showViewing = new ImageView();
            final ImageView showGrabState = new ImageView();
            final SimpleObjectProperty<Wrapper> wrapper = new SimpleObjectProperty<>(null);
            ChangeListener<Boolean> copyStateListener =
                (observable, oldValue, newValue) -> {
                  if (wrapper.get().tagLifted.get())
                    showGrabState.setImage(icon("content-cut.png"));
                  else showGrabState.setImage(null);
                };
            Listener.ScalarSet<ProjectLayer, String> nameSetListener =
                (target, value) -> {
                  setText(value);
                };
            final HBox graphic = new HBox();
            BinderRoot cleanup;

            {
              addEventFilter(
                  MouseEvent.MOUSE_CLICKED,
                  e -> {
                    if (wrapper.getValue() == null) return;
                    if (e.getClickCount() >= 2) {
                      window.selectForView(context, wrapper.getValue());
                      e.consume();
                    }
                  });
              MenuItem setView = new MenuItem(localization.getString("set.view"));
              setView.disableProperty().bind(Bindings.isNull(wrapper));
              setView.setOnAction(
                  e -> {
                    window.selectForView(context, wrapper.getValue());
                  });
              wrapper.addListener(
                  (observable, oldValue, newValue) -> {
                    if (oldValue != null) {
                      viewingCleanup.destroy();
                      viewingStyleCleanup.destroy();
                      oldValue.tagLifted.removeListener(copyStateListener);
                      ((ProjectLayer) oldValue.getValue()).removeNameSetListeners(nameSetListener);
                    }
                    if (newValue != null) {
                      viewingCleanup =
                          CustomBinding.bind(
                              showViewing.imageProperty(),
                              new DoubleHalfBinder<EditHandle, CanvasHandle>(
                                      window.selectedForEditTreeIconBinder,
                                      window.selectedForViewTreeIconBinder)
                                  .map(
                                      (edit, view) -> {
                                        boolean isEdit =
                                            edit != null && edit.getWrapper() == wrapper.get();
                                        boolean isView =
                                            view != null && view.getWrapper() == wrapper.get();
                                        if (isEdit && isView)
                                          return opt(icon("viewing-editing.png"));
                                        if (isView) return opt(icon("viewing.png"));
                                        if (isEdit) return opt(icon("editing.png"));
                                        return opt(null);
                                      }));
                      viewingStyleCleanup =
                          bindStyle(
                              this,
                              "pyxyzygy-disabled",
                              new IndirectHalfBinder<Boolean>(
                                  wrapper,
                                  w -> {
                                    if (wrapper.get() == null) return opt(false);
                                    if (wrapper.get().getParent() == null) return opt(false);
                                    return opt(
                                        new ScalarHalfBinder<Boolean>(
                                                ((GroupChildWrapper) wrapper.get().getParent())
                                                    .node,
                                                "enabled")
                                            .map(e -> opt(!e)));
                                  }));
                      newValue.tagLifted.addListener(copyStateListener);
                      copyStateListener.changed(null, null, null);
                      ((ProjectLayer) newValue.getValue()).addNameSetListeners(nameSetListener);
                    } else {
                      setText("");
                      showViewing.setImage(null);
                      showGrabState.setImage(null);
                    }
                  });
              setContextMenu(new ContextMenu(setView));
              showViewing.setFitHeight(16);
              showViewing.setFitWidth(16);
              showViewing.setPreserveRatio(true);
              showGrabState.setFitHeight(16);
              showGrabState.setFitWidth(16);
              showGrabState.setPreserveRatio(true);
              graphic.getChildren().addAll(showViewing, showGrabState);
              setGraphic(graphic);
            }

            @Override
            protected void updateItem(Wrapper item, boolean empty) {
              if (cleanup != null) {
                cleanup.destroy();
                cleanup = null;
              }
              if (item != null)
                cleanup =
                    HelperJFX.bindStyle(
                        this,
                        "link-selected",
                        new PropertyHalfBinder<>(item.getConfig().selectedSomewhere));
              wrapper.set(item);
              super.updateItem(item, empty);
            }
          };
        });
    MenuItem addCamera = new MenuItem(localization.getString("add.camera"));
    addCamera.setOnAction(
        e -> {
          Camera camera = Camera.create(context.model);
          camera.initialNameSet(
              context.model, context.namer.uniqueName(localization.getString("camera")));
          camera.initialOffsetSet(context.model, Vector.ZERO);
          camera.initialFrameStartSet(context.model, 0);
          camera.initialFrameLengthSet(context.model, 12);
          camera.initialFrameRateSet(context.model, 10);
          double cameraFactor = context.project.tileSize() / 200.0;
          camera.initialHeightSet(context.model, (int) (240 * cameraFactor));
          camera.initialWidthSet(context.model, (int) (320 * cameraFactor));
          context.change(
              null,
              c -> {
                c.project(context.project).topAdd(camera);
              });
        });
    MenuItem addGroup = new MenuItem(localization.getString("add.group"));
    addGroup.setOnAction(
        e -> {
          GroupLayer group = GroupLayer.create(context.model);
          group.initialOffsetSet(context.model, Vector.ZERO);
          group.initialNameSet(context.model, context.namer.uniqueName(Global.getGroupLayerName()));
          context.change(
              null,
              c -> {
                addNew(group, c);
              });
        });
    MenuItem addImage = new MenuItem(localization.getString("add.true.color.layer"));
    addImage.setOnAction(
        e -> {
          System.out.format("z1\n");
          TrueColorImageLayer image = TrueColorImageLayer.create(context.model);
          image.initialOffsetSet(context.model, Vector.ZERO);
          image.initialNameSet(
              context.model, context.namer.uniqueName(Global.getTrueColorLayerName()));
          TrueColorImageFrame frame = TrueColorImageFrame.create(context.model);
          frame.initialLengthSet(context.model, -1);
          frame.initialOffsetSet(context.model, new Vector(0, 0));
          image.initialFramesAdd(context.model, ImmutableList.of(frame));
          System.out.format("z2\n");
          context.change(
              null,
              c -> {
                System.out.format("z3\n");
                addNew(image, c);
                System.out.format("z4\n");
              });
        });
    MenuItem addPalette = new MenuItem(localization.getString("add.palette.layer"));
    addPalette.setOnAction(
        e -> {
          ObservableList<Optional<Palette>> source = FXCollections.observableArrayList();
          for (Palette e1 : context.project.palettes()) source.add(Optional.of(e1));
          source.add(Optional.empty());
          ComboBox<Optional<Palette>> cb = new ComboBox(source);
          cb.getSelectionModel().select(last(source));
          cb.setMaxWidth(Double.MAX_VALUE);
          cb.setCellFactory(
              param ->
                  new ListCell<>() {
                    @Override
                    protected void updateItem(Optional<Palette> item, boolean empty) {
                      if (empty || item == null) setText("");
                      else if (!item.isPresent()) setText(localization.getString("new.palette"));
                      else setText(item.get().name());
                      super.updateItem(item, empty);
                    }
                  });
          cb.setButtonCell(cb.getCellFactory().call(null));
          window
              .dialog(localization.getString("choose.a.palette.for.the.new.layer"))
              .addContent(cb)
              .addAction(
                  ButtonType.OK,
                  true,
                  () -> {
                    context.change(
                        null,
                        c -> {
                          Optional<Palette> palette0 = cb.getSelectionModel().getSelectedItem();
                          Palette palette;
                          if (!palette0.isPresent()) {
                            palette = Palette.create(context.model);
                            palette.initialNameSet(
                                context.model, context.namer.uniqueName(Global.getPaletteName()));
                            palette.initialNextIdSet(context.model, 2);
                            PaletteColor transparent = PaletteColor.create(context.model);
                            transparent.initialIndexSet(context.model, 0);
                            transparent.initialColorSet(
                                context.model, TrueColor.fromJfx(Color.TRANSPARENT));
                            PaletteColor black = PaletteColor.create(context.model);
                            black.initialIndexSet(context.model, 1);
                            black.initialColorSet(context.model, TrueColor.fromJfx(Color.BLACK));
                            palette.initialEntriesAdd(
                                context.model, ImmutableList.of(transparent, black));
                            Palette finalPalette = palette;
                            c.project(context.project).palettesAdd(finalPalette);
                          } else {
                            palette = palette0.get();
                          }
                          PaletteImageLayer image = PaletteImageLayer.create(context.model);
                          image.initialOffsetSet(context.model, Vector.ZERO);
                          image.initialNameSet(
                              context.model,
                              context.namer.uniqueName(Global.getPaletteLayerName()));
                          image.initialPaletteSet(context.model, palette);
                          PaletteImageFrame frame = PaletteImageFrame.create(context.model);
                          frame.initialLengthSet(context.model, -1);
                          frame.initialOffsetSet(context.model, new Vector(0, 0));
                          image.initialFramesAdd(context.model, ImmutableList.of(frame));
                          addNew(image, c);
                          PaletteWrapper.addPaletteUser(image);
                        });
                    return true;
                  })
              .addAction(
                  ButtonType.CANCEL,
                  false,
                  () -> {
                    return true;
                  })
              .go();
        });
    MenuItem importImage = new MenuItem(localization.getString("import.png"));
    importImage.setOnAction(
        e -> {
          FileChooser fileChooser = new FileChooser();
          fileChooser.setInitialDirectory(new File(GUILaunch.profileConfig.importDir));
          fileChooser
              .getExtensionFilters()
              .add(new FileChooser.ExtensionFilter(localization.getString("png.image"), "*.png"));
          Optional.ofNullable(fileChooser.showOpenDialog(window.stage))
              .map(f -> f.toPath())
              .ifPresent(
                  p -> {
                    TrueColorImage data = TrueColorImage.deserialize(p.toString());
                    TrueColorImageLayer image = TrueColorImageLayer.create(context.model);
                    image.initialOffsetSet(context.model, Vector.ZERO);
                    image.initialNameSet(
                        context.model, context.namer.uniqueName(p.getFileName().toString()));
                    TrueColorImageFrame frame = TrueColorImageFrame.create(context.model);
                    frame.initialLengthSet(context.model, -1);
                    frame.initialOffsetSet(context.model, Vector.ZERO);
                    image.initialFramesAdd(context.model, ImmutableList.of(frame));
                    Rectangle base = new Rectangle(0, 0, data.getWidth(), data.getHeight());

                    Rectangle offset = base.shift(base.span().divide(2));
                    Rectangle unitBounds = offset.divideContains(context.project.tileSize());
                    Vector localOffset =
                        offset
                            .corner()
                            .minus(unitBounds.corner().multiply(context.project.tileSize()));
                    for (int x = 0; x < unitBounds.width; ++x) {
                      for (int y = 0; y < unitBounds.height; ++y) {
                        final int x0 = x;
                        final int y0 = y;
                        TrueColorImage cut =
                            data.copy(
                                x0 * context.project.tileSize() - localOffset.x,
                                y0 * context.project.tileSize() - localOffset.y,
                                context.project.tileSize(),
                                context.project.tileSize());
                        frame.initialTilesPutAll(
                            context.model,
                            ImmutableMap.of(
                                unitBounds.corner().plus(x0, y0).to1D(),
                                TrueColorTileHelp.create(context, cut)));
                      }
                    }
                    context.change(
                        null,
                        c -> {
                          addNew(image, c);
                        });
                  });
        });
    MenuItem duplicateButton =
        new MenuItem(
            localization.getString("unlinked.duplicate"), new ImageView(icon("content-copy.png")));
    duplicateButton
        .disableProperty()
        .bind(Bindings.isEmpty(tree.getSelectionModel().getSelectedIndices()));
    duplicateButton.setOnAction(
        e -> {
          context.change(
              null,
              c -> {
                duplicate(c);
              });
        });
    MenuItem linkButton =
        new MenuItem(
            localization.getString("linked.duplicate"), new ImageView(icon("link-plus.png")));
    linkButton
        .disableProperty()
        .bind(Bindings.isEmpty(tree.getSelectionModel().getSelectedIndices()));
    linkButton.setOnAction(
        e -> {
          context.change(
              null,
              c -> {
                link(c);
              });
        });
    MenuButton addButton = HelperJFX.menuButton("plus.png");
    addButton
        .getItems()
        .addAll(
            duplicateButton,
            linkButton,
            new SeparatorMenuItem(),
            addImage,
            addPalette,
            addGroup,
            addCamera,
            new SeparatorMenuItem(),
            importImage);
    Button mainDuplicateButton =
        HelperJFX.button("content-copy.png", localization.getString("unlinked.duplicate"));
    mainDuplicateButton
        .disableProperty()
        .bind(Bindings.isEmpty(tree.getSelectionModel().getSelectedIndices()));
    mainDuplicateButton.setOnAction(
        e -> {
          context.change(
              null,
              c -> {
                duplicate(c);
              });
        });
    Button removeButton = HelperJFX.button("minus.png", localization.getString("remove"));
    removeButton
        .disableProperty()
        .bind(Bindings.isEmpty(tree.getSelectionModel().getSelectedIndices()));
    removeButton.setOnAction(
        e -> {
          context.change(
              null,
              c -> {
                delete(context, c);
              });
        });
    Button moveUpButton = HelperJFX.button("arrow-up.png", localization.getString("move.up"));
    moveUpButton
        .disableProperty()
        .bind(Bindings.isEmpty(tree.getSelectionModel().getSelectedItems()));
    moveUpButton.setOnAction(
        e -> {
          Wrapper selected = tree.getSelectionModel().getSelectedItem().getValue();
          int index = selected.parentIndex;
          int dest = index - 1;
          if (dest < 0) return;
          Wrapper parent;
          if (selected.getParent() != null) { // In group layer - get layer instead
            selected = selected.getParent();
            parent = selected.getParent();
          } else parent = null;

          Wrapper selected1 = selected;
          context.change(
              new History.Tuple("struct_move"),
              c -> {
                if (parent.getValue() != null) {
                  GroupLayer realParent = (GroupLayer) parent.getValue();
                  GroupChild realChild = (GroupChild) selected1.getValue();
                  c.groupLayer(realParent).childrenRemove(index, 1);
                  c.groupLayer(realParent).childrenAdd(dest, realChild);
                } else {
                  c.project(context.project).topRemove(index, 1);
                  c.project(context.project).topAdd(dest, (ProjectLayer) selected1.getValue());
                }
              });
        });
    Button moveDownButton = HelperJFX.button("arrow-down.png", localization.getString("move.down"));
    moveDownButton
        .disableProperty()
        .bind(Bindings.isEmpty(tree.getSelectionModel().getSelectedItems()));
    moveDownButton.setOnAction(
        e -> {
          TreeItem<Wrapper> item = tree.getSelectionModel().getSelectedItem();
          Wrapper selected = item.getValue();
          int index = selected.parentIndex;
          int dest = index + 1;
          if (dest > item.getParent().getChildren().size() - 1) return;
          Wrapper parent;
          if (selected.getParent() != null) { // In group layer - get layer instead
            selected = selected.getParent();
            parent = selected.getParent();
          } else parent = null;

          Wrapper selected1 = selected;
          context.change(
              new History.Tuple("struct_move"),
              c -> {
                if (parent.getValue() != null) {
                  GroupLayer realParent = (GroupLayer) parent.getValue();
                  GroupChild realChild = (GroupChild) selected1.getValue();
                  c.groupLayer(realParent).childrenRemove(index, 1);
                  c.groupLayer(realParent).childrenAdd(dest, realChild);
                } else {
                  c.project(context.project).topRemove(index, 1);
                  c.project(context.project).topAdd(dest, (ProjectLayer) selected1.getValue());
                }
              });
        });
    MenuItem cutButton = new MenuItem(localization.getString("lift"));
    cutButton.disableProperty().bind(Bindings.isEmpty(tree.getSelectionModel().getSelectedItems()));
    cutButton.setOnAction(
        e -> {
          lift();
        });
    MenuItem linkBeforeButton = new MenuItem(localization.getString("place.before"));
    linkBeforeButton.disableProperty().bind(Bindings.isEmpty(taggedLifted));
    linkBeforeButton.setOnAction(
        e -> {
          context.change(
              null,
              c -> {
                placeBefore(c);
              });
        });
    MenuItem linkInButton = new MenuItem(localization.getString("place.in"));
    linkInButton.disableProperty().bind(Bindings.isEmpty(taggedLifted));
    linkInButton.setOnAction(
        e -> {
          context.change(
              null,
              c -> {
                placeIn(c);
              });
        });
    MenuItem linkAfterButton = new MenuItem(localization.getString("place.after"));
    linkAfterButton.disableProperty().bind(Bindings.isEmpty(taggedLifted));
    linkAfterButton.setOnAction(
        e -> {
          context.change(
              null,
              c -> {
                placeAfter(c);
              });
        });
    MenuItem unlinkButton = new MenuItem(localization.getString("unlink"));
    unlinkButton
        .disableProperty()
        .bind(Bindings.isEmpty(tree.getSelectionModel().getSelectedItems()));
    unlinkButton.setOnAction(
        e -> {
          context.change(
              null,
              c -> {
                Wrapper edit = window.getSelectedForEdit().getWrapper();
                separate(context, c, edit);
              });
        });
    /* WIP
    MenuItem mergeUpButton = new MenuItem("Merge up");
    CustomBinding.bind(mergeUpButton.disableProperty(),
    		new CustomBinding.PropertyHalfBinder<>(tree.getSelectionModel().selectedItemProperty()).map(i -> {
    			if (i.getValue().getParent() == null)
    				return opt(false);
    			int index = i.getValue().parentIndex;
    			if (index == 0)
    				return opt(false);
    			Wrapper base = i.getValue();
    			Wrapper over = i.getParent().getChildren().get(index - 1).getValue();
    			if (!(
    					base instanceof TrueColorImageNodeWrapper && over instanceof TrueColorImageNodeWrapper
    			) || (
    					base instanceof PaletteImageNodeWrapper &&
    							over instanceof PaletteImageNodeWrapper &&
    							((PaletteImageNodeWrapper) base).node.palette() ==
    									((PaletteImageNodeWrapper) over).node.palette()
    			))
    				return opt(false);
    			return opt(true);
    		})
    );
    mergeUpButton.setOnAction(e -> {
    	TreeItem<Wrapper> i = tree.getSelectionModel().getSelectedItem();
    	int index = i.getValue().parentIndex;
    	Wrapper base = i.getValue();
    	Wrapper over = i.getParent().getChildren().get(index - 1).getValue();
    	if (over.getValue() instanceof TrueColorImageLayer) {
    		TrueColorImage
    	} else if (over.getValue() instanceof PaletteImageLayer) {

    	} else
    		throw new Assertion();
    	context.change(null, c -> {
    	});
    });
     */
    MenuButton stackButton = HelperJFX.menuButton("pencil.png");
    stackButton
        .getItems()
        .addAll(
            cutButton,
            new SeparatorMenuItem(),
            linkBeforeButton,
            linkInButton,
            linkAfterButton,
            unlinkButton);
    toolbar =
        new ToolBar(
            addButton,
            mainDuplicateButton,
            removeButton,
            moveUpButton,
            moveDownButton,
            stackButton);

    HBox opacityBox = new HBox();
    {
      opacityBox.setSpacing(3);
      opacityBox.setAlignment(Pos.CENTER_LEFT);

      HalfBinder<GroupChildWrapper> groupChildBinder =
          window.selectedForEditOpacityBinder.map(
              e -> {
                if (e == null || e.getWrapper().getParent() == null) return opt(null);
                return opt((GroupChildWrapper) e.getWrapper().getParent());
              });

      opacityAllDisableRoot =
          CustomBinding.bind(
              opacityBox.disableProperty(), groupChildBinder.map(e -> opt(e == null)));

      ToggleButton enabled = HelperJFX.toggleButton("eye.png", localization.getString("enabled"));
      opacityEnabledGraphicRoot =
          CustomBinding.bind(
              enabled.getGraphic().opacityProperty(),
              new PropertyHalfBinder<>(enabled.selectedProperty()).map(b -> opt(b ? 1 : 0.5)));
      opacityEnabledRoot =
          CustomBinding.bindBidirectional(
              new IndirectBinder<Boolean>(
                  groupChildBinder,
                  childWrapper -> {
                    if (childWrapper == null) return opt(null);
                    GroupChild layerNode = childWrapper.node;
                    return opt(
                        new ScalarBinder<Boolean>(
                            layerNode,
                            "enabled",
                            v ->
                                context.change(
                                    new History.Tuple(childWrapper, "enabled"),
                                    c -> c.groupChild(layerNode).enabledSet(v))));
                  }),
              new PropertyBinder<>(enabled.selectedProperty()));

      Slider opacity = new Slider();
      opacity.setMin(0);
      opacity.setMax(opacityMax);
      opacityOpacityRoot =
          CustomBinding.bindBidirectional(
              new IndirectBinder<Integer>(
                  groupChildBinder,
                  childWrapper -> {
                    if (childWrapper == null) return Optional.empty();
                    GroupChild layerNode = childWrapper.node;
                    return opt(
                        new ScalarBinder<Integer>(
                            layerNode,
                            "opacity",
                            v ->
                                context.change(
                                    new History.Tuple(childWrapper, "opacity"),
                                    c -> c.groupChild(layerNode).opacitySet(v))));
                  }),
              new PropertyBinder<>(opacity.valueProperty())
                  .bimap(d -> Optional.of((int) (double) d), i -> opt((double) (int) i)));
      HBox.setHgrow(opacity, Priority.ALWAYS);
      opacityBox.getChildren().addAll(enabled, opacity);
    }

    layout.getChildren().addAll(toolbar, pad(opacityBox), tree);
    VBox.setVgrow(tree, Priority.ALWAYS);
  }

  public void populate() {
    List<Integer> editPath = context.config.editPath;
    List<Integer> viewPath = context.config.viewPath;

    TreeItem<Wrapper> rootTreeItem = new TreeItem<>();
    prepareTreeItem(rootTreeItem);
    tree.setRoot(rootTreeItem);

    topAddRoot =
        context.project.addTopAddListeners(
            (target, at, value) -> {
              List<TreeItem<Wrapper>> newItems = new ArrayList<>();
              for (int i = 0; i < value.size(); ++i) {
                ProjectLayer v = value.get(i);
                Wrapper child = Window.createNode(context, null, at + i, v);
                child.tree.addListener(
                    (observable, oldValue, newValue) -> {
                      tree.getRoot().getChildren().set(child.parentIndex, newValue);
                    });
                newItems.add(child.tree.getValue());
              }
              tree.getRoot().getChildren().addAll(at, newItems);
            });
    topRemoveRoot =
        context.project.addTopRemoveListeners(
            (target, at, count) -> {
              List<TreeItem<Wrapper>> temp = tree.getRoot().getChildren().subList(at, at + count);
              temp.forEach(i -> i.getValue().remove(context));
              temp.clear();
              for (int i = at; i < tree.getRoot().getChildren().size(); ++i) {
                tree.getRoot().getChildren().get(i).getValue().setParentIndex(at + i);
              }
              if (target.topLength() == 0) {
                tree.getSelectionModel().clearSelection();
                window.selectForView(context, null);
                window.selectForEdit(context, null);
              }
            });
    topMoveToRoot =
        context.project.addTopMoveToListeners(
            (target, source, count, dest) -> {
              moveTo(tree.getRoot().getChildren(), source, count, dest);
              for (int i = Math.min(source, dest); i < tree.getRoot().getChildren().size(); ++i)
                tree.getRoot().getChildren().get(i).getValue().setParentIndex(i);
            });

    if (main) {
      window.selectForEdit(context, null);
      Wrapper editNode = findNode(rootTreeItem, viewPath);
      if (editNode != null) {
        window.selectForView(context, editNode);
        tree.getSelectionModel().clearSelection();
        tree.getSelectionModel().select(findNode(rootTreeItem, editPath).tree.get());
      }
    }
    postInit = true;
  }

  public static Wrapper findNode(TreeItem<Wrapper> root, List<Integer> path) {
    if (path.isEmpty()) return root.getValue();
    int next = Math.min(root.getChildren().size() - 1, path.get(0));
    if (next == -1) return root.getValue();
    return findNode(root.getChildren().get(next), sublist(path, 1));
  }

  private void delete(Context context, ChangeStepBuilder change) {
    Wrapper edit = window.getSelectedForEdit().getWrapper();
    if (edit == null) return;
    edit.delete(context, change);
  }

  private void placeAfter(ChangeStepBuilder change) {
    Wrapper destination = window.getSelectedForEdit().getWrapper();
    if (destination == null) {
      place(change, null, false, null);
    } else {
      Wrapper pasteParent = destination.getParent();
      if (pasteParent != null) pasteParent = pasteParent.getParent();
      place(change, pasteParent, false, destination);
    }
  }

  private void placeIn(ChangeStepBuilder change) {
    Wrapper destination = window.getSelectedForEdit().getWrapper();
    if (destination == null) {
      place(change, null, true, null);
    } else {
      place(change, destination, true, null);
    }
  }

  private void placeBefore(ChangeStepBuilder change) {
    Wrapper destination = window.getSelectedForEdit().getWrapper();
    if (destination == null) {
      place(change, null, true, null);
    } else {
      Wrapper pasteParent = destination.getParent();
      if (pasteParent != null) pasteParent = pasteParent.getParent();
      place(change, pasteParent, true, destination);
    }
  }

  private void placeAuto(ChangeStepBuilder change) {
    Wrapper destination = getSelection();
    if (destination == null) {
      placeAfter(change);
    } else {
      if (destination.takesChildren() == NONE) {
        placeAfter(change);
      } else {
        placeIn(change);
      }
    }
  }

  private void clearTagLifted() {
    taggedLifted.forEach(w -> w.tagLifted.set(false));
    taggedLifted.clear();
  }

  private void lift() {
    tree.getSelectionModel().getSelectedItems().stream()
        .forEach(
            s -> {
              Wrapper wrapper = s.getValue();
              if (wrapper.tagLifted.get()) {
                taggedLifted.remove(wrapper);
                wrapper.tagLifted.set(false);
              } else {
                taggedLifted.add(wrapper);
                wrapper.tagLifted.set(true);
              }
            });
  }

  private void duplicate(ChangeStepBuilder change) {
    Wrapper source = window.getSelectedForEdit().getWrapper();
    ProjectLayer clone = source.separateClone(context);
    addNewAfter(source, clone, change);
  }

  private void link(ChangeStepBuilder change) {
    Wrapper source = window.getSelectedForEdit().getWrapper();
    addNewAfter(source, (ProjectLayer) source.getValue(), change);
  }

  private Wrapper getSelection() {
    return Optional.ofNullable(tree.getSelectionModel().getSelectedItem())
        .map(t -> t.getValue())
        .orElse(null);
  }

  /**
   * Adds the single node wherever it can, starting from the selection
   *
   * @param node
   */
  private void addNew(ProjectLayer node, ChangeStepBuilder change) {
    Wrapper edit = getSelection();
    Wrapper placeAt = edit;
    int index = 0;
    while (placeAt != null) {
      if (placeAt instanceof GroupNodeWrapper) {
        if (!confirmPlaceLocation(placeAt, node)) throw new Assertion();
        change
            .groupLayer((GroupLayer) placeAt.getValue())
            .childrenAdd(index, createGroupChild(node));
        return;
      }
      index = placeAt.parentIndex + 1;
      placeAt = placeAt.getParent();
    }
    System.out.format("top add new node %s\n", node.name());
    change.project(context.project).topAdd(node);
  }

  private void addNewAfter(Wrapper placeAfter, ProjectLayer node, ChangeStepBuilder change) {
    if (placeAfter.getParent() == null) {
      int index = placeAfter.parentIndex;
      change.project(context.project).topAdd(index + 1, node);
    } else {
      int index = placeAfter.getParent().parentIndex;
      change
          .groupLayer(((GroupLayer) placeAfter.getParent().getParent().getValue()))
          .childrenAdd(index + 1, createGroupChild(node));
    }
  }

  /**
   * Confirm there won't be recursion when placing new wrapper (or node)
   *
   * @param firstParent
   * @param node
   * @return
   */
  private boolean confirmPlaceLocation(Wrapper firstParent, ProjectObject node) {
    // Omit items that would have infinite recursion if pasted
    if (firstParent != null) {
      Wrapper parent = firstParent;
      while (parent != null) {
        if (parent.getValue() == node) {
          logger.write(
              "Warning: Can't place relative to copied element - destination is a child of element; skipping element.");
          return false;
        }
        parent = parent.getParent();
      }
    }
    return true;
  }

  /**
   * Places exactly within parent/top before/after reference/start=end
   *
   * @param change
   * @param pasteParent a GroupLayer/derivative wrapper
   * @param before how the nodes should be placed relative to reference
   * @param reference a GroupChild
   */
  private void place(
      ChangeStepBuilder change, Wrapper pasteParent, boolean before, Wrapper reference) {
    List<Wrapper> placable = new ArrayList<>();
    Consumer<Wrapper> check =
        wrapper -> {
          if (wrapper == reference) {
            logger.write("Warning: Can't place relative to copied element; skipping element.");
            return;
          }

          if (!confirmPlaceLocation(pasteParent, (ProjectLayer) wrapper.getValue())) return;
          placable.add(wrapper);
        };
    taggedLifted.forEach(check);
    if (placable.isEmpty()) {
      logger.write("Warning: No nodes left to place!");
    }
    if (pasteParent != null) {
      int dest;
      if (reference != null) {
        dest = reference.parentIndex + (before ? 0 : 1);
      } else {
        dest = 0;
      }
      List<GroupChild> children = new ArrayList<>();
      for (Wrapper wrapper : placable) {
        boolean lifted = taggedLifted.contains(wrapper);
        if (lifted) {
          if (wrapper.getParent() != null && wrapper.getParent().parentIndex < dest) dest -= 1;
          wrapper.delete(context, change);
        }
        if (wrapper.getParent() != null) {
          Wrapper childParent = wrapper.getParent();
          if (lifted) {
            // Just transfering the group child
            children.add((GroupChild) childParent.getValue());
          } else {
            // Create a new group child
            children.add(createGroupChild((ProjectLayer) wrapper.getValue()));
          }
        } else {
          GroupChild layer = createGroupChild((ProjectLayer) wrapper.getValue());
          children.add(layer);
        }
      }
      change.groupLayer((GroupLayer) pasteParent.getValue()).childrenAdd(dest, children);
    } else {
      int dest;
      if (reference != null) {
        dest = reference.parentIndex + (before ? 0 : 1);
      } else {
        dest = 0;
      }
      List<ProjectLayer> children = new ArrayList<>();
      for (Wrapper wrapper : placable) {
        boolean lifted = taggedLifted.contains(wrapper);
        if (lifted) {
          if (wrapper.getParent() == null && wrapper.parentIndex < dest) dest -= 1;
          wrapper.delete(context, change);
        }
        children.add((ProjectLayer) wrapper.getValue());
      }
      change.project(context.project).topAdd(dest, children);
    }
    clearTagLifted();
  }

  private GroupChild createGroupChild(ProjectLayer node) {
    GroupChild child = GroupChild.create(context.model);
    child.initialInnerSet(context.model, node);
    child.initialOpacitySet(context.model, opacityMax);
    child.initialEnabledSet(context.model, true);
    GroupPositionFrame positionFrame = GroupPositionFrame.create(context.model);
    positionFrame.initialLengthSet(context.model, -1);
    positionFrame.initialOffsetSet(context.model, new Vector(0, 0));
    child.initialPositionFramesAdd(context.model, ImmutableList.of(positionFrame));
    GroupTimeFrame timeFrame = GroupTimeFrame.create(context.model);
    timeFrame.initialLengthSet(context.model, -1);
    timeFrame.initialInnerOffsetSet(context.model, 0);
    timeFrame.initialInnerLoopSet(context.model, 0);
    child.initialTimeFramesAdd(context.model, ImmutableList.of(timeFrame));
    return child;
  }

  public Node getWidget() {
    return layout;
  }

  public void selectedForEdit(Context context, EditHandle newValue) {
    if (main)
      context.config.editPath =
          getPath(newValue.getWrapper().tree.get()).collect(Collectors.toList());
  }

  public void selectedForView(Context context, CanvasHandle newValue) {
    if (main)
      context.config.viewPath =
          getPath(newValue.getWrapper().tree.get()).collect(Collectors.toList());
  }
}
