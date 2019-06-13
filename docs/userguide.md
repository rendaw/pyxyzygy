# Pyxyzygy User Guide

## Profiles

A profile stores settings such as where projects are stored, default project type, brush configurations, background and ghost colors, etc.

## Projects

A project is a collection of related images and palettes.

There are two types of projects: normal and pixel projects.  The only difference between the two is how large the image memory chunks are.

A **normal** project is optimized for screen resolution drawing - if you use it for pixel art the project will use slightly more space.

A **pixel** project is optimized for small (pixel-wise) strokes.  If you use it for screen resolution drawing it might be slightly sluggish.

A project is stored on disk as a directory with a `project.luxem` file.  You can move and back-up project folders as long as you don't delete or modify any of the files within.

## The main window

![Main window](mainwindow.jpg)

1. [Project structure](project_section.md)
   Add, remove, rearrange layers and scenes, and tabs for other settings.
2. [Editor](editor_section.md)
   Draw here.
3. [Timeline](timeline_section.md)
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
