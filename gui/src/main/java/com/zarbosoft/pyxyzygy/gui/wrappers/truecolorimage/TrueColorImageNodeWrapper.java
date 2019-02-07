package com.zarbosoft.pyxyzygy.gui.wrappers.truecolorimage;

import com.zarbosoft.pyxyzygy.CanvasHandle;
import com.zarbosoft.pyxyzygy.EditHandle;
import com.zarbosoft.pyxyzygy.ProjectContext;
import com.zarbosoft.pyxyzygy.Wrapper;
import com.zarbosoft.pyxyzygy.config.NodeConfig;
import com.zarbosoft.pyxyzygy.config.TrueColorImageNodeConfig;
import com.zarbosoft.pyxyzygy.model.ProjectNode;
import com.zarbosoft.pyxyzygy.model.ProjectObject;
import com.zarbosoft.pyxyzygy.model.TrueColorImageFrame;
import com.zarbosoft.pyxyzygy.model.TrueColorImageNode;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.scene.control.TabPane;
import javafx.scene.control.TreeItem;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zarbosoft.pyxyzygy.ProjectContext.uniqueName1;

public class TrueColorImageNodeWrapper extends Wrapper {
	public final TrueColorImageNode node;
	final TrueColorImageNodeConfig config;
	private final Wrapper parent;
	public TrueColorImageCanvasHandle canvasHandle;

	// Cache values when there's no canvas
	public TrueColorImageNodeWrapper(ProjectContext context, Wrapper parent, int parentIndex, TrueColorImageNode node) {
		this.node = node;
		this.config = (TrueColorImageNodeConfig) context.config.nodes.computeIfAbsent(node.id(),
				id -> new TrueColorImageNodeConfig(context)
		);
		this.parent = parent;
		this.parentIndex = parentIndex;
		tree.set(new TreeItem<>(this));
	}

	@Override
	public Wrapper getParent() {
		return parent;
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
	public CanvasHandle buildCanvas(ProjectContext context, CanvasHandle parent) {
		if (canvasHandle == null)
			canvasHandle = new TrueColorImageCanvasHandle(context, parent, this);
		return canvasHandle;
	}

	@Override
	public EditHandle buildEditControls(ProjectContext context, TabPane tabPane) {
		return new TrueColorImageEditHandle(context, this, tabPane);
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
	public void remove(ProjectContext context) {
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
