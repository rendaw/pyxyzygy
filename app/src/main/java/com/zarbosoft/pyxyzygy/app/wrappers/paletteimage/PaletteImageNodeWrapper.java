package com.zarbosoft.pyxyzygy.app.wrappers.paletteimage;

import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.config.NodeConfig;
import com.zarbosoft.pyxyzygy.app.config.PaletteBrush;
import com.zarbosoft.pyxyzygy.app.config.PaletteImageNodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.PaletteTile;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.app.wrappers.FrameFinder;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.BaseImageCanvasHandle;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.BaseImageNodeWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.WrapTile;
import com.zarbosoft.pyxyzygy.core.PaletteImage;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.v0.*;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import com.zarbosoft.pyxyzygy.seed.model.v0.Rectangle;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;
import static com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext.uniqueName1;

public class PaletteImageNodeWrapper extends BaseImageNodeWrapper<PaletteImageNode, PaletteImageFrame, PaletteTileBase, PaletteImage> {
	final PaletteImageNodeConfig config;
	public final ProjectContext.PaletteWrapper palette;

	public static FrameFinder<PaletteImageNode, PaletteImageFrame> frameFinder =
			new FrameFinder<PaletteImageNode, PaletteImageFrame>() {
				@Override
				public PaletteImageFrame frameGet(PaletteImageNode node, int i) {
					return node.framesGet(i);
				}

				@Override
				public int frameCount(PaletteImageNode node) {
					return node.framesLength();
				}

				@Override
				public int frameLength(PaletteImageFrame frame) {
					return frame.length();
				}
			};
	public final CustomBinding.HalfBinder<PaletteBrush> brushBinder;
	public final CustomBinding.IndirectBinder<Integer> paletteSelOffsetBinder;
	public final CustomBinding.HalfBinder<ProjectObject> paletteSelectionBinder;

	public PaletteImageNodeWrapper(
			ProjectContext context, Wrapper parent, int parentIndex, PaletteImageNode node
	) {
		super(parent, parentIndex, node, frameFinder);
		config = (PaletteImageNodeConfig) context.config.nodes.computeIfAbsent(node.id(),
				k -> new PaletteImageNodeConfig(context)
		);
		this.brushBinder =
				new CustomBinding.DoubleHalfBinder<ObservableList<PaletteBrush>, Integer, PaletteBrush>(new CustomBinding.ListPropertyHalfBinder<>(
						GUILaunch.profileConfig.paletteBrushes),
						new CustomBinding.DoubleHalfBinder<>(config.tool, config.brush, (t, index) -> {
							if (t != PaletteImageNodeConfig.Tool.BRUSH)
								return opt(null);
							return opt(index);
						}),
						(brushes, index) -> {
							if (index == null)
								return opt(null);
							if (index >= brushes.size())
								return opt(null);
							return opt(brushes.get(index));
						}
				);
		paletteSelOffsetBinder =
				new CustomBinding.IndirectBinder<Integer>(new CustomBinding.IndirectHalfBinder<Boolean>(brushBinder,
						b -> b == null ? opt(null) : opt(b.useColor)
				),
						b -> opt((Boolean) b ? brushBinder.get().get().paletteOffset : config.paletteOffset)
				);
		paletteSelectionBinder = new CustomBinding.DoubleHalfBinder<>(paletteSelOffsetBinder,
				new CustomBinding.ListHalfBinder<ProjectObject>(node.palette(), "entries"),
				(offset, entries) -> {
					if (entries.size() <= offset)
						return opt(null);
					else
						return opt(entries.get(offset));
				}
		);
		this.palette = context.getPaletteWrapper(node.palette());
	}

	@Override
	public CanvasHandle buildCanvas(
			ProjectContext context, Window window, CanvasHandle parent
	) {
		if (canvasHandle == null)
			canvasHandle =
					new BaseImageCanvasHandle<PaletteImageNode, PaletteImageFrame, PaletteTileBase, PaletteImage>(
							context,
							parent,
							this
					) {
						private Runnable paletteChangeListener;

						{
							paletteChangeListener = () -> {
								wrapTiles.forEach((k, t) -> {
									t.update(context, wrapper.tileGet(frame, k));
								});
							};
							palette.listeners.add(paletteChangeListener);
						}

						@Override
						public void remove(ProjectContext context) {
							super.remove(context);
							palette.listeners.remove(paletteChangeListener);
						}
					};
		return canvasHandle;
	}

	@Override
	public void dump(PaletteImage image, String name) {
		// TODO
		throw new Assertion();
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
	public Listener.ScalarSet<PaletteImageFrame, Vector> addFrameOffsetListener(
			PaletteImageFrame frame, Listener.ScalarSet<PaletteImageFrame, Vector> listener
	) {
		return frame.addOffsetSetListeners(listener);
	}

	@Override
	public void removeFrameOffsetListener(
			PaletteImageFrame frame, Listener.ScalarSet<PaletteImageFrame, Vector> listener
	) {
		frame.removeOffsetSetListeners(listener);
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
				return HelperJFX.toImage(((PaletteTile) tile).getData(context), palette.colors);
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
		image.replace(other, x, y);
	}

	@Override
	public void drop(
			ProjectContext context,
			ChangeStepBuilder change,
			PaletteImageFrame frame,
			Rectangle unitBounds,
			PaletteImage image
	) {
		for (int x = 0; x < unitBounds.width; ++x) {
			for (int y = 0; y < unitBounds.height; ++y) {
				final int x0 = x;
				final int y0 = y;
				PaletteImage shot =
						image.copy(x0 * context.tileSize, y0 * context.tileSize, context.tileSize, context.tileSize);
				change
						.paletteImageFrame(frame)
						.tilesPut(unitBounds.corner().plus(x0, y0).to1D(), PaletteTile.create(context, shot));
			}
		}
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
				canvas.replace(tile.getData(context), renderX, renderY);
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

	@Override
	public void clear(
			ProjectContext context, PaletteImage image, Vector offset, Vector span
	) {
		image.clear(offset.x, offset.y, span.x, span.y);
	}
}
