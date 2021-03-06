<a name="readme"><img src="https://gitlab.com/rendaw/pyxyzygy/raw/experimental/itch/banner.svg" width="100%" alt="pyxyzygy: heavenly alignment of pixels">

<div><br><br></div>

**pyxyzygy** is a pixel art and animation tool.

Build it yourself or install it from [https://rendaw.itch.io/pyxyzygy](https://rendaw.itch.io/pyxyzygy).

**pyxyzygy**はドット絵とアニメーション特定ツールです。

じい分でビルドするか[https://rendaw.itch.io/pyxyzygy](https://rendaw.itch.io/pyxyzygy)でインストールできます。

<div><br></div>

# User guide

We recommend you launch **pyxyzygy** from the **itch.io** client but bat/sh launch scripts are included for your convenience.

Read the illustrated [user guide](docs/userguide.md)!

**itch.io**アプリで**pyxyzygy**を使用することにおすすめします。

日本語の[ユーザーガイド](ldocs/ja/userguide.md)をご参考ください！

# Automation

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

If you'd like to help translate documentation, please go [here](https://gitlocalize.com/repo/2608).

翻訳で手伝うことは[ここ](https://gitlocalize.com/repo/2608)です。
