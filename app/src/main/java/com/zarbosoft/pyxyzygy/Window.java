package com.zarbosoft.pyxyzygy;

import com.zarbosoft.pyxyzygy.config.TrueColor;
import com.zarbosoft.pyxyzygy.model.*;
import com.zarbosoft.pyxyzygy.parts.editor.Editor;
import com.zarbosoft.pyxyzygy.parts.structure.Structure;
import com.zarbosoft.pyxyzygy.parts.timeline.Timeline;
import com.zarbosoft.pyxyzygy.widgets.TitledPane;
import com.zarbosoft.pyxyzygy.widgets.TrueColorPicker;
import com.zarbosoft.pyxyzygy.widgets.WidgetFormBuilder;
import com.zarbosoft.pyxyzygy.wrappers.camera.CameraWrapper;
import com.zarbosoft.pyxyzygy.wrappers.group.GroupLayerWrapper;
import com.zarbosoft.pyxyzygy.wrappers.group.GroupNodeWrapper;
import com.zarbosoft.pyxyzygy.wrappers.truecolorimage.TrueColorImageNodeWrapper;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.zarbosoft.pyxyzygy.Launch.nameHuman;

public class Window {
	public List<FrameMapEntry> timeMap;
	public SimpleObjectProperty<EditHandle> selectedForEdit = new SimpleObjectProperty<>();
	public SimpleObjectProperty<CanvasHandle> selectedForView = new SimpleObjectProperty<>();
	public TabPane leftTabs;
	public Set<KeyCode> pressed = new HashSet<>();
	public Editor editor;

	public void start(ProjectContext context, Stage primaryStage) {
		primaryStage.setOnCloseRequest(e -> {
			context.shutdown();
		});

		Stream
				.of(new Hotkeys.Action(Hotkeys.Scope.GLOBAL,
							"undo",
							"Undo",
							Hotkeys.Hotkey.create(KeyCode.Z, true, false, false)
					) {
						@Override
						public void run(ProjectContext context) {
							context.history.undo();
						}
					},
					new Hotkeys.Action(
							Hotkeys.Scope.GLOBAL,
							"redo",
							"Redo",
							Hotkeys.Hotkey.create(KeyCode.Y, true, false, false)
					) {

						@Override
						public void run(ProjectContext context) {
							context.history.redo();
						}
					})
				.forEach(context.hotkeys::register);

		leftTabs = new TabPane();
		leftTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
		final Tab structureTab = new Tab("Structure");
		final Tab configTab = new Tab("Config");
		leftTabs.getTabs().addAll(structureTab, configTab);

		Structure structure = new Structure(context, this);
		structureTab.setContent(structure.getWidget());

		editor = new Editor(context, this);
		Timeline timeline = new Timeline(context, this);

		SplitPane specificLayout = new SplitPane();
		specificLayout.setOrientation(Orientation.VERTICAL);
		specificLayout.getItems().addAll(editor.getWidget());
		SplitPane.setResizableWithParent(timeline.getWidget(), false);

		VBox configLayout = new VBox();
		configLayout.setSpacing(3);
		configLayout
				.getChildren()
				.addAll(new TitledPane("Project", new WidgetFormBuilder().twoLine("Background color", () -> {
							TrueColorPicker w = new TrueColorPicker();
							w.colorProxyProperty.set(context.config.backgroundColor.get().toJfx());
							w.colorProxyProperty.addListener((observable, oldValue, newValue) -> context.config.backgroundColor.set(
									TrueColor.fromJfx(newValue)));
							return w;
						}).build()),
						new TitledPane("Global", new WidgetFormBuilder().intSpinner("Max undo", 1, 100000, spinner -> {
							spinner.getValueFactory().setValue(Launch.config.maxUndo);
							spinner
									.getValueFactory()
									.valueProperty()
									.addListener((observable, oldValue, newValue) -> Launch.config.maxUndo = newValue);
						}).button(button -> {
							button.setText("Clear undo/redo");
							button.setOnAction(e -> {
								Alert confirm = new Alert(
										Alert.AlertType.CONFIRMATION,
										"Are you sure you wish to clear undo/redo?"
								);
								confirm.showAndWait().filter(b -> b == ButtonType.OK).ifPresent(x -> {
									context.history.clearHistory();
								});
							});
						}).check("Show origin", checkBox -> {
							checkBox.selectedProperty().bindBidirectional(Launch.config.showOrigin);
						}).build()),
						new TitledPane("Hotkeys", (
								(Supplier<Node>) () -> {
									TableColumn<Hotkeys.Action, String> scope = new TableColumn("Scope");
									scope.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().scope.name()));
									TableColumn<Hotkeys.Action, String> description = new TableColumn("Description");
									description.setCellValueFactory(param -> new SimpleStringProperty(param.getValue().description));
									TableColumn<Hotkeys.Action, String> key = new TableColumn("Key");
									key.setCellValueFactory(param -> param.getValue().key.asString());
									TableView<Hotkeys.Action> table = new TableView<>();
									table.getColumns().addAll(scope, description, key);
									table.setItems(context.hotkeys.actions);
									return table;
								}
						).get())
				);
		configTab.setContent(configLayout);

		SplitPane generalLayout = new SplitPane();
		generalLayout.setOrientation(Orientation.HORIZONTAL);
		generalLayout.getItems().addAll(specificLayout);
		SplitPane.setResizableWithParent(leftTabs, false);

		Scene scene = new Scene(generalLayout, 1200, 800);

		context.config.maxCanvas.addListener(new ChangeListener<Boolean>() {
			{
				changed(null,null,false);
			}
			@Override
			public void changed(
					ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue
			) {
				if (newValue) {
					generalLayout.getItems().remove(leftTabs);
					specificLayout.getItems().remove(timeline.getWidget());
				} else {
					specificLayout.getItems().add(1, timeline.getWidget());
					specificLayout.setDividerPositions(context.config.timelineSplit);
					generalLayout.getItems().add(0, leftTabs);
					generalLayout.setDividerPositions(context.config.tabsSplit);
				}
			}
		});

		specificLayout
				.getDividers()
				.get(0)
				.positionProperty()
				.addListener((observable, oldValue, newValue) -> context.config.timelineSplit = newValue.doubleValue());
		generalLayout
				.getDividers()
				.get(0)
				.positionProperty()
				.addListener((observable, oldValue, newValue) -> context.config.tabsSplit = newValue.doubleValue());

		scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			pressed.add(e.getCode());
			System.out.format("(on) pressed: %s\n", pressed);
			if (context.hotkeys.event(context, Hotkeys.Scope.GLOBAL, e))
				e.consume();
		});
		scene.addEventFilter(KeyEvent.KEY_RELEASED,e -> {
			pressed.remove(e.getCode());
			System.out.format("(off) pressed: %s\n", pressed);
		});
		scene.getStylesheets().addAll(getClass().getResource("widgets/colorpicker/style.css").toExternalForm(),
				getClass().getResource("widgets/brushbutton/style.css").toExternalForm()
		);

		selectedForEdit.addListener((observable, oldValue, newValue) -> {
			if (oldValue != null) {
				oldValue.remove(context);
			}
		});

		primaryStage.setTitle(String.format("%s - %s", context.path.getFileName().toString(), nameHuman));
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

	public static List<CanvasHandle> getAncestorsOutward(CanvasHandle start, CanvasHandle target) {
		List<CanvasHandle> ancestors = new ArrayList<>();
		CanvasHandle at = target.getParent();
		while (at != start) {
			ancestors.add(at);
			at = at.getParent();
		}
		return ancestors;
	}
	public static List<CanvasHandle> getAncestorsInward(CanvasHandle start, CanvasHandle target) {
		List<CanvasHandle> ancestors = getAncestorsOutward(start, target);
		Collections.reverse(ancestors);
		return ancestors;
	}

	public static DoubleVector toLocal(CanvasHandle wrapper, DoubleVector v) {
		for (CanvasHandle parent : getAncestorsInward(null, wrapper)) {
			v = parent.toInner(v);
		}
		return v;
	}

	/**
	 * Not really global but canvas-space
	 * @param wrapper
	 * @param v
	 * @return
	 */
	public static DoubleVector toGlobal(CanvasHandle wrapper, DoubleVector v) {
		DoubleVector zero = new DoubleVector(0,0);
		for (CanvasHandle parent : getAncestorsOutward(null, wrapper)) {
			v = v.minus(parent.toInner(zero));
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
