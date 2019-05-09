package com.zarbosoft.pyxyzygy.app.wrappers.paletteimage;

import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.BaseToolSelect;
import com.zarbosoft.pyxyzygy.core.PaletteImage;
import com.zarbosoft.pyxyzygy.core.model.v0.ChangeStepBuilder;
import com.zarbosoft.pyxyzygy.core.model.v0.PaletteImageFrame;
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

public class ToolSelect extends BaseToolSelect<PaletteImageFrame, PaletteImage> {
	final PaletteImageEditHandle editHandle;

	public ToolSelect(PaletteImageEditHandle editHandle) {
		super(editHandle.wrapper, editHandle.wrapper.canvasHandle.zoom);
		this.editHandle = editHandle;
	}

	static DataFormat dataFormat = new DataFormat(nameSymbol + "/palette-image");

	@Override
	protected void copy(ClipboardContent content, PaletteImage lifted) {
		uncheck(() -> {
			{
				Path temp = Files.createTempFile(nameSymbol + "-copy", ".palimg");
				temp.toFile().deleteOnExit();
				lifted.serialize(temp.toString());
				try (InputStream source = Files.newInputStream(temp)) {
					content.put(dataFormat, ByteBuffer.wrap(source.readAllBytes()));
				}
			}
			content.putImage(toImage(lifted));
		});
	}

	@Override
	protected Image toImage(PaletteImage buffer) {
		return HelperJFX.toImage(buffer, editHandle.wrapper.palette.colors);
	}

	@Override
	protected void propertiesSet(Node node) {
		editHandle.toolProperties.set(this, node);
	}

	@Override
	protected void propertiesClear() {
		editHandle.toolProperties.clear(this);
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
	public void clear(
			ProjectContext context, ChangeStepBuilder change, Rectangle bounds
	) {
		editHandle.wrapper.canvasHandle.clear(context, change, bounds);
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
				Path temp = Files.createTempFile(nameSymbol + "-paste", ".palimg");
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
