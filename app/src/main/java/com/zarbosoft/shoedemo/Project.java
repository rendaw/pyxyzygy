package com.zarbosoft.shoedemo;

import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.luxem.write.RawWriter;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.shoedemo.deserialize.DeserializationContext;
import com.zarbosoft.shoedemo.deserialize.IDListState;
import com.zarbosoft.shoedemo.model.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.zarbosoft.rendaw.common.Common.uncheck;

public class Project extends ProjectBase {
	public List<Arrangement> arrangements = new ArrayList<>();

	public Project(Path path) {
		this.root = path;
		this.tileDir = root.resolve("tiles");
		uncheck(() -> Files.createDirectories(tileDir));
	}

	public void serialize(RawWriter writer) {
		writer.recordBegin();
		writer.key("nextId").primitive(Long.toString(nextId));
		writer.key("arrangements").arrayBegin();
		for (Arrangement arrangement : arrangements)
			arrangement.serialize(writer);
		writer.arrayEnd();
		writer.key("objects").arrayBegin();
		for (ProjectNode object : objectMap.values())
			((ProjectObjectInterface) object).serialize(writer);
		writer.arrayEnd();
		writer.recordEnd();
	}

	public static Project deserialize(Path path) {
		return uncheck(() -> {
			DeserializationContext context = new DeserializationContext();
			Project out;
			try (InputStream source = Files.newInputStream(path.resolve("project.luxem"))) {
				out= (Project) new StackReader().read(source, new Deserializer(context, path));
			}
			context.finishers.forEach(finisher -> finisher.finish(context));
			return out;
		});
	}
	public static Change deserializeChange(long id) {
		return uncheck(() -> {
			try (InputStream source = Files.newInputStream(ChangeStep.path(this, id)) {
				return (Project) new StackReader().read(source, new Deserializer(context, path));
			}
		});
	}

	public static class Deserializer extends StackReader.RecordState {
		private final DeserializationContext context;
		private final Project out;

		Deserializer(DeserializationContext context, Path path) {
			this.context = context;
			out = new Project(path);
		}

		@Override
		public void value(Object value) {
			if ("nextId".equals(key)) {
				out.nextId = Long.parseLong((String) value);
			} else if ("arrangements".equals(key)) {
				out.arrangements = (List<Arrangement>) value;
			} else if ("objects".equals(key)) {
				// nop
			} else
				throw new Assertion();
		}

		@Override
		public StackReader.State array() {
			if ("arrangements".equals(key)) {
				return new IDListState();
			} else if ("objects".equals(key)) {
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
