[top](userguide.md)

# Project section

This section has a number of tabs - most are self explanatory but here's an overview of the `Project` tab.

![Project tab](structure.jpg)

* **1** / The project tree

   A project in pyxyzyg is composed of layers.

   Layers at the root of the project tree (layers with no parent layers) are "scenes" - scenes are viewed separately. You can turn any layer into a scene by moving it to the root of the tree.

   In the above image the layer named "Group" forms a scene, and "Palette layer" is a non-scene.

   Note: If you just want to view some layers without the rest of the scene, you can change the viewed layer as described below, which is quicker than making a new scene.

   Disabled layers/scenes have their names faded.  When a layer with linked copies is selected, those copies will be shown in bold.

## Viewing and editing

There are two circle icons to the left of the layers:

* **2** / The filled circle indicates what's being edited - in the image, the layer called `Palette layer` is being edited.

   The tools shown in the editor and the layer settings are controlled by this.  You can select another layer to edit by clicking on it.

* **3** / The open circle indicates what's being shown.

   In this case, `Group` and all its children are displayed.  If you double click a layer you'll change the view that subtree.  This is useful if you want to work on just a couple layers that are part of a larger image, but don't want to manually hide all the other layers.

   The shown layer will automatically adjust when you select a layer to edit that's outside the current shown subtree.

   There are hotkeys to move the view closer to the edited note and closer to the root - see the `Settings` tab for all currently available hotkeys.

## Adding, removing, copying layers

The toolbar at the top contains all the buttons for modifying the project structure.

* **4** / Add a layer or duplicate a layer

   See information on layers [here](userguide.md#layers).

* **5** / Duplicate a layer

   Duplicating a layer creates a new, unlinked copy of the current edit layer.

* **6** / Delete a layer

   Removes the layer

* **7** **8** / Move a layer up/down

   This will move a scene up or down or move a layer up or down in the current group.  Lower layers in a group obscure the layers above them.

* **9** / Other options

   Lifting a layer marks it with a scissors icon.  Placing moves all marked items to the selected location.  You can also press `ctrl+x` to toggle whether a layer is lifted.  Press `esc` to un-lift everything.

   Unlink makes the selected layer a distinct layer if the layer was a linked copy of another layer.  After unlinking a layer modifying it won't affect the other layer.

## Changing layer visibility

The two controls below the toolbar allow you to quickly change layer visibility.

* **10** / Toggle layer visibility

   Note!  A layer will only disappear when you deselect it.  Selected layers are always visible.

* **11** / Change layer opacity