package com.zarbosoft.pyxyzygy.app.model.v0;

import com.zarbosoft.interface1.TypeInfo;
import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.luxem.write.RawWriter;
import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.config.RootProjectConfig;
import com.zarbosoft.pyxyzygy.core.PaletteColors;
import com.zarbosoft.pyxyzygy.core.model.v0.*;
import com.zarbosoft.pyxyzygy.seed.deserialize.ModelDeserializationContext;
import com.zarbosoft.pyxyzygy.seed.model.Dirtyable;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import com.zarbosoft.pyxyzygy.seed.model.v0.ProjectContextBase;
import com.zarbosoft.pyxyzygy.seed.model.v0.ProjectObjectInterface;
import com.zarbosoft.pyxyzygy.seed.model.v0.TrueColor;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.DeadCode;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.zarbosoft.pyxyzygy.app.Global.logger;
import static com.zarbosoft.rendaw.common.Common.atomicWrite;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public class ProjectContext extends ProjectContextBase implements Dirtyable {
	public final static String version = "v0";
	public static Map<String, Image> iconCache = new HashMap<>();
	public static Map<String, Integer> names = new HashMap<>();
	public Project project;
	public RootProjectConfig config;
	public int tileSize;

	public History history;
	public Hotkeys hotkeys;

	public static class PaletteWrapper {
		public PaletteColors colors;
		public List<Runnable> cleanup = new ArrayList<>();
		public List<PaletteImageNode> users = new ArrayList<>();
		public List<Runnable> listeners = new ArrayList<>();
	}

	private Map<Palette, PaletteWrapper> colors = new HashMap<>();

	public void addPaletteUser(PaletteImageNode image) {
		getPaletteWrapper(image.palette()).users.add(image);
	}

	public PaletteWrapper getPaletteWrapper(Palette palette) {
		return this.colors.computeIfAbsent(palette, k -> {
			PaletteWrapper out = new PaletteWrapper();
			out.colors = new PaletteColors();
			palette.mirrorEntries(out.cleanup, v -> {
				if (v instanceof PaletteColor) {
					final Listener.ScalarSet<PaletteColor, TrueColor> colorChangeListener = (target, value) -> {
						out.colors.set(((PaletteColor) v).index(), value.r, value.g, value.b, value.a);
						out.listeners.forEach(l -> l.run());
					};
					((PaletteColor) v).addColorSetListeners(colorChangeListener);
					return () -> {
						out.colors.set(((PaletteColor) v).index(), (byte) 0, (byte) 0, (byte) 0, (byte) 0);
						((PaletteColor) v).removeColorSetListeners(colorChangeListener);
					};
				} else if (v instanceof PaletteSeparator) {
					return () -> {};
				} else
					throw new Assertion();
			}, r -> {
				r.run();
			}, i -> {
				out.listeners.forEach(l -> l.run());
			});
			return out;
		});
	}

	public PaletteColors getPaletteColors(Palette palette) {
		return getPaletteWrapper(palette).colors;
	}

	public static class Tuple {
		final List data;

		public Tuple(Object... data) {
			this.data = Arrays.asList(data);
		}

		@Override
		public boolean equals(Object obj) {
			Tuple other = (Tuple) obj;
			if (data.size() != other.data.size())
				return false;
			for (int i = 0; i < data.size(); ++i)
				if (!Objects.equals(data.get(i), other.data.get(i)))
					return false;
			return true;
		}
	}

	private Instant lastChangeTime = Instant.EPOCH;
	private Tuple lastChangeUnique = null;

	public void change(Tuple unique, Consumer<ChangeStepBuilder> consumer) {
		Instant now = Instant.now();
		if (unique == null ||
				lastChangeUnique == null ||
				!unique.equals(lastChangeUnique) ||
				now.isAfter(lastChangeTime.plusSeconds(1))) {
			history.finishChange();
		}
		lastChangeTime = now;
		lastChangeUnique = unique;
		history.change(consumer);
	}

	public static void countUniqueName(String name) {
		Pattern re = Pattern.compile("(.*) [0-9]+");
		name = re.matcher(name).replaceAll(r -> r.group(1));
		uniqueName(name);
	}

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
				((Project) o).palettes().forEach(incCount);
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
			} else if (o instanceof TrueColorTileBase) {
			} else if (o instanceof PaletteImageFrame) {
				((PaletteImageFrame) o).tiles().values().forEach(incCount);
			} else if (o instanceof PaletteImageNode) {
				incCount.accept(((PaletteImageNode) o).palette());
				((PaletteImageNode) o).frames().forEach(incCount);
			} else if (o instanceof PaletteTileBase) {
			} else if (o instanceof Palette) {
				((Palette) o).entries().forEach(incCount);
			} else if (o instanceof PaletteColor) {
			} else if (o instanceof PaletteSeparator) {
			} else {
				throw new Assertion(String.format("Unhandled type %s\n", o.getClass()));
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

	public ProjectContext(Path path) {
		super(path);
		uncheck(() -> {
			Files.createDirectories(path);
			Files.createDirectories(changesDir);
			Files.createDirectories(tileDir);
		});
		Global.shutdown.add(() -> {
			Timer timer = flushTimer.getAndSet(null);
			if (timer != null)
				timer.cancel();
			flushAll();
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
					logger.writeException(e, "Error during project flush");
				}
			}
		}, 5000);
	}

	public void initConfig() {
		config = ConfigBase.deserialize(new TypeInfo(RootProjectConfig.class), path.resolve("config.luxem"), () -> {
			RootProjectConfig config = new RootProjectConfig();
			config.trueColor.set(TrueColor.fromJfx(Color.BLACK));
			return config;
		});
	}

	private Path projectPath() {
		return Global.projectPath(path);
	}

	@Override
	public void dirtyFlush(ProjectContextBase context) {
		atomicWrite(projectPath(), dest -> {
			RawWriter writer = new RawWriter(dest, (byte) ' ', 4);
			writer.type(version);
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

	@Override
	public Object migrate() {
		throw new Assertion();
	}

	@Override
	public void clearHistory() {
		history.clearHistory();
	}

	@Override
	public boolean needsMigrate() {
		return false;
	}

	public static class Deserializer extends StackReader.RecordState {
		private final ModelDeserializationContext context;
		private final ProjectContext out;

		public Deserializer(ModelDeserializationContext context, Path path) {
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
						if ("TrueColorTile".equals(type))
							return new TrueColorTile.Deserializer(context);
						else if ("PaletteTile".equals(type))
							return new PaletteTile.Deserializer(context);
						else
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
