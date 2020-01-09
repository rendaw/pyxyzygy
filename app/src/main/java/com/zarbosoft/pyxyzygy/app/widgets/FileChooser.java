package com.zarbosoft.pyxyzygy.app.widgets;

import com.zarbosoft.javafxbinders.BinderRoot;
import com.zarbosoft.javafxbinders.CustomBinding;
import com.zarbosoft.javafxbinders.DoubleHalfBinder;
import com.zarbosoft.javafxbinders.HalfBinder;
import com.zarbosoft.javafxbinders.PropertyBinder;
import com.zarbosoft.javafxbinders.PropertyHalfBinder;
import com.zarbosoft.javafxbinders.SelectionModelBinder;
import com.zarbosoft.javafxbinders.VariableBinder;
import com.zarbosoft.rendaw.common.ChainComparator;
import com.zarbosoft.rendaw.common.Common;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextField;
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
import javafx.scene.text.TextAlignment;
import javafx.util.Callback;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.zarbosoft.pyxyzygy.app.Global.localization;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.icon;
import static com.zarbosoft.rendaw.common.Common.noopConsumer;
import static com.zarbosoft.rendaw.common.Common.opt;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public abstract class FileChooser extends VBox {
  @SuppressWarnings("unused")
  private final BinderRoot rootChoice; // GC root

  private final DialogInterface builder;
  @SuppressWarnings("unused")
  private final BinderRoot cwdListener;

  public Image directoryImage = icon("folder.png");
  final VariableBinder<Path> cwd = new VariableBinder<>();
  final Map<Path, ChooserEntry> entries = new HashMap<>();
  private final ListView<ChooserEntry> list;
  private Consumer<Path> concludeOpen;
  private Consumer<Path> concludeNew;
  private final HalfBinder<Path> resolvedPath;

  public void go() {
    builder.go();
  }

  class ChooserEntry {
    final java.nio.file.Path path;
    final boolean isDirectory;
    final Instant modified;
    final boolean isOpenable;

    ChooserEntry(java.nio.file.Path path) {
      this.path = path;
      isDirectory = Files.isDirectory(path);
      modified = uncheck(() -> Files.getLastModifiedTime(path).toInstant());
      isOpenable = pathIsOpenable(path);
    }
  }

  public abstract boolean pathIsOpenable(Path path);

  public abstract void concludeCancel();

  public abstract DialogInterface subdialog(String title);

  public FileChooser withNew(String label, Image icon, Consumer<Path> concludeNew) {
    this.concludeNew = concludeNew;
    builder.addAction(
        ButtonType.OK,
        true,
        b -> {
          ImageView graphic = new ImageView();
          graphic.setUserData(
              resolvedPath.addListener(
                  p -> {
                    if (Files.isDirectory(p)) {
                      graphic.setImage(icon("folder-open-outline.png"));
                      b.setDisable(false);
                      b.setText(localization.getString("enter"));
                      return;
                    }
                    graphic.setImage(icon);
                    b.setDisable(Files.exists(p));
                    b.setText(label);
                  }));
          b.setGraphic(graphic);
        },
        () -> {
          return defaultActSelection();
        });
    return this;
  }

  public FileChooser withOpen(String label, Image icon, Consumer<Path> concludeOpen) {
    this.concludeOpen = concludeOpen;
    builder.addAction(
        new ButtonType(label),
        true,
        b -> {
          ImageView graphic = new ImageView();
          graphic.setUserData(
              resolvedPath.addListener(
                  p -> {
                    if (!pathIsOpenable(p) && Files.isDirectory(p)) {
                      graphic.setImage(icon("folder-open-outline.png"));
                      b.setDisable(false);
                      b.setText(localization.getString("enter"));
                      return;
                    }
                    graphic.setImage(icon);
                    b.setDisable(!Files.exists(p) || !pathIsOpenable(p));
                    b.setText(label);
                  }));
          b.setGraphic(graphic);
        },
        () -> {
          return defaultActSelection();
        });
    return this;
  }

  private boolean defaultActSelection() {
    Path path = resolvedPath.get();
    if (!Files.exists(path)) {
      if (concludeNew != null) {
        concludeNew.accept(path);
        return true;
      }
    } else if (pathIsOpenable(path)) {
      if (concludeOpen != null) {
        concludeOpen.accept(path);
        return true;
      }
    } else {
      if (Files.isDirectory(path)) {
        cwd.set(path);
      }
    }
    return false;
  }

  public FileChooser(
      Path initialPath, String initialName, DialogInterface builder, String explanationText) {
    this.builder = builder;

    Label here = new Label();
    here.setTextFill(Color.GRAY);
    here.setTextOverrun(OverrunStyle.LEADING_WORD_ELLIPSIS);
    here.setMinWidth(200);
    here.setTextAlignment(TextAlignment.RIGHT);
    here.setPrefWidth(here.getMinWidth());
    HBox.setHgrow(here, Priority.ALWAYS);
    TextField text = new TextField();
    HBox hereLayout = new HBox();
    hereLayout.setAlignment(Pos.CENTER_RIGHT);
    hereLayout.setSpacing(4);
    hereLayout.setPadding(new Insets(4));
    hereLayout.getChildren().addAll(here, text);

    Button up = HelperJFX.button("arrow-up.png", localization.getString("leave.directory"));
    up.getStyleClass().add("pyx-flat");
    up.setOnAction(
        e -> {
          java.nio.file.Path parent = cwd.get().getParent();
          if (parent == null) return;
          cwd.set(parent);
        });
    Button refresh = HelperJFX.button("refresh.png", localization.getString("refresh"));
    refresh.getStyleClass().add("pyx-flat");
    refresh.setOnAction(
        e -> {
          refresh();
        });
    Region space = new Region();
    space.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(space, Priority.ALWAYS);
    Button createDirectory =
        HelperJFX.button("folder-plus.png", localization.getString("create.directory"));
    createDirectory.getStyleClass().add("fe-flat");
    createDirectory.setOnAction(
        e -> {
          Label explanation2 = new Label(localization.getString("enter.a.name.for.the.directory"));
          explanation2.setWrapText(true);
          TextField entry = new TextField();
          HBox.setHgrow(entry, Priority.ALWAYS);
          HBox horizontal = new HBox(new Label(localization.getString("directory.name")), entry);
          horizontal.setSpacing(2);
          horizontal.setAlignment(Pos.CENTER_LEFT);
          VBox vertical = new VBox(explanation2, horizontal);
          vertical.setSpacing(3);
          subdialog(localization.getString("new.folder"))
              .addContent(vertical)
              .addAction(
                  ButtonType.OK,
                  true,
                  noopConsumer,
                  () -> {
                    java.nio.file.Path newPath = cwd.get().resolve(entry.getText());
                    uncheck(() -> Files.createDirectory(newPath));
                    cwd.set(newPath);
                    return true;
                  })
              .addAction(ButtonType.CANCEL, false, noopConsumer, () -> true)
              .focus(entry)
              .go();
        });
    HBox toolbar = new HBox();
    toolbar.setSpacing(3);
    toolbar.setPadding(new Insets(4));
    toolbar.getChildren().addAll(up, refresh, space, createDirectory);

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
                        defaultActSelection();
                      }
                    });
              }

              @Override
              protected void updateItem(ChooserEntry entry, boolean b) {
                if (entry == null) {
                  setText("");
                  icon.setImage(null);
                  setDisable(false);
                  setTextFill(Color.BLACK);
                } else {
                  setText(entry.path.getFileName().toString());
                  if (entry.isDirectory) {
                    if (entry.isOpenable) icon.setImage(null);
                    else icon.setImage(directoryImage);
                    setDisable(false);
                    setTextFill(Color.BLACK);
                  } else {
                    icon.setImage(null);
                    if (entry.isOpenable) {
                      setDisable(false);
                      setTextFill(Color.BLACK);
                    } else {
                      setDisable(true);
                      setTextFill(Color.GRAY);
                    }
                  }
                }
                super.updateItem(entry, b);
              }
            };
          }
        });
    VBox listLayout = new VBox();
    listLayout.getChildren().addAll(toolbar, list);

    builder.addAction(
        ButtonType.CANCEL,
        false,
        noopConsumer,
        () -> {
          concludeCancel();
          return true;
        });

    setFillWidth(true);
    setSpacing(6);
    getChildren().addAll(hereLayout, listLayout);
    builder.addContent(this);

    if (explanationText != null) {
      Label explanation = new Label(explanationText);
      explanation.setPadding(new Insets(4));
      explanation.setWrapText(true);
      explanation.setMinWidth(50);
      getChildren().add(0, explanation);
    }

    addEventFilter(
        KeyEvent.KEY_PRESSED,
        e -> {
          if (e.getCode() == KeyCode.ENTER) {
            e.consume();
            defaultActSelection();
            return;
          }
          if (e.getCode() == KeyCode.ESCAPE) {
            e.consume();
            concludeCancel();
          }
        });

    cwdListener = cwd.addListener((_path) -> {
      text.setText("");
      entries.clear();
      refresh();
    });
    cwd.set(initialPath);
    this.rootChoice =
        CustomBinding.<java.nio.file.Path>bindBidirectional(
            new PropertyBinder<String>(text.textProperty())
                .<java.nio.file.Path>bimap(
                    t -> Optional.of(cwd.get().resolve(t)),
                    (java.nio.file.Path v) -> opt(v.getFileName().toString())),
            new SelectionModelBinder<>(list.getSelectionModel())
                .<java.nio.file.Path>bimap(
                    e -> Optional.ofNullable(e).map(v -> v.path),
                    (java.nio.file.Path v) -> opt(entries.get(v))));
    PropertyHalfBinder<String> textBinder = new PropertyHalfBinder<>(text.textProperty());
    resolvedPath = new DoubleHalfBinder<>(cwd, textBinder).map((c, t) -> opt(c.resolve(t)));

    here.textProperty().bind(cwd.map(p -> opt(p.toString() + "/")).bind(() -> ""));
    list.getSelectionModel().clearSelection();
    if (initialName == null) list.getSelectionModel().select(0);
    else text.setText(initialName);
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
                        .trueFirst(e -> !e.path.getFileName().toString().startsWith("."))
                        .greaterFirst(e -> e.isDirectory)
                        .greaterFirst(e -> e.modified)
                        .build())
                .collect(Collectors.toList()));
  }
}
