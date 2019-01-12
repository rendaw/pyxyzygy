package com.zarbosoft.shoedemo;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.rendaw.common.ChainComparator;
import com.zarbosoft.shoedemo.model.*;
import com.zarbosoft.shoedemo.model.Vector;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.zarbosoft.rendaw.common.Common.last;
import static com.zarbosoft.shoedemo.Main.moveTo;
import static com.zarbosoft.shoedemo.Main.opacityMax;
import static com.zarbosoft.shoedemo.Window.icon;
import static com.zarbosoft.shoedemo.ProjectContext.uniqueName;
import static com.zarbosoft.shoedemo.Wrapper.TakesChildren.NONE;

public class Structure {
	private final ProjectContext context;
	private final Window window;
	private WidgetHandle properties;
	VBox layout = new VBox();
	TreeView<Wrapper> treeView;
	ScrollPane treeScroll = new ScrollPane();
	ToolBar toolbar;
	SplitPane split = new SplitPane();
	Set<Wrapper> taggedViewing = new HashSet<>();
	Set<Wrapper> taggedLifted = new HashSet<>();
	Set<Wrapper> taggedCopied = new HashSet<>();

	public void selectForEdit(Wrapper wrapper) {
		Wrapper preParent = wrapper;
		Wrapper parent = wrapper.getParent();
		boolean found = false;
		while (parent != null) {
			if (window.selectedForView.getValue() == parent) {
				found = true;
				break;
			}
			preParent = parent;
			parent = parent.getParent();
		}
		if (found) {
			window.selectedForEdit.set(wrapper);
		} else {
			window.selectedForEdit.set(null);
			selectForView(preParent);
			window.selectedForEdit.set(wrapper);
		}
	}

	public void selectForView(Wrapper wrapper) {
		for (Wrapper w : taggedViewing)
			w.tagViewing.set(false);
		taggedViewing.clear();
		wrapper.tagViewing.set(true);
		taggedViewing.add(wrapper);
		window.selectedForView.set(wrapper);
	}

	public void treeItemAdded(TreeItem<Wrapper> item) {
		treeView.getSelectionModel().select(item);
	}

	public void treeItemRemoved(TreeItem<Wrapper> item) {
		Wrapper wrapper = item.getValue();
		if (wrapper != null) {
			if (wrapper.tagCopied.get())
				taggedCopied.remove(wrapper);
			if (wrapper.tagLifted.get())
				taggedLifted.remove(wrapper);
			if (wrapper.tagViewing.get())
				taggedViewing.remove(wrapper);
		}
	}

	private void prepareTreeItem(TreeItem<Wrapper> item) {
		item.setExpanded(true);
		item.getChildren().forEach(c -> prepareTreeItem(c));
		item.getChildren().addListener((ListChangeListener<? super TreeItem<Wrapper>>) c -> {
			while (c.next()) {
				if (!c.getAddedSubList().isEmpty()) {
					c.getAddedSubList().forEach(a -> {
						prepareTreeItem(a);
						treeItemAdded(a);
					});
				}
				c.getRemoved().forEach(r -> treeItemRemoved(r));
			}
		});
	}

	Structure(ProjectContext context, Window window) {
		this.context = context;
		this.window = window;
		treeView = new TreeView();
		treeView.setShowRoot(false);
		TreeItem<Wrapper> rootTreeItem = new TreeItem<>();
		prepareTreeItem(rootTreeItem);
		treeView.setRoot(rootTreeItem);
		treeView.setMinHeight(0);
		treeView.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.isControlDown()) {
				switch (e.getCode()) {
					case C:
						link();
						break;
					case X:
						lift();
						break;
					case V:
						placeAuto();
						break;
					case D:
						duplicate();
						break;
				}
			} else {
				switch (e.getCode()) {
					case DELETE:
						delete(context);
						break;
				}
			}
		});
		treeView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<TreeItem<Wrapper>>) c -> {
			while (c.next()) {
				List<? extends TreeItem<Wrapper>> added = c.getAddedSubList();
				System.out.format("select added %s\n", added.size());
				if (added.isEmpty())
					return;
				TreeItem<Wrapper> first = added.get(0);
				System.out.format("select added first %s\n", first);
				if (first.getValue() == null)
					return;
				selectForEdit(first.getValue());
			}
		});
		treeView.setCellFactory((TreeView<Wrapper> param) -> {
			return new TreeCell<Wrapper>() {
				final ImageView showViewing = new ImageView();
				final ImageView showGrabState = new ImageView();
				final SimpleObjectProperty<Wrapper> wrapper = new SimpleObjectProperty<>(null);
				ChangeListener<Boolean> viewingListener = (observable, oldValue, newValue) -> {
					if (newValue)
						showViewing.setImage(icon("eye.svg"));
					else
						showViewing.setImage(null);
				};
				ChangeListener<Boolean> copyStateListener = (observable, oldValue, newValue) -> {
					if (wrapper.get().tagCopied.get())
						showGrabState.setImage(icon("content-copy.svg"));
					else if (wrapper.get().tagLifted.get())
						showGrabState.setImage(icon("content-cut.svg"));
					else
						showGrabState.setImage(null);
				};
				ProjectNode.NameSetListener nameSetListener = (target, value) -> {
					setText(value);
				};
				final HBox graphic = new HBox();
				Runnable cleanup;

				{
					addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
						if (wrapper.getValue() == null)
							return;
						if (e.getClickCount() >= 2) {
							selectForView(wrapper.getValue());
						}
					});
					MenuItem setView = new MenuItem("Set View");
					setView.disableProperty().bind(Bindings.isNull(wrapper));
					setView.setOnAction(e -> {
						selectForView(wrapper.getValue());
					});
					wrapper.addListener((observable, oldValue, newValue) -> {
						if (oldValue != null) {
							oldValue.tagViewing.removeListener(viewingListener);
							oldValue.tagLifted.removeListener(copyStateListener);
							oldValue.tagCopied.removeListener(copyStateListener);
							((ProjectNode) oldValue.getValue()).removeNameSetListeners(nameSetListener);
						}
						if (newValue != null) {
							newValue.tagViewing.addListener(viewingListener);
							viewingListener.changed(null, null, newValue.tagViewing.get());
							newValue.tagCopied.addListener(copyStateListener);
							newValue.tagLifted.addListener(copyStateListener);
							copyStateListener.changed(null, null, null);
							((ProjectNode) newValue.getValue()).addNameSetListeners(nameSetListener);
						} else {
							setText("");
							showViewing.setImage(null);
							showGrabState.setImage(null);
						}
					});
					setContextMenu(new ContextMenu(setView));
					showViewing.setFitHeight(16);
					showViewing.setFitWidth(16);
					showViewing.setPreserveRatio(true);
					showGrabState.setFitHeight(16);
					showGrabState.setFitWidth(16);
					showGrabState.setPreserveRatio(true);
					graphic.getChildren().addAll(showViewing, showGrabState);
					setGraphic(graphic);
				}

				@Override
				protected void updateItem(Wrapper item, boolean empty) {
					if (cleanup != null) {
						cleanup.run();
						cleanup = null;
					}
					wrapper.set(item);
					super.updateItem(item, empty);
				}
			};
		});
		treeScroll.setContent(treeView);
		treeScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
		treeScroll.setFitToHeight(true);
		treeScroll.setFitToWidth(true);
		MenuItem addCamera = new MenuItem("Add Camera");
		addCamera.setOnAction(e -> {
			Camera camera = Camera.create(context);
			camera.initialNameSet(context, uniqueName("Camera"));
			camera.initialOpacitySet(context, opacityMax);
			camera.initialEndSet(context, 50);
			camera.initialFrameRateSet(context, 120);
			camera.initialHeightSet(context, 240);
			camera.initialWidthSet(context, 320);
			context.history.change(c -> c.project(context.project).topAdd(camera));
		});
		MenuItem addGroup = new MenuItem("Add Group");
		addGroup.setOnAction(e -> {
			GroupNode group = GroupNode.create(context);
			group.initialOpacitySet(context, opacityMax);
			group.initialNameSet(context, uniqueName("Group"));
			addNew(group);
		});
		MenuItem addImage = new MenuItem("Add Image");
		addImage.setOnAction(e -> {
			ImageNode image = ImageNode.create(context);
			image.initialOpacitySet(context, opacityMax);
			image.initialNameSet(context, uniqueName("Image"));
			ImageFrame frame = ImageFrame.create(context);
			frame.initialLengthSet(context, -1);
			frame.initialOffsetSet(context, new Vector(0, 0));
			image.initialFramesAdd(context, ImmutableList.of(frame));
			addNew(image);
		});
		MenuButton addButton = Window.menuButton("plus.svg");
		addButton.getItems().addAll(addCamera, addGroup, addImage);
		Button removeButton = Window.button("minus.svg", "Remove");
		removeButton.disableProperty().bind(Bindings.isEmpty(treeView.getSelectionModel().getSelectedIndices()));
		removeButton.setOnAction(e -> {
			delete(context);
		});
		Button moveUpButton = Window.button("arrow-up.svg", "Move Up");
		moveUpButton.disableProperty().bind(Bindings.isEmpty(treeView.getSelectionModel().getSelectedIndices()));
		moveUpButton.setOnAction(e -> {
			List<TreeItem<Wrapper>> selected = treeView.getSelectionModel().getSelectedItems();
			TreeItem<Wrapper> firstParent = selected.get(0).getParent();
			List<TreeItem<Wrapper>> removeOrder = selected
					.stream()
					.filter(s -> s.getParent() == firstParent)
					.sorted(new ChainComparator<TreeItem<Wrapper>>()
							.greaterFirst(s -> s.getValue().parentIndex)
							.build())
					.collect(Collectors.toList());
			int dest = removeOrder.get(0).getValue().parentIndex;
			if (dest == 0)
				return;
			dest -= 1;
			List<ProjectNode> add =
					removeOrder.stream().map(s -> (ProjectNode) s.getValue().getValue()).collect(Collectors.toList());
			removeOrder.forEach(s -> s.getValue().delete(context));
			if (firstParent.getValue() != null) {
				firstParent.getValue().addChildren(context, dest, add);
			} else {
				final int dest1 = dest;
				context.history.change(c -> c.project(context.project).topAdd(dest1, add));
			}
			context.history.finishChange();
		});
		Button moveDownButton = Window.button("arrow-down.svg", "Move Down");
		moveDownButton.disableProperty().bind(Bindings.isEmpty(treeView.getSelectionModel().getSelectedIndices()));
		moveDownButton.setOnAction(e -> {
			List<TreeItem<Wrapper>> selected = treeView.getSelectionModel().getSelectedItems();
			TreeItem<Wrapper> firstParent = selected.get(0).getParent();
			List<TreeItem<Wrapper>> removeOrder = selected
					.stream()
					.filter(s -> s.getParent() == firstParent)
					.sorted(new ChainComparator<TreeItem<Wrapper>>()
							.greaterFirst(s -> s.getValue().parentIndex)
							.build())
					.collect(Collectors.toList());
			int dest = last(removeOrder).getValue().parentIndex;
			if (dest == firstParent.getChildren().size() - 1)
				return;
			dest = dest - removeOrder.size() + 1;
			List<ProjectNode> add =
					removeOrder.stream().map(s -> (ProjectNode) s.getValue().getValue()).collect(Collectors.toList());
			removeOrder.forEach(s -> s.getValue().delete(context));
			if (firstParent.getValue() != null) {
				firstParent.getValue().addChildren(context, dest, add);
			} else {
				final int dest1 = dest;
				context.history.change(c -> c.project(context.project).topAdd(dest1, add));
			}
			context.history.finishChange();
		});
		Button duplicateButton = Window.button("content-copy.svg", "Duplicate");
		duplicateButton.disableProperty().bind(Bindings.isEmpty(treeView.getSelectionModel().getSelectedIndices()));
		duplicateButton.setOnAction(e -> {
			duplicate();
		});
		MenuItem linkBeforeButton = new MenuItem("Before");
		linkBeforeButton.setOnAction(e -> {
			placeBefore();
		});
		MenuItem linkInButton = new MenuItem("In");
		linkInButton.setOnAction(e -> {
			placeIn();
		});
		MenuItem linkAfterButton = new MenuItem("After");
		linkAfterButton.setOnAction(e -> {
			placeAfter();
		});
		MenuButton linkButton = Window.menuButton("content-paste.svg");
		linkButton.getItems().addAll(linkBeforeButton, linkInButton, linkAfterButton);
		toolbar = new ToolBar(addButton, duplicateButton, removeButton, moveUpButton, moveDownButton, linkButton);
		layout.getChildren().addAll(toolbar, treeScroll);
		VBox.setVgrow(treeScroll, Priority.ALWAYS);

		split.setOrientation(Orientation.VERTICAL);
		split.getItems().add(layout);
		split.setDividerPositions(1);

		context.project.addTopAddListeners((target, at, value) -> {
			List<TreeItem<Wrapper>> newItems = new ArrayList<>();
			for (int i = 0; i < value.size(); ++i) {
				ProjectNode v = value.get(i);
				Wrapper child = Window.createNode(context, null, at + i, v);
				child.tree.addListener((observable, oldValue, newValue) -> {
					treeView.getRoot().getChildren().set(child.parentIndex, newValue);
				});
				newItems.add(child.tree.getValue());
			}
			treeView.getRoot().getChildren().addAll(at, newItems);
		});
		context.project.addTopRemoveListeners((target, at, count) -> {
			List<TreeItem<Wrapper>> temp = treeView.getRoot().getChildren().subList(at, at + count);
			temp.forEach(i -> i.getValue().remove(context));
			temp.clear();
			for (int i = at; i < treeView.getRoot().getChildren().size(); ++i) {
				treeView.getRoot().getChildren().get(i).getValue().parentIndex = at + i;
			}
		});
		context.project.addTopMoveToListeners((target, source, count, dest) -> {
			moveTo(treeView.getRoot().getChildren(), source, count, dest);
			for (int i = Math.min(source, dest); i < treeView.getRoot().getChildren().size(); ++i)
				treeView.getRoot().getChildren().get(i).getValue().parentIndex = i;
		});
		context.project.addTopClearListeners(target -> {
			treeView.getRoot().getChildren().forEach(c -> c.getValue().remove(context));
			treeView.getRoot().getChildren().clear();
		});
		window.selectedForEdit.addListener(new ChangeListener<Wrapper>() {
			{
				changed(null, null, window.selectedForEdit.get());
			}

			@Override
			public void changed(
					ObservableValue<? extends Wrapper> observable, Wrapper oldValue, Wrapper newValue
			) {
				if (properties != null) {
					properties.remove();
				}
				if (oldValue != null) {
					split.getItems().remove(1);
					split.setDividerPositions(1);
				}
				if (newValue != null) {
					VBox wrapper = new VBox();
					wrapper.setPadding(new Insets(3, 3, 3, 3));
					properties = newValue.createProperties(context);
					wrapper.getChildren().add(properties.getWidget());
					split.getItems().add(wrapper);
					split.setDividerPositions(0.7);
					SplitPane.setResizableWithParent(wrapper, false);
				}
			}
		});
	}

	private void delete(ProjectContext context) {
		Wrapper edit = window.selectedForEdit.get();
		if (edit == null)
			return;
		edit.delete(context);
		context.history.finishChange();
	}

	private void placeAfter() {
		Wrapper destination = window.selectedForEdit.get();
		if (destination == null) {
			place(null, false, null);
		} else {
			Wrapper pasteParent = destination.getParent();
			place(pasteParent, false, destination);
		}
	}

	private void placeIn() {
		Wrapper destination = window.selectedForEdit.get();
		if (destination == null) {
			place(null, true, null);
		} else {
			place(destination, true, null);
		}
	}

	private void placeBefore() {
		Wrapper destination = window.selectedForEdit.get();
		if (destination == null) {
			place(null, true, null);
		} else {
			Wrapper pasteParent = destination.getParent();
			place(pasteParent, true, destination);
		}
	}

	private void placeAuto() {
		Wrapper destination = getSelection();
		if (destination == null) {
			placeAfter();
		} else {
			if (destination.takesChildren() == NONE) {
				placeAfter();
			} else {
				placeIn();
			}
		}
	}

	private void link() {
		clearTagLifted();
		clearTagCopied();
		treeView.getSelectionModel().getSelectedItems().stream().forEach(s -> {
			Wrapper wrapper = s.getValue();
			taggedCopied.add(wrapper);
			wrapper.tagCopied.set(true);
		});
	}

	private void clearTagCopied() {
		taggedCopied.forEach(w -> w.tagCopied.set(false));
		taggedCopied.clear();
	}

	private void clearTagLifted() {
		taggedLifted.forEach(w -> w.tagLifted.set(false));
		taggedLifted.clear();
	}

	private void lift() {
		clearTagLifted();
		clearTagCopied();
		treeView.getSelectionModel().getSelectedItems().stream().forEach(s -> {
			Wrapper wrapper = s.getValue();
			taggedLifted.add(wrapper);
			wrapper.tagLifted.set(true);
		});
	}

	private void duplicate() {
		Wrapper destination = window.selectedForEdit.get();
		ProjectNode clone = destination.separateClone(context);
		addNew(clone);
	}

	private Wrapper getSelection() {
		return Optional.ofNullable(treeView.getSelectionModel().getSelectedItem()).map(t -> t.getValue()).orElse(null);
	}

	/**
	 * Adds the single node wherever it can, starting from the selection
	 *
	 * @param node
	 */
	private void addNew(ProjectNode node) {
		Wrapper edit = getSelection();
		Wrapper placeAt = edit;
		int index = 0;
		while (placeAt != null) {
			if (placeAt.addChildren(context, index, ImmutableList.of(node)))
				return;
			index = placeAt.parentIndex + 1;
			placeAt = placeAt.getParent();
		}
		context.history.change(c -> c.project(context.project).topAdd(node));
		context.history.finishChange();
	}

	/**
	 * Places exactly within parent/top before/after reference/start=end
	 *
	 * @param pasteParent
	 * @param before
	 * @param reference
	 */
	private void place(Wrapper pasteParent, boolean before, Wrapper reference) {
		List<ProjectNode> nodes = new ArrayList<>();
		Consumer<Wrapper> check = wrapper -> {
			// Can't place relative to copied element
			if (wrapper == reference)
				return;

			// Omit items that would have infinite recursion if pasted
			if (pasteParent != null) {
				Wrapper parent = pasteParent;
				while (parent != null) {
					if (parent == wrapper)
						return;
					parent = parent.getParent();
				}
			}

			// Register for placing
			nodes.add((ProjectNode) wrapper.getValue());
		};
		taggedLifted.forEach(check);
		taggedCopied.forEach(check);
		if (pasteParent != null) {
			if (reference != null) {
				if (!pasteParent.addChildren(context, reference.parentIndex + (before ? -1 : 1), nodes))
					return;
			} else {
				if (!pasteParent.addChildren(context, before ? 0 : -1, nodes))
					return;
			}
		} else {
			if (reference != null) {
				context.history.change(c -> c
						.project(context.project)
						.topAdd(reference.parentIndex + (before ? -1 : 1), nodes));
			} else {
				if (before)
					context.history.change(c -> c.project(context.project).topAdd(0, nodes));
				else
					context.history.change(c -> c.project(context.project).topAdd(context.project.topLength(), nodes));
			}
		}
		taggedLifted.forEach(c -> c.delete(context));
		context.history.finishChange();
		clearTagCopied();
		clearTagLifted();
	}

	public Node getWidget() {
		return split;
	}
}
