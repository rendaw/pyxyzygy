<img src="https://gitlab.com/rendaw/pyxyzygy/raw/experimental/itch/banner.svg" width="100%" alt="pyxyzygy: heavenly alignment of pixels">

<div><br><br></div>

**pyxyzygy** is a pixel art and animation tool.

Build it yourself or buy it at [https://rendaw.itch.io/pyxyzygy](https://rendaw.itch.io/pyxyzygy) to support the project.

<div><br></div>

# Getting started

We recommend you launch **pyxyzygy** from the **itch.io** client but bat/sh launch scripts are included for your convenience.

Read the illustrated [user guide](docs/mainwindow.md)!

# CLI usage

From the installation root run:

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