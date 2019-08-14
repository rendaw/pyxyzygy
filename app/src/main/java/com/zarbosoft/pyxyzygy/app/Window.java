package com.zarbosoft.pyxyzygy.app;

import com.google.common.base.Throwables;
import com.zarbosoft.automodel.lib.ProjectObject;
import com.zarbosoft.javafxbinders.BinderRoot;
import com.zarbosoft.javafxbinders.CustomBinding;
import com.zarbosoft.javafxbinders.DoubleHalfBinder;
import com.zarbosoft.javafxbinders.HalfBinder;
import com.zarbosoft.javafxbinders.IndirectBinder;
import com.zarbosoft.javafxbinders.IndirectHalfBinder;
import com.zarbosoft.javafxbinders.ListElementsHalfBinder;
import com.zarbosoft.javafxbinders.ListPropertyHalfBinder;
import com.zarbosoft.javafxbinders.ManualHalfBinder;
import com.zarbosoft.javafxbinders.PropertyBinder;
import com.zarbosoft.javafxbinders.PropertyHalfBinder;
import com.zarbosoft.pyxyzygy.app.config.InitialLayers;
import com.zarbosoft.pyxyzygy.app.config.NodeConfig;
import com.zarbosoft.pyxyzygy.app.parts.editor.Editor;
import com.zarbosoft.pyxyzygy.app.parts.structure.Structure;
import com.zarbosoft.pyxyzygy.app.parts.timeline.Timeline;
import com.zarbosoft.pyxyzygy.app.widgets.ChildrenReplacer;
import com.zarbosoft.pyxyzygy.app.widgets.ClosableScene;
import com.zarbosoft.pyxyzygy.app.widgets.ContentReplacer;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.app.widgets.TitledPane;
import com.zarbosoft.pyxyzygy.app.widgets.TrueColorPicker;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.app.wrappers.camera.CameraWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupChildWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.paletteimage.PaletteImageNodeWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.truecolorimage.TrueColorImageNodeWrapper;
import com.zarbosoft.pyxyzygy.core.model.latest.Camera;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupChild;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupLayer;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteImageLayer;
import com.zarbosoft.pyxyzygy.core.model.latest.TrueColorImageLayer;
import com.zarbosoft.pyxyzygy.seed.TrueColor;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.DeadCode;
import com.zarbosoft.rendaw.common.Pair;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToolBar;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zarbosoft.automodel.lib.Logger.logger;
import static com.zarbosoft.pyxyzygy.app.Global.localization;
import static com.zarbosoft.pyxyzygy.app.Global.nameHuman;
import static com.zarbosoft.pyxyzygy.app.parts.structure.Structure.findNode;
import static com.zarbosoft.pyxyzygy.app.parts.structure.Structure.getPath;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.icon;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.pad;
import static com.zarbosoft.rendaw.common.Common.opt;
import static com.zarbosoft.rendaw.common.Common.unopt;

public class Window {
  public List<FrameMapEntry>
      timeMap; // Visual time map - how time map between edit and view is visualized
  public ManualHalfBinder<Wrapper> selectedForEditWrapperEnabledBinder = new ManualHalfBinder<>();
  public ManualHalfBinder<EditHandle> selectedForEditOriginBinder = new ManualHalfBinder<>();
  public ManualHalfBinder<EditHandle> selectedForEditOpacityBinder = new ManualHalfBinder<>();
  public ManualHalfBinder<EditHandle> selectedForEditFramerateBinder = new ManualHalfBinder<>();
  public ManualHalfBinder<EditHandle> selectedForEditOnionBinder = new ManualHalfBinder<>();
  public ManualHalfBinder<EditHandle> selectedForEditCameraBorderBinder = new ManualHalfBinder<>();
  public ManualHalfBinder<EditHandle> selectedForEditPlayingBinder = new ManualHalfBinder<>();
  public ManualHalfBinder<EditHandle> selectedForEditTreeIconBinder = new ManualHalfBinder<>();
  private EditHandle selectedForEdit = null;
  public ManualHalfBinder<CanvasHandle> selectedForViewZoomControlBinder = new ManualHalfBinder<>();
  public ManualHalfBinder<CanvasHandle> selectedForViewMaxFrameBinder = new ManualHalfBinder<>();
  public ManualHalfBinder<CanvasHandle> selectedForViewPlayingBinder = new ManualHalfBinder<>();
  public ManualHalfBinder<CanvasHandle> selectedForViewFrameBinder = new ManualHalfBinder<>();
  public ManualHalfBinder<CanvasHandle> selectedForViewTreeIconBinder = new ManualHalfBinder<>();
  private CanvasHandle selectedForView = null;
  public Set<KeyCode> pressed = new HashSet<>();
  public Editor editor;
  private Tab layerTab;
  public final ContentReplacer<Node> layerTabContent =
      new ContentReplacer<Node>() {

        @Override
        protected void innerSet(String title, Node content) {
          layerTab.setContent2(content);
        }

        @Override
        protected void innerClear() {
          layerTab.setContent2(null);
        }
      };
  public final ContentReplacer<Cursor> editorCursor =
      new ContentReplacer<Cursor>() {
        @Override
        protected void innerSet(String title, Cursor content) {
          editor.outerCanvas.setCursor(content);
        }

        @Override
        protected void innerClear() {
          editor.outerCanvas.setCursor(null);
        }
      };
  private ToolBar toolBar;
  public ChildrenReplacer<Node> toolBarChildren;
  public ChildrenReplacer<MenuItem> menuChildren;
  public Stage stage;
  public Timeline timeline;
  private StackPane stack;
  public Structure structure;

  @SuppressWarnings("unused")
  private BinderRoot rootTabWidth;

  private ChangeListener<? super Boolean> maxListener;
  private TabPane leftTabs;

  @SuppressWarnings("unused")
  private BinderRoot rootCanRedo;

  @SuppressWarnings("unused")
  private BinderRoot rootCanUndo;

  @SuppressWarnings("unused")
  private BinderRoot zoomSpinRoot;

  public static HalfBinder<Number> effectiveWidthBinder(Node node) {
    return new PropertyHalfBinder<Bounds>(node.layoutBoundsProperty())
        .map(b -> opt(node.minWidth(0)));
  }

  public void showLayerTab() {
    leftTabs.getSelectionModel().select(layerTab);
  }

  public static class Tab extends javafx.scene.control.Tab {
    ScrollPane scrollPane = new ScrollPane();
    private BinderRoot minWidthRoot;
    HalfBinder<ScrollBar> scrollBarBinder =
        new PropertyHalfBinder<>(scrollPane.skinProperty())
            .map(
                s ->
                    s == null
                        ? opt(null)
                        : scrollPane.lookupAll(".scroll-bar").stream()
                            .filter(n -> ((ScrollBar) n).getOrientation() == Orientation.VERTICAL)
                            .findFirst()
                            .map(n -> opt((ScrollBar) n))
                            .orElse(Optional.empty()));

    {
      setContent(scrollPane);
      scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
      scrollPane.setFitToWidth(true);
      // TODO rewrite scrollpane because incompetent javafx devs hardcoded wheel scroll amount to be
      // a percent
      // of the scrollable area
    }

    public Tab(String title) {
      super(title);
    }

    public void setContent2(Node node) {
      if (minWidthRoot != null) {
        minWidthRoot.destroy();
        minWidthRoot = null;
      }
      if (node != null) {
        minWidthRoot =
            CustomBinding.bind(
                scrollPane.minWidthProperty(),
                new DoubleHalfBinder<>(
                        new IndirectHalfBinder<Number>(
                            scrollBarBinder,
                            s -> s == null ? opt(null) : opt(effectiveWidthBinder(s))),
                        effectiveWidthBinder(node))
                    .map(
                        (scrollWidth, contentWidth) ->
                            opt(
                                scrollWidth == null
                                    ? 0
                                    : scrollWidth.doubleValue() + contentWidth.doubleValue())));
      }
      scrollPane.setContent(node);
    }
  }

  public EditHandle getSelectedForEdit() {
    return selectedForEdit;
  }

  public CanvasHandle getSelectedForView() {
    return selectedForView;
  }

  public void selectForView(Context context, Wrapper wrapper) {
    selectForView(context, wrapper, false);
  }

  public void selectForView(Context context, Wrapper wrapper, boolean fromEdit) {
    Wrapper oldViewWrapper = selectedForView == null ? null : selectedForView.getWrapper();
    if (oldViewWrapper == wrapper) return;
    do {
      // Delete old canvas handles--

      // Case: Old parent of new - delete with exclude subtree
      {
        Wrapper at = wrapper;
        while (at != null) {
          if (at == oldViewWrapper) break;
          at = at.getParent();
        }
        if (at == oldViewWrapper) {
          if (selectedForView != null) selectedForView.remove(context, wrapper);
          break;
        }
      }

      // Case: New parent of old - no delete
      {
        Wrapper at = oldViewWrapper;
        while (at != null) {
          if (at == wrapper) break;
          at = at.getParent();
        }
        if (at == wrapper) {
          break;
        }
      }

      // Case: Neither - delete old no exclude
      {
        if (selectedForView != null) selectedForView.remove(context, null);
        break;
      }
    } while (false);
    selectedForView = null;

    if (wrapper == null) {
      timeline.setNodes(selectedForView, selectedForEdit);
      return;
    }

    selectedForView = wrapper.buildCanvas(context, this, null);

    selectedForViewZoomControlBinder.clear();
    selectedForViewMaxFrameBinder.clear();
    selectedForViewPlayingBinder.clear();
    selectedForViewFrameBinder.clear();
    selectedForViewTreeIconBinder.clear();

    editor.selectedForViewChanged(context, selectedForView);

    if (!fromEdit) timeline.setNodes(selectedForView, selectedForEdit);

    structure.selectedForView(context, selectedForView);

    selectedForViewZoomControlBinder.set(selectedForView);
    selectedForViewMaxFrameBinder.set(selectedForView);
    selectedForViewPlayingBinder.set(selectedForView);
    selectedForViewFrameBinder.set(selectedForView);
    selectedForViewTreeIconBinder.set(selectedForView);
  }

  public Optional<Wrapper> findNotViewParent(Wrapper parent, Wrapper child) {
    Wrapper lastPotentialView = null;
    Wrapper nextPotentialView = child;
    while (nextPotentialView != null) {
      if (parent == nextPotentialView) {
        return Optional.empty();
      }
      lastPotentialView = nextPotentialView;
      nextPotentialView = nextPotentialView.getParent();
    }
    return opt(lastPotentialView);
  }

  public void selectForEdit(Context context, Wrapper wrapper) {
    selectedForEditWrapperEnabledBinder.set(wrapper);

    /*
    Canvas is always created first if necessary
    Then edit handle
    Then various callbacks called
     */
    EditHandle oldValue = selectedForEdit;
    selectedForEdit = null;
    if (wrapper == null) {
      timeline.setNodes(selectedForView, selectedForEdit);
      return;
    }

    // Clear binder states while transitioning
    selectedForEditOriginBinder.clear();
    selectedForEditOpacityBinder.clear();
    selectedForEditFramerateBinder.clear();
    selectedForEditOnionBinder.clear();
    selectedForEditCameraBorderBinder.clear();
    selectedForEditPlayingBinder.clear();
    selectedForEditTreeIconBinder.clear();

    // Transition
    Optional<Wrapper> notViewParent =
        findNotViewParent(selectedForView == null ? null : selectedForView.getWrapper(), wrapper);
    if (!notViewParent.isPresent()) {
      // wrapper parent is already view
      selectedForEdit = wrapper.buildEditControls(context, this);
    } else {
      Wrapper useView = unopt(notViewParent);
      if (wrapper.getConfig().viewPath != null && !wrapper.getConfig().viewPath.isEmpty()) {
        Wrapper found = findNode(structure.tree.getRoot(), wrapper.getConfig().viewPath);
        if (!findNotViewParent(found, wrapper).isPresent()) useView = found;
      }
      selectForView(context, useView, true);
      selectedForEdit = wrapper.buildEditControls(context, this);
    }
    {
      List<Integer> path =
          getPath(selectedForView.getWrapper().tree.get()).collect(Collectors.toList());
      selectedForEdit.getWrapper().getConfig().viewPath = path;
    }

    // React to change
    structure.selectedForEdit(context, selectedForEdit);

    timeline.setNodes(selectedForView, selectedForEdit);

    editor.selectedForEditChanged(context, selectedForEdit);

    selectedForEditOriginBinder.set(selectedForEdit);
    selectedForEditOpacityBinder.set(selectedForEdit);
    selectedForEditFramerateBinder.set(selectedForEdit);
    selectedForEditOnionBinder.set(selectedForEdit);
    selectedForEditCameraBorderBinder.set(selectedForEdit);
    selectedForEditPlayingBinder.set(selectedForEdit);
    selectedForEditTreeIconBinder.set(selectedForEdit);

    // Mark selected items in structure
    NodeConfig oldConfig;
    if (oldValue != null) {
      oldValue.remove(context, this);
      oldConfig = oldValue.getWrapper().getConfig();
    } else {
      oldConfig = null;
    }
    NodeConfig newConfig;
    if (selectedForEdit != null) {
      newConfig = selectedForEdit.getWrapper().getConfig();
    } else {
      newConfig = null;
    }
    if (newConfig != oldConfig) {
      if (oldConfig != null) oldConfig.selectedSomewhere.set(false);
      if (newConfig != null) newConfig.selectedSomewhere.set(true);
    }
  }

  public void start(Context context, Stage primaryStage, boolean main) {
    this.stage = primaryStage;
    Thread.currentThread()
        .setUncaughtExceptionHandler(
            (thread, e) -> {
              logger.writeException(e, "Uncaught error");
              TextArea textArea = new TextArea(Throwables.getStackTraceAsString(e));
              textArea.setEditable(false);
              textArea.setWrapText(true);
              textArea.setMaxWidth(Double.MAX_VALUE);
              textArea.setMaxHeight(Double.MAX_VALUE);
              dialog(localization.getString("an.unexpected.error.occurred"))
                  .addContent(new TitledPane(localization.getString("trace"), textArea))
                  .addAction(ButtonType.OK, true, () -> true)
                  .go();
            });

    /*
    Window hotkeys should always have ctrl/alt because they preempt all other form editing including typing.
     */
    Stream.of(
            new Hotkeys.Action(
                Hotkeys.Scope.GLOBAL,
                "undo",
                localization.getString("undo"),
                Hotkeys.Hotkey.create(KeyCode.Z, true, false, false)) {
              @Override
              public void run(Context context, Window window) {
                context.model.undo();
              }
            },
            new Hotkeys.Action(
                Hotkeys.Scope.GLOBAL,
                "redo",
                localization.getString("redo"),
                Hotkeys.Hotkey.create(KeyCode.Y, true, false, false)) {

              @Override
              public void run(Context context, Window window) {
                context.model.redo();
              }
            })
        .forEach(context.hotkeys::register);

    leftTabs = new TabPane();
    leftTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
    final Tab structureTab = new Tab(localization.getString("project"));
    final Tab configTab = new Tab(localization.getString("settings"));
    layerTab = new Tab(localization.getString("layer"));
    layerTab.disableProperty().bind(layerTab.contentProperty().isNull());
    leftTabs.getTabs().addAll(structureTab, layerTab, configTab);
    this.rootTabWidth =
        CustomBinding.bind(
            leftTabs.minWidthProperty(),
            new ListPropertyHalfBinder<>(leftTabs.getTabs())
                .<List<Pair<Node, Bounds>>>indirectMap(
                    l ->
                        opt(
                            new ListElementsHalfBinder<>(
                                l.stream()
                                    .map(
                                        t ->
                                            new DoubleHalfBinder<Node, Bounds>(
                                                t.contentProperty(),
                                                new IndirectHalfBinder<Bounds>(
                                                    /* Ignored but included to trigger updates */
                                                    t.contentProperty(),
                                                    c ->
                                                        opt(
                                                            c == null
                                                                ? null
                                                                : c.layoutBoundsProperty()))))
                                    .collect(Collectors.toList()))))
                .map(
                    l -> {
                      double out =
                          l.stream()
                              .filter(t -> t.first != null)
                              .mapToDouble(t -> t.first.minWidth(-1))
                              .max()
                              .orElse(0);
                      return opt(out);
                    }));

    structure = new Structure(context, this, main);
    structureTab.setContent(structure.getWidget());

    Region menuSpring = new Region();
    menuSpring.setMinWidth(1);
    HBox.setHgrow(menuSpring, Priority.ALWAYS);

    Button resetScroll =
        HelperJFX.button(
            "image-filter-center-focus-weak.png", localization.getString("recenter.view"));
    resetScroll.setOnAction(
        e -> {
          editor.updateScroll(context, DoubleVector.zero);
        });

    HBox zoomBox = new HBox();
    {
      zoomBox.setAlignment(Pos.CENTER_LEFT);
      zoomBox.setFillHeight(true);
      Spinner<Integer> spinner = new Spinner<Integer>();
      spinner.setMaxWidth(Double.MAX_VALUE);
      spinner.setPrefWidth(60);
      spinner.setEditable(true);
      spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(-10, 50));
      zoomSpinRoot =
          CustomBinding.bindBidirectional(
              new IndirectBinder<>(
                  selectedForViewZoomControlBinder,
                  v -> opt(v == null ? null : v.getWrapper().getConfig().zoom)),
              new PropertyBinder<Integer>(spinner.getValueFactory().valueProperty()));
      final ImageView imageView = new ImageView(icon("zoom.png"));
      zoomBox.getChildren().addAll(imageView, spinner);
    }

    MenuButton menuButton = new MenuButton(null, new ImageView(icon("menu.png")));
    menuButton.disableProperty().bind(Bindings.isEmpty(menuButton.getItems()));
    menuChildren =
        new ChildrenReplacer<MenuItem>() {
          private List<MenuItem> was;

          @Override
          protected void innerSet(String title, List<MenuItem> content) {
            was = content;
            menuButton.getItems().addAll(content);
          }

          @Override
          protected void innerClear() {
            if (was != null) menuButton.getItems().removeAll(was);
          }
        };

    HBox toolbarExtra = new HBox();
    toolbarExtra.setSpacing(3);
    toolbarExtra.setFillHeight(true);
    toolbarExtra.setAlignment(Pos.CENTER_LEFT);
    toolBarChildren =
        new ChildrenReplacer<Node>() {
          @Override
          protected void innerSet(String title, List<Node> content) {
            toolbarExtra.getChildren().addAll(content);
          }

          @Override
          protected void innerClear() {
            toolbarExtra.getChildren().clear();
          }
        };

    MenuItem undoButton = new MenuItem(localization.getString("undo"));
    rootCanUndo =
        CustomBinding.bind(undoButton.disableProperty(), context.canUndo.map(b -> opt(!b)));
    undoButton.setOnAction(
        e -> {
          context.model.undo();
        });
    MenuItem redoButton = new MenuItem(localization.getString("redo"));
    rootCanRedo =
        CustomBinding.bind(redoButton.disableProperty(), context.canRedo.map(b -> opt(!b)));
    redoButton.setOnAction(
        e -> {
          context.model.redo();
        });
    menuButton.getItems().addAll(undoButton, redoButton, new SeparatorMenuItem());

    toolBar = new ToolBar();
    toolBar.getItems().addAll(toolbarExtra, menuSpring, zoomBox, resetScroll, menuButton);

    editor = new Editor(context, this);
    timeline = new Timeline(context, this);

    VBox editorBox = new VBox();
    editorBox.setFillWidth(true);
    VBox.setVgrow(editor.getWidget(), Priority.ALWAYS);
    editorBox.getChildren().addAll(toolBar, editor.getWidget());

    SplitPane specificLayout = new SplitPane();
    specificLayout.setOrientation(Orientation.VERTICAL);
    specificLayout.getItems().addAll(editorBox);
    SplitPane.setResizableWithParent(timeline.getWidget(), false);

    VBox configLayout = new VBox();
    configLayout.setSpacing(3);
    configLayout
        .getChildren()
        .addAll(
            new TitledPane(
                localization.getString("profile"),
                new WidgetFormBuilder()
                    .twoLine(
                        localization.getString("background.color"),
                        () -> {
                          TrueColorPicker w = new TrueColorPicker();
                          w.colorProxyProperty.set(
                              GUILaunch.profileConfig.backgroundColor.get().toJfx());
                          w.colorProxyProperty.addListener(
                              (observable, oldValue, newValue) ->
                                  GUILaunch.profileConfig.backgroundColor.set(
                                      TrueColor.fromJfx(newValue)));
                          return w;
                        })
                    .twoLine(
                        localization.getString("ghost.previous.color"),
                        () -> {
                          TrueColorPicker w = new TrueColorPicker();
                          w.colorProxyProperty.set(
                              GUILaunch.profileConfig.ghostPreviousColor.get().toJfx());
                          w.colorProxyProperty.addListener(
                              (observable, oldValue, newValue) ->
                                  GUILaunch.profileConfig.ghostPreviousColor.set(
                                      TrueColor.fromJfx(newValue)));
                          return w;
                        })
                    .twoLine(
                        localization.getString("ghost.next.color"),
                        () -> {
                          TrueColorPicker w = new TrueColorPicker();
                          w.colorProxyProperty.set(
                              GUILaunch.profileConfig.ghostNextColor.get().toJfx());
                          w.colorProxyProperty.addListener(
                              (observable, oldValue, newValue) ->
                                  GUILaunch.profileConfig.ghostNextColor.set(
                                      TrueColor.fromJfx(newValue)));
                          return w;
                        })
                    .enumDropDown(
                        localization.getString("default.layers"),
                        InitialLayers.class,
                        v -> {
                          switch (v) {
                            case BOTH:
                              return localization.getString("initial.layers.both");
                            case TRUE_COLOR:
                              return localization.getString("true.color.layer");
                            case PIXEL:
                              return localization.getString("pixel.layer");
                            default:
                              throw new DeadCode();
                          }
                        },
                        GUILaunch.profileConfig.newProjectInitialLayers)
                    .intSpinner(
                        localization.getString("default.zoom"),
                        -20,
                        20,
                        spinner -> {
                          spinner
                              .getValueFactory()
                              .setValue(GUILaunch.profileConfig.defaultZoom.get());
                          spinner
                              .getValueFactory()
                              .valueProperty()
                              .addListener(
                                  (observable, oldValue, newValue) ->
                                      GUILaunch.profileConfig.defaultZoom.set(newValue));
                        })
                    .intSpinner(
                        localization.getString("max.undo"),
                        1,
                        100000,
                        spinner -> {
                          spinner.getValueFactory().setValue(GUILaunch.profileConfig.maxUndo);
                          spinner
                              .getValueFactory()
                              .valueProperty()
                              .addListener(
                                  (observable, oldValue, newValue) ->
                                      GUILaunch.profileConfig.maxUndo = newValue);
                        })
                    .button(
                        button -> {
                          button.setText(localization.getString("clear.undo.redo"));
                          button.setOnAction(
                              e -> {
                                dialog(
                                        localization.getString(
                                            "are.you.sure.you.wish.to.clear.undo.redo"))
                                    .addAction(
                                        ButtonType.OK,
                                        false,
                                        () -> {
                                          context.model.clearHistory();
                                          return true;
                                        })
                                    .addAction(ButtonType.CANCEL, true, () -> true)
                                    .go();
                              });
                        })
                    .check(
                        localization.getString("show.origin"),
                        checkBox -> {
                          checkBox
                              .selectedProperty()
                              .bindBidirectional(GUILaunch.profileConfig.showOrigin);
                        })
                    .check(
                        localization.getString("show.timeline"),
                        checkBox -> {
                          checkBox
                              .selectedProperty()
                              .bindBidirectional(GUILaunch.profileConfig.showTimeline);
                        })
                    .build()),
            new TitledPane(
                localization.getString("global"),
                new WidgetFormBuilder()
                    .intSpinner(
                        localization.getString("tile.cache.mb"),
                        0,
                        1024 * 16,
                        spinner -> {
                          spinner
                              .getValueFactory()
                              .setValue(GUILaunch.globalConfig.cacheSize.get());
                          spinner
                              .getValueFactory()
                              .valueProperty()
                              .addListener(
                                  (observable, oldValue, newValue) ->
                                      GUILaunch.globalConfig.cacheSize.set(newValue));
                        })
                    .build()),
            new TitledPane(
                localization.getString("hotkeys"),
                ((Supplier<Node>)
                        () -> {
                          TableColumn<Hotkeys.Action, String> scope =
                              new TableColumn(localization.getString("scope"));
                          scope.setCellValueFactory(
                              param -> new SimpleStringProperty(param.getValue().scope.name()));
                          TableColumn<Hotkeys.Action, String> description =
                              new TableColumn(localization.getString("hotkey.description"));
                          description.setCellValueFactory(
                              param -> new SimpleStringProperty(param.getValue().description));
                          TableColumn<Hotkeys.Action, String> key =
                              new TableColumn(localization.getString("key"));
                          key.setCellValueFactory(param -> param.getValue().key.asString());
                          TableView<Hotkeys.Action> table = new TableView<>();
                          table.getColumns().addAll(scope, description, key);
                          table.setItems(context.hotkeys.actions);
                          return table;
                        })
                    .get()));
    configTab.setContent2(pad(configLayout));

    SplitPane generalLayout = new SplitPane();
    generalLayout.setOrientation(Orientation.HORIZONTAL);
    generalLayout.getItems().addAll(specificLayout);
    SplitPane.setResizableWithParent(leftTabs, false);

    stack = new StackPane(generalLayout);

    ClosableScene scene =
        new ClosableScene(stack, 1200, 800) {
          @Override
          public void close() {
            primaryStage.maximizedProperty().removeListener(maxListener);
          }
        };

    new DoubleHalfBinder<Boolean, Boolean>(
            new PropertyHalfBinder<>(context.config.maxCanvas),
            new PropertyHalfBinder<>(GUILaunch.profileConfig.showTimeline))
        .map(
            (max, show) -> {
              return opt(!max && show);
            })
        .addListener(
            show -> {
              if (show) {
                specificLayout.getItems().add(1, timeline.getWidget());
                specificLayout.setDividerPositions(context.config.timelineSplit);
                specificLayout
                    .getDividers()
                    .get(0)
                    .positionProperty()
                    .addListener(
                        (observable, oldValue, newValue) ->
                            context.config.timelineSplit = newValue.doubleValue());
              } else {
                specificLayout.getItems().remove(timeline.getWidget());
              }
            });

    context.config.maxCanvas.addListener(
        new ChangeListener<Boolean>() {
          {
            changed(null, null, context.config.maxCanvas.get());
          }

          @Override
          public void changed(
              ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
            if (newValue) {
              generalLayout.getItems().remove(leftTabs);
            } else {
              generalLayout.getItems().add(0, leftTabs);
              generalLayout.setDividerPositions(context.config.tabsSplit);
            }
          }
        });
    if (!context.config.maxCanvas.get())
      generalLayout
          .getDividers()
          .get(0)
          .positionProperty()
          .addListener(
              (observable, oldValue, newValue) ->
                  context.config.tabsSplit = newValue.doubleValue());

    scene.addEventFilter(
        KeyEvent.KEY_PRESSED,
        e -> {
          pressed.add(e.getCode());
          if (context.hotkeys.event(context, this, Hotkeys.Scope.GLOBAL, e)) e.consume();
        });
    scene.addEventFilter(
        KeyEvent.KEY_RELEASED,
        e -> {
          pressed.remove(e.getCode());
        });
    scene
        .getStylesheets()
        .addAll(
            getClass().getResource("widgets/style.css").toExternalForm(),
            getClass().getResource("widgets/colorpicker/style.css").toExternalForm(),
            getClass().getResource("widgets/brushbutton/style.css").toExternalForm());

    structure.populate();

    HelperJFX.switchStage(
        primaryStage,
        String.format("%s - %s", context.model.path.getFileName().toString(), nameHuman),
        scene,
        GUILaunch.profileConfig.maximize,
        main
            ? (Global.fixedProject
                ? e -> Global.shutdown()
                : e -> {
                  e.consume();
                  context.model.close();
                  GUILaunch.selectProject(primaryStage);
                })
            : e -> {});
    primaryStage
        .maximizedProperty()
        .addListener(
            maxListener =
                (observable, oldValue, newValue) -> {
                  GUILaunch.profileConfig.maximize = newValue.booleanValue();
                });
  }

  public static List<CanvasHandle> getAncestorsOutward(CanvasHandle start, CanvasHandle target) {
    List<CanvasHandle> ancestors = new ArrayList<>();
    start = start.getParent(); // Loop check is exclusive of start, so go above the stated start
    CanvasHandle at = target.getParent();
    while (at != start) {
      ancestors.add(at);
      at = at.getParent();
    }
    return ancestors;
  }

  public static List<CanvasHandle> getAncestorsInward(CanvasHandle start, CanvasHandle target) {
    List<CanvasHandle> ancestors = getAncestorsOutward(start, target);
    Collections.reverse(ancestors);
    return ancestors;
  }

  public static DoubleVector toLocal(
      CanvasHandle selectedForView, CanvasHandle translateTo, DoubleVector v) {
    for (CanvasHandle parent : getAncestorsInward(selectedForView, translateTo)) {
      v = parent.toInnerPosition(v);
    }
    return v;
  }

  /**
   * Not really global but canvas-space
   *
   * @param wrapper
   * @param v
   * @return
   */
  public static DoubleVector toGlobal(CanvasHandle wrapper, DoubleVector v) {
    DoubleVector zero = new DoubleVector(0, 0);
    for (CanvasHandle parent : getAncestorsOutward(null, wrapper)) {
      v = v.minus(parent.toInnerPosition(zero));
    }
    return v;
  }

  public static Wrapper createNode(
      Context context, Wrapper parent, int parentIndex, ProjectObject node) {
    if (false) {
      throw new Assertion();
    } else if (node instanceof Camera) {
      return new CameraWrapper(context, parent, parentIndex, (Camera) node);
    } else if (node instanceof GroupLayer) {
      return new GroupNodeWrapper(context, parent, parentIndex, (GroupLayer) node);
    } else if (node instanceof GroupChild) {
      return new GroupChildWrapper(context, parent, parentIndex, (GroupChild) node);
    } else if (node instanceof TrueColorImageLayer) {
      return new TrueColorImageNodeWrapper(
          context, parent, parentIndex, (TrueColorImageLayer) node);
    } else if (node instanceof PaletteImageLayer) {
      return new PaletteImageNodeWrapper(context, parent, parentIndex, (PaletteImageLayer) node);
    } else throw new Assertion();
  }

  public class DialogBuilder {
    private final String title;
    VBox layout = new VBox();
    HBox buttons = new HBox();

    private Node defaultNode;
    private Supplier<Boolean> defaultAction;

    private Pane grey;
    private DialogPane content;

    public DialogBuilder(String title) {
      this.title = title;
      layout.setSpacing(3);
      content = new DialogPane();
    }

    public DialogBuilder setDefault(Node node) {
      this.defaultNode = node;
      return this;
    }

    public DialogBuilder addContent(Node node) {
      this.layout.getChildren().add(node);
      return this;
    }

    public DialogBuilder addAction(ButtonType type, boolean isDefault, Supplier<Boolean> callback) {
      content.getButtonTypes().add(type);
      Button button = (Button) content.lookupButton(type);
      if (isDefault) {
        defaultNode = button;
        defaultAction = callback;
      }
      button.addEventHandler(
          ActionEvent.ACTION,
          ae -> {
            if (callback.get()) {
              close();
            }
          });
      return this;
    }

    public void close() {
      stack.getChildren().removeAll(grey);
    }

    public void go() {
      buttons.setSpacing(4);
      buttons.setAlignment(Pos.CENTER_RIGHT);

      layout.getChildren().add(buttons);

      content.setHeaderText(title);
      content.setBorder(
          new Border(
              new BorderStroke(
                  Color.DARKGRAY,
                  BorderStrokeStyle.SOLID,
                  CornerRadii.EMPTY,
                  new BorderWidths(2))));
      content.setPadding(new Insets(5));
      content
          .layoutXProperty()
          .bind(stage.widthProperty().divide(2.0).subtract(content.widthProperty().divide(2.0)));
      content
          .layoutYProperty()
          .bind(stage.heightProperty().divide(2.0).subtract(content.heightProperty().divide(2.0)));
      content.setContent(layout);

      grey = new Pane();
      grey.setBackground(
          new Background(
              new BackgroundFill(new Color(0, 0, 0, 0.4), CornerRadii.EMPTY, Insets.EMPTY)));
      grey.minWidthProperty().bind(stage.widthProperty());
      grey.minHeightProperty().bind(stage.heightProperty());
      grey.getChildren().add(content);

      stack.getChildren().addAll(grey);
      EventHandler<KeyEvent> keyEventEventHandler =
          e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
              close();
            } else if (e.getCode() == KeyCode.ENTER) {
              if (defaultAction.get()) {
                close();
              }
            } else {
              return;
            }
            e.consume();
          };
      grey.addEventFilter(KeyEvent.KEY_PRESSED, keyEventEventHandler);
      content.addEventFilter(KeyEvent.KEY_PRESSED, keyEventEventHandler);
      if (defaultNode != null) defaultNode.requestFocus();
    }
  }

  public DialogBuilder dialog(String title) {
    return new DialogBuilder(title);
  }

  public void error(Exception e, String title, String message) {
    logger.writeException(e, title + "\n" + message);
    TextArea textArea = new TextArea(Throwables.getStackTraceAsString(e));
    textArea.setEditable(false);
    textArea.setWrapText(true);
    textArea.setMaxWidth(Double.MAX_VALUE);
    textArea.setMaxHeight(Double.MAX_VALUE);
    dialog(message)
        .addContent(new TitledPane(localization.getString("trace"), textArea))
        .addAction(ButtonType.OK, true, () -> true)
        .go();
  }
}
