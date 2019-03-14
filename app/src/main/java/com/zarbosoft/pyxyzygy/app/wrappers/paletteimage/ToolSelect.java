package com.zarbosoft.pyxyzygy.app.wrappers.paletteimage;

import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.BaseToolSelect;
import com.zarbosoft.pyxyzygy.core.PaletteImage;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.seed.model.v0.Rectangle;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import com.zarbosoft.rendaw.common.Pair;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static com.zarbosoft.pyxyzygy.app.Global.nameSymbol;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public class ToolSelect extends BaseToolSelect<PaletteImage> {
	final PaletteImageEditHandle editHandle;

	public ToolSelect(ProjectContext context, Window window, PaletteImageEditHandle editHandle) {
		super(context, window, editHandle.wrapper.canvasHandle.zoom);
		this.editHandle = editHandle;
	}

	@Override
	protected void place(
			ProjectContext context,
			PaletteImage lifted,
			Rectangle bounds,
			Rectangle destQuantizedBounds,
			Rectangle dropBounds,
			Vector offset
	) {
		TrueColorImage composeCanvas = TrueColorImage.create(dropBounds.width, dropBounds.height);
		editHandle.wrapper.canvasHandle.renderPalette(context, composeCanvas, dropBounds);
		composeCanvas.compose(lifted, offset.x, offset.y);
		editHandle.wrapper.canvasHandle.drop(context, destQuantizedBounds, composeCanvas);
	}

	static DataFormat dataFormat = new DataFormat(nameSymbol + "/palette-image");

	@Override
	protected void copy(ClipboardContent content, PaletteImage lifted) {
		uncheck(() -> {
			Path temp = Files.createTempFile(nameSymbol + "-copy", ".palimg");
			temp.toFile().deleteOnExit();
			lifted.serialize(temp.toString());
			try (InputStream source = Files.newInputStream(temp)) {
				content.put(dataFormat, ByteBuffer.wrap(source.readAllBytes()));
			}
		});
	}

	@Override
	protected Image toImage(PaletteImage buffer) {
		return HelperJFX.toImage(buffer);
	}

	@Override
	protected void propertiesSet(Node node) {
		editHandle.paintTab.setContent(node);
	}

	@Override
	protected void overlayAdd(Node... nodes) {
		editHandle.overlay.getChildren().addAll(nodes);
	}

	@Override
	protected void overlayRemove(Node... nodes) {
		editHandle.overlay.getChildren().removeAll(nodes);
	}

	@Override
	public void clear(ProjectContext context, Rectangle bounds) {
		editHandle.wrapper.canvasHandle.clear(context, bounds);
	}

	@Override
	protected PaletteImage grab(ProjectContext context, Rectangle rectangle) {
		TrueColorImage buffer = TrueColorImage.create(rectangle.width, rectangle.height);
		editHandle.wrapper.canvasHandle.render(context, buffer, rectangle);
		return buffer;
	}

	@Override
	protected Vector negViewCenter(Window window) {
		return editHandle.wrapper.canvasHandle
				.toInner(window.selectedForView.get().getWrapper().getConfig().scroll.get())
				.toInt();
	}

	@Override
	protected Pair<PaletteImage, Vector> uncopy(ProjectContext context) {
		PaletteImage out;
		out = Optional.ofNullable(Clipboard.getSystemClipboard().getContent(dataFormat)).map(b -> {
			if (!(b instanceof ByteBuffer))
				return null;
			return uncheck(() -> {
				Path temp = Files.createTempFile(nameSymbol + "-paste", ".blob");
				temp.toFile().deleteOnExit();
				try (OutputStream dest = Files.newOutputStream(temp)) {
					dest.write(((ByteBuffer) b).array());
					dest.flush();
					return PaletteImage.deserialize(temp.toString());
				}
			});
		}).orElse(null);
		if (out == null) {
			// TODO: from PNG
		}
		if (out == null)
			return null;
		return new Pair<>(out, new Vector(out.getWidth(), out.getHeight()));
	}
}
