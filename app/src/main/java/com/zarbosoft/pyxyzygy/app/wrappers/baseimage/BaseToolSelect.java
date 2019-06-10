package com.zarbosoft.pyxyzygy.app.wrappers.baseimage;

import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.core.model.v0.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectObject;
import com.zarbosoft.pyxyzygy.nearestneighborimageview.NearestNeighborImageView;
import com.zarbosoft.pyxyzygy.seed.model.v0.Rectangle;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import com.zarbosoft.rendaw.common.Pair;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableNumberValue;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.shape.StrokeType;

import static com.zarbosoft.pyxyzygy.app.Global.copyHotkey;
import static com.zarbosoft.pyxyzygy.app.Global.cutHotkey;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.icon;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.pad;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public abstract class BaseToolSelect<F extends ProjectObject, L> extends Tool {
  final int handleSize = 25;
  final int handlePad = 4;
  final SimpleIntegerProperty zoom;
  private final BaseImageNodeWrapper<?, F, ?, L> wrapper;

  abstract class State {
    public abstract void markStart(ProjectContext context, Window window, DoubleVector start);

    public abstract void mark(ProjectContext context, DoubleVector start, DoubleVector end);

    public abstract void remove(ProjectContext context);
  }

  abstract class Interactive {
    public abstract void mark(ProjectContext context, DoubleVector start, DoubleVector end);
  }

  class SelectRect extends javafx.scene.shape.Rectangle {
    {
      setStrokeType(StrokeType.OUTSIDE);
      setStroke(Color.GRAY);
      setFill(Color.TRANSPARENT);
      setVisible(false);
      setBlendMode(BlendMode.DIFFERENCE);
    }

    SelectRect(ObservableNumberValue zoom) {
      zoom.addListener((observable, oldValue, newValue) -> onZoom(newValue.doubleValue()));
      onZoom(zoom.getValue().doubleValue());
    }

    public void onZoom(double zoom) {
      setStrokeWidth(1.0 / zoom);
    }
  }

  class SelectInside extends SelectRect {
    SelectInside(ObservableNumberValue zoom) {
      super(zoom);
    }

    @Override
    public void onZoom(double zoom) {
      super.onZoom(zoom);
      double v = 5.0 / zoom;
      SelectInside.this.getStrokeDashArray().setAll(v, v);
    }
  }

  class SelectHandle extends SelectRect {
    {
      setStroke(Color.LIGHTGRAY);
      setOpacity(0.5);
    }

    SelectHandle(ObservableNumberValue zoom) {
      super(zoom);
    }

    @Override
    public void onZoom(double zoom) {
      super.onZoom(zoom);
      double v = 3.0 / zoom;
      setArcHeight(v);
      setArcWidth(v);
    }
  }

  class SelectVHandle extends SelectHandle {
    SelectVHandle(ObservableNumberValue zoom) {
      super(zoom);
    }

    @Override
    public void onZoom(double zoom) {
      super.onZoom(zoom);
      setHeight(handleSize / zoom);
    }
  }

  class SelectHHandle extends SelectHandle {
    SelectHHandle(ObservableNumberValue zoom) {
      super(zoom);
    }

    @Override
    public void onZoom(double zoom) {
      super.onZoom(zoom);
      setWidth(handleSize / zoom);
    }
  }

  class StateMove extends State {
    final SelectInside originalRectangle;
    final SelectInside rectangle;
    final Group imageGroup = new Group();
    final L lifted;
    private final Rectangle originalBounds;
    private Rectangle bounds;
    private DoubleVector start;
    private Vector startCorner;
    Hotkeys.Action[] actions =
        new Hotkeys.Action[] {
          new Hotkeys.Action(
              Hotkeys.Scope.CANVAS,
              "cancel",
              "Cancel",
              Hotkeys.Hotkey.create(KeyCode.ESCAPE, false, false, false)) {
            @Override
            public void run(ProjectContext context, Window window) {
              setState(context, new StateCreate(context, window));
            }
          },
          new Hotkeys.Action(
              Hotkeys.Scope.CANVAS,
              "place",
              "Place",
              Hotkeys.Hotkey.create(KeyCode.ENTER, false, false, true)) {
            @Override
            public void run(ProjectContext context, Window window) {
              context.change(
                  null,
                  c -> {
                    place(context, c, window);
                  });
            }
          },
          new Hotkeys.Action(Hotkeys.Scope.CANVAS, "cut", "Cut", cutHotkey) {
            @Override
            public void run(ProjectContext context, Window window) {
              context.change(
                  null,
                  c -> {
                    cut(context, c, window);
                  });
            }
          },
          new Hotkeys.Action(Hotkeys.Scope.CANVAS, "copy", "copy", copyHotkey) {
            @Override
            public void run(ProjectContext context, Window window) {
              copy();
            }
          },
        };

    StateMove(
        ProjectContext context,
        Window window,
        Rectangle originalBounds,
        Rectangle bounds,
        L lifted) {
      this.originalBounds = originalBounds;
      this.bounds = bounds;

      for (Hotkeys.Action action : actions) context.hotkeys.register(action);

      if (originalBounds != null) {
        originalRectangle = new SelectInside(zoom);
        originalRectangle.setWidth(originalBounds.width);
        originalRectangle.setHeight(originalBounds.height);
        originalRectangle.setLayoutX(originalBounds.x);
        originalRectangle.setLayoutY(originalBounds.y);
        originalRectangle.setVisible(true);
        overlayAdd(originalRectangle);
      } else {
        originalRectangle = null;
      }
      rectangle = new SelectInside(zoom);
      rectangle.setStroke(Color.WHITE);
      rectangle.setVisible(true);
      rectangle.setWidth(bounds.width);
      rectangle.setHeight(bounds.height);
      this.lifted = lifted;

      ImageView image = NearestNeighborImageView.create();
      image.imageProperty().bind(Bindings.createObjectBinding(() -> toImage(lifted), zoom));
      imageGroup.getChildren().add(image);
      overlayAdd(rectangle, imageGroup);
      setPosition(bounds.corner());

      propertiesSet(
          pad(
              new WidgetFormBuilder()
                  .buttons(
                      u ->
                          u.button(
                                  b -> {
                                    b.setText("Place");
                                    b.setGraphic(new ImageView(icon("arrow-collapse-down16.png")));
                                    b.setOnAction(
                                        e -> {
                                          context.change(
                                              null,
                                              c -> {
                                                place(context, c, window);
                                              });
                                        });
                                  })
                              .button(
                                  b -> {
                                    b.setText("Clear");
                                    b.setGraphic(new ImageView(icon("eraser-variant.png")));
                                    b.setOnAction(
                                        e -> {
                                          context.change(
                                              null,
                                              c -> {
                                                clear(context, c, originalBounds);
                                              });
                                          setState(context, new StateCreate(context, window));
                                        });
                                  })
                              .button(
                                  b -> {
                                    b.setText("Cut");
                                    b.setGraphic(new ImageView(icon("content-cut.png")));
                                    b.setOnAction(
                                        e ->
                                            uncheck(
                                                () -> {
                                                  context.change(
                                                      null,
                                                      c -> {
                                                        cut(context, c, window);
                                                      });
                                                }));
                                  })
                              .button(
                                  b -> {
                                    b.setText("Copy");
                                    b.setGraphic(new ImageView(icon("content-copy.png")));
                                    b.setOnAction(
                                        e -> {
                                          copy();
                                        });
                                  }))
                  .separator()
                  .button(
                      b -> {
                        b.setText("Cancel");
                        b.setOnAction(
                            e -> {
                              setState(context, new StateCreate(context, window));
                            });
                      })
                  .build()));
    }

    private void copy() {
      ClipboardContent content = new ClipboardContent();
      BaseToolSelect.this.copy(content, lifted);
      Clipboard.getSystemClipboard().setContent(content);
    }

    private void cut(ProjectContext context, ChangeStepBuilder change, Window window) {
      copy();
      if (originalBounds != null) clear(context, change, originalBounds);
      setState(context, new StateCreate(context, window));
    }

    private void place(ProjectContext context, ChangeStepBuilder change, Window window) {
      if (originalBounds != null) clear(context, change, originalBounds);
      wrapper.modify(
          context,
          change,
          bounds,
          (image, corner) -> {
            wrapper.imageCompose(image, lifted, bounds.x - corner.x, bounds.y - corner.y);
          });
      setState(context, new StateCreate(context, window));
    }

    @Override
    public void markStart(ProjectContext context, Window window, DoubleVector start) {
      if (!bounds.contains(start.toInt())) {
        setState(context, new StateCreate(context, window));
        state.markStart(context, window, start);
        return;
      }
      this.start = start;
      this.startCorner = bounds.corner();
    }

    @Override
    public void mark(ProjectContext context, DoubleVector __, DoubleVector end) {
      setPosition(startCorner.plus(end.minus(start).toInt()));
    }

    void setPosition(Vector vector) {
      bounds = vector.toRect(bounds.width, bounds.height);
      rectangle.setLayoutX(bounds.x);
      rectangle.setLayoutY(bounds.y);
      imageGroup.setLayoutX(bounds.x);
      imageGroup.setLayoutY(bounds.y);
    }

    @Override
    public void remove(ProjectContext context) {
      overlayRemove(originalRectangle, rectangle, imageGroup);
      for (Hotkeys.Action action : actions) context.hotkeys.unregister(action);
    }
  }

  protected abstract void copy(ClipboardContent content, L lifted);

  protected abstract Image toImage(L buffer);

  protected abstract void propertiesSet(Node node);

  protected abstract void propertiesClear();

  protected abstract void overlayAdd(Node... nodes);

  protected abstract void overlayRemove(Node... nodes);

  public abstract void clear(ProjectContext context, ChangeStepBuilder change, Rectangle bounds);

  public class StateCreate extends State {
    final SelectInside rectangle;
    final SelectHHandle left;
    final SelectHHandle right;
    final SelectVHandle top;
    final SelectVHandle bottom;
    SimpleObjectProperty<Rectangle> inside = new SimpleObjectProperty<>(new Rectangle(0, 0, 0, 0));

    Interactive mark;

    Hotkeys.Action[] actions =
        new Hotkeys.Action[] {
          new Hotkeys.Action(
              Hotkeys.Scope.CANVAS,
              "cancel",
              "Cancel",
              Hotkeys.Hotkey.create(KeyCode.ESCAPE, false, false, false)) {
            @Override
            public void run(ProjectContext context, Window window) {
              cancel(context, window);
            }
          },
          new Hotkeys.Action(
              Hotkeys.Scope.CANVAS,
              "lift",
              "Lift",
              Hotkeys.Hotkey.create(KeyCode.ENTER, false, false, false)) {
            @Override
            public void run(ProjectContext context, Window window) {
              lift(context, window);
            }
          },
          new Hotkeys.Action(Hotkeys.Scope.CANVAS, "cut", "Cut", cutHotkey) {
            @Override
            public void run(ProjectContext context, Window window) {
              context.change(
                  null,
                  c -> {
                    cut(context, c, window);
                  });
            }
          },
          new Hotkeys.Action(Hotkeys.Scope.CANVAS, "copy", "Copy", copyHotkey) {
            @Override
            public void run(ProjectContext context, Window window) {
              copy(context);
            }
          },
        };

    class LeftHandle extends Interactive {
      final DoubleVector start;
      final Rectangle startInside;

      LeftHandle(DoubleVector start, Rectangle startInside) {
        this.start = start;
        this.startInside = startInside;
      }

      @Override
      public void mark(ProjectContext context, DoubleVector __, DoubleVector end) {
        int diffX = (int) (end.x - start.x);
        int newWidth = startInside.width - diffX;
        if (newWidth < 0)
          setInside(
              new Rectangle(
                  startInside.x + startInside.width, startInside.y, -newWidth, startInside.height));
        else
          setInside(
              new Rectangle(startInside.x + diffX, startInside.y, newWidth, startInside.height));
      }
    }

    class RightHandle extends Interactive {
      final DoubleVector start;
      final Rectangle startInside;

      RightHandle(DoubleVector start, Rectangle startInside) {
        this.start = start;
        this.startInside = startInside;
      }

      @Override
      public void mark(ProjectContext context, DoubleVector __, DoubleVector end) {
        int diffX = (int) (end.x - start.x);
        int newWidth = startInside.width + diffX;
        if (newWidth < 0)
          setInside(
              new Rectangle(
                  startInside.x + newWidth, startInside.y, -newWidth, startInside.height));
        else setInside(new Rectangle(startInside.x, startInside.y, newWidth, startInside.height));
      }
    }

    class TopHandle extends Interactive {
      final DoubleVector start;
      final Rectangle startInside;

      TopHandle(DoubleVector start, Rectangle startInside) {
        this.start = start;
        this.startInside = startInside;
      }

      @Override
      public void mark(ProjectContext context, DoubleVector __, DoubleVector end) {
        int diff = (int) (end.y - start.y);
        int newHeight = startInside.height - diff;
        if (newHeight < 0)
          setInside(
              new Rectangle(
                  startInside.x,
                  startInside.y + startInside.height,
                  startInside.width,
                  -newHeight));
        else
          setInside(
              new Rectangle(startInside.x, startInside.y + diff, startInside.width, newHeight));
      }
    }

    class BottomHandle extends Interactive {
      final DoubleVector start;
      final Rectangle startInside;

      BottomHandle(DoubleVector start, Rectangle startInside) {
        this.start = start;
        this.startInside = startInside;
      }

      @Override
      public void mark(ProjectContext context, DoubleVector __, DoubleVector end) {
        int diff = (int) (end.y - start.y);
        int newHeight = startInside.height + diff;
        if (newHeight < 0)
          setInside(
              new Rectangle(
                  startInside.x, startInside.y + newHeight, startInside.width, -newHeight));
        else setInside(new Rectangle(startInside.x, startInside.y, startInside.width, newHeight));
      }
    }

    class FromScratchHandle extends Interactive {
      final DoubleVector start;

      FromScratchHandle(DoubleVector start) {
        this.start = start;
      }

      @Override
      public void mark(ProjectContext context, DoubleVector __, DoubleVector end) {
        setInside(new BoundsBuilder().circle(start, 0).circle(end, 0).buildInt());
      }
    }

    class MoveHandle extends Interactive {
      final DoubleVector start;
      final Rectangle startInside;

      MoveHandle(DoubleVector start, Rectangle startInside) {
        this.start = start;
        this.startInside = startInside;
      }

      @Override
      public void mark(ProjectContext context, DoubleVector __, DoubleVector end) {
        setInside(startInside.shift(end.minus(start).toInt()));
      }
    }

    public StateCreate(ProjectContext context, Window window) {
      for (Hotkeys.Action action : actions) context.hotkeys.register(action);

      ObservableNumberValue zoom = window.editor.zoomFactor;
      rectangle = new SelectInside(zoom);
      left = new SelectHHandle(zoom);
      left.layoutXProperty()
          .bind(
              Bindings.createDoubleBinding(
                  () -> inside.get().x - (handleSize + handlePad) / zoom.doubleValue(),
                  zoom,
                  inside));
      right = new SelectHHandle(zoom);
      right
          .layoutXProperty()
          .bind(
              Bindings.createDoubleBinding(
                  () -> inside.get().x + inside.get().width + handlePad / zoom.doubleValue(),
                  zoom,
                  inside));
      top = new SelectVHandle(zoom);
      top.layoutYProperty()
          .bind(
              Bindings.createDoubleBinding(
                  () -> inside.get().y - (handleSize + handlePad) / zoom.doubleValue(),
                  inside,
                  zoom));
      bottom = new SelectVHandle(zoom);
      bottom
          .layoutYProperty()
          .bind(
              Bindings.createDoubleBinding(
                  () -> inside.get().y + inside.get().height + handlePad / zoom.doubleValue(),
                  inside,
                  zoom));
      final IntegerBinding insideX = Bindings.createIntegerBinding(() -> inside.get().x, inside);
      final IntegerBinding insideY = Bindings.createIntegerBinding(() -> inside.get().y, inside);
      final IntegerBinding insideWidth =
          Bindings.createIntegerBinding(() -> inside.get().width, inside);
      final IntegerBinding insideHeight =
          Bindings.createIntegerBinding(() -> inside.get().height, inside);
      rectangle.layoutXProperty().bind(insideX);
      rectangle.layoutYProperty().bind(insideY);
      rectangle.widthProperty().bind(insideWidth);
      rectangle.heightProperty().bind(insideHeight);
      left.layoutYProperty().bind(insideY);
      left.heightProperty().bind(insideHeight);
      right.layoutYProperty().bind(insideY);
      right.heightProperty().bind(insideHeight);
      top.layoutXProperty().bind(insideX);
      top.widthProperty().bind(insideWidth);
      bottom.layoutXProperty().bind(insideX);
      bottom.widthProperty().bind(insideWidth);
      BooleanBinding visible =
          Bindings.createBooleanBinding(
              () -> inside.get().width > 0 && inside.get().height > 0, inside);
      rectangle.visibleProperty().bind(visible);
      left.visibleProperty().bind(visible);
      right.visibleProperty().bind(visible);
      top.visibleProperty().bind(visible);
      bottom.visibleProperty().bind(visible);

      overlayAdd(rectangle, left, right, top, bottom);

      propertiesSet(
          pad(
              new WidgetFormBuilder()
                  .buttons(
                      g ->
                          g.button(
                                  b -> {
                                    b.setText("Lift");
                                    b.setGraphic(new ImageView(icon("arrow-expand-up.png")));
                                    b.setOnAction(
                                        e -> {
                                          lift(context, window);
                                        });
                                  })
                              .button(
                                  b -> {
                                    b.setText("Clear");
                                    b.setGraphic(new ImageView(icon("eraser-variant.png")));
                                    b.setOnAction(
                                        e -> {
                                          context.change(
                                              null,
                                              c -> {
                                                clear(context, c, window);
                                              });
                                        });
                                  }))
                  .buttons(
                      g ->
                          g.button(
                                  b -> {
                                    b.setText("Cut");
                                    b.setGraphic(new ImageView(icon("content-cut.png")));
                                    b.setOnAction(
                                        e ->
                                            uncheck(
                                                () -> {
                                                  context.change(
                                                      null,
                                                      c -> {
                                                        cut(context, c, window);
                                                      });
                                                }));
                                  })
                              .button(
                                  b -> {
                                    b.setText("Copy");
                                    b.setGraphic(new ImageView(icon("content-copy.png")));
                                    b.setOnAction(
                                        e -> {
                                          copy(context);
                                        });
                                  }))
                  .separator()
                  .button(
                      b -> {
                        b.setText("Cancel");
                        b.setOnAction(
                            e -> {
                              cancel(context, window);
                            });
                      })
                  .build()));
    }

    private void cancel(ProjectContext context, Window window) {
      setState(context, new StateCreate(context, window));
    }

    private void cut(ProjectContext context, ChangeStepBuilder change, Window window) {
      if (inside.get().empty()) return;
      copy(context);
      clear(context, change, window);
    }

    private void copy(ProjectContext context) {
      if (inside.get().empty()) return;
      ClipboardContent content = new ClipboardContent();
      BaseToolSelect.this.copy(content, wrapper.grab(context, inside.get()));
      Clipboard.getSystemClipboard().setContent(content);
    }

    private void clear(ProjectContext context, ChangeStepBuilder change, Window window) {
      if (inside.get().empty()) return;
      BaseToolSelect.this.clear(context, change, inside.get());
      cancel(context, window);
    }

    private void lift(ProjectContext context, Window window) {
      if (inside.get().empty()) return;
      setState(
          context,
          new StateMove(
              context, window, inside.get(), inside.get(), wrapper.grab(context, inside.get())));
    }

    @Override
    public void markStart(ProjectContext context, Window window, DoubleVector start) {
      if (inside.get().contains(start.toInt())) {
        mark = new MoveHandle(start, inside.get());
      } else if (left.getBoundsInParent().contains(start.toJfx())) {
        mark = new LeftHandle(start, inside.get());
      } else if (right.getBoundsInParent().contains(start.toJfx())) {
        mark = new RightHandle(start, inside.get());
      } else if (top.getBoundsInParent().contains(start.toJfx())) {
        mark = new TopHandle(start, inside.get());
      } else if (bottom.getBoundsInParent().contains(start.toJfx())) {
        mark = new BottomHandle(start, inside.get());
      } else {
        setInside(new Rectangle(0, 0, 0, 0));
        mark = new FromScratchHandle(start);
      }
    }

    private void setInside(Rectangle rectangle1) {
      inside.set(rectangle1);
    }

    @Override
    public void mark(ProjectContext context, DoubleVector start, DoubleVector end) {
      mark.mark(context, start, end);
    }

    @Override
    public void remove(ProjectContext context) {
      overlayRemove(rectangle, left, right, top, bottom);
      for (Hotkeys.Action action : actions) context.hotkeys.unregister(action);
    }
  }

  State state;

  public BaseToolSelect(BaseImageNodeWrapper<?, F, ?, L> wrapper, SimpleIntegerProperty zoom) {
    this.wrapper = wrapper;
    this.zoom = zoom;
  }

  public void setState(ProjectContext context, State newState) {
    if (state != null) state.remove(context);
    state = newState;
  }

  @Override
  public void markStart(
      ProjectContext context, Window window, DoubleVector start, DoubleVector globalStart) {
    state.markStart(context, window, start);
  }

  @Override
  public void mark(
      ProjectContext context,
      Window window,
      DoubleVector start,
      DoubleVector end,
      DoubleVector globalStart,
      DoubleVector globalEnd) {
    state.mark(context, start, end);
  }

  @Override
  public void remove(ProjectContext context, Window window) {
    state.remove(context);
    propertiesClear();
  }

  public void paste(ProjectContext context, Window window) {
    Pair<L, Vector> image0 = uncopy(context);
    if (image0 == null) return;
    Vector size = image0.second;
    if (size.equals(Vector.ZERO)) return;
    L image = image0.first;
    Rectangle placeAt = new Rectangle(0, 0, size.x, size.y);
    placeAt = placeAt.unshift(placeAt.span().divide(2));
    placeAt = placeAt.unshift(negViewCenter(window));
    setState(context, new StateMove(context, window, null, placeAt, image));
  }

  protected abstract Vector negViewCenter(Window window);

  protected abstract Pair<L, Vector> uncopy(ProjectContext context);

  @Override
  public void cursorMoved(ProjectContext context, Window window, DoubleVector position) {}
}
