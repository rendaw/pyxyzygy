package com.zarbosoft.pyxyzygy.app.parts.structure;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.zarbosoft.pyxyzygy.app.*;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.model.v0.TrueColorTile;
import com.zarbosoft.pyxyzygy.app.widgets.HelperJFX;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.v0.*;
import com.zarbosoft.pyxyzygy.seed.model.Listener;
import com.zarbosoft.pyxyzygy.seed.model.v0.Rectangle;
import com.zarbosoft.pyxyzygy.seed.model.v0.TrueColor;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.ChainComparator;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.zarbosoft.pyxyzygy.app.Global.logger;
import static com.zarbosoft.pyxyzygy.app.Global.opacityMax;
import static com.zarbosoft.pyxyzygy.app.Misc.moveTo;
import static com.zarbosoft.pyxyzygy.app.Misc.opt;
import static com.zarbosoft.pyxyzygy.app.Wrapper.TakesChildren.NONE;
import static com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext.uniqueName;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.icon;
import static com.zarbosoft.pyxyzygy.app.widgets.HelperJFX.pad;
import static com.zarbosoft.rendaw.common.Common.last;
import static com.zarbosoft.rendaw.common.Common.sublist;

public class Structure {
	private final ProjectContext context;
	private final Window window;
	private final boolean main;
	VBox layout = new VBox();
	TreeView<Wrapper> tree;
	ToolBar toolbar;
	Set<Wrapper> taggedViewing = new HashSet<>();
	Set<Wrapper> taggedLifted = new HashSet<>();
	Set<Wrapper> taggedCopied = new HashSet<>();
	Hotkeys.Action[] actions = new Hotkeys.Action[] {
			new Hotkeys.Action(Hotkeys.Scope.STRUCTURE, "link", "Copy", Global.copyHotkey) {
				@Override
				public void run(ProjectContext context, Window window) {
					link();
				}
			},
			new Hotkeys.Action(Hotkeys.Scope.STRUCTURE, "lift", "Cut", Global.cutHotkey) {
				@Override
				public void run(ProjectContext context, Window window) {
					lift();
				}
			},
			new Hotkeys.Action(Hotkeys.Scope.STRUCTURE, "place-auto", "Paste linked", Global.pasteHotkey) {
				@Override
				public void run(ProjectContext context, Window window) {
					context.change(null, c -> {
						placeAuto(c);
					});
				}
			},
			new Hotkeys.Action(Hotkeys.Scope.STRUCTURE,
					"duplicate",
					"Duplicate",
					Hotkeys.Hotkey.create(KeyCode.D, true, false, false)
			) {
				@Override
				public void run(ProjectContext context, Window window) {
					context.change(null, c -> {
						duplicate(c);
					});
				}
			},
			new Hotkeys.Action(Hotkeys.Scope.STRUCTURE,
					"delete",
					"Delete",
					Hotkeys.Hotkey.create(KeyCode.DELETE, false, false, false)
			) {
				@Override
				public void run(ProjectContext context, Window window) {
					context.change(null, c -> {
						delete(context, c);
					});
				}
			}
	};
	boolean postInit = false;
	public boolean suppressSelect = false;

	public void selectForEdit(Wrapper wrapper) {
		if (wrapper == null)
			return;
		Wrapper preParent = wrapper;
		Wrapper parent = wrapper.getParent();
		boolean found = false;
		while (parent != null) {
			if (window.selectedForView.get().getWrapper() == parent) {
				found = true;
				break;
			}
			preParent = parent;
			parent = parent.getParent();
		}
		if (found) {
			window.selectedForEdit.set(wrapper.buildEditControls(context, window));
		} else {
			window.selectedForEdit.set(null);
			selectForView(preParent);
			window.selectedForEdit.set(wrapper.buildEditControls(context, window));
		}
		if (main)
			context.config.editPath = getPath(wrapper.tree.get()).collect(Collectors.toList());
	}

	public static Stream<Integer> getPath(TreeItem<Wrapper> leaf) {
		if (leaf.getParent() == null)
			return Stream.empty();
		else
			return Stream.concat(getPath(leaf.getParent()), Stream.of(leaf.getParent().getChildren().indexOf(leaf)));
	}

	public void selectForView(Wrapper wrapper) {
		for (Wrapper w : taggedViewing)
			w.tagViewing.set(false);
		taggedViewing.clear();
		if (wrapper != null) {
			wrapper.tagViewing.set(true);
			taggedViewing.add(wrapper);
			window.selectedForView.set(wrapper.buildCanvas(context, window, null));
			if (main)
				context.config.viewPath = getPath(wrapper.tree.get()).collect(Collectors.toList());
		}
	}

	public void treeItemAdded(TreeItem<Wrapper> item) {
		if (!postInit)
			return;
		if (!suppressSelect) {
			tree.getSelectionModel().clearSelection();
			Platform.runLater(() -> {
				// Layer child initialization happens after tree node mirroring
				// Thus canvas may not be created yet
				tree.getSelectionModel().select(item);
			});
		}
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

	public Structure(ProjectContext context, Window window, boolean main) {
		this.main = main;
		this.context = context;
		this.window = window;

		for (Hotkeys.Action action : actions)
			context.hotkeys.register(action);

		tree = new TreeView();
		tree.getStyleClass().addAll("part-structure");
		tree.setShowRoot(false);
		tree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		tree.setMinHeight(0);
		tree.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (context.hotkeys.event(context, window, Hotkeys.Scope.STRUCTURE, e))
				e.consume();
		});
		tree.getSelectionModel().getSelectedItems().addListener((ListChangeListener<TreeItem<Wrapper>>) c -> {
			while (c.next()) {
				List<? extends TreeItem<Wrapper>> added = c.getAddedSubList();
				if (added.isEmpty())
					return;
				TreeItem<Wrapper> first = added.get(0);
				if (first.getValue() == null)
					return;
				selectForEdit(first.getValue());
			}
		});
		tree.setCellFactory((TreeView<Wrapper> param) -> {
			return new TreeCell<Wrapper>() {
				final ImageView showViewing = new ImageView();
				final ImageView showGrabState = new ImageView();
				final SimpleObjectProperty<Wrapper> wrapper = new SimpleObjectProperty<>(null);
				ChangeListener<Boolean> viewingListener = (observable, oldValue, newValue) -> {
					if (newValue)
						showViewing.setImage(icon("eye.png"));
					else
						showViewing.setImage(null);
				};
				ChangeListener<Boolean> copyStateListener = (observable, oldValue, newValue) -> {
					if (wrapper.get().tagCopied.get())
						showGrabState.setImage(icon("content-copy.png"));
					else if (wrapper.get().tagLifted.get())
						showGrabState.setImage(icon("content-cut.png"));
					else
						showGrabState.setImage(null);
				};
				Listener.ScalarSet<ProjectNode, String> nameSetListener = (target, value) -> {
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
					if (item != null)
						cleanup = HelperJFX.bindStyle(this,
								"link-selected",
								new CustomBinding.PropertyHalfBinder<>(item.getConfig().selectedSomewhere)
						);
					wrapper.set(item);
					super.updateItem(item, empty);
				}
			};
		});
		MenuItem addCamera = new MenuItem("Add camera");
		addCamera.setOnAction(e -> {
			Camera camera = Camera.create(context);
			camera.initialNameSet(context, uniqueName("Camera"));
			camera.initialOffsetSet(context, Vector.ZERO);
			camera.initialOpacitySet(context, opacityMax);
			camera.initialFrameStartSet(context, 0);
			camera.initialFrameLengthSet(context, 12);
			camera.initialFrameRateSet(context, 120);
			double cameraFactor = context.tileSize / 200.0;
			camera.initialHeightSet(context, (int) (240 * cameraFactor));
			camera.initialWidthSet(context, (int) (320 * cameraFactor));
			context.change(null, c -> {
				c.project(context.project).topAdd(camera);
			});
		});
		MenuItem addGroup = new MenuItem("Add group");
		addGroup.setOnAction(e -> {
			GroupNode group = GroupNode.create(context);
			group.initialOffsetSet(context, Vector.ZERO);
			group.initialOpacitySet(context, opacityMax);
			group.initialNameSet(context, uniqueName(Global.groupLayerName));
			context.change(null, c -> {
				addNew(group, c);
			});
		});
		MenuItem addImage = new MenuItem("Add true color layer");
		addImage.setOnAction(e -> {
			TrueColorImageNode image = TrueColorImageNode.create(context);
			image.initialOffsetSet(context, Vector.ZERO);
			image.initialOpacitySet(context, opacityMax);
			image.initialNameSet(context, uniqueName(Global.trueColorLayerName));
			TrueColorImageFrame frame = TrueColorImageFrame.create(context);
			frame.initialLengthSet(context, -1);
			frame.initialOffsetSet(context, new Vector(0, 0));
			image.initialFramesAdd(context, ImmutableList.of(frame));
			context.change(null, c -> {
				addNew(image, c);
			});
		});
		MenuItem addPalette = new MenuItem("Add palette layer");
		addPalette.setOnAction(e -> {
			ObservableList<Optional<Palette>> source = FXCollections.observableArrayList();
			for (Palette e1 : context.project.palettes())
				source.add(Optional.of(e1));
			source.add(Optional.empty());
			ComboBox<Optional<Palette>> cb = new ComboBox(source);
			cb.getSelectionModel().select(last(source));
			cb.setMaxWidth(Double.MAX_VALUE);
			cb.setCellFactory(param -> new ListCell<>() {
				@Override
				protected void updateItem(Optional<Palette> item, boolean empty) {
					if (empty || item == null)
						setText("");
					else if (!item.isPresent())
						setText("New palette...");
					else
						setText(item.get().name());
					super.updateItem(item, empty);
				}
			});
			cb.setButtonCell(cb.getCellFactory().call(null));
			window.dialog("Choose a palette for the new layer").addContent(cb).addAction(ButtonType.OK, true, () -> {
				context.change(null, c -> {
					Optional<Palette> palette0 = cb.getSelectionModel().getSelectedItem();
					Palette palette;
					if (!palette0.isPresent()) {
						palette = Palette.create(context);
						palette.initialNameSet(context, uniqueName(Global.paletteName));
						palette.initialNextIdSet(context, 2);
						PaletteColor transparent = PaletteColor.create(context);
						transparent.initialIndexSet(context, 0);
						transparent.initialColorSet(context, TrueColor.fromJfx(Color.TRANSPARENT));
						PaletteColor black = PaletteColor.create(context);
						black.initialIndexSet(context, 1);
						black.initialColorSet(context, TrueColor.fromJfx(Color.BLACK));
						palette.initialEntriesAdd(context, ImmutableList.of(transparent, black));
						Palette finalPalette = palette;
						c.project(context.project).palettesAdd(finalPalette);
					} else {
						palette = palette0.get();
					}
					PaletteImageNode image = PaletteImageNode.create(context);
					image.initialOffsetSet(context, Vector.ZERO);
					image.initialOpacitySet(context, opacityMax);
					image.initialNameSet(context, uniqueName(Global.paletteLayerName));
					image.initialPaletteSet(context, palette);
					PaletteImageFrame frame = PaletteImageFrame.create(context);
					frame.initialLengthSet(context, -1);
					frame.initialOffsetSet(context, new Vector(0, 0));
					image.initialFramesAdd(context, ImmutableList.of(frame));
					addNew(image, c);
					context.addPaletteUser(image);
				});
				return true;
			}).addAction(ButtonType.CANCEL, false, () -> {
				return true;
			}).go();
		});
		MenuItem importImage = new MenuItem("Import PNG");
		importImage.setOnAction(e -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setInitialDirectory(new File(GUILaunch.profileConfig.importDir));
			fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG Image", "*.png"));
			Optional.ofNullable(fileChooser.showOpenDialog(window.stage)).map(f -> f.toPath()).ifPresent(p -> {
				TrueColorImage data = TrueColorImage.deserialize(p.toString());
				TrueColorImageNode image = TrueColorImageNode.create(context);
				image.initialOffsetSet(context, Vector.ZERO);
				image.initialOpacitySet(context, opacityMax);
				image.initialNameSet(context, uniqueName(p.getFileName().toString()));
				TrueColorImageFrame frame = TrueColorImageFrame.create(context);
				frame.initialLengthSet(context, -1);
				frame.initialOffsetSet(context, Vector.ZERO);
				image.initialFramesAdd(context, ImmutableList.of(frame));
				Rectangle base = new Rectangle(0, 0, data.getWidth(), data.getHeight());

				Rectangle offset = base.shift(base.span().divide(2));
				Rectangle unitBounds = offset.divideContains(context.tileSize);
				Vector localOffset = offset.corner().minus(unitBounds.corner().multiply(context.tileSize));
				for (int x = 0; x < unitBounds.width; ++x) {
					for (int y = 0; y < unitBounds.height; ++y) {
						final int x0 = x;
						final int y0 = y;
						TrueColorImage cut = data.copy(x0 * context.tileSize - localOffset.x,
								y0 * context.tileSize - localOffset.y,
								context.tileSize,
								context.tileSize
						);
						frame.initialTilesPutAll(context,
								ImmutableMap.of(unitBounds.corner().plus(x0, y0).to1D(),
										TrueColorTile.create(context, cut)
								)
						);
					}
				}
				context.change(null, c -> {
					addNew(image, c);
				});
			});
		});
		MenuItem duplicateButton = new MenuItem("Duplicate");
		duplicateButton.disableProperty().bind(Bindings.isEmpty(tree.getSelectionModel().getSelectedIndices()));
		duplicateButton.setOnAction(e -> {
			context.change(null, c -> {
				duplicate(c);
			});
		});
		MenuButton addButton = HelperJFX.menuButton("plus.png");
		addButton.getItems().addAll(addImage, addPalette, importImage, addGroup, addCamera, duplicateButton);
		Button removeButton = HelperJFX.button("minus.png", "Remove");
		removeButton.disableProperty().bind(Bindings.isEmpty(tree.getSelectionModel().getSelectedIndices()));
		removeButton.setOnAction(e -> {
			context.change(null, c -> {
				delete(context, c);
			});
		});
		Button moveUpButton = HelperJFX.button("arrow-up.png", "Move up");
		moveUpButton.disableProperty().bind(Bindings.isEmpty(tree.getSelectionModel().getSelectedItems()));
		moveUpButton.setOnAction(e -> {
			List<TreeItem<Wrapper>> selected = tree.getSelectionModel().getSelectedItems();
			if (selected.isEmpty())
				throw new Assertion();
			TreeItem<Wrapper> firstParent = selected.get(0).getParent();
			List<TreeItem<Wrapper>> removeOrder = selected
					.stream()
					.filter(s -> s.getParent() == firstParent)
					.sorted(new ChainComparator<TreeItem<Wrapper>>()
							.greaterFirst(s -> s.getValue().parentIndex)
							.build())
					.collect(Collectors.toList());
			int dest = last(removeOrder).getValue().parentIndex - 1;
			if (dest < 0)
				return;
			List<ProjectNode> add =
					removeOrder.stream().map(s -> (ProjectNode) s.getValue().getValue()).collect(Collectors.toList());

			context.change(new ProjectContext.Tuple("struct_move"), c -> {
				removeOrder.forEach(s -> s.getValue().delete(context, c));
				if (firstParent.getValue() != null) {
					firstParent.getValue().addChildren(context, c, dest, add);
				} else {
					c.project(context.project).topAdd(dest, add);
				}
			});
		});
		Button moveDownButton = HelperJFX.button("arrow-down.png", "Move down");
		moveDownButton.disableProperty().bind(Bindings.isEmpty(tree.getSelectionModel().getSelectedItems()));
		moveDownButton.setOnAction(e -> {
			List<TreeItem<Wrapper>> selected = tree.getSelectionModel().getSelectedItems();
			if (selected.isEmpty())
				throw new Assertion();
			TreeItem<Wrapper> firstParent = selected.get(0).getParent();
			List<TreeItem<Wrapper>> removeOrder = selected
					.stream()
					.filter(s -> s.getParent() == firstParent)
					.sorted(new ChainComparator<TreeItem<Wrapper>>()
							.greaterFirst(s -> s.getValue().parentIndex)
							.build())
					.collect(Collectors.toList());
			int dest = last(removeOrder).getValue().parentIndex + 1;
			if (dest > firstParent.getChildren().size() - removeOrder.size())
				return;
			List<ProjectNode> add =
					removeOrder.stream().map(s -> (ProjectNode) s.getValue().getValue()).collect(Collectors.toList());

			context.change(new ProjectContext.Tuple("struct_move"), c -> {
				removeOrder.forEach(s -> s.getValue().delete(context, c));
				if (firstParent.getValue() != null) {
					firstParent.getValue().addChildren(context, c, dest, add);
				} else {
					c.project(context.project).topAdd(dest, add);
				}
			});
		});
		MenuItem cutButton = new MenuItem("Cut");
		cutButton.setOnAction(e -> {
			lift();
		});
		MenuItem copyButton = new MenuItem("Copy");
		copyButton.setOnAction(e -> {
			link();
		});
		MenuItem linkBeforeButton = new MenuItem("Paste before");
		linkBeforeButton.setOnAction(e -> {
			context.change(null, c -> {
				placeBefore(c);
			});
		});
		MenuItem linkInButton = new MenuItem("Paste in");
		linkInButton.setOnAction(e -> {
			context.change(null, c -> {
				placeIn(c);
			});
		});
		MenuItem linkAfterButton = new MenuItem("Paste after");
		linkAfterButton.setOnAction(e -> {
			context.change(null, c -> {
				placeAfter(c);
			});
		});
		MenuItem unlinkButton = new MenuItem("Unlink");
		unlinkButton.setOnAction(e -> {
			context.change(null, c -> {
				unlink(c);
			});
		});
		MenuButton linkButton = HelperJFX.menuButton("pencil.png");
		linkButton
				.getItems()
				.addAll(cutButton, copyButton, linkBeforeButton, linkInButton, linkAfterButton, unlinkButton);
		toolbar = new ToolBar(addButton, removeButton, moveUpButton, moveDownButton, linkButton);

		HBox opacityBox = new HBox();
		{
			opacityBox.setSpacing(3);

			Slider opacity = new Slider();
			opacity.setMin(0);
			opacity.setMax(opacityMax);
			CustomBinding.bindBidirectional(new CustomBinding.IndirectBinder<Integer>(new CustomBinding.PropertyHalfBinder<>(
							window.selectedForEdit),
							e -> opt(e == null ?
									null :
									new CustomBinding.ScalarBinder<Integer>(e.getWrapper().getValue(),
											"opacity",
											v -> context.change(new ProjectContext.Tuple(e.getWrapper(), "opacity"),
													c -> c.projectNode((ProjectNode) e.getWrapper().getValue()).opacitySet(v)
											)
									))
					),
					new CustomBinding.PropertyBinder<>(opacity.valueProperty()).bimap(d -> Optional.of((int) (double) d),
							i -> (double) (int) i
					)
			);
			HBox.setHgrow(opacity, Priority.ALWAYS);
			opacityBox.getChildren().addAll(new Label("Opacity"), opacity);
		}

		layout.getChildren().addAll(toolbar, pad(opacityBox), tree);
		VBox.setVgrow(tree, Priority.ALWAYS);
	}

	public void populate() {
		List<Integer> editPath = context.config.editPath;
		List<Integer> viewPath = context.config.viewPath;

		TreeItem<Wrapper> rootTreeItem = new TreeItem<>();
		prepareTreeItem(rootTreeItem);
		tree.setRoot(rootTreeItem);

		context.project.addTopAddListeners((target, at, value) -> {
			List<TreeItem<Wrapper>> newItems = new ArrayList<>();
			for (int i = 0; i < value.size(); ++i) {
				ProjectNode v = value.get(i);
				Wrapper child = Window.createNode(context, null, at + i, v);
				child.tree.addListener((observable, oldValue, newValue) -> {
					tree.getRoot().getChildren().set(child.parentIndex, newValue);
				});
				newItems.add(child.tree.getValue());
			}
			tree.getRoot().getChildren().addAll(at, newItems);
		});
		context.project.addTopRemoveListeners((target, at, count) -> {
			List<TreeItem<Wrapper>> temp = tree.getRoot().getChildren().subList(at, at + count);
			temp.forEach(i -> i.getValue().remove(context));
			temp.clear();
			for (int i = at; i < tree.getRoot().getChildren().size(); ++i) {
				tree.getRoot().getChildren().get(i).getValue().setParentIndex(at + i);
			}
		});
		context.project.addTopMoveToListeners((target, source, count, dest) -> {
			moveTo(tree.getRoot().getChildren(), source, count, dest);
			for (int i = Math.min(source, dest); i < tree.getRoot().getChildren().size(); ++i)
				tree.getRoot().getChildren().get(i).getValue().setParentIndex(i);
		});
		context.project.addTopClearListeners(target -> {
			tree.getRoot().getChildren().forEach(c -> c.getValue().remove(context));
			tree.getRoot().getChildren().clear();
		});

		if (main) {
			window.selectedForEdit.set(null);
			selectForView(findNode(rootTreeItem, viewPath));
			tree.getSelectionModel().clearSelection();
			tree.getSelectionModel().select(findNode(rootTreeItem, editPath).tree.get());
		}
		postInit = true;
	}

	private static Wrapper findNode(TreeItem<Wrapper> root, List<Integer> path) {
		if (path.isEmpty())
			return root.getValue();
		return findNode(root.getChildren().get(path.get(0)), sublist(path, 1));
	}

	private void delete(ProjectContext context, ChangeStepBuilder change) {
		Wrapper edit = window.selectedForEdit.get().getWrapper();
		if (edit == null)
			return;
		edit.delete(context, change);
	}

	private void placeAfter(ChangeStepBuilder change) {
		Wrapper destination = window.selectedForEdit.get().getWrapper();
		if (destination == null) {
			place(change, null, false, null);
		} else {
			Wrapper pasteParent = destination.getParent();
			place(change, pasteParent, false, destination);
		}
	}

	private void placeIn(ChangeStepBuilder change) {
		Wrapper destination = window.selectedForEdit.get().getWrapper();
		if (destination == null) {
			place(change, null, true, null);
		} else {
			place(change, destination, true, null);
		}
	}

	private void placeBefore(ChangeStepBuilder change) {
		Wrapper destination = window.selectedForEdit.get().getWrapper();
		if (destination == null) {
			place(change, null, true, null);
		} else {
			Wrapper pasteParent = destination.getParent();
			place(change, pasteParent, true, destination);
		}
	}

	private void placeAuto(ChangeStepBuilder change) {
		Wrapper destination = getSelection();
		if (destination == null) {
			placeAfter(change);
		} else {
			if (destination.takesChildren() == NONE) {
				placeAfter(change);
			} else {
				placeIn(change);
			}
		}
	}

	private void link() {
		clearTagLifted();
		clearTagCopied();
		tree.getSelectionModel().getSelectedItems().stream().forEach(s -> {
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
		tree.getSelectionModel().getSelectedItems().stream().forEach(s -> {
			Wrapper wrapper = s.getValue();
			taggedLifted.add(wrapper);
			wrapper.tagLifted.set(true);
		});
	}

	private void duplicate(ChangeStepBuilder change) {
		Wrapper destination = window.selectedForEdit.get().getWrapper();
		ProjectNode clone = destination.separateClone(context);
		addNew(clone, change);
	}

	private void unlink(ChangeStepBuilder change) {
		Wrapper edit = window.selectedForEdit.get().getWrapper();
		if (edit == null)
			return;
		ProjectNode clone = edit.separateClone(context);
		addNew(clone, change);
		edit.delete(context, change);
	}

	private Wrapper getSelection() {
		return Optional.ofNullable(tree.getSelectionModel().getSelectedItem()).map(t -> t.getValue()).orElse(null);
	}

	/**
	 * Adds the single node wherever it can, starting from the selection
	 *
	 * @param node
	 */
	private void addNew(ProjectNode node, ChangeStepBuilder change) {
		Wrapper edit = getSelection();
		Wrapper placeAt = edit;
		int index = 0;
		while (placeAt != null) {
			if (placeAt.addChildren(context, change, index, ImmutableList.of(node)))
				return;
			index = placeAt.parentIndex + 1;
			placeAt = placeAt.getParent();
		}
		change.project(context.project).topAdd(node);
	}

	/**
	 * Places exactly within parent/top before/after reference/start=end
	 *
	 * @param change
	 * @param pasteParent
	 * @param before
	 * @param reference
	 */
	private void place(
			ChangeStepBuilder change, Wrapper pasteParent, boolean before, Wrapper reference
	) {
		List<ProjectNode> nodes = new ArrayList<>();
		Consumer<Wrapper> check = wrapper -> {
			// Can't place relative to copied element
			if (wrapper == reference) {
				logger.write("Warning: Can't paste relative to copied element; dropping element.");
				return;
			}

			// Omit items that would have infinite recursion if pasted
			if (pasteParent != null) {
				Wrapper parent = pasteParent;
				while (parent != null) {
					if (parent == wrapper) {
						logger.write(
								"Warning: Can't paste relative to copied element - destination is a child of element; dropping element.");
						return;
					}
					parent = parent.getParent();
				}
			}

			// Register for placing
			nodes.add((ProjectNode) wrapper.getValue());
		};
		taggedLifted.forEach(check);
		taggedCopied.forEach(check);
		if (nodes.isEmpty()) {
			logger.write("Warning: No nodes left to place!");
		}
		if (pasteParent != null) {
			if (reference != null) {
				if (!pasteParent.addChildren(context, change, reference.parentIndex + (before ? -1 : 1), nodes)) {
					logger.write("Warning: Parent refused to place nodes.");
					return;
				}
			} else {
				if (!pasteParent.addChildren(context, change, before ? 0 : -1, nodes)) {
					logger.write("Warning: Parent refused to place nodes.");
					return;
				}
			}
		} else {
			if (reference != null) {
				change.project(context.project).topAdd(reference.parentIndex + (before ? -1 : 1), nodes);
			} else {
				if (before)
					change.project(context.project).topAdd(0, nodes);
				else
					change.project(context.project).topAdd(context.project.topLength(), nodes);
			}
		}
		taggedLifted.forEach(c -> c.delete(context, change));
		clearTagCopied();
		clearTagLifted();
	}

	public Node getWidget() {
		return layout;
	}
}
