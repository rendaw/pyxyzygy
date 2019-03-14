package com.zarbosoft.pyxyzygy.app.wrappers.paletteimage;

import com.zarbosoft.pyxyzygy.app.EditHandle;
import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.Wrapper;
import com.zarbosoft.pyxyzygy.app.config.NodeConfig;
import com.zarbosoft.pyxyzygy.app.config.PaletteImageNodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.PaletteTile;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.app.wrappers.FrameFinder;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.BaseImageNodeWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.WrapTile;
import com.zarbosoft.pyxyzygy.core.PaletteImage;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.v0.PaletteImageFrame;
import com.zarbosoft.pyxyzygy.core.model.v0.PaletteImageNode;
import com.zarbosoft.pyxyzygy.core.model.v0.PaletteTileBase;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectNode;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import com.zarbosoft.pyxyzygy.seed.model.v0.Rectangle;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import javafx.scene.image.Image;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext.uniqueName1;

public class PaletteImageNodeWrapper extends BaseImageNodeWrapper<PaletteImageNode, PaletteImageFrame, PaletteTileBase, PaletteImage> {
	final PaletteImageNodeConfig config;
	private final ProjectContext.PaletteWrapper palette;

	public PaletteImageNodeWrapper(
			ProjectContext context,
			Wrapper parent,
			int parentIndex,
			PaletteImageNode node,
			FrameFinder<PaletteImageNode, PaletteImageFrame> frameFinder
	) {
		super(parent, parentIndex, node, frameFinder);
		config = (PaletteImageNodeConfig) context.config.nodes.computeIfAbsent(
				node.id(),
				k -> new PaletteImageNodeConfig(context)
		);
		this.palette = context.getPaletteWrapper(node.palette());
	}

	@Override
	public <I> Runnable mirrorFrames(
			PaletteImageNode node,
			List<I> list,
			Function<PaletteImageFrame, I> create,
			Consumer<I> remove,
			Consumer<Integer> change
	) {
		return node.mirrorFrames(list, create, remove, change);
	}

	@Override
	protected Listener.ScalarSet<PaletteImageFrame, Vector> addFrameOffsetSetListener(
			PaletteImageFrame frame, Listener.ScalarSet<PaletteImageFrame, Vector> listener
	) {
		return frame.addOffsetSetListeners(listener);
	}

	@Override
	public Listener.ScalarSet<PaletteImageFrame, Integer> addFrameLengthSetListener(
			PaletteImageFrame frame, Listener.ScalarSet<PaletteImageFrame, Integer> listener
	) {
		return frame.addLengthSetListeners(listener);
	}

	@Override
	public void removeFrameOffsetSetListener(
			PaletteImageFrame frame, Listener.ScalarSet<PaletteImageFrame, Vector> listener
	) {
		frame.removeOffsetSetListeners(listener);
	}

	@Override
	public void removeFrameLengthSetListener(
			PaletteImageFrame frame, Listener.ScalarSet<PaletteImageFrame, Integer> listener
	) {
		frame.removeLengthSetListeners(listener);
	}

	@Override
	public Listener.MapPutAll<PaletteImageFrame, Long, PaletteTileBase> addFrameTilesPutAllListener(
			PaletteImageFrame frame, Listener.MapPutAll<PaletteImageFrame, Long, PaletteTileBase> listener
	) {
		return frame.addTilesPutAllListeners(listener);
	}

	@Override
	public Listener.Clear<PaletteImageFrame> addFrameTilesClearListener(
			PaletteImageFrame frame, Listener.Clear<PaletteImageFrame> listener
	) {
		return frame.addTilesClearListeners(listener);
	}

	@Override
	public void removeFrameTilesPutAllListener(
			PaletteImageFrame frame, Listener.MapPutAll<PaletteImageFrame, Long, PaletteTileBase> listener
	) {
		frame.removeTilesPutAllListeners(listener);
	}

	@Override
	public void removeFrameTilesClearListener(
			PaletteImageFrame frame, Listener.Clear<PaletteImageFrame> listener
	) {
		frame.removeTilesClearListeners(listener);
	}

	@Override
	public WrapTile<PaletteTileBase> createWrapTile(int x, int y) {
		return new WrapTile<PaletteTileBase>(x, y) {
			@Override
			public Image getImage(
					ProjectContext context, PaletteTileBase tile
			) {
				return HelperJFX.toImage(((PaletteTile) tile).getData(context), node.palette());
			}
		};
	}

	@Override
	public PaletteTileBase tileGet(PaletteImageFrame frame, long key) {
		return frame.tilesGet(key);
	}

	@Override
	public void renderCompose(
			ProjectContext context, TrueColorImage gc, PaletteTileBase tile, int x, int y
	) {
		PaletteImage data = ((PaletteTile) tile).getData(context);
		gc.compose(data, palette.colors, x, y, 1);
	}

	@Override
	public void imageCompose(PaletteImage image, PaletteImage other, int x, int y) {
		image.compose(other, x, y);
	}

	@Override
	public void drop(
			ProjectContext context, PaletteImageFrame frame, Rectangle unitBounds, PaletteImage image
	) {

	}

	@Override
	public PaletteImage grab(ProjectContext context, Rectangle unitBounds, Rectangle bounds) {
		PaletteImage canvas = PaletteImage.create(bounds.width, bounds.height);
		for (int x = 0; x < unitBounds.width; ++x) {
			for (int y = 0; y < unitBounds.height; ++y) {
				PaletteTile tile = (PaletteTile) tileGet(canvasHandle.frame, unitBounds.corner().plus(x, y).to1D());
				if (tile == null)
					continue;
				final int renderX = (x + unitBounds.x) * context.tileSize - bounds.x;
				final int renderY = (y + unitBounds.y) * context.tileSize - bounds.y;
				canvas.place(tile.getData(context), renderX, renderY);
			}
		}
		return canvas;
	}

	@Override
	public NodeConfig getConfig() {
		return config;
	}

	@Override
	public EditHandle buildEditControls(ProjectContext context, Window window) {
		return new PaletteImageEditHandle(context, window, this);
	}

	@Override
	public ProjectNode separateClone(ProjectContext context) {
		PaletteImageNode clone = PaletteImageNode.create(context);
		clone.initialNameSet(context, uniqueName1(node.name()));
		clone.initialOpacitySet(context, node.opacity());
		clone.initialPaletteSet(context, node.palette());
		clone.initialFramesAdd(context, node.frames().stream().map(frame -> {
			PaletteImageFrame newFrame = PaletteImageFrame.create(context);
			newFrame.initialOffsetSet(context, frame.offset());
			newFrame.initialLengthSet(context, frame.length());
			newFrame.initialTilesPutAll(context, frame.tiles());
			return newFrame;
		}).collect(Collectors.toList()));
		return clone;
	}
}
