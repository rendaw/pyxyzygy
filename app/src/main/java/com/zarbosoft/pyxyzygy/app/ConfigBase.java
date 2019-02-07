package com.zarbosoft.pyxyzygy.app;

import com.google.common.collect.Lists;
import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.luxem.Luxem;
import com.zarbosoft.luxem.read.ReadTypeGrammar;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

import static com.zarbosoft.rendaw.common.Common.atomicWrite;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public class ConfigBase {
	public Path path;
	public final Timer configFlushTimer;

	public ConfigBase() {
		configFlushTimer = new Timer(name(), true);
	}

	private String name() {
		return String.format("config-manager-%s", getClass().getTypeName());
	}

	protected void start() {
		configFlushTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				Platform.runLater(() -> {
					try {
						flushConfig();
					} catch (Exception e) {
						System.out.format("Error flushing config");
						e.printStackTrace();
						System.out.flush();
						System.err.flush();
					}
				});
			}
		}, Duration.ofMinutes(5).toMillis(), Duration.ofMinutes(5).toMillis());
	}

	public static <T extends ConfigBase> T deserialize(TypeInfo configType, Path configDir, Supplier<T> create) {
		Path configPath = configDir.resolve("config.luxem");
		T out = uncheck(() -> {
			Files.createDirectories(configDir);
			try (InputStream source = Files.newInputStream(configPath)) {
				return (T) Luxem.parse(null, configType).map(configTypeMap).from(source).findFirst().get();
			} catch (NoSuchFileException e) {
				return create.get();
			}
		});
		out.start();
		out.path = configPath;
		return out;
	}

	public static ReadTypeGrammar.TypeMap configTypeMap = new ReadTypeGrammar.TypeMap()
			.add(new TypeInfo(DoubleVector.class), new ReadTypeGrammar.TypeMapEntry<DoubleVector>() {
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
					return new TypeInfo(info.field, ArrayList.class, new TypeInfo[] {new TypeInfo(Double.class)});
				}
			})
			.add(new TypeInfo(SimpleIntegerProperty.class), new ReadTypeGrammar.TypeMapEntry<SimpleIntegerProperty>() {
				@Override
				public void setIn(Object object, TypeInfo field, Object value) {
					uncheck(() -> ((SimpleIntegerProperty) field.field.get(object)).set((Integer) value));
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
			.add(new TypeInfo(SimpleStringProperty.class), new ReadTypeGrammar.TypeMapEntry<SimpleStringProperty>() {
				@Override
				public void setIn(Object object, TypeInfo field, Object value) {
					uncheck(() -> ((SimpleStringProperty) field.field.get(object)).set((String) value));
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
			.add(new TypeInfo(SimpleBooleanProperty.class), new ReadTypeGrammar.TypeMapEntry<SimpleBooleanProperty>() {
				@Override
				public void setIn(Object object, TypeInfo field, Object value) {
					uncheck(() -> ((SimpleBooleanProperty) field.field.get(object)).set((Boolean) value));
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
			.add(new TypeInfo(SimpleObjectProperty.class), new ReadTypeGrammar.TypeMapEntry<SimpleObjectProperty>() {
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
					return new TypeInfo(info.field, info.parameters[0].type, info.parameters[0].parameters);
				}
			})
			.add(new TypeInfo(ObservableList.class), new ReadTypeGrammar.TypeMapEntry() {
				@Override
				public void setIn(Object object, TypeInfo field, Object value) {
					uncheck(() -> ((ObservableList) field.field.get(object)).addAll((Collection) value));
				}

				@Override
				public TypeInfo serializedType(TypeInfo info) {
					return new TypeInfo(info.field, ArrayList.class, info.parameters);
				}
			})
			.add(new TypeInfo(ObservableMap.class), new ReadTypeGrammar.TypeMapEntry() {
				@Override
				public void setIn(Object object, TypeInfo field, Object value) {
					uncheck(() -> ((ObservableMap) field.field.get(object)).putAll((Map) value));
				}

				@Override
				public TypeInfo serializedType(TypeInfo info) {
					return new TypeInfo(info.field, HashMap.class, info.parameters);
				}
			});

	public void shutdown() {
		flushConfig();
		configFlushTimer.cancel();
	}

	public void flushConfig() {
		System.out.format("flushing config [%s]\n", name());
		atomicWrite(path, stream -> {
			Luxem.write(this).map(configTypeMap).pretty().toStream(stream);
		});
		System.out.format("flushing config DONE\n");
	}
}
