package com.zarbosoft.shoedemo;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.luxem.write.RawWriter;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.DeadCode;
import com.zarbosoft.shoedemo.deserialize.ModelDeserializationContext;
import com.zarbosoft.shoedemo.model.*;
import javafx.beans.property.SimpleObjectProperty;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.zarbosoft.rendaw.common.Common.atomicWrite;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public class ProjectContext extends ProjectContextBase implements Dirtyable {
	public Project project;
	public int tileSize = 256;

	public History history;
	public List<FrameMapEntry> timeMap;
	public SimpleObjectProperty<Wrapper> selectedForEdit = new SimpleObjectProperty<>();
	public SimpleObjectProperty<Wrapper> selectedForView = new SimpleObjectProperty<>();

	public void debugCheckRefCounts() {
		Map<Long, Long> counts = new HashMap<>();
		Consumer<ProjectObjectInterface> incCount =
				o1 -> counts.compute(o1.id(), (i, count) -> count == null ? 1 : count + 1);
		objectMap.values().forEach(o -> {
			if (false) {
				throw new Assertion();
			} else if (o instanceof Project) {
				((Project) o).top().forEach(x -> {
					incCount.accept(x);
				});
			} else if (o instanceof Camera) {
				if (((Camera) o).inner() != null) {
					incCount.accept(((Camera) o).inner());
				}
			} else if (o instanceof GroupLayer) {
				((GroupLayer) o).timeFrames().forEach(frame -> {
					incCount.accept(frame);
				});
				((GroupLayer) o).positionFrames().forEach(frame -> {
					incCount.accept(frame);
				});
			} else if (o instanceof GroupNode) {
				((GroupNode) o).layers().forEach(layer -> {
					incCount.accept(layer);
				});
			} else if (o instanceof GroupPositionFrame) {
			} else if (o instanceof GroupTimeFrame) {
			} else if (o instanceof ImageFrame) {
				((ImageFrame) o).tiles().values().forEach(tile -> {
					incCount.accept(tile);
				});
			} else if (o instanceof ImageNode) {
				((ImageNode) o).frames().forEach(frame -> {
					incCount.accept(frame);
				});
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

	// Flushing
	/**
	 * This controls writing in UI thread vs reading from flush+render threads
	 */
	public AtomicBoolean alive = new AtomicBoolean(true);
	public ReadWriteLock lock = new ReentrantReadWriteLock();
	private Map<Dirtyable, Object> dirty = new ConcurrentHashMap<>();
	public Semaphore flushSemaphore = new Semaphore(0);

	{
		Runtime.getRuntime().addShutdownHook(new Thread(this::flushAll));
		new Thread() {
			@Override
			public void run() {
				while (alive.get()) {
					int got = 0;
					while (flushSemaphore.tryAcquire())
						got += 1;
					if (got == 0)
						uncheck(() -> flushSemaphore.acquire());
					flushAll();
				}
				System.out.format("Flush thread dying\n");
			}
		}.start();
	}

	public ProjectContext(Path path) {
		super(path);
	}

	private void flushAll() {
		Lock readLock = lock.readLock();
		Iterator<Map.Entry<Dirtyable, Object>> i = dirty.entrySet().iterator();
		while (i.hasNext()) {
			Dirtyable dirty = i.next().getKey();
			i.remove();
			readLock.lock();
			try {
				System.out.format("flushing %s\n", dirty);
				dirty.dirtyFlush(ProjectContext.this);
			} finally {
				readLock.unlock();
			}
		}
	}

	//

	public void setDirty(Dirtyable dirty) {
		this.dirty.put(dirty, Object.class);
		flushSemaphore.release();
	}

	public static ProjectContext create(Path path) {
		ProjectContext out = new ProjectContext(path);
		out.project = Project.create(out);
		out.history = new History(out, ImmutableList.of(), ImmutableList.of());
		return out;
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
			out.history = new History(out, context.undoHistory, context.redoHistory);
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
