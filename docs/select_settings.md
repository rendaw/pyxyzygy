[top](userguide.md)

# Select settings

The select tool has multiple states.  The select tool starts in the "nothing selected" state.

If you have image data on the clipboard, you can press `ctrl+v` at any time in the editor and the clipboard contents will be turned into a "lifted" selection automatically.

## When nothing's lifted

Click and drag in the editor to indicate a selection region.

The indicated region will have handles on each side for adjusting the region's size, and the region can be dragged to move it elsewhere.

![unlifted settings](selectsettings1.jpg)

* **1** / Lift

   Lift the indicated region.  A lifted region can be moved before placing.

* **2** / Clear

   Erase anything in the indicated region

* **3** / Cut

   Put the region in the clipboard and erase it.

* **4** / Copy

   Put the region in the clipboard.

* **5** / Cancel

   Reset the indicated region.

## When lifted

The handles will disappear from the indicated region meaning the region has been "lifted".  You can drag the region to where you want to place it or cancel.  The layer isn't modified until you press `Place`, `Clear`, or `Cut`.

![lifted settings](selectsettings2.jpg)

* **1** / Place

   Drop the lifted region back into the layer.

* **2** / Clear

   Delete the lifted region.

* **3** / Cut

   Put the region in the clipboard and erase it.

* **4** / Copy

   Put the region in the clipboard.

* **5** / Cancel

   Put the lifted region back in its original place and un-lift it.
