package com.zarbosoft.pyxyzygy.app.wrappers.paletteimage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.collect.Streams;
import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.config.PaletteBrush;
import com.zarbosoft.pyxyzygy.app.config.PaletteImageNodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.PaletteTile;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.*;
import com.zarbosoft.pyxyzygy.app.widgets.binding.*;
import com.zarbosoft.pyxyzygy.app.wrappers.ToolMove;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.BrushButton;
import com.zarbosoft.pyxyzygy.core.PaletteImage;
import com.zarbosoft.pyxyzygy.core.model.v0.*;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import com.zarbosoft.pyxyzygy.seed.model.v0.TrueColor;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zarbosoft.pyxyzygy.app.Global.localization;
import static com.zarbosoft.pyxyzygy.app.Global.pasteHotkey;
import static com.zarbosoft.pyxyzygy.app.Misc.*;
import static com.zarbosoft.pyxyzygy.app.config.NodeConfig.TOOL_MOVE;
import static com.zarbosoft.pyxyzygy.app.config.PaletteImageNodeConfig.*;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.*;
import static com.zarbosoft.rendaw.common.Common.enumerate;

public class PaletteImageEditHandle extends EditHandle {
  final PaletteImageNodeWrapper wrapper;
  private final TitledPane toolPane;
  private final Listener.ScalarSet<PaletteImageLayer, Palette> paletteListener;
  private BinderRoot paletteAddSeparatorCleanup;
  private BinderRoot colorPickerDisableCleanup;
  private BinderRoot paletteMoveDownCleanup;
  private BinderRoot paletteMoveUpCleanup;
  private BinderRoot paletteRemoveCleanup;
  private BinderRoot paletteAddCleanup;
  private Runnable paletteTilesCleanup;
  private BinderRoot colorPickerCleanup;
  List<Runnable> cleanup = new ArrayList<>();
  List<BinderRoot> cleanup2 = new ArrayList<>();

  private final Runnable brushesCleanup;
  Group overlay;
  private final Hotkeys.Action[] actions;
  Tool tool = null;
  ContentReplacer<Node> toolProperties =
      new ContentReplacer<Node>() {

        @Override
        protected void innerSet(String title, Node content) {
          toolPane.setContent(content);
        }

        @Override
        protected void innerClear() {
          toolPane.setContent(null);
        }
      };

  public final SimpleDoubleProperty mouseX = new SimpleDoubleProperty(0);
  public final SimpleDoubleProperty mouseY = new SimpleDoubleProperty(0);
  public final SimpleIntegerProperty positiveZoom = new SimpleIntegerProperty(1);

  public final Map<ProjectObject, ColorTile> tiles = new HashMap<>();

  interface PaletteTileBase extends Garb {
    void setIndex(int index);

    int index();
  }

  interface PaletteState extends Garb {

    void merge(ProjectContext context, int index);
  }

  public final SimpleObjectProperty<PaletteState> paletteState = new SimpleObjectProperty<>();

  class SeparatorTile extends Region implements PaletteTileBase {
    final PaletteSeparator self;
    private final BinderRoot cleanupBorder;
    private int index;

    SeparatorTile(PaletteSeparator self) {
      this.self = self;
      Circle circle = new Circle();
      circle.setFill(Color.BLACK);
      circle.setOpacity(0.8);
      circle.setRadius(3);
      widthProperty()
          .addListener(
              (observable, oldValue, newValue) -> {
                circle.setCenterX(newValue.doubleValue() / 2);
              });
      heightProperty()
          .addListener(
              (observable, oldValue, newValue) -> {
                circle.setCenterY(newValue.doubleValue() / 2);
              });
      getChildren().addAll(circle);
      setPadding(new Insets(10));
      getStyleClass().addAll("palette-separator");
      cleanupBorder =
          HelperJFX.bindStyle(
              this,
              "selected",
              wrapper.paletteSelectionBinder.map(
                  o -> Optional.of(o != null && o.id() == self.id())));
      addEventFilter(
          MouseEvent.MOUSE_CLICKED,
          e -> {
            wrapper.paletteSelOffsetBinder.set(index);
          });
      setMouseTransparent(false);
    }

    @Override
    public void setIndex(int index) {
      this.index = index;
    }

    @Override
    public int index() {
      return index;
    }

    @Override
    public void destroy(ProjectContext context, Window window) {
      cleanupBorder.destroy();
    }
  }

  class ColorTile extends ColorSwatch implements PaletteTileBase {
    public int index;
    public final PaletteColor color;
    private final BinderRoot cleanupBorder;
    private final BinderRoot cleanupColor;

    {
      getStyleClass().add("large");
    }

    ColorTile(ProjectContext context, PaletteColor color) {
      super(2);
      this.color = color;
      this.cleanupColor =
          new ScalarHalfBinder<TrueColor>(
                  color::addColorSetListeners, color::removeColorSetListeners)
              .addListener(
                  c0 -> {
                    Color c = c0.toJfx();
                    colorProperty.set(c);
                  });
      cleanupBorder =
          HelperJFX.bindStyle(
              this,
              "selected",
              wrapper.paletteSelectionBinder.map(
                  o -> Optional.of(o != null && o.id() == color.id())));
      addEventFilter(
          MouseEvent.MOUSE_CLICKED,
          e -> {
            if (paletteState.get() != null) {
              paletteState.get().merge(context, index);
              paletteState.set(null);
            } else {
              wrapper.paletteSelOffsetBinder.set(index);
            }
          });
      tiles.put(color, this);
    }

    @Override
    public void destroy(ProjectContext context, Window window) {
      cleanupBorder.destroy();
      cleanupColor.destroy();
      tiles.remove(color);
    }

    @Override
    public void setIndex(int index) {
      this.index = index;
    }

    @Override
    public int index() {
      return index;
    }
  }

  private void setBrush(int brush) {
    wrapper.config.lastBrush = wrapper.config.brush.get();
    wrapper.config.brush.set(brush);
  }

  public PaletteImageEditHandle(
      ProjectContext context, Window window, final PaletteImageNodeWrapper wrapper) {
    this.wrapper = wrapper;
    positiveZoom.bind(wrapper.canvasHandle.zoom);

    actions =
        Streams.concat(
                Stream.of(
                    new Hotkeys.Action(
                        Hotkeys.Scope.CANVAS,
                        "paste",
                        localization.getString("paste"),
                        pasteHotkey) {
                      @Override
                      public void run(ProjectContext context, Window window) {
                        wrapper.config.tool.set(PaletteImageNodeConfig.TOOL_SELECT);
                        ((ToolSelect) tool).paste(context, window);
                      }
                    },
                    new Hotkeys.Action(
                        Hotkeys.Scope.CANVAS,
                        "last-brush",
                        localization.getString("last.brush"),
                        Hotkeys.Hotkey.create(KeyCode.SPACE, false, false, false)) {
                      @Override
                      public void run(ProjectContext context, Window window) {
                        if (wrapper.config.tool.get() == PaletteImageNodeConfig.TOOL_BRUSH) {
                          if (wrapper.config.lastBrush < 0
                              || wrapper.config.lastBrush
                                  >= GUILaunch.profileConfig.paletteBrushes.size()) return;
                          setBrush(wrapper.config.lastBrush);
                        } else {
                          wrapper.config.tool.set(PaletteImageNodeConfig.TOOL_BRUSH);
                        }
                      }
                    },
                    new Hotkeys.Action(
                        Hotkeys.Scope.CANVAS,
                        "select",
                        localization.getString("select"),
                        Hotkeys.Hotkey.create(KeyCode.S, false, false, false)) {
                      @Override
                      public void run(ProjectContext context, Window window) {
                        wrapper.config.tool.set(PaletteImageNodeConfig.TOOL_SELECT);
                      }
                    },
                    new Hotkeys.Action(
                        Hotkeys.Scope.CANVAS,
                        "move",
                        localization.getString("move.layer"),
                        Hotkeys.Hotkey.create(KeyCode.M, false, false, false)) {
                      @Override
                      public void run(ProjectContext context, Window window) {
                        wrapper.config.tool.set(PaletteImageNodeConfig.TOOL_MOVE);
                      }
                    },
                    new Hotkeys.Action(
                        Hotkeys.Scope.CANVAS,
                        "move-frame",
                        localization.getString("move.frame.contents"),
                        Hotkeys.Hotkey.create(KeyCode.F, false, false, false)) {
                      @Override
                      public void run(ProjectContext context, Window window) {
                        wrapper.config.tool.set(TOOL_FRAME_MOVE);
                      }
                    }),
                enumerate(
                        Stream.of(
                            KeyCode.DIGIT1,
                            KeyCode.DIGIT2,
                            KeyCode.DIGIT3,
                            KeyCode.DIGIT4,
                            KeyCode.DIGIT5,
                            KeyCode.DIGIT6,
                            KeyCode.DIGIT7,
                            KeyCode.DIGIT8,
                            KeyCode.DIGIT9,
                            KeyCode.DIGIT0))
                    .map(
                        p ->
                            new Hotkeys.Action(
                                Hotkeys.Scope.CANVAS,
                                String.format("brush-%s", p.first + 1),
                                String.format(localization.getString("brush.s"), p.first + 1),
                                Hotkeys.Hotkey.create(p.second, false, false, false)) {
                              @Override
                              public void run(ProjectContext context, Window window) {
                                if (p.first >= GUILaunch.profileConfig.paletteBrushes.size())
                                  return;
                                setBrush(p.first);
                                wrapper.config.tool.set(PaletteImageNodeConfig.TOOL_BRUSH);
                              }
                            }))
            .toArray(Hotkeys.Action[]::new);
    for (Hotkeys.Action action : actions) context.hotkeys.register(action);

    // Overlay
    overlay = wrapper.canvasHandle.overlay;

    Wrapper.ToolToggle move =
        new Wrapper.ToolToggle(
            wrapper, "cursor-move16.png", localization.getString("move.layer"), TOOL_MOVE);
    Wrapper.ToolToggle select =
        new Wrapper.ToolToggle(
            wrapper, "select.png", localization.getString("select"), TOOL_SELECT);

    window.timeline.toolBoxContents.set(
        this,
        ImmutableList.of(
            new Wrapper.ToolToggle(
                wrapper,
                "cursor-frame-move.png",
                localization.getString("move.frame.contents"),
                TOOL_FRAME_MOVE)));

    // Brushes
    MenuItem menuNew = new MenuItem(localization.getString("new.brush"));
    menuNew.setOnAction(
        e -> {
          PaletteBrush brush = new PaletteBrush();
          brush.name.set(context.namer.uniqueName(localization.getString("new.brush")));
          brush.size.set(20);
          GUILaunch.profileConfig.paletteBrushes.add(brush);
          if (GUILaunch.profileConfig.paletteBrushes.size() == 1) {
            setBrush(0);
          }
        });
    MenuItem menuDelete = new MenuItem(localization.getString("delete.brush"));
    BooleanBinding brushNotSelected =
        Bindings.createBooleanBinding(
            () ->
                wrapper.config.tool.get() != PaletteImageNodeConfig.TOOL_BRUSH
                    || !Range.closedOpen(0, GUILaunch.profileConfig.paletteBrushes.size())
                        .contains(wrapper.config.brush.get()),
            GUILaunch.profileConfig.paletteBrushes,
            wrapper.config.tool,
            wrapper.config.brush);
    menuDelete.disableProperty().bind(brushNotSelected);
    menuDelete.setOnAction(
        e -> {
          int index = GUILaunch.profileConfig.paletteBrushes.indexOf(wrapper.config.brush.get());
          GUILaunch.profileConfig.paletteBrushes.remove(index);
          if (GUILaunch.profileConfig.paletteBrushes.isEmpty()) {
            setBrush(0);
          } else {
            setBrush(Math.max(0, index - 1));
          }
        });
    MenuItem menuLeft = new MenuItem(localization.getString("move.brush.left"));
    menuLeft.disableProperty().bind(brushNotSelected);
    menuLeft.setOnAction(
        e -> {
          int index = wrapper.config.brush.get();
          if (index == 0) return;
          PaletteBrush brush = GUILaunch.profileConfig.paletteBrushes.get(index);
          GUILaunch.profileConfig.paletteBrushes.remove(index);
          int newIndex = index - 1;
          GUILaunch.profileConfig.paletteBrushes.add(newIndex, brush);
          setBrush(newIndex);
        });
    MenuItem menuRight = new MenuItem(localization.getString("move.brush.right"));
    menuRight.disableProperty().bind(brushNotSelected);
    menuRight.setOnAction(
        e -> {
          int index = wrapper.config.brush.get();
          if (index == GUILaunch.profileConfig.paletteBrushes.size() - 1) return;
          PaletteBrush brush = GUILaunch.profileConfig.paletteBrushes.get(index);
          GUILaunch.profileConfig.paletteBrushes.remove(index);
          int newIndex = index + 1;
          GUILaunch.profileConfig.paletteBrushes.add(newIndex, brush);
          setBrush(newIndex);
        });
    window.menuChildren.set(this, ImmutableList.of(menuNew, menuDelete, menuLeft, menuRight));

    HBox brushesBox = new HBox();
    brushesBox.setSpacing(3);
    brushesBox.setAlignment(Pos.CENTER_LEFT);
    brushesCleanup =
        Misc.mirror(
            GUILaunch.profileConfig.paletteBrushes,
            brushesBox.getChildren(),
            b ->
                new BrushButton(
                    b.size,
                    new DoubleIndirectHalfBinder<Integer, List<ProjectObject>, TrueColor>(
                            new IndirectHalfBinder<>(
                                b.useColor,
                                (Boolean u) ->
                                    opt(u ? b.paletteOffset : wrapper.config.paletteOffset)),
                            new IndirectHalfBinder<>(
                                wrapper.paletteBinder,
                                palette -> opt(new ListHalfBinder<>(palette, "entries"))),
                            (Integer i, List<ProjectObject> l) -> {
                              if (i >= l.size()) return opt(new ConstHalfBinder(null));
                              ProjectObject o = l.get(i);
                              if (o instanceof PaletteColor) {
                                return opt(new ScalarHalfBinder<TrueColor>(o, "color"));
                              } else if (o instanceof PaletteSeparator) {
                                return opt(new ConstHalfBinder(null));
                              } else throw new Assertion();
                            })
                        .map(t -> opt(t == null ? null : t.toJfx())),
                    wrapper.brushBinder.map(b1 -> opt(b1 == b))) {
                  @Override
                  public void selectBrush() {
                    wrapper.config.tool.set(PaletteImageNodeConfig.TOOL_BRUSH);
                    wrapper.config.brush.set(GUILaunch.profileConfig.paletteBrushes.indexOf(b));
                  }
                },
            button -> ((BrushButton) button).destroy(context, window),
            Misc.noopConsumer());

    window.toolBarChildren.set(this, ImmutableList.of(move, select, brushesBox));

    // Tab
    paletteState.addListener(
        (observable, oldValue, newValue) -> {
          if (oldValue != null) {
            oldValue.destroy(context, window);
          }
        });
    TilePane colors = new TilePane();
    HBox.setHgrow(colors, Priority.ALWAYS);
    colors.setHgap(2);
    colors.setVgap(2);
    paletteListener =
        wrapper.node.addPaletteSetListeners(
            (target, palette) -> {
              cleanPalette();
              paletteTilesCleanup =
                  palette.mirrorEntries(
                      colors.getChildren(),
                      new Function<ProjectObject, Node>() {
                        /* Keep as class - lambda form causes bytebuddy to go berserk */
                        @Override
                        public Node apply(ProjectObject e) {
                          PaletteTileBase tile;
                          if (e instanceof PaletteColor) {
                            tile = new ColorTile(context, (PaletteColor) e);
                          } else if (e instanceof PaletteSeparator) {
                            tile = new SeparatorTile((PaletteSeparator) e);
                          } else throw new Assertion();
                          return (Node) tile;
                        }
                      },
                      r0 -> {
                        PaletteTileBase r = (PaletteTileBase) r0;
                        r.destroy(context, window);
                      },
                      start -> {
                        paletteState.set(null);
                        for (int i = start; i < colors.getChildren().size(); ++i)
                          ((PaletteTileBase) colors.getChildren().get(i)).setIndex(i);
                      });
            });

    ContentReplacer<Cursor> paletteCursor =
        new ContentReplacer<Cursor>() {
          @Override
          protected void innerSet(String title, Cursor content) {
            colors.setCursor(content);
          }

          @Override
          protected void innerClear() {
            colors.setCursor(null);
          }
        };
    class MergeState implements PaletteState {
      private final int index;

      public MergeState(int index) {
        paletteCursor.set(this, topCenterCursor("call-merge32.png"));
        this.index = index;
      }

      @Override
      public void destroy(ProjectContext context, Window window) {
        paletteCursor.clear(this);
      }

      public Optional<Integer> getColor(int i) {
        ProjectObject entry = wrapper.node.palette().entriesGet(i);
        if (entry instanceof PaletteColor) {
          return Optional.of(((PaletteColor) entry).index());
        } else return Optional.empty();
      }

      @Override
      public void merge(ProjectContext context, int index) {
        try {
          if (index == this.index) {
            return;
          }
          final int old = getColor(this.index).get();
          final Optional<Integer> newOpt = getColor(index);
          if (!newOpt.isPresent()) {
            return;
          }
          final int newIndex = newOpt.get();
          ProjectContext.PaletteWrapper palette = context.getPaletteWrapper(wrapper.node.palette());
          context.change(
              null,
              c -> {
                palette.users.forEach(
                    p ->
                        p.frames()
                            .forEach(
                                f ->
                                    new ArrayList<>(f.tiles().keySet())
                                        .forEach(
                                            t -> {
                                              PaletteImage oldTile =
                                                  ((PaletteTile) f.tilesGet(t)).getData(context);
                                              PaletteImage newTile =
                                                  oldTile.copy(
                                                      0,
                                                      0,
                                                      oldTile.getWidth(),
                                                      oldTile.getHeight());
                                              newTile.mergeColor(old, newIndex);
                                              c.paletteImageFrame(f)
                                                  .tilesPut(
                                                      t, PaletteTile.create(context, newTile));
                                            })));
                c.palette(wrapper.node.palette()).entriesRemove(this.index, 1);
              });
          wrapper.paletteSelOffsetBinder.set(newIndex);
        } finally {
          paletteState.set(null);
        }
      }
    }

    VBox tabBox = new VBox();
    tabBox
        .getChildren()
        .addAll(
            new TitledPane(
                localization.getString("layer"),
                new WidgetFormBuilder()
                    .apply(b -> cleanup.add(Misc.nodeFormFields(context, b, wrapper)))
                    .buttons(
                        bb ->
                            bb.button(
                                    b -> {
                                      b.setText(localization.getString("layer"));
                                      b.setGraphic(new ImageView(icon("link-off.png")));
                                      b.setTooltip(
                                          new Tooltip(localization.getString("make.layer.unique")));
                                      b.setOnAction(
                                          e ->
                                              context.change(
                                                  null, c -> separate(context, c, wrapper)));
                                    })
                                .button(
                                    b -> {
                                      b.setText(localization.getString("palette"));
                                      b.setGraphic(new ImageView(icon("link-off.png")));
                                      b.setTooltip(
                                          new Tooltip(
                                              localization.getString("make.palette.unique")));
                                      b.setOnAction(
                                          e ->
                                              context.change(
                                                  null,
                                                  c -> {
                                                    Palette palette = wrapper.node.palette();
                                                    Palette newPalette = Palette.create(context);
                                                    newPalette.initialNameSet(
                                                        context,
                                                        context.namer.uniqueName(palette.name()));
                                                    newPalette.initialNextIdSet(
                                                        context, palette.nextId());
                                                    newPalette.initialEntriesAdd(
                                                        context,
                                                        palette.entries().stream()
                                                            .map(
                                                                entry -> {
                                                                  if (entry
                                                                      instanceof PaletteColor) {
                                                                    PaletteColor out =
                                                                        PaletteColor.create(
                                                                            context);
                                                                    out.initialColorSet(
                                                                        context,
                                                                        ((PaletteColor) entry)
                                                                            .color());
                                                                    out.initialIndexSet(
                                                                        context,
                                                                        ((PaletteColor) entry)
                                                                            .index());
                                                                    return out;
                                                                  } else if (entry
                                                                      instanceof PaletteSeparator) {
                                                                    return PaletteSeparator.create(
                                                                        context);
                                                                  } else throw new Assertion();
                                                                })
                                                            .collect(Collectors.toList()));
                                                    c.project(context.project)
                                                        .palettesAdd(newPalette);
                                                    c.paletteImageLayer(wrapper.node)
                                                        .paletteSet(newPalette);
                                                  }));
                                    }))
                    .build()),
            new TitledPane(
                localization.getString("palette"),
                new WidgetFormBuilder()
                    .text(
                        localization.getString("name"),
                        t -> {
                          cleanup2.add(
                              CustomBinding.bindBidirectional(
                                  new IndirectBinder<>(
                                      wrapper.paletteBinder,
                                      palette ->
                                          opt(
                                              new ScalarBinder<String>(
                                                  palette::addNameSetListeners,
                                                  palette::removeNameSetListeners,
                                                  v ->
                                                      context.change(
                                                          new ProjectContext.Tuple(
                                                              wrapper, "palette_name"),
                                                          c -> c.palette(palette).nameSet(v))))),
                                  new PropertyBinder<>(t.textProperty())));
                        })
                    .span(
                        () -> {
                          TrueColorPicker colorPicker = new TrueColorPicker();
                          colorPickerDisableCleanup =
                              CustomBinding.bind(
                                  colorPicker.disableProperty(),
                                  new DoubleHalfBinder<>(
                                          wrapper.config.tool,
                                          new DoubleHalfBinder<>(
                                                  wrapper.paletteSelOffsetBinder,
                                                  wrapper.paletteSelectionBinder)
                                              .map(
                                                  p ->
                                                      opt(
                                                          p.first == null
                                                              || p.first == 0
                                                              || p.second == null
                                                              || p.second
                                                                  instanceof PaletteSeparator)))
                                      .map(
                                          (tool, second) ->
                                              opt(second || !TOOL_BRUSH.equals(tool))));
                          GridPane.setHalignment(colorPicker, HPos.CENTER);
                          colorPickerCleanup =
                              CustomBinding.bindBidirectional(
                                  new IndirectBinder<TrueColor>(
                                      wrapper.paletteSelectionBinder,
                                      e -> {
                                        if (e == null) return opt(null);
                                        if (e instanceof PaletteColor) {
                                          return opt(
                                              new ScalarBinder<TrueColor>(
                                                  e,
                                                  "color",
                                                  v ->
                                                      context.change(
                                                          new ProjectContext.Tuple(e, "color"),
                                                          c ->
                                                              c.paletteColor((PaletteColor) e)
                                                                  .colorSet(v))));
                                        } else if (e instanceof PaletteSeparator) {
                                          return opt(null);
                                        } else throw new Assertion();
                                      }),
                                  new PropertyBinder<Color>(colorPicker.colorProxyProperty)
                                      .<TrueColor>bimap(
                                          c ->
                                              c == null
                                                  ? Optional.empty()
                                                  : opt(TrueColor.fromJfx(c)),
                                          c -> c == null ? opt(null) : opt(c.toJfx())));
                          return colorPicker;
                        })
                    .span(
                        () -> {
                          Button add =
                              HelperJFX.button("plus.png", localization.getString("new.color"));
                          Button addSeparator =
                              HelperJFX.button(
                                  "circle-medium.png", localization.getString("new.separator"));
                          ToggleButton merge =
                              new ToggleButton(null, new ImageView(icon("minus.png"))) {
                                @Override
                                public void fire() {
                                  int index = wrapper.paletteSelOffsetBinder.get().get();
                                  if (index < 0) throw new Assertion();
                                  if (paletteState.get() == null) {
                                    ProjectObject sel = unopt(wrapper.paletteSelectionBinder.get());
                                    if (sel instanceof PaletteColor) {
                                      paletteState.set(new MergeState(index));
                                    } else if (sel instanceof PaletteSeparator) {
                                      context.change(
                                          null,
                                          c -> {
                                            c.palette(wrapper.node.palette())
                                                .entriesRemove(index, 1);
                                          });
                                      wrapper.paletteSelOffsetBinder.set(Math.max(index - 1, 0));
                                    } else throw new Assertion();
                                  } else {
                                    paletteState.set(null);
                                  }
                                }
                              };
                          merge.setTooltip(new Tooltip(localization.getString("merge.colors")));
                          Button moveUp =
                              HelperJFX.button("arrow-up.png", localization.getString("move.back"));
                          Button moveDown =
                              HelperJFX.button(
                                  "arrow-down.png", localization.getString("move.next"));

                          VBox tools = new VBox();
                          tools.setSpacing(3);
                          tools.getChildren().addAll(add, addSeparator, merge, moveUp, moveDown);

                          HBox layout = new HBox();
                          layout.setSpacing(5);
                          layout.getChildren().addAll(tools, colors);

                          paletteAddCleanup =
                              CustomBinding.bind(
                                  add.disableProperty(),
                                  wrapper.paletteSelectionBinder.map(p -> opt(p == null)));
                          add.setOnAction(
                              _e -> {
                                ProjectObject selectedColor0 =
                                    unopt(wrapper.paletteSelectionBinder.get());
                                PaletteColor newColor = PaletteColor.create(context);
                                if (selectedColor0 instanceof PaletteColor) {
                                  PaletteColor selectedColor = (PaletteColor) selectedColor0;
                                  newColor.initialColorSet(context, selectedColor.color());
                                } else if (selectedColor0 instanceof PaletteSeparator) {
                                  newColor.initialColorSet(
                                      context, TrueColor.rgba(0, 0, 0, (byte) 255));
                                } else throw new Assertion();
                                Palette palette = wrapper.node.palette();
                                int id = palette.nextId();
                                newColor.initialIndexSet(context, id);
                                context.change(
                                    null,
                                    c -> {
                                      c.palette(palette).nextIdSet(id + 1);
                                      c.palette(palette)
                                          .entriesAdd(
                                              palette.entries().indexOf(selectedColor0) + 1,
                                              newColor);
                                    });
                                wrapper.paletteSelOffsetBinder.set(tiles.get(newColor).index);
                              });
                          paletteAddSeparatorCleanup =
                              CustomBinding.bind(
                                  addSeparator.disableProperty(),
                                  wrapper.paletteSelectionBinder.map(p -> opt(p == null)));
                          addSeparator.setOnAction(
                              _e -> {
                                Palette palette = wrapper.node.palette();
                                PaletteColor selectedColor =
                                    (PaletteColor) wrapper.paletteSelectionBinder.get().get();
                                PaletteSeparator sep = PaletteSeparator.create(context);
                                context.change(
                                    null,
                                    c -> {
                                      c.palette(palette)
                                          .entriesAdd(
                                              palette.entries().indexOf(selectedColor) + 1, sep);
                                    });
                              });
                          paletteRemoveCleanup =
                              CustomBinding.bind(
                                  merge.disableProperty(),
                                  wrapper.paletteSelOffsetFixedBinder.map(
                                      i -> opt(i == null || i <= 0)));
                          merge
                              .selectedProperty()
                              .bind(
                                  Bindings.createBooleanBinding(
                                      () -> paletteState.get() instanceof MergeState,
                                      paletteState));
                          paletteMoveUpCleanup =
                              CustomBinding.bind(
                                  moveUp.disableProperty(),
                                  wrapper.paletteSelOffsetFixedBinder.map(
                                      i -> opt(i == null || i <= 1)));
                          moveUp.setOnAction(
                              e -> {
                                int index = unopt(wrapper.paletteSelOffsetBinder.get());
                                int newOffset = index - 1;
                                Palette palette = wrapper.node.palette();
                                context.change(
                                    null,
                                    c -> c.palette(palette).entriesMoveTo(index, 1, newOffset));
                                wrapper.paletteSelOffsetBinder.set(newOffset);
                              });
                          paletteMoveDownCleanup =
                              CustomBinding.bind(
                                  moveDown.disableProperty(),
                                  wrapper.paletteSelOffsetBinder.map(
                                      i ->
                                          opt(
                                              i == null
                                                  || i < 1
                                                  || i >= colors.getChildren().size() - 1)));
                          moveDown.setOnAction(
                              e -> {
                                int index = unopt(wrapper.paletteSelOffsetBinder.get());
                                int newOffset = index + 1;
                                Palette palette = wrapper.node.palette();
                                context.change(
                                    null,
                                    c -> c.palette(palette).entriesMoveTo(index, 1, newOffset));
                                wrapper.paletteSelOffsetBinder.set(newOffset);
                              });

                          return layout;
                        })
                    .build()),
            toolPane = new TitledPane(localization.getString("tool"), null));
    window.layerTabContent.set(this, pad(tabBox));

    wrapper.config.tool.addListener(
        new ChangeListener<String>() {
          private ChangeListener<Number> brushListener;
          private ListChangeListener<PaletteBrush> brushesListener;

          {
            changed(null, null, wrapper.config.tool.get());
          }

          @Override
          public void changed(
              ObservableValue<? extends String> observable, String oldValue, String newValue) {
            if (brushListener != null) {
              wrapper.config.brush.removeListener(brushListener);
              brushListener = null;
            }
            if (brushesListener != null) {
              GUILaunch.profileConfig.paletteBrushes.removeListener(brushesListener);
              brushesListener = null;
            }
            if (PaletteImageNodeConfig.TOOL_MOVE.equals(newValue)) {
              setTool(
                  context,
                  window,
                  () -> {
                    return new ToolMove(window, wrapper);
                  });
            } else if (TOOL_FRAME_MOVE.equals(newValue)) {
              setTool(
                  context,
                  window,
                  () -> {
                    return new ToolFrameMove(window, wrapper);
                  });
            } else if (PaletteImageNodeConfig.TOOL_SELECT.equals(newValue)) {
              setTool(
                  context,
                  window,
                  () -> {
                    ToolSelect out = new ToolSelect(PaletteImageEditHandle.this);
                    out.setState(context, out.new StateCreate(context, window));
                    return out;
                  });
            } else if (PaletteImageNodeConfig.TOOL_BRUSH.equals(newValue)) {
              Runnable update =
                  new Runnable() {
                    PaletteBrush lastBrush;

                    @Override
                    public void run() {
                      int i = wrapper.config.brush.get();
                      if (!Range.closedOpen(0, GUILaunch.profileConfig.paletteBrushes.size())
                          .contains(i)) return;
                      PaletteBrush brush = GUILaunch.profileConfig.paletteBrushes.get(i);
                      setTool(
                          context,
                          window,
                          () -> new ToolBrush(window, PaletteImageEditHandle.this, brush));
                    }
                  };
              wrapper.config.brush.addListener((observable1, oldValue1, newValue1) -> update.run());
              GUILaunch.profileConfig.paletteBrushes.addListener(
                  brushesListener = c -> update.run());
              update.run();
            } else {
              throw new Assertion();
            }
          }
        });

    cleanup.add(
        () -> {
          window.layerTabContent.clear(this);
        });
  }

  private void cleanPalette() {
    if (paletteTilesCleanup != null) {
      paletteTilesCleanup.run();
      paletteTilesCleanup = null;
    }
  }

  private void setTool(ProjectContext context, Window window, Supplier<Tool> newTool) {
    if (tool != null) {
      tool.remove(context, window);
      tool = null;
    }
    tool = newTool.get();
  }

  @Override
  public void remove(ProjectContext context, Window window) {
    cleanPalette();
    wrapper.node.removePaletteSetListeners(paletteListener);
    if (tool != null) {
      tool.remove(context, window);
      tool = null;
    }
    brushesCleanup.run();
    paletteAddCleanup.destroy();
    paletteAddSeparatorCleanup.destroy();
    paletteRemoveCleanup.destroy();
    paletteMoveUpCleanup.destroy();
    paletteMoveDownCleanup.destroy();
    colorPickerCleanup.destroy();
    colorPickerDisableCleanup.destroy();
    cleanup.forEach(Runnable::run);
    cleanup2.forEach(BinderRoot::destroy);
    for (Hotkeys.Action action : actions) context.hotkeys.unregister(action);
    window.menuChildren.clear(this);
    window.toolBarChildren.clear(this);
    if (paletteState.get() != null) {
      paletteState.get().destroy(context, window);
    }
    window.timeline.toolBoxContents.clear(this);
  }

  @Override
  public void cursorMoved(ProjectContext context, Window window, DoubleVector vector) {
    vector =
        Window.toLocal(window.getSelectedForView(), wrapper.canvasHandle, vector).minus(offset());
    mouseX.set(vector.x);
    mouseY.set(vector.y);
  }

  @Override
  public Wrapper getWrapper() {
    return wrapper;
  }

  private Vector offset() {
    return wrapper.node.offset().plus(wrapper.canvasHandle.frame.offset());
  }

  @Override
  public void markStart(ProjectContext context, Window window, DoubleVector start) {
    if (tool == null) return;
    tool.markStart(
        context,
        window,
        Window.toLocal(window.getSelectedForView(), wrapper.canvasHandle, start).minus(offset()),
        start);
  }

  @Override
  public CanvasHandle getCanvas() {
    return wrapper.canvasHandle;
  }

  @Override
  public void mark(ProjectContext context, Window window, DoubleVector start, DoubleVector end) {
    if (tool == null) return;
    Vector offset = offset();
    tool.mark(
        context,
        window,
        Window.toLocal(window.getSelectedForView(), wrapper.canvasHandle, start).minus(offset),
        Window.toLocal(window.getSelectedForView(), wrapper.canvasHandle, end).minus(offset),
        start,
        end);
  }
}
