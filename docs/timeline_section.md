[top](userguide.md)

# Timeline section

![Timeline section](timeline.jpg)

* **1** / Time

   There are two rows of numbers here.

   The top row is the _absolute time_ of the scene tree you're looking at.  This always goes from 1 and increases (only some numbers are shown though).

   The bottom row is the _mapped time_ of the layer you're editing.  This is the same as the absolute time in most cases - the only time this is different is if you manipulated time using [group](group_layer.md) time mapping frames to compose sub-animations.

   Click on this bar to change the current frame.

* **2** **3** **4** / Preview/render time span

   Adjust **2** this to change where to loop when previewing the currently shown scene tree.  On a Camera layer this is how you adjust what to render when exporting.

   **3** is the frame rate.

   **4** toggles preview.  When previewing, layer ghosts are disabled.  You can also toggle preview by pressing `space` in the editor.

* **5** / Frames

   The rounded square indicates a frame.  Click on a frame to select/deselect it.  The currently selected frame affects operations in the toolbar, but things like painting always affect the currently shown frame (not the selected frame).

* **6** / Add frame

   This creates a new empty frame at the current time.  If there's already a frame at the current time, this creates a frame at the current time and pushes the current frame back.

* **7** / Duplicate frame

   This creates a duplicate of the selected frame or else the currently shown frame at the current time.

* **8** **9** / Swap frame left/right

   This swaps frames, keeping the overall timing the same.

* **10** / Delete frame

* **11** / Clear frame

   Remove paint/offsets/etc from the current frame.

* **12** **13** / Toggle previous/next frame ghost

   Also called onion skin by those with lower aesthetic sense.

* **14** / Change frame offset

   Move a frame - you can use this to adjust the horizontal and vertical offset of a frame relative to the others.
