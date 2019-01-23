package com.zarbosoft.shoedemo.wrappers.truecolorimage;

import com.zarbosoft.internal.shoedemo_seed.model.Rectangle;
import com.zarbosoft.internal.shoedemo_seed.model.Vector;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.shoedemo.*;
import com.zarbosoft.shoedemo.config.NodeConfig;
import com.zarbosoft.shoedemo.config.TrueColorImageNodeConfig;
import com.zarbosoft.shoedemo.model.*;
import com.zarbosoft.shoedemo.wrappers.group.Tool;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;

import java.util.List;
import java.util.stream.Collectors;

import static com.zarbosoft.shoedemo.ProjectContext.uniqueName1;

public class TrueColorImageNodeWrapper extends Wrapper {
	public final TrueColorImageNode node;
	final TrueColorImageNodeConfig config;
	private final Wrapper parent;
	private final TrueColorImageNode.FramesAddListener framesAddListener;
	private final TrueColorImageNode.FramesRemoveListener framesRemoveListener;
	private final TrueColorImageNode.FramesMoveToListener framesMoveListener;
	Tool tool = null;

	// Cache values when there's no canvas
	DoubleRectangle bounds = new DoubleRectangle(0,0 ,0 ,0 );
	private int frameNumber = 0;
	SimpleIntegerProperty zoom = new SimpleIntegerProperty(1);

	TrueColorImageFrame frame;
	TrueColorImageCanvasHandle canvasHandle;

	public TrueColorImageNodeWrapper(ProjectContext context, Wrapper parent, int parentIndex, TrueColorImageNode node) {
		this.node = node;
		this.config = (TrueColorImageNodeConfig) context.config.nodes.computeIfAbsent(node.id(), id -> new TrueColorImageNodeConfig());
		this.parent = parent;
		this.parentIndex = parentIndex;
		tree.set(new TreeItem<>(this));
		this.framesAddListener = node.addFramesAddListeners((target, at, value) -> setFrame(context, frameNumber));
		this.framesRemoveListener =
				node.addFramesRemoveListeners((target, at, count) -> setFrame(context, frameNumber));
		this.framesMoveListener =
				node.addFramesMoveToListeners((target, source, count, dest) -> setFrame(context, frameNumber));
	}

	@Override
	public Wrapper getParent() {
		return parent;
	}

	@Override
	public DoubleVector toInner(DoubleVector vector) {
		return vector;
	}

	@Override
	public ProjectObject getValue() {
		return node;
	}

	@Override
	public NodeConfig getConfig() {
		return config;
	}

	@Override
	public void setViewport(ProjectContext context, DoubleRectangle newBounds1, int zoom) {
		System.out.format("image scroll; b %s\n", newBounds1);
		DoubleRectangle oldBounds1 = this.bounds;
		this.bounds = newBounds1;
		boolean zoomChanged = this.zoom.get() == zoom;
		this.zoom.set(zoom);
		if (canvasHandle == null)
			return;
		canvasHandle.updateViewport(context, oldBounds1, newBounds1, zoomChanged);
	}

	@Override
	public WidgetHandle buildCanvas(ProjectContext context) {
		System.out.format("building image canvas\n");
		return this.canvasHandle = new TrueColorImageCanvasHandle(context,this);
	}

	@Override
	public void cursorMoved(ProjectContext context, DoubleVector vector) {

	}

	@Override
	public EditControlsHandle buildEditControls(ProjectContext context, TabPane tabPane) {
		return new TrueColorImageEditHandle(context, this, tabPane);
	}

	@Override
	public void markStart(ProjectContext context, DoubleVector start) {
		if (tool == null)
			return;
		start = Window.toLocal(this, start);
		tool.markStart(context, start);
	}

	@Override
	public void mark(ProjectContext context, DoubleVector start, DoubleVector end) {
		if (tool == null)
			return;
		start = Window.toLocal(this, start);
		end = Window.toLocal(this, end);
		tool.mark(context, start, end);
	}

	@Override
	public boolean addChildren(ProjectContext context, int at, List<ProjectNode> child) {
		return false;
	}

	@Override
	public void delete(ProjectContext context) {
		if (parent != null)
			parent.removeChild(context, parentIndex);
		else
			context.history.change(c -> c.project(context.project).topRemove(parentIndex, 1));
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

	public Rectangle render(ProjectContext context, TrueColorImage gc, Rectangle crop) {
		Rectangle tileBounds = crop.quantize(context.tileSize);
		//System.out.format("render tb %s\n", tileBounds);
		for (int x = 0; x < tileBounds.width; ++x) {
			for (int y = 0; y < tileBounds.height; ++y) {
				Tile tile = (Tile) frame.tilesGet(tileBounds.corner().plus(x, y).to1D());
				if (tile == null)
					continue;
				final int renderX = (x + tileBounds.x) * context.tileSize - crop.x;
				final int renderY = (y + tileBounds.y) * context.tileSize - crop.y;
				//System.out.format("composing at %s %s op %s\n", renderX, renderY, opacity);
				System.out.flush();
				TrueColorImage data = tile.getData(context);
				gc.compose(data, renderX, renderY, (float) 1);
			}
		}
		return tileBounds;
	}

	TrueColorImageFrame findFrame(int frameNumber) {
		return findFrame(node, frameNumber).frame;
	}

	public void drop(ProjectContext context,Rectangle unitBounds, TrueColorImage image) {
		for (int x = 0; x < unitBounds.width; ++x) {
			for (int y = 0; y < unitBounds.height; ++y) {
				final int x0 = x;
				final int y0 = y;
				System.out.format("\tcopy %s %s: %s %s by %s %s\n",
						x0,
						y0,
						x0 * context.tileSize,
						y0 * context.tileSize,
						context.tileSize,
						context.tileSize
				);
				TrueColorImage shot =
						image.copy(x0 * context.tileSize, y0 * context.tileSize, context.tileSize, context.tileSize);
				context.history.change(c -> c
						.trueColorImageFrame(frame)
						.tilesPut(unitBounds.corner().plus(x0, y0).to1D(), Tile.create(context, shot)));
			}
		}
	}

	void clear(ProjectContext context, Rectangle bounds) {
		Rectangle quantizedBounds = bounds.quantize(context.tileSize);
		Rectangle clearBounds = quantizedBounds.multiply(context.tileSize);
		TrueColorImage clearCanvas = TrueColorImage.create(clearBounds.width, clearBounds.height);
		render(context, clearCanvas, clearBounds);
		Vector offset = bounds.corner().minus(clearBounds.corner());
		clearCanvas.clear(offset.x, offset.y, bounds.width, bounds.height);
		drop(context, quantizedBounds, clearCanvas);
	}

	public static class FrameResult {
		public final TrueColorImageFrame frame;
		public final int at;
		public final int frameIndex;

		public FrameResult(TrueColorImageFrame frame, int at, int frameIndex) {
			this.frame = frame;
			this.at = at;
			this.frameIndex = frameIndex;
		}
	}

	public static FrameResult findFrame(TrueColorImageNode node, int frame) {
		int at = 0;
		for (int i = 0; i < node.framesLength(); ++i) {
			TrueColorImageFrame pos = node.frames().get(i);
			if ((i == node.framesLength() - 1) || (frame >= at && (pos.length() == -1 || frame < at + pos.length()))) {
				return new FrameResult(pos, at, i);
			}
			at += pos.length();
		}
		throw new Assertion();
	}

	@Override
	public void setFrame(ProjectContext context, int frameNumber) {
		this.frameNumber = frameNumber;
		TrueColorImageFrame oldFrame = frame;
		frame = findFrame(frameNumber);
		System.out.format("set frame %s: %s vs %s\n", frameNumber, oldFrame, frame);
		if (canvasHandle == null) return;
		if (oldFrame != frame) canvasHandle.updateFrame(context);
	}

	@Override
	public void remove(ProjectContext context) {
		node.removeFramesAddListeners(framesAddListener);
		node.removeFramesRemoveListeners(framesRemoveListener);
		node.removeFramesMoveToListeners(framesMoveListener);
		context.config.nodes.remove(node.id());
	}

	@Override
	public void removeChild(ProjectContext context, int index) {
		throw new Assertion();
	}

	@Override
	public TakesChildren takesChildren() {
		return TakesChildren.NONE;
	}
}
