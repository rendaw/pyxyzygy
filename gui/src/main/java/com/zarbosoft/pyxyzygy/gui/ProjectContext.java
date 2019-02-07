package com.zarbosoft.pyxyzygy.gui;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.luxem.write.RawWriter;
import com.zarbosoft.pyxyzygy.core.model.*;
import com.zarbosoft.pyxyzygy.gui.config.ProjectConfig;
import com.zarbosoft.pyxyzygy.gui.config.TrueColor;
import com.zarbosoft.pyxyzygy.seed.deserialize.ModelDeserializationContext;
import com.zarbosoft.pyxyzygy.seed.model.Dirtyable;
import com.zarbosoft.pyxyzygy.seed.model.ProjectContextBase;
import com.zarbosoft.pyxyzygy.seed.model.ProjectObjectInterface;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.DeadCode;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.zarbosoft.rendaw.common.Common.atomicWrite;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public class ProjectContext extends ProjectContextBase implements Dirtyable {
	public static Map<String, Image> iconCache = new HashMap<>();
	public static Map<String, Integer> names = new HashMap<>();
	public Project project;
	public ProjectConfig config;
	public int tileSize;

	public History history;
	public Hotkeys hotkeys;

	/**
	 * Start at count 1 (unwritten) for machine generated names
	 *
	 * @param name
	 * @return
	 */
	public static String uniqueName(String name) {
		int count = names.compute(name, (n, i) -> i == null ? 1 : i + 1);
		return count == 1 ? name : String.format("%s (%s)", name, count);
	}

	/**
	 * Start at count 2 if the first element is not in the map (user decided name)
	 *
	 * @param name
	 * @return
	 */
	public static String uniqueName1(String name) {
		int count = names.compute(name, (n, i) -> i == null ? 2 : i + 1);
		return count == 1 ? name : String.format("%s (%s)", name, count);
	}

	public void debugCheckRefCounts() {
		Map<Long, Long> counts = new HashMap<>();
		Consumer<ProjectObjectInterface> incCount =
				o1 -> counts.compute(o1.id(), (i, count) -> count == null ? 1 : count + 1);
		objectMap.values().forEach(o -> {
			if (false) {
				throw new Assertion();
			} else if (o instanceof Project) {
				((Project) o).top().forEach(incCount);
			} else if (o instanceof GroupLayer) {
				if (((GroupLayer) o).inner() != null) {
					incCount.accept(((GroupLayer) o).inner());
				}
				((GroupLayer) o).timeFrames().forEach(incCount);
				((GroupLayer) o).positionFrames().forEach(incCount);
			} else if (o instanceof GroupNode) {
				((GroupNode) o).layers().forEach(incCount);
			} else if (o instanceof GroupPositionFrame) {
			} else if (o instanceof GroupTimeFrame) {
			} else if (o instanceof TrueColorImageFrame) {
				((TrueColorImageFrame) o).tiles().values().forEach(incCount);
			} else if (o instanceof TrueColorImageNode) {
				((TrueColorImageNode) o).frames().forEach(incCount);
			} else if (o instanceof TileBase) {
			} else {
				throw new Assertion(String.format("Unhandled type %s\n", o));
			}
		});
		history.change.changeStep.changes.forEach(change -> change.debugRefCounts(incCount));
		history.undoHistory.forEach(id -> history.get(id).changes.forEach(change -> change.debugRefCounts(incCount)));
		history.redoHistory.forEach(id -> history.get(id).changes.forEach(change -> change.debugRefCounts(incCount)));
		objectMap.values().forEach(o -> {
			long got = ((ProjectObject) o).refCount();
			long expected = counts.getOrDefault(o.id(), 0L);
			if (got != expected) {
				throw new Assertion(String.format("Ref count for %s id %s : %s should be %s\n",
						o,
						o.id(),
						got,
						expected
				));
			}
		});
	}

	// Config flushing

	// Flushing
	/**
	 * This controls writing in UI thread vs reading from flush+render threads
	 */
	public AtomicReference<Timer> flushTimer = new AtomicReference<>();
	public ReadWriteLock lock = new ReentrantReadWriteLock();
	private Map<Dirtyable, Object> dirty = new ConcurrentHashMap<>();

	public void shutdown() {
		Timer timer = flushTimer.getAndSet(null);
		if (timer != null)
			timer.cancel();
		flushAll();
		config.shutdown();
		Launch.shutdown();
	}

	public ProjectContext(Path path) {
		super(path);
		uncheck(() -> {
			Files.createDirectories(path);
			Files.createDirectories(changesDir);
			Files.createDirectories(tileDir);
		});
	}

	private void flushAll() {
		Lock readLock = lock.readLock();
		Iterator<Map.Entry<Dirtyable, Object>> i = dirty.entrySet().iterator();
		while (i.hasNext()) {
			Dirtyable dirty = i.next().getKey();
			i.remove();
			readLock.lock();
			try {
				dirty.dirtyFlush(ProjectContext.this);
			} finally {
				readLock.unlock();
			}
		}
	}

	//

	public void setDirty(Dirtyable dirty) {
		this.dirty.put(dirty, Object.class);
		Timer newTimer;
		String timerName = "dirty-flush";
		Timer oldTimer = flushTimer.getAndSet(newTimer = new Timer(timerName));
		if (oldTimer != null)
			oldTimer.cancel();
		newTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					flushAll();
				} catch (Exception e) {
					System.out.format("Error duing flush");
					e.printStackTrace();
					System.out.flush();
					System.err.flush();
				}
			}
		}, 5000);
	}

	public static ProjectContext create(Path path, int tileSize) {
		ProjectContext out = new ProjectContext(path);
		out.tileSize = tileSize;
		out.project = Project.create(out);
		out.history = new History(out, ImmutableList.of(), ImmutableList.of(), null);
		out.hotkeys = new Hotkeys();
		out.initConfig();
		return out;
	}

	private void initConfig() {
		config = ConfigBase.deserialize(new TypeInfo(ProjectConfig.class), path, () -> {
			ProjectConfig config = new ProjectConfig();
			config.trueColor.set(TrueColor.fromJfx(Color.BLACK));
			return config;
		});
	}

	private static Path path(Path base) {
		return base.resolve("project.luxem");
	}

	private Path path() {
		return path(path);
	}

	@Override
	public void dirtyFlush(ProjectContextBase context) {
		atomicWrite(path(), dest -> {
			RawWriter writer = new RawWriter(dest, (byte) ' ', 4);
			writer.recordBegin();
			writer.key("tileSize").primitive(Integer.toString(tileSize));
			writer.key("nextId").primitive(Long.toString(nextId));
			writer.key("objects").arrayBegin();
			for (ProjectObjectInterface object : objectMap.values())
				object.serialize(writer);
			writer.arrayEnd();
			history.serialize(writer);
			writer.key("activeChange").primitive(Long.toString(history.change.changeStep.cacheId.id));
			writer.recordEnd();
		});
	}

	public static ProjectContext deserialize(Path path) {
		return uncheck(() -> {
			ModelDeserializationContext context = new ModelDeserializationContext();
			ProjectContext out;
			try (InputStream source = Files.newInputStream(path(path))) {
				out = (ProjectContext) new StackReader().read(source, new StackReader.ArrayState() {
					@Override
					public StackReader.State array() {
						throw new IllegalStateException("Project data should be a record.");
					}

					@Override
					public StackReader.State record() {
						return new Deserializer(context, path);
					}
				}).get(0);
			}
			context.finishers.forEach(finisher -> finisher.finish(context));
			context.objectMap
					.values()
					.stream()
					.filter(o -> o instanceof Project)
					.findFirst()
					.ifPresent(p -> out.project = (Project) p);
			out.history = new History(out, context.undoHistory, context.redoHistory, context.activeChange);
			out.hotkeys = new Hotkeys();
			out.initConfig();
			return out;
		});
	}

	public static class Deserializer extends StackReader.RecordState {
		private final ModelDeserializationContext context;
		private final ProjectContext out;

		Deserializer(ModelDeserializationContext context, Path path) {
			this.context = context;
			out = new ProjectContext(path);
		}

		@Override
		public void value(Object value) {
			if ("nextId".equals(key)) {
				out.nextId = Long.parseLong((String) value);
			} else if ("tileSize".equals(key)) {
				out.tileSize = Integer.parseInt((String) value);
			} else if ("objects".equals(key)) {
				out.objectMap =
						((List<ProjectObjectInterface>) value).stream().collect(Collectors.toMap(v -> v.id(), v -> v));
			} else if ("undo".equals(key)) {
				context.undoHistory = (List<Long>) value;
			} else if ("redo".equals(key)) {
				context.redoHistory = (List<Long>) value;
			} else if ("activeChange".equals(key)) {
				context.activeChange = Long.parseLong((String) value);
			} else
				throw new Assertion();
		}

		@Override
		public StackReader.State array() {
			if (false) {
				throw new DeadCode();
			} else if ("objects".equals(key)) {
				return new StackReader.ArrayState() {
					@Override
					public void value(Object value) {
						data.add(value);
					}

					@Override
					public StackReader.State record() {
						if ("Tile".equals(type))
							return new Tile.Deserializer(context);
						return DeserializeHelper.deserializeModel(context, type);
					}
				};
			} else if ("undo".equals(key)) {
				return new StackReader.ArrayState() {
					@Override
					public void value(Object value) {
						super.value(Long.parseLong((String) value));
					}
				};
			} else if ("redo".equals(key)) {
				return new StackReader.ArrayState() {
					@Override
					public void value(Object value) {
						super.value(Long.parseLong((String) value));
					}
				};
			} else
				throw new Assertion();
		}

		@Override
		public StackReader.State record() {
			throw new Assertion();
		}

		@Override
		public Object get() {
			return out;
		}
	}
}
