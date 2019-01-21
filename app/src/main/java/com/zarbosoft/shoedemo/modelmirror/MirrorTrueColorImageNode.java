package com.zarbosoft.shoedemo.modelmirror;

import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;
import com.zarbosoft.shoedemo.*;
import com.zarbosoft.shoedemo.config.NodeConfig;
import com.zarbosoft.shoedemo.config.TrueColor;
import com.zarbosoft.shoedemo.config.TrueColorBrush;
import com.zarbosoft.shoedemo.model.*;
import com.zarbosoft.shoedemo.model.Vector;
import com.zarbosoft.shoedemo.widgets.BrushButton;
import com.zarbosoft.shoedemo.widgets.TrueColorPicker;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.transform.Scale;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.stream.Collectors;

import static com.zarbosoft.shoedemo.HelperJFX.icon;
import static com.zarbosoft.shoedemo.HelperJFX.pad;
import static com.zarbosoft.shoedemo.Main.*;
import static com.zarbosoft.shoedemo.ProjectContext.uniqueName;
import static com.zarbosoft.shoedemo.ProjectContext.uniqueName1;

public class MirrorTrueColorImageNode extends ObjectMirror {
	private final TrueColorImageNode node;
	private final ObjectMirror parent;

	public MirrorTrueColorImageNode(ObjectMirror parent, TrueColorImageNode node) {
		this.node = node;
		this.parent = parent;
		this.parentIndex = -1;
		tree.set(new TreeItem<>(this));
	}

	@Override
	public ObjectMirror getParent() {
		return parent;
	}

	@Override
	public ProjectObject getValue() {
		return node;
	}

	@Override
	public void remove(ProjectContext context) {
		context.config.nodes.remove(node.id());
	}
}
