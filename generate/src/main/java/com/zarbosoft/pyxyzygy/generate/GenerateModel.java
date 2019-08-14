package com.zarbosoft.pyxyzygy.generate;

import com.squareup.javapoet.CodeBlock;
import com.zarbosoft.automodel.task.AutoModel;
import com.zarbosoft.automodel.task.AutoObject;
import com.zarbosoft.automodel.task.AutoType;
import com.zarbosoft.automodel.task.ListType;
import com.zarbosoft.automodel.task.MapType;
import com.zarbosoft.pyxyzygy.seed.TrueColor;
import com.zarbosoft.pyxyzygy.seed.Vector;

import java.nio.file.Path;

public class GenerateModel extends TaskBase {
  @Override
  public void run() {
    new AutoModel("com.zarbosoft.pyxyzygy.core.model")
        .version(
            "v1",
            version -> {
              AutoObject projectLayer =
                  version
                      .obj("ProjectLayer")
                      .isAbstract()
                      .field("name", AutoType.string, f -> f.versioned())
                      .field(
                          "metadata",
                          new MapType(AutoType.string, AutoType.string),
                          f -> f.versioned())
                      .field(
                          "offset",
                          AutoType.of(Vector.class),
                          f -> f.versioned().def(CodeBlock.of("$T.ZERO", Vector.class)));
              AutoObject paletteEntry = version.obj("PaletteEntry");
              AutoObject palette =
                  version
                      .obj("Palette")
                      .field("nextId", AutoType.integer, f -> f.versioned())
                      .field("name", AutoType.string, f -> f.versioned())
                      .field("entries", new ListType(paletteEntry), f -> f.versioned());

              version
                  .obj("PaletteColor")
                  .parent(paletteEntry)
                  .field("index", AutoType.integer, f -> f.versioned())
                  .field(
                      "color",
                      AutoType.of(TrueColor.class),
                      f -> f.versioned().def("$T.BLACK", TrueColor.class));
              version.obj("PaletteSeparator").parent(paletteEntry);
              version
                  .obj("PaletteImageLayer")
                  .parent(projectLayer)
                  .field("palette", palette, f -> f.versioned())
                  .field(
                      "frames",
                      new ListType(
                          version
                              .obj("PaletteImageFrame")
                              .field("length", AutoType.integer, f -> f.versioned())
                              .field(
                                  "offset",
                                  AutoType.of(Vector.class),
                                  f -> f.versioned().def(CodeBlock.of("$T.ZERO", Vector.class)))
                              .field(
                                  "tiles",
                                  new MapType(AutoType.lon, version.obj("PaletteTile")),
                                  f -> f.versioned())),
                      f -> f.versioned())
                  .field("prelength", AutoType.integer, f -> f.versioned().def("$L", 0));
              version
                  .obj("TrueColorImageLayer")
                  .parent(projectLayer)
                  .field(
                      "frames",
                      new ListType(
                          version
                              .obj("TrueColorImageFrame")
                              .field("length", AutoType.integer, f -> f.versioned())
                              .field(
                                  "offset",
                                  AutoType.of(Vector.class),
                                  f -> f.versioned().def(CodeBlock.of("$T.ZERO", Vector.class)))
                              .field(
                                  "tiles",
                                  new MapType(AutoType.lon, version.obj("TrueColorTile")),
                                  f -> f.versioned())),
                      f -> f.versioned())
                  .field("prelength", AutoType.integer, f -> f.versioned().def("$L", 0));

              AutoObject groupLayer =
                  version
                      .obj("GroupLayer")
                      .parent(projectLayer)
                      .field(
                          "children",
                          new ListType(
                              version
                                  .obj("GroupChild")
                                  .field(
                                      "timeFrames",
                                      new ListType(
                                          version
                                              .obj("GroupTimeFrame")
                                              .field(
                                                  "length",
                                                  AutoType.integer,
                                                  f ->
                                                      f.versioned().comment("-1 = infinite length"))
                                              .field(
                                                  "innerOffset",
                                                  AutoType.integer,
                                                  f -> f.versioned().comment("-1 = disabled"))
                                              .field(
                                                  "innerLoop",
                                                  AutoType.integer,
                                                  f -> f.versioned())),
                                      f1 -> f1.versioned())
                                  .field(
                                      "timePrelength",
                                      AutoType.integer,
                                      f -> f.versioned().def("$L", 0))
                                  .field(
                                      "positionFrames",
                                      new ListType(
                                          version
                                              .obj("GroupPositionFrame")
                                              .field("length", AutoType.integer, f -> f.versioned())
                                              .field(
                                                  "offset",
                                                  AutoType.of(Vector.class),
                                                  f -> f.versioned().def("$T.ZERO", Vector.class))),
                                      f1 -> f1.versioned())
                                  .field(
                                      "positionPrelength",
                                      AutoType.integer,
                                      f -> f.versioned().def("$L", 0))
                                  .field("inner", projectLayer, f -> f.versioned())
                                  .field(
                                      "opacity",
                                      AutoType.integer,
                                      f -> f.versioned().comment("0-100000"))
                                  .field("enabled", AutoType.bool, f -> f.versioned())),
                          f -> f.versioned());

              version
                  .obj("Camera")
                  .parent(groupLayer)
                  .field("width", AutoType.integer, f -> f.versioned())
                  .field("height", AutoType.integer, f -> f.versioned())
                  .field("frameStart", AutoType.integer, f -> f.versioned())
                  .field("frameLength", AutoType.integer, f -> f.versioned())
                  .field("frameRate", AutoType.integer, f -> f.versioned());

              version
                  .rootObj("Project")
                  .field("tileSize", AutoType.integer, f -> f.persist())
                  .field("top", new ListType(projectLayer), f -> f.versioned())
                  .field("palettes", new ListType(palette), f -> f.versioned())
                  .field("tileDir", AutoType.of(Path.class), f -> f.mutable().def("null"));
            })
        .generate(path);
  }
}
