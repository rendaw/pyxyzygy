package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.automodel.lib.History;
import com.zarbosoft.javafxbinders.BinderRoot;
import com.zarbosoft.javafxbinders.CustomBinding;
import com.zarbosoft.javafxbinders.PropertyBinder;
import com.zarbosoft.pyxyzygy.app.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.app.widgets.binders.ScalarBinder;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupChildWrapper;
import com.zarbosoft.pyxyzygy.core.model.latest.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupChild;
import com.zarbosoft.pyxyzygy.core.model.latest.ProjectLayer;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zarbosoft.pyxyzygy.app.Global.localization;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.icon;
import static com.zarbosoft.rendaw.common.Common.opt;

public class Misc {
  public static void moveTo(List list, int source, int count, int dest) {
    if (list.get(0) instanceof Wrapper) throw new Assertion(); // DEBUG
    List temp0 = list.subList(source, source + count);
    List temp1 = new ArrayList(temp0);
    temp0.clear();
    list.addAll(dest, temp1);
  }

  public static void moveWrapperTo(List<Wrapper> list, int source, int count, int dest) {
    List temp0 = list.subList(source, source + count);
    List temp1 = new ArrayList(temp0);
    temp0.clear();
    list.addAll(dest, temp1);
    for (int i = Math.min(source, dest); i < list.size(); ++i) {
      list.get(i).setParentIndex(i);
    }
  }

  public static Runnable nodeFormFields(
      Context context, WidgetFormBuilder builder, Wrapper wrapper) {
    return new Runnable() {
      private BinderRoot enabledCleanup;
      private BinderRoot opacityCleanup;
      private BinderRoot nameCleanup;

      {
        ProjectLayer node = (ProjectLayer) wrapper.getValue();

        GroupChildWrapper groupChildWrapper;
        if (wrapper.getParent() == null) groupChildWrapper = null;
        else {
          groupChildWrapper = (GroupChildWrapper) wrapper.getParent();
        }

        builder.text(
            localization.getString("layer.name"),
            t -> {
              this.nameCleanup =
                  CustomBinding.bindBidirectional(
                      new ScalarBinder<>(
                          node::addNameSetListeners,
                          node::removeNameSetListeners,
                          v ->
                              context.change(
                                  new History.Tuple(wrapper, "name"),
                                  c -> c.projectLayer(node).nameSet(v))),
                      new PropertyBinder<>(t.textProperty()));
            });

        if (groupChildWrapper != null) {
          builder.check(
              localization.getString("enabled"),
              cb -> {
                enabledCleanup =
                    CustomBinding.bindBidirectional(
                        new ScalarBinder<Boolean>(
                            groupChildWrapper.node,
                            "enabled",
                            v ->
                                context.change(
                                    new History.Tuple(groupChildWrapper, "enabled"),
                                    c -> c.groupChild(groupChildWrapper.node).enabledSet(v))),
                        new PropertyBinder<>(cb.selectedProperty()));
              });
          builder.slider(
              localization.getString("opacity"),
              0,
              Global.opacityMax,
              slider -> {
                slider.setValue(groupChildWrapper.node.opacity());
                opacityCleanup =
                    CustomBinding.bindBidirectional(
                        new ScalarBinder<Integer>(
                            groupChildWrapper.node,
                            "opacity",
                            v ->
                                context.change(
                                    new History.Tuple(wrapper, "opacity"),
                                    c -> c.groupChild(groupChildWrapper.node).opacitySet(v))),
                        new PropertyBinder<>(slider.valueProperty())
                            .bimap(d -> Optional.of((int) (double) d), i -> opt((double) (int) i)));
              });
        }
      }

      @Override
      public void run() {
        nameCleanup.destroy();
        if (enabledCleanup != null) enabledCleanup.destroy();
        if (opacityCleanup != null) opacityCleanup.destroy();
      }
    };
  }

  public static void separate(Context context, ChangeStepBuilder c, Wrapper wrapper) {
    ProjectLayer replacement = wrapper.separateClone(context);
    int at = wrapper.parentIndex;
    if (wrapper.getParent() == null) {
      c.project(context.project).topAdd(at, replacement);
      c.project(context.project).topRemove(at + 1, 1);
    } else {
      GroupChild parent = (GroupChild) wrapper.getParent().getValue();
      c.groupChild(parent).innerSet(replacement);
    }
  }

  public static void separateFormField(
      Context context, WidgetFormBuilder builder, Wrapper wrapper) {
    builder.buttons(
        build ->
            build.button(
                b -> {
                  b.setText(localization.getString("layer"));
                  b.setGraphic(new ImageView(icon("link-off.png")));
                  b.setTooltip(new Tooltip("Make layer unique"));
                  b.setOnAction(e -> context.change(null, c -> separate(context, c, wrapper)));
                }));
  }

  public static <T, R> Runnable mirror(
      ObservableList<T> source,
      List<R> target,
      Function<T, R> add,
      Consumer<R> remove,
      Consumer<Integer> update) {
    return new Runnable() {
      private ListChangeListener<T> listener;
      private boolean dead = false;

      {
        listener =
            c -> {
              if (dead) return;
              while (c.next()) {
                if (c.wasAdded()) {
                  target.addAll(
                      c.getFrom(),
                      c.getAddedSubList().stream().map(add).collect(Collectors.toList()));
                } else if (c.wasRemoved()) {
                  List<R> removing = target.subList(c.getFrom(), c.getFrom() + c.getRemovedSize());
                  removing.forEach(remove);
                  removing.clear();
                } else if (c.wasPermutated()) {
                  throw new Assertion();
                } else if (c.wasUpdated()) {
                  throw new Assertion();
                }
                update.accept(c.getFrom());
              }
            };
        source.addListener(listener);
        target.addAll(source.stream().map(add).collect(Collectors.toList()));
        update.accept(0);
      }

      @Override
      public void run() {
        dead = true;
        source.removeListener(listener);
        target.forEach(remove);
      }
    };
  }
}
