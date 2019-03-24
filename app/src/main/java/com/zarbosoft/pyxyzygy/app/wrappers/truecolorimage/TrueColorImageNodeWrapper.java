package com.zarbosoft.pyxyzygy.app.wrappers.truecolorimage;

import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.config.NodeConfig;
import com.zarbosoft.pyxyzygy.app.config.PaletteBrush;
import com.zarbosoft.pyxyzygy.app.config.TrueColorBrush;
import com.zarbosoft.pyxyzygy.app.config.TrueColorImageNodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.model.v0.TrueColorTile;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.app.wrappers.FrameFinder;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.BaseImageNodeWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.WrapTile;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.v0.ProjectNode;
import com.zarbosoft.pyxyzygy.core.model.v0.TrueColorImageFrame;
import com.zarbosoft.pyxyzygy.core.model.v0.TrueColorImageNode;
import com.zarbosoft.pyxyzygy.core.model.v0.TrueColorTileBase;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import com.zarbosoft.pyxyzygy.seed.model.v0.Rectangle;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;
import static com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext.uniqueName1;

public class TrueColorImageNodeWrapper extends BaseImageNodeWrapper<TrueColorImageNode, TrueColorImageFrame, TrueColorTileBase, TrueColorImage> {
	final TrueColorImageNodeConfig config;
	public static FrameFinder<TrueColorImageNode, TrueColorImageFrame> frameFinder =
			new FrameFinder<TrueColorImageNode, TrueColorImageFrame>() {
				@Override
				public TrueColorImageFrame frameGet(TrueColorImageNode node, int i) {
					return node.framesGet(i);
				}

				@Override
				public int frameCount(TrueColorImageNode node) {
					return node.framesLength();
				}

				@Override
				public int frameLength(TrueColorImageFrame frame) {
					return frame.length();
				}
			};
	public final CustomBinding.HalfBinder<TrueColorBrush> brushBinder;

	// Cache values when there's no canvas
	public TrueColorImageNodeWrapper(ProjectContext context, Wrapper parent, int parentIndex, TrueColorImageNode node) {
		super(parent, parentIndex, node, frameFinder);
		this.config = (TrueColorImageNodeConfig) context.config.nodes.computeIfAbsent(node.id(),
				id -> new TrueColorImageNodeConfig(context)
		);
		this.brushBinder =
				new CustomBinding.DoubleIndirectHalfBinder<ObservableList<TrueColorBrush>, Integer, TrueColorBrush>(
						new CustomBinding.ListPropertyHalfBinder<>(GUILaunch.config.trueColorBrushes),
						new CustomBinding.DoubleIndirectHalfBinder<>(
								config.tool,
								config.brush,
								(t, index) -> {
									if (t != TrueColorImageNodeConfig.Tool.BRUSH)
										return opt(null);
									return opt(index);
								}
						),
						(brushes, index) -> {
							if (index == null)
								return opt(null);
							if (index >= brushes.size())
								return opt(null);
							return opt(brushes.get(index));
						}
				);
	}

	@Override
	public EditHandle buildEditControls(ProjectContext context, Window window) {
		return new TrueColorImageEditHandle(context, window, this);
	}

	@Override
	public NodeConfig getConfig() {
		return config;
	}

	@Override
	public ProjectNode separateClone(ProjectContext context) {
		TrueColorImageNode clone = TrueColorImageNode.create(context);
		clone.initialNameSet(context, uniqueName1(node.name()));
		clone.initialOpacitySet(context, node.opacity());
		clone.initialFramesAdd(context, node.frames().stream().map(frame -> {
			TrueColorImageFrame newFrame = TrueColorImageFrame.create(context);
			newFrame.initialOffsetSet(context, frame.offset());
			newFrame.initialLengthSet(context, frame.length());
			newFrame.initialTilesPutAll(context, frame.tiles());
			return newFrame;
		}).collect(Collectors.toList()));
		return clone;
	}

	@Override
	public <I> Runnable mirrorFrames(
			TrueColorImageNode node,
			List<I> list,
			Function<TrueColorImageFrame, I> create,
			Consumer<I> remove,
			Consumer<Integer> change
	) {
		return node.mirrorFrames(list, create, remove, change);
	}

	@Override
	protected Listener.ScalarSet<TrueColorImageFrame, Vector> addFrameOffsetSetListener(
			TrueColorImageFrame frame, Listener.ScalarSet<TrueColorImageFrame, Vector> listener
	) {
		return frame.addOffsetSetListeners(listener);
	}

	@Override
	public Listener.ScalarSet<TrueColorImageFrame, Integer> addFrameLengthSetListener(
			TrueColorImageFrame frame, Listener.ScalarSet<TrueColorImageFrame, Integer> listener
	) {
		return frame.addLengthSetListeners(listener);
	}

	@Override
	public void removeFrameOffsetSetListener(
			TrueColorImageFrame frame, Listener.ScalarSet<TrueColorImageFrame, Vector> listener
	) {
		frame.removeOffsetSetListeners(listener);
	}

	@Override
	public void removeFrameLengthSetListener(
			TrueColorImageFrame frame, Listener.ScalarSet<TrueColorImageFrame, Integer> listener
	) {
		frame.removeLengthSetListeners(listener);
	}

	@Override
	public Listener.MapPutAll<TrueColorImageFrame, Long, TrueColorTileBase> addFrameTilesPutAllListener(
			TrueColorImageFrame frame, Listener.MapPutAll<TrueColorImageFrame, Long, TrueColorTileBase> listener
	) {
		return frame.addTilesPutAllListeners(listener);
	}

	@Override
	public Listener.Clear<TrueColorImageFrame> addFrameTilesClearListener(
			TrueColorImageFrame frame, Listener.Clear<TrueColorImageFrame> listener
	) {
		return frame.addTilesClearListeners(listener);
	}

	@Override
	public void removeFrameTilesPutAllListener(
			TrueColorImageFrame frame, Listener.MapPutAll<TrueColorImageFrame, Long, TrueColorTileBase> listener
	) {
		frame.removeTilesPutAllListeners(listener);
	}

	@Override
	public void removeFrameTilesClearListener(
			TrueColorImageFrame frame, Listener.Clear<TrueColorImageFrame> listener
	) {
		frame.removeTilesClearListeners(listener);
	}

	@Override
	public WrapTile<TrueColorTileBase> createWrapTile(int x, int y) {
		return new WrapTile<TrueColorTileBase>(x, y) {
			@Override
			public Image getImage(ProjectContext context, TrueColorTileBase tile) {
				return HelperJFX.toImage(((TrueColorTile) tile).getData(context));
			}
		};
	}

	@Override
	public TrueColorTileBase tileGet(TrueColorImageFrame frame, long key) {
		return frame.tilesGet(key);
	}

	@Override
	public void renderCompose(
			ProjectContext context, TrueColorImage gc, TrueColorTileBase tile, int x, int y
	) {
		TrueColorImage data = ((TrueColorTile) tile).getData(context);
		gc.compose(data, x, y, (float) 1);
	}

	@Override
	public void imageCompose(TrueColorImage image, TrueColorImage other, int x, int y) {
		image.compose(other, x, y, 1);
	}

	@Override
	public void drop(
			ProjectContext context, TrueColorImageFrame frame, Rectangle unitBounds, TrueColorImage image
	) {
		for (int x = 0; x < unitBounds.width; ++x) {
			for (int y = 0; y < unitBounds.height; ++y) {
				final int x0 = x;
				final int y0 = y;
				TrueColorImage shot =
						image.copy(x0 * context.tileSize, y0 * context.tileSize, context.tileSize, context.tileSize);
				context.history.change(c -> c
						.trueColorImageFrame(frame)
						.tilesPut(unitBounds.corner().plus(x0, y0).to1D(), TrueColorTile.create(context, shot)));
			}
		}
	}

	@Override
	public void dump(TrueColorImage image, String name) {
		image.serialize(name);
	}

	@Override
	public TrueColorImage grab(
			ProjectContext context, Rectangle unitBounds, Rectangle bounds
	) {
		TrueColorImage canvas = TrueColorImage.create(bounds.width, bounds.height);
		canvasHandle.render(context, canvas, bounds, unitBounds);
		return canvas;
	}

	@Override
	public void clear(
			ProjectContext context, TrueColorImage image, Vector offset, Vector span
	) {
		image.clear(offset.x, offset.y, span.x, span.y);
	}
}
