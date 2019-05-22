package com.zarbosoft.pyxyzygy.app.wrappers.truecolorimage;

import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.config.NodeConfig;
import com.zarbosoft.pyxyzygy.app.config.TrueColorBrush;
import com.zarbosoft.pyxyzygy.app.config.TrueColorImageNodeConfig;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.model.v0.TrueColorTile;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.app.widgets.binding.DoubleHalfBinder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.HalfBinder;
import com.zarbosoft.pyxyzygy.app.widgets.binding.ListPropertyHalfBinder;
import com.zarbosoft.pyxyzygy.app.wrappers.FrameFinder;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.BaseImageNodeWrapper;
import com.zarbosoft.pyxyzygy.app.wrappers.baseimage.WrapTile;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.v0.*;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import com.zarbosoft.pyxyzygy.seed.model.v0.Rectangle;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import javafx.collections.ObservableList;
import javafx.scene.image.Image;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zarbosoft.pyxyzygy.app.GUILaunch.CACHE_OBJECT;
import static com.zarbosoft.pyxyzygy.app.Misc.opt;
import static com.zarbosoft.pyxyzygy.app.config.TrueColorImageNodeConfig.TOOL_BRUSH;
import static com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext.uniqueName1;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public class TrueColorImageNodeWrapper extends BaseImageNodeWrapper<TrueColorImageLayer, TrueColorImageFrame, TrueColorTileBase, TrueColorImage> {
	final TrueColorImageNodeConfig config;
	public static FrameFinder<TrueColorImageLayer, TrueColorImageFrame> frameFinder =
			new FrameFinder<TrueColorImageLayer, TrueColorImageFrame>() {
				@Override
				public TrueColorImageFrame frameGet(TrueColorImageLayer node, int i) {
					return node.framesGet(i);
				}

				@Override
				public int frameCount(TrueColorImageLayer node) {
					return node.framesLength();
				}

				@Override
				public int frameLength(TrueColorImageFrame frame) {
					return frame.length();
				}
			};
	public final HalfBinder<TrueColorBrush> brushBinder;

	// Cache values when there's no canvas
	public TrueColorImageNodeWrapper(ProjectContext context, Wrapper parent, int parentIndex, TrueColorImageLayer node) {
		super(parent, parentIndex, node, frameFinder);
		this.config = (TrueColorImageNodeConfig) context.config.nodes.computeIfAbsent(node.id(),
				id -> new TrueColorImageNodeConfig(context)
		);
		this.brushBinder =
				new DoubleHalfBinder<ObservableList<TrueColorBrush>, Integer>(new ListPropertyHalfBinder<>(
						GUILaunch.profileConfig.trueColorBrushes),
						new DoubleHalfBinder<>(config.tool, config.brush).map(p -> {
							String t = p.first;
							Number index = p.second;
							if (!TOOL_BRUSH.equals(t))
								return opt(null);
							return opt(index.intValue());
						})
				).map(p -> {
					ObservableList<TrueColorBrush> brushes = p.first;
					Integer index = p.second;
					if (index == null)
						return opt(null);
					if (index >= brushes.size())
						return opt(null);
					return opt(brushes.get(index));
				});
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
	public ProjectLayer separateClone(ProjectContext context) {
		TrueColorImageLayer clone = TrueColorImageLayer.create(context);
		clone.initialNameSet(context, uniqueName1(node.name()));
		clone.initialOffsetSet(context, node.offset());
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
			TrueColorImageLayer node,
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
				return uncheck(() -> GUILaunch.imageCache.get(Objects.hash(CACHE_OBJECT, tile.id()),
						() -> HelperJFX.toImage(((TrueColorTile) tile).getData(context))
				));
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
			ProjectContext context,
			ChangeStepBuilder change,
			TrueColorImageFrame frame,
			Rectangle unitBounds,
			TrueColorImage image
	) {
		for (int x = 0; x < unitBounds.width; ++x) {
			for (int y = 0; y < unitBounds.height; ++y) {
				final int x0 = x;
				final int y0 = y;
				TrueColorImage shot =
						image.copy(x0 * context.tileSize, y0 * context.tileSize, context.tileSize, context.tileSize);
				change
						.trueColorImageFrame(frame)
						.tilesPut(unitBounds.corner().plus(x0, y0).to1D(), TrueColorTile.create(context, shot));
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

	@Override
	public void removeFrameOffsetListener(
			TrueColorImageFrame frame, Listener.ScalarSet<TrueColorImageFrame, Vector> listener
	) {
		frame.removeOffsetSetListeners(listener);
	}
}
