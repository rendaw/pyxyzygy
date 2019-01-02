package com.zarbosoft.shoedemo;

import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.luxem.write.RawWriter;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.shoedemo.deserialize.DeserializationContext;
import com.zarbosoft.shoedemo.model.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.zarbosoft.rendaw.common.Common.uncheck;

public class ProjectContext extends ProjectContextBase {
	public Path path;
	public Project project;
	public int tileSize = 256;
	private List<ChangeStep> undoHistory = new ArrayList<>();
	private List<ChangeStep> redoHistory = new ArrayList<>();
	public ChangeStepBuilder change = new ChangeStepBuilder(this);

	public static ProjectContext create(Path path) {
		ProjectContext out = new ProjectContext();
		out.path = path;
		out.project = Project.create(out);
		return out;
	}

	public void serialize(RawWriter writer) {
		writer.recordBegin();
		writer.key("tileSize").primitive(Integer.toString(tileSize));
		writer.key("nextId").primitive(Long.toString(nextId));
		writer.key("objects").arrayBegin();
		project.serialize(writer);
		for (com.zarbosoft.internal.shoedemo_generate.premodel.ProjectObject object : objectMap.values())
			((ProjectObjectInterface) object).serialize(writer);
		writer.arrayEnd();
		writer.recordEnd();
	}

	public static ProjectContext deserialize(Path path) {
		return uncheck(() -> {
			DeserializationContext context = new DeserializationContext();
			ProjectContext out;
			try (InputStream source = Files.newInputStream(path.resolve("project.luxem"))) {
				out = (ProjectContext) new StackReader().read(source, new Deserializer(context, path));
			}
			context.finishers.forEach(finisher -> finisher.finish(context));
			context.objectMap
					.values()
					.stream()
					.filter(o -> o instanceof Project)
					.findFirst()
					.ifPresent(p -> out.project = (Project) p);
			out.path = path;
			return out;
		});
	}

	public void finishChange() {
		if (change.changeStep.changes.isEmpty())
			return;
		undoHistory.add(change.changeStep);
		for (ChangeStep c : redoHistory) c.remove(this);
		redoHistory.clear();
		change = new ChangeStepBuilder(this);
		System.out.format("change done; undo %s, redo %s\n", undoHistory.size(), redoHistory.size());
	}

	public void clearHistory() {
		finishChange();
		for (ChangeStep c : undoHistory) c.remove(this);
		undoHistory.clear();
		for (ChangeStep c : redoHistory) c.remove(this);
		redoHistory.clear();
	}

	public void undo() {
		if (undoHistory.isEmpty())
			return;
		ChangeStep selected = undoHistory.remove(undoHistory.size() - 1);
		redoHistory.add(selected.apply(this, nextId++));
		selected.remove(this);
		System.out.format("undo done; undo %s, redo %s\n", undoHistory.size(), redoHistory.size());
	}

	public void redo() {
		if (redoHistory.isEmpty())
			return;
		ChangeStep selected = redoHistory.remove(redoHistory.size() - 1);
		undoHistory.add(selected.apply(this, nextId++));
		System.out.format("redo done; undo %s, redo %s\n", undoHistory.size(), redoHistory.size());
	}

	public static class Deserializer extends StackReader.RecordState {
		private final DeserializationContext context;
		private final ProjectContext out;

		Deserializer(DeserializationContext context, Path path) {
			this.context = context;
			out = new ProjectContext();
			out.path = path;
		}

		@Override
		public void value(Object value) {
			if ("nextId".equals(key)) {
				out.nextId = Long.parseLong((String) value);
			} else if ("tileSize".equals(key)) {
				out.tileSize = Integer.parseInt((String) value);
			} else if ("objects".equals(key)) {
				out.objectMap = ((List<ProjectObjectInterface>) value)
						.stream()
						.collect(Collectors.toMap(v -> v.id(), v -> (com.zarbosoft.internal.shoedemo_generate.premodel.ProjectObject) v));
			} else
				throw new Assertion();
		}

		@Override
		public StackReader.State array() {
			if ("objects".equals(key)) {
				return new StackReader.ArrayState() {
					@Override
					public StackReader.State record() {
						return DeserializeHelper.deserializeModel(context, type);
					}
				};
			} else
				throw new Assertion();
		}

		@Override
		public StackReader.State record() {
			throw new Assertion();
		}
	}
}
