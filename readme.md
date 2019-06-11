<img src="https://gitlab.com/rendaw/pyxyzygy/raw/experimental/itch/banner.svg" width="100%" alt="pyxyzygy: heavenly alignment of pixels">

<div><br><br></div>

**pyxyzygy** is a pixel art and animation tool.

Build it yourself or buy it at [https://rendaw.itch.io/pyxyzygy](https://rendaw.itch.io/pyxyzygy) to support the project.

<div><br></div>

# GUI usage

Launch **pyxyzygy** from the **itch.io** client or from the installation root using the arguments in `build/itch_manifest.toml`.

If you do not specify any arguments you will be prompted to choose or create a new project.  If you specify an existing project path as the first argument **pyxyzygy** will open it.  If you specify a path to a project directory that doesn't yet exist for the first argument followed by the image mode (`normal` or `pixel`) **pyxyzygy** will create and open it.

### Additional hotkeys

* `ctrl + click` - sample color from current node
* `ctrl + shift + click` - sample merged color from campus
* `shift + click` - draw line

# CLI usage

From the **itch.io** installation root (java reduced image) run:

```
./java/bin/java -p modules -m com.zarbosoft.pyxyzygy.app/com.zarbosoft.pyxyzygy.app.CLIMain ARGUMENTS
```

To see the different options run:
```
./java/bin/java -p modules -m com.zarbosoft.pyxyzygy.app/com.zarbosoft.pyxyzygy.app.CLIMain --help
```

# Building

The build process is configured primarily for CI.  You should be able to start GUIMain from your IDE if you first build the native components and icons.

To build native components, in `native/` run:
```
./build.py /usr/lib/jvm/java-11-openjdk/ linux g++ so /usr/include /usr/lib
```

To build icons, in `graphics/` run `build.py`.

# Contributing

Please format your code with [google-java-format](https://github.com/google/google-java-format).