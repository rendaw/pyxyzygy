package com.zarbosoft.shoedemo.model;

import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.luxem.write.RawWriter;
import com.zarbosoft.shoedemo.ProjectContext;
import com.zarbosoft.shoedemo.deserialize.ModelDeserializationContext;
import com.zarbosoft.shoedemo.deserialize.GeneralMapState;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static com.zarbosoft.rendaw.common.Common.uncheck;

public class Tile extends TileBase implements Dirtyable {
	public WeakReference<WritableImage> data;
	private WritableImage dirtyData;

	public static Tile create(ProjectContext context, WritableImage data) {
		Tile out = new Tile();
		out.id = context.nextId++;
		out.data = new WeakReference<>(data);
		out.dirtyData = data;
		context.setDirty(out);
		return out;
	}

	@Override
	public void incRef(ProjectContextBase project) {
		refCount += 1;
		if (refCount == 1) {
			project.objectMap.put(id, this);
		}
	}

	@Override
	public void decRef(ProjectContextBase project) {
		refCount -= 1;
		if (refCount == 0) {
			project.objectMap.remove(id);
			uncheck(() -> Files.delete(path(project)));
		}
	}

	@Override
	public void serialize(RawWriter writer) {
		writer.type("Tile");
		writer.recordBegin();
		writer.key("id").primitive(Long.toString(id));
		writer.key("refCount").primitive(Long.toString(refCount));
		writer.recordEnd();
	}

	@Override
	public void dirtyFlush(ProjectContextBase context) {
		uncheck(() -> ImageIO.write(SwingFXUtils.fromFXImage(dirtyData, null), "PNG", path(context).toFile()));
		dirtyData = null;
	}

	public WritableImage getData(ProjectContext context) {
		WritableImage data = this.data.get();
		if (data == null) {
			data = SwingFXUtils.toFXImage(uncheck(() -> ImageIO.read(path(context).toFile())), null);
			this.data = new WeakReference<>(data);
		}
		return data;
	}

	public static Path path(ProjectContextBase context, long id) {
		return context.tileDir.resolve(Objects.toString(id));
	}

	private Path path(ProjectContextBase context) {
		return path(context, id);
	}

	public static class Deserializer extends StackReader.RecordState {
		private final ModelDeserializationContext context;

		private final Tile out;

		public Deserializer(ModelDeserializationContext context) {
			this.context = context;
			out = new Tile();
		}

		@Override
		public void value(Object value) {
			if ("id".equals(key)) {
				out.id = Long.valueOf((String) value);
				return;
			}
			if ("refCount".equals(key)) {
				out.refCount = Long.valueOf((String) value);
				return;
			}
			throw new RuntimeException(String.format("Unknown key (%s)", key));
		}

		@Override
		public StackReader.State array() {
			if ("metadata".equals(key))
				return new GeneralMapState() {
					public void value(Object value) {
						data.put((String) key, (String) value);
					}
				};
			throw new RuntimeException(String.format("Key (%s) is unknown or is not an array", key));
		}

		@Override
		public StackReader.State record() {
			if ("offset".equals(key)) {
				return new Vector.Deserializer();
			}
			throw new RuntimeException(String.format("Key (%s) is unknown or is not an record", key));
		}

		@Override
		public Object get() {
			context.objectMap.put(out.id(), out);
			return super.get();
		}
	}
}
