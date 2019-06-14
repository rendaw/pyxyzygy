# Pyxyzygy User Guide

## Profile selection

A profile stores settings such as where projects are stored, default project type, brush configurations, background and ghost colors, etc.

![Profile select](newprofile.jpg)

* **1** / A profile

* **2** / Create a new profile

* **3** / Remove profile

* **4** **5** / Move profile up/down

* **6** / Rename profile

* **7** / Load profile

* **8** / Exit

You can skip this screen by adding `--profile-id` with the profile id of the profile you'd like to use by default to the command line.  To look up the profile id you have to inspect the profile files in the pyxyzygy config directory manually, at the moment.

## Project selection

A project is a collection of related images and palettes.

There are two types of projects: normal and pixel projects.  The only difference between the two is how large the image memory chunks are.

A **normal** project is optimized for screen resolution drawing - if you use it for pixel art the project will use slightly more space.

A **pixel** project is optimized for small (pixel-wise) strokes.  If you use it for screen resolution drawing it might be slightly sluggish.

A project is stored on disk as a directory with a `project.luxem` file.  You can move and back-up project folders as long as you don't delete or modify any of the files within.

![Project selection](newproject.jpg)

* **1** / The current directory

* **2** / The name of the selected project or directory.  When creating a new project, enter the name here.

* **3** / Ascend to the parent directory

* **4** / Refresh the directory listing

* **5** / Create a new director

* **6** / A project

* **7** / A subdirectory

* **8** **9** / Create a normal or pixel optimized project

* **10** / Create a new project with the name in **2**

* **11** / Open the selected project

* **12** / Exit project selection

## The main window

![Main window](mainwindow.jpg)

* **1** / [Project structure](project_section.md)

   Add, remove, rearrange layers and scenes, and tabs for other settings.

* **2** [Editor](editor_section.md)

   Draw here.

* **3** [Timeline](timeline_section.md)

   Tools for animation.

## Types of layers

* [Group](group_layer.md) layers
* [Palette](palette_layer.md) layers
* [True color](true_color_layer.md) layers
* [Camera](camera_layer.md) layers

## Additional mouse hotkeys

* `ctrl + click` - sample color from current node
* `ctrl + shift + click` - sample merged color from campus
* `shift + click` - draw line
