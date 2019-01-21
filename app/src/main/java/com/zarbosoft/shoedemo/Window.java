package com.zarbosoft.shoedemo;

import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;
import com.zarbosoft.shoedemo.model.*;
import com.zarbosoft.shoedemo.parts.editor.Editor;
import com.zarbosoft.shoedemo.parts.timeline.Timeline;
import com.zarbosoft.shoedemo.structuretree.CameraWrapper;
import com.zarbosoft.shoedemo.wrappers.group.GroupLayerWrapper;
import com.zarbosoft.shoedemo.wrappers.group.GroupNodeWrapper;
import com.zarbosoft.shoedemo.wrappers.truecolorimage.TrueColorImageNodeWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Window {
	public List<FrameMapEntry> timeMap;
	public SimpleObjectProperty<Wrapper> selectedForEdit = new SimpleObjectProperty<>();
	public SimpleObjectProperty<Wrapper> selectedForView = new SimpleObjectProperty<>();
	private Wrapper.EditControlsHandle editPropertiesHandle;

	public void start(ProjectContext context, Stage primaryStage) {
		primaryStage.setOnCloseRequest(e -> {
			context.shutdown();
		});

		Structure structure = new Structure(context, this);

		Editor editor = new Editor(context, this);
		Timeline timeline = new Timeline(context, this);

		SplitPane specificLayout = new SplitPane();
		specificLayout.setOrientation(Orientation.VERTICAL);
		specificLayout.getItems().addAll(editor.getWidget(), timeline.getWidget());
		SplitPane.setResizableWithParent(timeline.getWidget(), false);
		specificLayout.setDividerPositions(0.7);

		TabPane leftTabs = new TabPane();
		leftTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		leftTabs.getTabs().add(new Tab("Structure",structure.getWidget() ));

		SplitPane generalLayout = new SplitPane();
		generalLayout.setOrientation(Orientation.HORIZONTAL);
		generalLayout.getItems().addAll(leftTabs, specificLayout);
		SplitPane.setResizableWithParent(leftTabs, false);
		generalLayout.setDividerPositions(0.3);

		Scene scene = new Scene(generalLayout, 1200, 800);

		scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.U || (e.isControlDown() && e.getCode() == KeyCode.Z)) {
				context.history.undo();
			} else if (e.isControlDown() && (e.getCode() == KeyCode.R || e.getCode() == KeyCode.Y)) {
				context.history.redo();
			}
		});
		scene.getStylesheets().add(getClass().getResource("widgets/colorpicker/style.css").toExternalForm());
		scene.getStylesheets().add(getClass().getResource("widgets/brushbutton/style.css").toExternalForm());

		selectedForEdit.addListener(new ChangeListener<Wrapper>() {
			{
				changed(null, null, selectedForEdit.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends Wrapper> observable, Wrapper oldValue, Wrapper newValue
			) {
				if (editPropertiesHandle != null) {
					editPropertiesHandle.remove(context);
					editPropertiesHandle = null;
				}
				if (newValue != null) {
					editPropertiesHandle = newValue.buildEditControls(context, leftTabs);
					editor.setEdit(newValue, editPropertiesHandle);
				}
			}
		});

		primaryStage.setTitle(String.format("%s - Shoe Demo 2", context.path.getFileName().toString()));
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	public Pair<Integer, FrameMapEntry> findTimeMapEntry(int outer) {
		int outerAt = 0;
		for (FrameMapEntry outerFrame : timeMap) {
			if (outer >= outerAt && (outerFrame.length == -1 || outer < outerAt + outerFrame.length)) {
				return new Pair<>(outerAt, outerFrame);
			}
			outerAt += outerFrame.length;
		}
		throw new Assertion();
	}

	public int timeToInner(int outer) {
		Pair<Integer, FrameMapEntry> entry = findTimeMapEntry(outer);
		if (entry.second.innerOffset == -1)
			return -1;
		return entry.second.innerOffset + outer - entry.first;
	}

	public static List<Wrapper> getAncestors(Wrapper start, Wrapper target) {
		List<Wrapper> ancestors = new ArrayList<>();
		Wrapper at = target.getParent();
		while (at != start) {
			ancestors.add(at);
			at = at.getParent();
		}
		Collections.reverse(ancestors);
		return ancestors;
	}

	public static DoubleVector toLocal(Wrapper wrapper, DoubleVector v) {
		for (Wrapper parent : getAncestors(null, wrapper)) {
			v = parent.toInner(v);
		}
		return v;
	}

	public static Wrapper createNode(ProjectContext context, Wrapper parent, int parentIndex, ProjectObject node) {
		if (false) {
			throw new Assertion();
		} else if (node instanceof Camera) {
			return new CameraWrapper(context, parent, parentIndex, (Camera) node);
		} else if (node instanceof GroupNode) {
			return new GroupNodeWrapper(context, parent, parentIndex, (GroupNode) node);
		} else if (node instanceof GroupLayer) {
			return new GroupLayerWrapper(context, parent, parentIndex, (GroupLayer) node);
		} else if (node instanceof TrueColorImageNode) {
			return new TrueColorImageNodeWrapper(context, parent, parentIndex, (TrueColorImageNode) node);
		} else
			throw new Assertion();
	}

}
