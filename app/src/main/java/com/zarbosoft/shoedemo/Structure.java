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
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.zarbosoft.rendaw.common.Common.last;
import static com.zarbosoft.shoedemo.Main.icon;
import static com.zarbosoft.shoedemo.Timeline.moveTo;

public class Structure {
	private final ProjectContext context;
	VBox layout = new VBox();
	TreeView<Wrapper> treeView;
	ScrollPane treeScroll = new ScrollPane();
	ToolBar toolbar;
	SplitPane split = new SplitPane();
	Set<Wrapper> taggedViewing = new HashSet<>();
	Set<Wrapper> taggedLifted = new HashSet<>();
	Set<Wrapper> taggedCopied = new HashSet<>();

	public void selectForEdit(Wrapper wrapper) {
		context.selectedForEdit.set(wrapper);
		Wrapper preParent = wrapper;
		Wrapper parent = wrapper.getParent();
		boolean found = false;
		while (parent != null) {
			if (context.selectedForView.getValue() == parent) {
				found = true;
				break;
			}
			preParent = parent;
			parent = parent.getParent();
		}
		if (!found) {
			selectForView(preParent);
		}
	}

	public void selectForView(Wrapper wrapper) {
		for (Wrapper w : taggedViewing)
			w.tagViewing.set(false);
		taggedViewing.clear();
		wrapper.tagViewing.set(true);
		taggedViewing.add(wrapper);
		context.selectedForView.set(wrapper);
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
		item.getChildren().addListener((ListChangeListener<? super TreeItem<Wrapper>>)c -> {
			c.next();
			if (!c.getAddedSubList().isEmpty()) {
				item.setExpanded(true);
				c.getAddedSubList().forEach(a -> {
					prepareTreeItem(a);
					treeItemAdded(a);
				});
			}
			c.getRemoved().forEach(r -> treeItemRemoved(r));
		});
	}

	Structure(ProjectContext context) {
		this.context = context;
		treeView = new TreeView();
		treeView.setShowRoot(false);
		TreeItem<Wrapper> rootTreeItem = new TreeItem<>();
		prepareTreeItem(rootTreeItem);
		treeView.setRoot(new TreeItem());
		treeView.setMinHeight(0);
		treeView.getSelectionModel().getSelectedItems().addListener((ListChangeListener<TreeItem<Wrapper>>) c -> {
			c.next();
			List<? extends TreeItem<Wrapper>> added = c.getAddedSubList();
			if (added.isEmpty())
				return;
			TreeItem<Wrapper> first = added.get(0);
			if (first.getValue() == null)
				return;
			selectForEdit(first.getValue());
		});
		treeView.setCellFactory(new Callback<TreeView<Wrapper>, TreeCell<Wrapper>>() {
			@Override
			public TreeCell<Wrapper> call(TreeView<Wrapper> param) {
				return new TreeCell<Wrapper>() {
					final SimpleObjectProperty<Wrapper> wrapper = new SimpleObjectProperty<>(null);
					final HBox graphic = new HBox();
					final ImageView showViewing = new ImageView();
					final ImageView showCopyState = new ImageView();
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
						setContextMenu(new ContextMenu(setView));
						showViewing.setFitHeight(16);
						showViewing.setFitWidth(16);
						showViewing.setPreserveRatio(true);
						showCopyState.setFitHeight(16);
						showCopyState.setFitWidth(16);
						showCopyState.setPreserveRatio(true);
						graphic.getChildren().addAll(showViewing, showCopyState);
						setGraphic(graphic);
					}

					@Override
					protected void updateItem(Wrapper item, boolean empty) {
						if (cleanup != null) {
							cleanup.run();
							cleanup = null;
						}
						if (item == null) {
							setText("");
							showViewing.setImage(null);
							showCopyState.setImage(null);
							wrapper.set(null);
						} else {
							wrapper.set(item);
							item.tagViewing.addListener((observable, oldValue, newValue) -> {
								if (newValue)
								showViewing.setImage(icon("eye.svg"));
								else showViewing.setImage(null);
							});
							ChangeListener<Boolean> copyStateListener = (observable, oldValue, newValue) -> {
								if (item.tagCopied.get())
									showCopyState.setImage(icon("content-copy.svg"));
								else if (item.tagLifted.get())
									showCopyState.setImage(icon("content-cut.svg"));
								else
									showCopyState.setImage(null);
							};
							item.tagCopied.addListener(copyStateListener);
							item.tagLifted.addListener(copyStateListener);
							ProjectNode valueNode = (ProjectNode) item.getValue();
							ProjectNode.NameSetListener listener = valueNode.addNameSetListeners((target, value) -> {
								setText(value);
							});
							cleanup = () -> valueNode.removeNameSetListeners(listener);
						}
						super.updateItem(item, empty);
					}
				};
			}
		});
		treeScroll.setContent(treeView);
		treeScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
		treeScroll.setFitToHeight(true);
		treeScroll.setFitToWidth(true);
		MenuItem addCamera = new MenuItem("Add Camera");
		addCamera.setOnAction(e -> {
			Camera camera = Camera.create(context);
			camera.initialNameSet(context, "New Camera");
			camera.initialEndSet(context, 50);
			camera.initialFrameRateSet(context, 120);
			camera.initialHeightSet(context, 240);
			camera.initialWidthSet(context, 320);
			context.change.project(context.project).topAdd(camera);
		});
		MenuItem addGroup = new MenuItem("Add Group");
		addGroup.setOnAction(e -> {
			GroupNode group = new GroupNode();
			group.initialNameSet(context, "New Group");
			add(group);
		});
		MenuItem addImage = new MenuItem("Add Image");
		addImage.setOnAction(e -> {
			ImageNode image = new ImageNode();
			image.initialNameSet(context, "New Image");
			ImageFrame frame = new ImageFrame();
			frame.initialLengthSet(context, -1);
			frame.initialOffsetSet(context, new Vector(0, 0));
			image.initialFramesAdd(context, ImmutableList.of(frame));
			add(image);
		});
		MenuButton addButton = Main.menuButton("plus.svg");
		addButton.getItems().addAll(addCamera, addGroup, addImage);
		Button removeButton = Main.button("minus.svg", "Remove");
		removeButton.disableProperty().bind(Bindings.isEmpty(treeView.getSelectionModel().getSelectedIndices()));
		removeButton.setOnAction(e -> {
			Wrapper edit = context.selectedForEdit.get();
			if (edit == null)
				return;
			edit.delete(context);
			context.finishChange();
		});
		Button moveUpButton = Main.button("arrow-up.svg", "Move Up");
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
				context.change.project(context.project).topAdd(dest, add);
			}
			context.finishChange();
		});
		Button moveDownButton = Main.button("arrow-down.svg", "Move Down");
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
				context.change.project(context.project).topAdd(dest, add);
			}
			context.finishChange();
		});
		Button separateButton = Main.button("format-page-break.svg", "Separate");
		separateButton.disableProperty().bind(Bindings.isEmpty(treeView.getSelectionModel().getSelectedIndices()));
		separateButton.setOnAction(e -> {
			TreeItem<Wrapper> selected = treeView.getSelectionModel().getSelectedItem();
			int dest = selected.getValue().parentIndex + 1;
			if (selected.getValue().getParent() == null) {
				context.change
						.project(context.project)
						.topAdd(dest, ImmutableList.of(selected.getValue().separateClone(context)));
			} else {
				selected
						.getValue()
						.getParent()
						.addChildren(context, dest, ImmutableList.of(selected.getValue().separateClone(context)));
			}
		});
		Button liftButton = Main.button("content-cut.svg", "Lift");
		liftButton.disableProperty().bind(Bindings.isEmpty(treeView.getSelectionModel().getSelectedIndices()));
		liftButton.setOnAction(e -> {
			taggedLifted.forEach(w -> w.tagLifted.set(false));
			taggedLifted.clear();
			taggedCopied.forEach(w -> w.tagCopied.set(false));
			taggedCopied.clear();
			treeView.getSelectionModel().getSelectedItems().stream().forEach(s -> {
				Wrapper wrapper = s.getValue();
				taggedLifted.add(wrapper);
				wrapper.tagLifted.set(true);
			});
		});
		Button copyButton = Main.button("content-copy.svg", "Copy");
		copyButton.disableProperty().bind(Bindings.isEmpty(treeView.getSelectionModel().getSelectedIndices()));
		copyButton.setOnAction(e -> {
			taggedLifted.forEach(w -> w.tagLifted.set(false));
			taggedLifted.clear();
			taggedCopied.forEach(w -> w.tagCopied.set(false));
			taggedCopied.clear();
			treeView.getSelectionModel().getSelectedItems().stream().forEach(s -> {
				Wrapper wrapper = s.getValue();
				taggedCopied.add(wrapper);
				wrapper.tagCopied.set(true);
			});
		});
		MenuItem pasteBeforeButton = new MenuItem("Before");
		pasteBeforeButton.setOnAction(e -> {
			Wrapper destination = context.selectedForEdit.get();
			if (destination == null) {
				paste(null, true, null);
			} else {
				Wrapper pasteParent = destination.getParent();
				paste(pasteParent, true, destination);
			}
		});
		MenuItem pasteInButton = new MenuItem("In");
		pasteInButton.setOnAction(e -> {
			Wrapper destination = context.selectedForEdit.get();
			if (destination == null) {
				paste(null, true, null);
			} else {
				paste(destination, true, null);
			}
		});
		MenuItem pasteAfterButton = new MenuItem("After");
		pasteAfterButton.setOnAction(e -> {
			Wrapper destination = context.selectedForEdit.get();
			if (destination == null) {
				paste(null, false, null);
			} else {
				Wrapper pasteParent = destination.getParent();
				paste(pasteParent, false, destination);
			}
		});
		MenuButton pasteButton = Main.menuButton("content-paste.svg");
		pasteButton.getItems().addAll(pasteBeforeButton, pasteInButton, pasteAfterButton);
		toolbar = new ToolBar(
				addButton,
				moveUpButton,
				moveDownButton,
				new Separator(),
				removeButton,
				new Separator(),
				separateButton,
				liftButton,
				copyButton,
				pasteButton
		);
		layout.getChildren().addAll(treeScroll, toolbar);
		VBox.setVgrow(treeScroll, Priority.ALWAYS);

		split.setOrientation(Orientation.VERTICAL);
		split.getItems().add(layout);
		split.setDividerPositions(1);

		context.project.addTopAddListeners((target, at, value) -> {
			List<TreeItem<Wrapper>> newItems = new ArrayList<>();
			for (int i = 0; i < value.size(); ++i) {
				ProjectNode v = value.get(i);
				Wrapper child = Main.createNode(context, null, at + i, v);
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
		context.selectedForEdit.addListener((observable, oldValue, newValue) -> {
			if (oldValue != null) {
				oldValue.destroyProperties();
				split.getItems().remove(1);
				split.setDividerPositions(1);
			}
			if (newValue != null) {
				Node widget = newValue.createProperties(context);
				split.getItems().add(widget);
				split.setDividerPositions(0.7);
				SplitPane.setResizableWithParent(widget, false);
			}
		});
	}

	private void add(ProjectNode node) {
		Wrapper edit =
				Optional.ofNullable(treeView.getSelectionModel().getSelectedItem()).map(t -> t.getValue()).orElse(null);
		if (edit != null) {
			if (edit.addChildren(context, 0, ImmutableList.of(node)))
				return;
			if (edit.getParent() != null &&
					edit.getParent().addChildren(context, edit.parentIndex + 1, ImmutableList.of(node)))
				return;
		}
		context.change.project(context.project).topAdd(node);
		context.finishChange();
	}

	private void paste(Wrapper pasteParent, boolean before, Wrapper reference) {
		List<ProjectNode> nodes = new ArrayList<>();
		Function<Wrapper, Boolean> check = wrapper -> {
			if (wrapper == reference)
				return false;
			// Drop items that would have infinite recursion if pasted
			if (pasteParent != null) {
				Wrapper parent = pasteParent;
				while (parent != null) {
					if (parent == wrapper)
						return false;
					parent = parent.getParent();
				}
			}
			nodes.add((ProjectNode) wrapper.getValue());
			return true;
		};
		taggedLifted.forEach(wrapper -> {
			if (!check.apply(wrapper))
				return;
			wrapper.delete(context);
		});
		taggedCopied.forEach(wrapper -> check.apply(wrapper));
		if (pasteParent != null) {
			if (reference != null) {
				pasteParent.addChildren(context, reference.parentIndex + (before ? -1 : 1), nodes);
			} else {
				pasteParent.addChildren(context, before ? 0 : -1, nodes);
			}
		} else {
			if (reference != null) {
				context.change.project(context.project).topAdd(reference.parentIndex + (before ? -1 : 1), nodes);
			} else {
				if (before)
					context.change.project(context.project).topAdd(0, nodes);
				else
					context.change.project(context.project).topAdd(context.project.topLength(), nodes);
			}
		}
		context.finishChange();
	}

	public Node getWidget() {
		return split;
	}
}
