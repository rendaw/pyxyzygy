package com.zarbosoft.pyxyzygy.app;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.Weigher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.zarbosoft.automodel.lib.Logger;
import com.zarbosoft.automodel.lib.ModelBase;
import com.zarbosoft.automodel.lib.PeekVersion;
import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.javafxbinders.BinderRoot;
import com.zarbosoft.javafxbinders.CustomBinding;
import com.zarbosoft.javafxbinders.DoubleHalfBinder;
import com.zarbosoft.javafxbinders.ListPropertyHalfBinder;
import com.zarbosoft.javafxbinders.PropertyBinder;
import com.zarbosoft.javafxbinders.PropertyHalfBinder;
import com.zarbosoft.javafxbinders.SelectionModelBinder;
import com.zarbosoft.luxem.Luxem;
import com.zarbosoft.luxem.tree.Typed;
import com.zarbosoft.luxem.write.TreeWriter;
import com.zarbosoft.pyxyzygy.app.config.InitialLayers;
import com.zarbosoft.pyxyzygy.app.config.PaletteBrush;
import com.zarbosoft.pyxyzygy.app.config.RootGlobalConfig;
import com.zarbosoft.pyxyzygy.app.config.RootProfileConfig;
import com.zarbosoft.pyxyzygy.app.config.TrueColorBrush;
import com.zarbosoft.pyxyzygy.app.widgets.ClosableScene;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.core.model.ModelVersions;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupChild;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupLayer;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupPositionFrame;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupTimeFrame;
import com.zarbosoft.pyxyzygy.core.model.latest.Model;
import com.zarbosoft.pyxyzygy.core.model.latest.Palette;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteColor;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteImageFrame;
import com.zarbosoft.pyxyzygy.core.model.latest.PaletteImageLayer;
import com.zarbosoft.pyxyzygy.core.model.latest.Project;
import com.zarbosoft.pyxyzygy.core.model.latest.ProjectLayer;
import com.zarbosoft.pyxyzygy.core.model.latest.TrueColorImageFrame;
import com.zarbosoft.pyxyzygy.core.model.latest.TrueColorImageLayer;
import com.zarbosoft.pyxyzygy.seed.TrueColor;
import com.zarbosoft.pyxyzygy.seed.Vector;
import com.zarbosoft.rendaw.common.ChainComparator;
import com.zarbosoft.rendaw.common.Common;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zarbosoft.automodel.lib.Logger.logger;
import static com.zarbosoft.pyxyzygy.app.Global.NO_LENGTH;
import static com.zarbosoft.pyxyzygy.app.Global.NO_LOOP;
import static com.zarbosoft.pyxyzygy.app.Global.appDirs;
import static com.zarbosoft.pyxyzygy.app.Global.blendMax;
import static com.zarbosoft.pyxyzygy.app.Global.configDir;
import static com.zarbosoft.pyxyzygy.app.Global.fixedProfile;
import static com.zarbosoft.pyxyzygy.app.Global.getGroupLayerName;
import static com.zarbosoft.pyxyzygy.app.Global.getNameHuman;
import static com.zarbosoft.pyxyzygy.app.Global.localization;
import static com.zarbosoft.pyxyzygy.app.Global.opacityMax;
import static com.zarbosoft.pyxyzygy.app.Global.shutdown;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.icon;
import static com.zarbosoft.pyxyzygy.core.mynative.get_allocated;
import static com.zarbosoft.rendaw.common.Common.atomicWrite;
import static com.zarbosoft.rendaw.common.Common.opt;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public class GUILaunch extends Application {
  public static RootProfileConfig profileConfig;
  public static RootGlobalConfig globalConfig;
  public static final List<Image> appIcons = new ArrayList<>();

  public static final int CACHE_OBJECT;
  public static final int CACHE_ONION_BEFORE;
  public static final int CACHE_ONION_AFTER;
  public static Cache<Integer, Image> imageCache;

  static {
    int index = 0;
    CACHE_OBJECT = index++;
    CACHE_ONION_BEFORE = index++;
    //noinspection UnusedAssignment
    CACHE_ONION_AFTER = index++;
  }

  public GUILaunch() {
    try {
      // Hack because JavaFX was designed by sea sponges
      Class glassAppClass = getClass().getClassLoader().loadClass("com.sun.glass.ui.Application");
      Object glassApp = glassAppClass.getDeclaredMethod("GetApplication").invoke(null);
      glassApp.getClass().getMethod("setName", String.class).invoke(glassApp, getNameHuman());
    } catch (Exception e) {
      logger.writeException(e, "Failed to set application name - ignore if not on Linux.");
    }
  }

  public static void main(String[] args) {
    launch(args);
  }

  public static void selectProfile(Stage primaryStage) {
    class ProfileDialog extends ClosableScene {
      @SuppressWarnings("unused")
      private final BinderRoot rootMoveUp; // GC root

      @SuppressWarnings("unused")
      private final BinderRoot rootMoveDown; // GC root

      private final ListView<RootGlobalConfig.Profile> list;

      ProfileDialog() {
        super(new VBox());

        Namer profileNames = new Namer();
        globalConfig.profiles.forEach(p -> profileNames.countUniqueName(p.name.get()));

        list = new ListView<>();
        HBox.setHgrow(list, Priority.ALWAYS);
        list.setItems(FXCollections.observableList(globalConfig.profiles));
        list.setCellFactory(
            new Callback<ListView<RootGlobalConfig.Profile>, ListCell<RootGlobalConfig.Profile>>() {
              @Override
              public ListCell<RootGlobalConfig.Profile> call(
                  ListView<RootGlobalConfig.Profile> param) {
                return new ListCell<>() {
                  {
                    setFocusTraversable(false);
                  }

                  @Override
                  protected void updateItem(RootGlobalConfig.Profile item, boolean empty) {
                    rebind(item);
                    super.updateItem(item, empty);
                  }

                  private void rebind(RootGlobalConfig.Profile profile1) {
                    if (profile1 == null) {
                      textProperty().unbind();
                      setText(null);
                      setGraphic(null);
                    } else {
                      textProperty().bind(profile1.name);
                      setGraphic(null);
                    }
                  }
                };
              }
            });
        globalConfig.profiles.stream()
            .filter(p -> p.id == globalConfig.lastId)
            .findFirst()
            .ifPresentOrElse(
                p -> list.getSelectionModel().select(p),
                () -> {
                  logger.write("Couldn't find default profile %s", globalConfig.lastId);
                  list.getSelectionModel().select(0);
                });
        list.setFocusTraversable(false);

        PropertyHalfBinder<Number> listIndexBinder =
            new PropertyHalfBinder<>(list.getSelectionModel().selectedIndexProperty());
        DoubleHalfBinder<ObservableList<RootGlobalConfig.Profile>, Number> listBinder =
            new DoubleHalfBinder<>(new ListPropertyHalfBinder<>(list.getItems()), listIndexBinder);

        Button newProfile = HelperJFX.button("plus.png", localization.getString("new.profile"));
        newProfile.setFocusTraversable(false);
        newProfile.setOnAction(
            e -> {
              RootGlobalConfig.Profile profile1 = new RootGlobalConfig.Profile();
              profile1.name.set(
                  profileNames.uniqueName(localization.getString("new.profile.default.name")));
              profile1.id = globalConfig.nextId++;
              list.getItems().add(profile1);
              list.getSelectionModel().clearSelection();
              list.getSelectionModel().select(profile1);
            });
        Button deleteProfile =
            HelperJFX.button("minus.png", localization.getString("delete.profile"));
        deleteProfile.setFocusTraversable(false);
        deleteProfile
            .disableProperty()
            .bind(list.getSelectionModel().selectedItemProperty().isNull());
        deleteProfile.setOnAction(
            e -> {
              RootGlobalConfig.Profile profile1 = list.getSelectionModel().getSelectedItem();
              Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
              confirm.setTitle(String.format(localization.getString("delete.s"), profile1.name));
              confirm.setHeaderText(
                  String.format(
                      localization.getString("are.you.sure.you.want.to.delete.profile.s"),
                      profile1.name));
              confirm.setContentText(
                  localization.getString("deleting.the.profile.will.also.delete.all.its.brushes"));
              Optional<ButtonType> result = confirm.showAndWait();
              if (!result.isPresent() || result.get() != ButtonType.OK) {
                return;
              }
              list.getItems().remove(profile1);
            });
        Button moveUpButton =
            HelperJFX.button("arrow-up.png", localization.getString("move.profile.up"));
        rootMoveUp =
            CustomBinding.bind(
                moveUpButton.disableProperty(), listIndexBinder.map(i -> opt(i.intValue() < 1)));
        moveUpButton.setOnAction(
            e -> {
              int i = listIndexBinder.asOpt().get().intValue();
              RootGlobalConfig.Profile profile = list.getItems().get(i);
              list.getItems().remove(i);
              list.getItems().add(i - 1, profile);
              list.getSelectionModel().clearAndSelect(i - 1);
            });
        Button moveDownButton =
            HelperJFX.button("arrow-down.png", localization.getString("move.profile.down"));
        rootMoveDown =
            CustomBinding.bind(
                moveDownButton.disableProperty(),
                listBinder.map(
                    (list, i) -> opt(i.intValue() == -1 || i.intValue() == list.size() - 1)));
        moveDownButton.setOnAction(
            e -> {
              int i = listIndexBinder.asOpt().get().intValue();
              RootGlobalConfig.Profile profile = list.getItems().get(i);
              list.getItems().remove(i);
              list.getItems().add(i + 1, profile);
              list.getSelectionModel().clearAndSelect(i + 1);
            });
        Button renameProfile =
            HelperJFX.button("textbox.png", localization.getString("rename.profile"));
        renameProfile.setFocusTraversable(false);
        renameProfile
            .disableProperty()
            .bind(list.getSelectionModel().selectedItemProperty().isNull());
        renameProfile.setOnAction(
            e -> {
              RootGlobalConfig.Profile profile1 = list.getSelectionModel().getSelectedItem();
              TextInputDialog dialog = new TextInputDialog(profile1.name.get());
              dialog.setTitle(localization.getString("rename.profile"));
              dialog.setHeaderText(
                  String.format(localization.getString("rename.profile.s"), profile1.name));
              dialog.setContentText(localization.getString("profile.name"));
              dialog.showAndWait().ifPresent(name -> profile1.name.setValue(name));
            });
        VBox listButtons = new VBox();
        listButtons.setSpacing(3);
        listButtons
            .getChildren()
            .addAll(newProfile, deleteProfile, moveUpButton, moveDownButton, renameProfile);

        HBox listLayout = new HBox();
        listLayout.setSpacing(3);
        listLayout.getChildren().addAll(list, listButtons);

        Button open =
            new Button(
                localization.getString("select"), new ImageView(icon("folder-open-outline.png")));
        open.disableProperty().bind(list.getSelectionModel().selectedItemProperty().isNull());
        open.setOnAction(
            e -> {
              select();
            });
        open.setDefaultButton(true);
        Button cancel = new Button(localization.getString("cancel"));
        cancel.setOnAction(
            e -> {
              Global.shutdown();
            });
        HBox buttonLayout = new HBox();
        buttonLayout.setSpacing(6);
        buttonLayout.setPadding(new Insets(6));
        buttonLayout.setAlignment(Pos.CENTER_RIGHT);
        buttonLayout.getChildren().addAll(open, cancel);

        VBox topLayout = (VBox) getRoot();
        topLayout.setSpacing(3);
        topLayout.setPadding(new Insets(3));
        topLayout.getChildren().addAll(listLayout, buttonLayout);

        addEventFilter(
            KeyEvent.KEY_PRESSED,
            e -> {
              if (e.getCode() == KeyCode.ENTER) {
                select();
                return;
              }
              if (e.getCode() == KeyCode.ESCAPE) {
                cancel.fire();
              }
            });
      }

      private void select() {
        long id = list.getSelectionModel().getSelectedItem().id;
        loadProfile(id);
        globalConfig.lastId = id;
        selectProject(primaryStage);
      }
    }

    HelperJFX.switchStage(
        primaryStage,
        String.format(localization.getString("s.choose.profile"), Global.getNameHuman()),
        new ProfileDialog(),
        false,
        e -> {
          Global.shutdown();
        });
  }

  public static void selectProject(Stage primaryStage) {
    Runnable cancelResponse =
        Global.fixedProfile
            ? () -> {
              Global.shutdown();
            }
            : () -> {
              selectProfile(primaryStage);
            };
    class ProjectDialog extends ClosableScene {
      @SuppressWarnings("unused")
      private final BinderRoot rootChoice; // GC root

      public Image directoryImage = icon("folder.png");
      final SimpleObjectProperty<Path> cwd = new SimpleObjectProperty<>();
      final Map<Path, ChooserEntry> entries = new HashMap<>();
      private final ListView<ChooserEntry> list;

      class ChooserEntry {
        final Path path;
        final boolean isDirectory;
        final boolean isProject;
        final Instant modified;

        ChooserEntry(Path path) {
          this.path = path;
          isDirectory = Files.isDirectory(path);
          isProject = Files.exists(path.resolve("project.luxem"));
          modified = uncheck(() -> Files.getLastModifiedTime(path).toInstant());
        }
      }

      ProjectDialog() {
        super(new VBox());

        cwd.addListener(
            (observableValue, path, t1) -> {
              entries.clear();
              refresh();
            });
        SimpleObjectProperty<Path> resolvedPath = new SimpleObjectProperty<>();

        BooleanBinding pathExists =
            Bindings.createBooleanBinding(() -> Files.exists(resolvedPath.get()), resolvedPath);
        BooleanBinding pathIsProject =
            Bindings.createBooleanBinding(
                () -> {
                  Path check = resolvedPath.get().resolve("project.luxem");
                  return Files.exists(check);
                },
                resolvedPath);

        Runnable enterPath =
            () -> {
              if (!Files.isDirectory(resolvedPath.get())) return;
              cwd.set(resolvedPath.get());
            };
        Runnable newResolvedPath =
            () -> {
              try {
                newProject(primaryStage, resolvedPath.get());
              } catch (Exception e) {
                logger.writeException(e, "Error creating new project");
                HelperJFX.exceptionPopup(
                    null,
                    e,
                    localization.getString("error.creating.new.project"),
                    localization.getString(
                        "an.error.occurred.while.trying.to.create.the.project.n.nmake.sure.you.have.permission.to.write.to.the.project.directory"));
              }
            };
        Runnable openResolvedPath =
            () -> {
              try {
                openProject(primaryStage, resolvedPath.get());
              } catch (Exception e) {
                logger.writeException(e, "Error opening project");
                HelperJFX.exceptionPopup(
                    null,
                    e,
                    localization.getString("error.opening.project"),
                    localization.getString(
                        "an.error.occurred.while.trying.to.create.the.project.n.nmake.sure.you.have.permission.to.write.to.the.project.directory"));
              }
            };
        Runnable defaultActSelection =
            () -> {
              if (!pathExists.get()) {
                newResolvedPath.run();
              } else if (pathIsProject.get()) {
                openResolvedPath.run();
              } else {
                enterPath.run();
              }
            };

        Label explanation =
            new Label(
                localization.getString("select.an.existing.project.directory.or.create.a.new.one"));
        explanation.setPadding(new Insets(4));
        explanation.setWrapText(true);
        explanation.setMinWidth(50);

        Label here = new Label();
        here.setTextFill(Color.GRAY);
        here.setTextOverrun(OverrunStyle.LEADING_WORD_ELLIPSIS);
        TextField text = new TextField();
        text.setOnAction(
            e -> {
              enterPath.run();
            });
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox hereLayout = new HBox();
        hereLayout.setAlignment(Pos.CENTER_RIGHT);
        hereLayout.setSpacing(4);
        hereLayout.setPadding(new Insets(4));
        hereLayout.getChildren().addAll(here, text);

        Button up = HelperJFX.button("arrow-up.png", localization.getString("leave.directory"));
        up.setOnAction(
            e -> {
              Path parent = cwd.get().getParent();
              if (parent == null) return;
              cwd.set(parent);
            });
        Button refresh = HelperJFX.button("refresh.png", localization.getString("refresh"));
        refresh.setOnAction(
            e -> {
              refresh();
            });
        Region space = new Region();
        space.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(space, Priority.ALWAYS);
        Button createDirectory =
            HelperJFX.button("folder-plus.png", localization.getString("create.directory"));
        createDirectory.setOnAction(
            e -> {
              TextInputDialog dialog = new TextInputDialog(localization.getString("new.folder"));
              dialog.setTitle(localization.getString("directory.name"));
              dialog.setContentText(localization.getString("enter.a.name.for.the.directory"));
              dialog
                  .showAndWait()
                  .ifPresent(
                      t ->
                          uncheck(
                              () -> {
                                Path newPath = cwd.get().resolve(t);
                                Files.createDirectory(newPath);
                                cwd.set(newPath);
                              }));
            });
        ToolBar toolbar = new ToolBar();
        toolbar.getItems().addAll(up, refresh, space, createDirectory);

        list = new ListView<>();
        list.setCellFactory(
            new Callback<ListView<ChooserEntry>, ListCell<ChooserEntry>>() {
              @Override
              public ListCell<ChooserEntry> call(ListView<ChooserEntry> entryListView) {
                return new ListCell<>() {
                  ImageView icon = new ImageView();

                  {
                    setGraphic(icon);
                    addEventFilter(
                        MouseEvent.MOUSE_CLICKED,
                        e -> {
                          if (e.getClickCount() == 2) {
                            defaultActSelection.run();
                          }
                        });
                  }

                  @Override
                  protected void updateItem(ChooserEntry entry, boolean b) {
                    if (entry == null) {
                      setText("");
                      icon.setImage(null);
                      setDisable(false);
                    } else {
                      setText(entry.path.getFileName().toString());
                      if (entry.isDirectory) {
                        if (entry.isProject) icon.setImage(null);
                        else icon.setImage(directoryImage);
                        setDisable(false);
                      } else {
                        icon.setImage(null);
                        setDisable(true);
                      }
                    }
                    super.updateItem(entry, b);
                  }
                };
              }
            });
        VBox listLayout = new VBox();
        listLayout.getChildren().addAll(toolbar, list);

        Button create =
            new Button(
                localization.getString("new.project"),
                new ImageView(icon("folder-plus-outline.png")));
        create.setOnAction(
            ev -> {
              newResolvedPath.run();
            });
        Button open =
            new Button(
                localization.getString("open.project"),
                new ImageView(icon("folder-open-outline.png")));
        open.setOnAction(
            ev -> {
              openResolvedPath.run();
            });
        Button cancel = new Button(localization.getString("cancel"));
        cancel.setOnAction(
            e -> {
              cancelResponse.run();
            });
        HBox buttonLayout = new HBox();
        buttonLayout.setSpacing(3);
        buttonLayout.setAlignment(Pos.CENTER_RIGHT);
        buttonLayout.getChildren().addAll(create, open, cancel);

        VBox top = (VBox) getRoot();
        top.setFillWidth(true);
        top.setSpacing(6);
        top.setPadding(new Insets(3));
        top.getChildren().addAll(explanation, hereLayout, listLayout, buttonLayout);

        addEventFilter(
            KeyEvent.KEY_PRESSED,
            e -> {
              if (e.getCode() == KeyCode.ENTER) {
                defaultActSelection.run();
                return;
              }
              if (e.getCode() == KeyCode.ESCAPE) {
                cancel.fire();
              }
            });

        cwd.addListener((observableValue, path, t1) -> text.setText(""));
        cwd.set(Paths.get(GUILaunch.profileConfig.lastDir));
        this.rootChoice =
            CustomBinding.<Path>bindBidirectional(
                new PropertyBinder<String>(text.textProperty())
                    .<Path>bimap(
                        t -> Optional.of(cwd.get().resolve(t)),
                        (Path v) -> opt(v.getFileName().toString())),
                new SelectionModelBinder<>(list.getSelectionModel())
                    .<Path>bimap(
                        e -> Optional.ofNullable(e).map(v -> v.path),
                        (Path v) -> opt(entries.get(v))));
        resolvedPath.bind(
            Bindings.createObjectBinding(
                () -> cwd.get().resolve(text.getText()), cwd, text.textProperty()));
        here.textProperty().bind(cwd.asString().concat("/"));
        create.disableProperty().bind(pathExists);
        open.disableProperty().bind(pathIsProject.not());
        list.getSelectionModel().clearSelection();
        list.getSelectionModel().select(0);
      }

      public void refresh() {
        list.getItems()
            .setAll(
                uncheck(() -> Files.list(cwd.get()))
                    .map(
                        p ->
                            entries.computeIfAbsent(
                                p,
                                p1 -> {
                                  try {
                                    return new ChooserEntry(p1);
                                  } catch (Common.UncheckedNoSuchFileException e) {
                                    return null;
                                  }
                                }))
                    .filter(e -> e != null)
                    .sorted(
                        new ChainComparator<ChooserEntry>()
                            .greaterFirst(e -> e.isDirectory)
                            .greaterFirst(e -> e.modified)
                            .build())
                    .collect(Collectors.toList()));
      }
    }
    HelperJFX.switchStage(
        primaryStage,
        String.format(localization.getString("s.choose.project"), Global.getNameHuman()),
        new ProjectDialog(),
        false,
        e -> {
          if (!fixedProfile) e.consume();
          cancelResponse.run();
        });
  }

  @Override
  public void start(Stage primaryStage) throws Exception {

    // Set up logging
    logger = new Logger.TerminalPlusFile(appDirs);
    Thread.currentThread()
        .setUncaughtExceptionHandler(
            (thread, e) -> {
              logger.writeException(e, localization.getString("unexpected.error"));
              HelperJFX.exceptionPopup(
                  primaryStage,
                  e,
                  localization.getString("unexpected.error"),
                  localization.getString("an.unexpected.error.occurred"));
            });

    try {
      // Error early if JNI config is broken
      new com.zarbosoft.pyxyzygy.core.mynativeJNI();

      // Set up stage
      Stream.of("appicon16.png", "appicon32.png", "appicon64.png")
          .map(s -> icon(s))
          .forEach(appIcons::add);
      primaryStage.getIcons().addAll(GUILaunch.appIcons);

      // Load global config
      globalConfig =
          ConfigBase.deserialize(
              new TypeInfo(RootGlobalConfig.class),
              configDir.resolve("profiles.luxem"),
              () -> {
                RootGlobalConfig config = new RootGlobalConfig();
                RootGlobalConfig.Profile profile = new RootGlobalConfig.Profile();
                profile.id = config.nextId++;
                profile.name.set(localization.getString("default"));
                config.profiles.add(profile);
                return config;
              });
      globalConfig.cacheSize.addListener(
          new ChangeListener<Number>() {
            {
              changed(null, null, globalConfig.cacheSize.get());
            }

            @Override
            public void changed(
                ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
              imageCache =
                  CacheBuilder.newBuilder()
                      .recordStats()
                      .maximumWeight(newValue.intValue() * 1024 * 1024)
                      .weigher(
                          (Weigher<Integer, Image>)
                              (key, value) -> (int) (value.getWidth() * value.getHeight() * 4))
                      .build();
            }
          });
      Timer cacheStatsTimer = new Timer();
      TimerTask cacheStatsTask =
          new TimerTask() {
            @Override
            public void run() {
              logger.write("Cache stats:\nSize: %s\n%s", imageCache.size(), imageCache.stats());
              logger.write(
                  "True color tile flush cache: %s",
                  TrueColorTileHelp.cache.size(), TrueColorTileHelp.cache.stats());
              logger.write(
                  "Palette tile flush cache: %s",
                  PaletteTileHelp.cache.size(), PaletteTileHelp.cache.stats());
              logger.write("Allocated (native image): %s", get_allocated());
            }
          };
      cacheStatsTimer.scheduleAtFixedRate(cacheStatsTask, 0, 1000 * 60 * 5);
      // cacheStatsTimer.scheduleAtFixedRate(cacheStatsTask, 0, 1000);
      shutdown.add(
          () -> {
            cacheStatsTimer.cancel();
            cacheStatsTask.run();
          });

      // Load profile
      Optional.ofNullable(getParameters().getNamed().get("profile-id"))
          .map(s -> Long.parseLong(s))
          .flatMap(
              id ->
                  globalConfig.profiles.stream().filter(p -> Objects.equals(id, p.id)).findFirst())
          .or(
              () ->
                  Optional.ofNullable(getParameters().getNamed().get("profile"))
                      .flatMap(
                          name ->
                              globalConfig.profiles.stream()
                                  .filter(p -> name.equals(p.name.get()))
                                  .findFirst()))
          .ifPresentOrElse(
              p -> {
                loadProfile(p.id);
                Global.fixedProfile = true;
                if (getParameters().getUnnamed().isEmpty()) {
                  selectProject(primaryStage);
                } else {
                  Global.fixedProject = true;
                  Path path = Paths.get(this.getParameters().getUnnamed().get(0));
                  if (Files.exists(path)) {
                    if (Files.exists(path.resolve("project.luxem"))) {
                      openProject(primaryStage, path);
                    } else {
                      throw new IllegalArgumentException(
                          String.format("Directory is not a project", path));
                    }
                  } else {
                    newProject(primaryStage, path);
                  }
                }
              },
              () -> {
                selectProfile(primaryStage);
              });
    } catch (Exception e) {
      logger.writeException(e, "Uncaught error");
      HelperJFX.exceptionPopup(
          null,
          e,
          localization.getString("unexpected.error"),
          localization.getString("an.unexpected.error.occurred"));
      shutdown();
    }
  }

  private static void loadProfile(long id) {
    profileConfig =
        ConfigBase.deserialize(
            new TypeInfo(RootProfileConfig.class),
            configDir.resolve(String.format("profile_%s.luxem", Long.toString(id))),
            () -> {
              RootProfileConfig config = new RootProfileConfig();
              {
                TrueColorBrush transparentBrush = new TrueColorBrush();
                transparentBrush.name.set(localization.getString("erase"));
                transparentBrush.size.set(50);
                transparentBrush.blend.set(blendMax);
                transparentBrush.color.set(TrueColor.fromJfx(Color.TRANSPARENT));
                transparentBrush.useColor.set(true);
                TrueColorBrush defaultBrush = new TrueColorBrush();
                defaultBrush.name.set(localization.getString("default"));
                defaultBrush.size.set(10);
                defaultBrush.blend.set(blendMax);
                defaultBrush.color.set(TrueColor.fromJfx(Color.BLACK));
                defaultBrush.useColor.set(true);
                config.trueColorBrushes.addAll(transparentBrush, defaultBrush);
              }
              {
                PaletteBrush transparentBrush = new PaletteBrush();
                transparentBrush.name.set(localization.getString("erase"));
                transparentBrush.size.set(50);
                transparentBrush.paletteOffset.set(0);
                transparentBrush.useColor.set(true);
                PaletteBrush defaultBrush = new PaletteBrush();
                defaultBrush.name.set(localization.getString("default"));
                defaultBrush.size.set(10);
                defaultBrush.useColor.set(false);
                config.paletteBrushes.addAll(transparentBrush, defaultBrush);
              }
              return config;
            });
  }

  public static void newProject(Stage primaryStage, Path path) {
    profileConfig.lastDir = path.getParent().toString();
    Model model =
        ModelVersions.create(
            path,
            64,
            context -> {
              Function<ProjectLayer, GroupChild> createChild =
                  l -> {
                    GroupChild groupChild = GroupChild.create(context);
                    groupChild.initialOpacitySet(context, opacityMax);
                    groupChild.initialEnabledSet(context, true);

                    GroupPositionFrame groupPositionFrame = GroupPositionFrame.create(context);
                    groupPositionFrame.initialLengthSet(context, NO_LENGTH);
                    groupPositionFrame.initialOffsetSet(context, Vector.ZERO);
                    groupChild.initialPositionFramesAdd(
                        context, ImmutableList.of(groupPositionFrame));

                    GroupTimeFrame groupTimeFrame = GroupTimeFrame.create(context);
                    groupTimeFrame.initialLengthSet(context, NO_LENGTH);
                    groupTimeFrame.initialInnerOffsetSet(context, 0);
                    groupTimeFrame.initialInnerLoopSet(context, NO_LOOP);
                    groupChild.initialTimeFramesAdd(context, ImmutableList.of(groupTimeFrame));

                    groupChild.initialInnerSet(context, l);

                    return groupChild;
                  };

              List<GroupChild> children = new ArrayList<>();
              if (profileConfig.newProjectInitialLayers.get() == InitialLayers.BOTH
                  || profileConfig.newProjectInitialLayers.get() == InitialLayers.TRUE_COLOR) {
                TrueColorImageLayer trueColorImageNode = TrueColorImageLayer.create(context);
                trueColorImageNode.initialNameSet(context, Global.getTrueColorLayerName());
                trueColorImageNode.initialOffsetSet(context, Vector.ZERO);
                TrueColorImageFrame trueColorImageFrame = TrueColorImageFrame.create(context);
                trueColorImageFrame.initialLengthSet(context, -1);
                trueColorImageFrame.initialOffsetSet(context, Vector.ZERO);
                trueColorImageNode.initialFramesAdd(context, ImmutableList.of(trueColorImageFrame));
                children.add(createChild.apply(trueColorImageNode));
              }
              if (profileConfig.newProjectInitialLayers.get() == InitialLayers.BOTH
                  || profileConfig.newProjectInitialLayers.get() == InitialLayers.PIXEL) {
                Palette palette = Palette.create(context);
                palette.initialNameSet(context, Global.getPaletteName());
                palette.initialNextIdSet(context, 2);
                PaletteColor transparent = PaletteColor.create(context);
                transparent.initialIndexSet(context, 0);
                transparent.initialColorSet(context, TrueColor.fromJfx(Color.TRANSPARENT));
                PaletteColor black = PaletteColor.create(context);
                black.initialIndexSet(context, 1);
                black.initialColorSet(context, TrueColor.fromJfx(Color.BLACK));
                palette.initialEntriesAdd(context, ImmutableList.of(transparent, black));
                ((Project) context.current.root)
                    .initialPalettesAdd(context, ImmutableList.of(palette));

                PaletteImageLayer paletteImageNode = PaletteImageLayer.create(context);
                paletteImageNode.initialPaletteSet(context, palette);
                paletteImageNode.initialNameSet(context, Global.getPaletteLayerName());
                paletteImageNode.initialOffsetSet(context, Vector.ZERO);
                PaletteImageFrame paletteImageFrame = PaletteImageFrame.create(context);
                paletteImageFrame.initialLengthSet(context, -1);
                paletteImageFrame.initialOffsetSet(context, Vector.ZERO);
                paletteImageNode.initialFramesAdd(context, ImmutableList.of(paletteImageFrame));
                children.add(createChild.apply(paletteImageNode));
              }

              GroupLayer groupNode = GroupLayer.create(context);
              groupNode.initialNameSet(context, getGroupLayerName());
              groupNode.initialOffsetSet(context, Vector.ZERO);
              groupNode.initialChildrenAdd(context, children);

              ((Project) context.current.root).initialTopAdd(context, ImmutableList.of(groupNode));
            },
            profileConfig.maxUndo);
    Context context = new Context(model);
    context.config.defaultZoom = profileConfig.defaultZoom.get();
    context.config.viewPath = ImmutableList.of(0);
    context.config.editPath = ImmutableList.of(0, 0);
    new Window().start(context, primaryStage, true);
  }

  public static ModelBase.DeserializeResult fixedDeserialize(Path path) {
    if ("v0".equals(PeekVersion.check(ModelBase.projectPath(path)))) {
      uncheck(
          () -> {
            List struct;
            try (InputStream source = Files.newInputStream(ModelBase.projectPath(path))) {
              struct = Luxem.parse(source);
            }
            Typed root = (Typed) struct.get(0);
            root.name = "v1";
            Object tileSize = ((Map) root.value).remove("tileSize");
            for (Object o1 : (List) ((Map) root.value).get("objects")) {
              Typed o = (Typed) o1;
              if ("Project".equals(o.name)) {
                ((Map) o.value).put("tileSize", tileSize);
              }
            }
            atomicWrite(
                ModelBase.projectPath(path),
                dest -> {
                  uncheck(() -> TreeWriter.write(dest, struct));
                });
          });
    }
    if ("v1".equals(PeekVersion.check(ModelBase.projectPath(path)))) {
      uncheck(
          () -> {
            List struct;
            try (InputStream source = Files.newInputStream(ModelBase.projectPath(path))) {
              struct = Luxem.parse(source);
            }
            Typed root = (Typed) struct.get(0);
            root.name = "v2";
            Map rootMap = (Map) root.value;
            String projectId = "0";
            for (Object o1 : (List) rootMap.get("objects")) {
              Typed o = (Typed) o1;
              if ("Project".equals(o.name)) {
                projectId = (String) ((Map) o.value).get("id");
              }
            }
            rootMap.put(
                "current",
                ImmutableMap.builder()
                    .put("id", projectId)
                    .put("activeChange", rootMap.remove("activeChange"))
                    .put("undo", rootMap.remove("undo"))
                    .put("redo", rootMap.remove("redo"))
                    .build());
            rootMap.put("snapshots", new ArrayList<>());
            atomicWrite(
                ModelBase.projectPath(path),
                dest -> {
                  uncheck(() -> TreeWriter.write(dest, struct));
                });
          });
    }
    return ModelVersions.deserialize(path, profileConfig.maxUndo);
  }

  public static void openProject(Stage primaryStage, Path path) {
    profileConfig.lastDir = path.getParent().toString();
    ModelBase.DeserializeResult deserializeResult = fixedDeserialize(path);
    if (deserializeResult.fixed) {
      Alert alert =
          new Alert(
              Alert.AlertType.ERROR,
              String.format(
                  localization.getString(
                      "there.were.one.or.more.errors.while.opening.the.project.n.n.s.attempted.to.fix.them.but.in.case.it.did.so.incorrectly.we.recommend.you.back.up.the.project.and.re.open.it"),
                  getNameHuman()),
              ButtonType.OK);
      alert.setTitle(String.format(localization.getString("s.error"), Global.getNameHuman()));
      alert.setHeaderText(localization.getString("error.opening.project"));
      alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
      alert.showAndWait();
    }
    Model context;
    if (deserializeResult.model.needsMigrate()) {
      Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
      confirm.initOwner(primaryStage);
      confirm.setTitle(localization.getString("clear.undo.redo"));
      confirm.setHeaderText(
          localization.getString("opening.this.project.will.clear.it.s.history.undo.redo.data"));
      confirm.setContentText(localization.getString("back.up.your.project.before.proceeding"));
      Optional<ButtonType> result = confirm.showAndWait();
      if (!result.isPresent() || result.get() != ButtonType.OK) {
        Global.shutdown();
        return;
      }
      deserializeResult.model.clearHistory();
      context = (Model) deserializeResult.model.migrate();
    } else {
      context = (Model) deserializeResult.model;
    }
    new Window().start(new Context(context), primaryStage, true);
  }
}
