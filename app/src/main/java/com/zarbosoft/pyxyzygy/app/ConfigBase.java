package com.zarbosoft.pyxyzygy.app;

import com.google.common.collect.Lists;
import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.luxem.Luxem;
import com.zarbosoft.luxem.read.ReadTypeGrammar;
import com.zarbosoft.luxem.read.TreeReader;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

import static com.zarbosoft.automodel.lib.Logger.logger;
import static com.zarbosoft.rendaw.common.Common.atomicWrite;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public class ConfigBase {
  public Path path;
  public final Timer configFlushTimer;
  public static final ScanResult scan =
      new ClassGraph()
          .enableSystemJarsAndModules()
          .enableAllInfo()
          .whitelistPackages("com.zarbosoft.pyxyzygy.app")
          .scan();

  public ConfigBase() {
    configFlushTimer = new Timer(name(), true);
    Global.shutdown.add(
        () -> {
          flushConfig();
          configFlushTimer.cancel();
        });
  }

  private String name() {
    return String.format("config-manager-%s", getClass().getTypeName());
  }

  protected void start() {
    configFlushTimer.scheduleAtFixedRate(
        new TimerTask() {
          @Override
          public void run() {
            Platform.runLater(
                () -> {
                  try {
                    flushConfig();
                  } catch (Exception e) {
                    logger.writeException(e, "Error flushing config %s", name());
                  }
                });
          }
        },
        Duration.ofMinutes(5).toMillis(),
        Duration.ofMinutes(5).toMillis());
  }

  public static <T extends ConfigBase> T deserialize(
      TypeInfo configType, Path configPath, Supplier<T> create) {
    T out =
        uncheck(
            () -> {
              Files.createDirectories(configPath.getParent());
              List tree;
              try (InputStream source = Files.newInputStream(configPath)) {
                tree = new TreeReader().read(source);
              } catch (NoSuchFileException e) {
                return create.get();
              }

              // Migrations
              Map root = (Map) tree.get(0);
              root.remove("newProjectNormalMode");

              // Parse from tree
              return (T)
                  Luxem.parse(scan, configType).map(configTypeMap).fromTree(tree).findFirst().get();
            });
    out.start();
    out.path = configPath;
    return out;
  }

  public static ReadTypeGrammar.TypeMap configTypeMap =
      new ReadTypeGrammar.TypeMap()
          .add(
              new TypeInfo(DoubleVector.class),
              new ReadTypeGrammar.TypeMapEntry<DoubleVector>() {
                @Override
                public DoubleVector convertIn(Object read) {
                  ArrayList<Double> got = (ArrayList<Double>) read;
                  return new DoubleVector(got.get(0), got.get(1));
                }

                @Override
                public Object convertOut(DoubleVector source) {
                  return Lists.newArrayList(source.x, source.y);
                }

                @Override
                public TypeInfo serializedType(TypeInfo info) {
                  return new TypeInfo(
                      info.field, ArrayList.class, new TypeInfo[] {new TypeInfo(Double.class)});
                }
              })
          .add(
              new TypeInfo(SimpleIntegerProperty.class),
              new ReadTypeGrammar.TypeMapEntry<SimpleIntegerProperty>() {
                @Override
                public void setIn(Object object, TypeInfo field, Object value) {
                  uncheck(
                      () -> ((SimpleIntegerProperty) field.field.get(object)).set((Integer) value));
                }

                @Override
                public Object convertOut(SimpleIntegerProperty source) {
                  return source.get();
                }

                @Override
                public TypeInfo serializedType(TypeInfo info) {
                  return new TypeInfo(info.field, int.class);
                }
              })
          .add(
              new TypeInfo(SimpleLongProperty.class),
              new ReadTypeGrammar.TypeMapEntry<SimpleLongProperty>() {
                @Override
                public void setIn(Object object, TypeInfo field, Object value) {
                  uncheck(() -> ((SimpleLongProperty) field.field.get(object)).set((Long) value));
                }

                @Override
                public Object convertOut(SimpleLongProperty source) {
                  return source.get();
                }

                @Override
                public TypeInfo serializedType(TypeInfo info) {
                  return new TypeInfo(info.field, long.class);
                }
              })
          .add(
              new TypeInfo(SimpleStringProperty.class),
              new ReadTypeGrammar.TypeMapEntry<SimpleStringProperty>() {
                @Override
                public void setIn(Object object, TypeInfo field, Object value) {
                  uncheck(
                      () -> ((SimpleStringProperty) field.field.get(object)).set((String) value));
                }

                @Override
                public Object convertOut(SimpleStringProperty source) {
                  return source.get();
                }

                @Override
                public TypeInfo serializedType(TypeInfo info) {
                  return new TypeInfo(info.field, String.class);
                }
              })
          .add(
              new TypeInfo(SimpleBooleanProperty.class),
              new ReadTypeGrammar.TypeMapEntry<SimpleBooleanProperty>() {
                @Override
                public void setIn(Object object, TypeInfo field, Object value) {
                  uncheck(
                      () -> ((SimpleBooleanProperty) field.field.get(object)).set((Boolean) value));
                }

                @Override
                public Object convertOut(SimpleBooleanProperty source) {
                  return source.get();
                }

                @Override
                public TypeInfo serializedType(TypeInfo info) {
                  return new TypeInfo(info.field, Boolean.class);
                }
              })
          .add(
              new TypeInfo(SimpleObjectProperty.class),
              new ReadTypeGrammar.TypeMapEntry<SimpleObjectProperty>() {
                @Override
                public void setIn(Object object, TypeInfo field, Object value) {
                  uncheck(() -> ((SimpleObjectProperty) field.field.get(object)).set(value));
                }

                @Override
                public Object convertOut(SimpleObjectProperty source) {
                  return source.get();
                }

                @Override
                public TypeInfo serializedType(TypeInfo info) {
                  return new TypeInfo(
                      info.field, info.parameters[0].type, info.parameters[0].parameters);
                }
              })
          .add(
              new TypeInfo(ObservableList.class),
              new ReadTypeGrammar.TypeMapEntry() {
                @Override
                public void setIn(Object object, TypeInfo field, Object value) {
                  uncheck(
                      () -> ((ObservableList) field.field.get(object)).addAll((Collection) value));
                }

                @Override
                public TypeInfo serializedType(TypeInfo info) {
                  return new TypeInfo(info.field, ArrayList.class, info.parameters);
                }
              })
          .add(
              new TypeInfo(ObservableMap.class),
              new ReadTypeGrammar.TypeMapEntry() {
                @Override
                public void setIn(Object object, TypeInfo field, Object value) {
                  uncheck(() -> ((ObservableMap) field.field.get(object)).putAll((Map) value));
                }

                @Override
                public TypeInfo serializedType(TypeInfo info) {
                  return new TypeInfo(info.field, HashMap.class, info.parameters);
                }
              });

  public void flushConfig() {
    atomicWrite(
        path,
        stream -> {
          Luxem.write(this).map(configTypeMap).pretty().toStream(stream);
        });
  }
}
