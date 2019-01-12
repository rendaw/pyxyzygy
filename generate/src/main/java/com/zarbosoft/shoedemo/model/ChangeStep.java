package com.zarbosoft.shoedemo.model;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.luxem.write.RawWriter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.zarbosoft.rendaw.common.Common.*;

@Configuration
public class ChangeStep implements Dirtyable {
	public static class CacheId {
		public final long id;

		public CacheId(long id) {
			this.id = id;
		}
	}

	public final CacheId cacheId;
	public List<Change> changes = new ArrayList<>();

	public ChangeStep(CacheId id) {
		this.cacheId = id;
	}

	public void add(Change change) {
		changes.add(change);
	}

	public ChangeStep apply(ProjectContextBase project) {
		ChangeStep out = new ChangeStep(new CacheId(project.nextId++));
		for (Change change : reversed(changes))
			change.apply(project, out);
		remove(project);
		return out;
	}

	public static Path path(ProjectContextBase context, long id) {
		return context.changesDir.resolve(Long.toString(id));
	}

	private Path path(ProjectContextBase context) {
		return path(context, cacheId.id);
	}

	public void remove(ProjectContextBase context) {
		for (Change c : changes)
			c.delete(context);
		uncheck(() -> Files.delete(path(context)));
	}

	@Override
	public void dirtyFlush(ProjectContextBase context) {
		atomicWrite(path(context), dest -> {
			RawWriter writer = new RawWriter(dest, (byte) ' ', 4);
			changes.forEach(c -> c.serialize(writer));
		});
	}
}
