open module com.zarbosoft.pyxyzygy {
	requires com.zarbosoft.pyxyzygy.seed;
	requires com.zarbosoft.pyxyzygy.core;
	requires javafx.graphics;
	requires javafx.controls;
	requires com.zarbosoft.appdirsj;
	requires java.desktop;
	requires net.bytebuddy;
	requires java.instrument;
	requires jdk.attach;
	requires java.management;
	requires com.zarbosoft.rendaw.common;
	requires com.google.common;
	requires com.zarbosoft.luxem;
	exports com.zarbosoft.pyxyzygy.gui;
	exports com.zarbosoft.pyxyzygy.gui.config;
	exports com.zarbosoft.pyxyzygy.gui.modelmirror;
	exports com.zarbosoft.pyxyzygy.gui.parts.editor;
	exports com.zarbosoft.pyxyzygy.gui.parts.structure;
	exports com.zarbosoft.pyxyzygy.gui.parts.timeline;
	exports com.zarbosoft.pyxyzygy.gui.widgets;
	exports com.zarbosoft.pyxyzygy.gui.wrappers.camera;
	exports com.zarbosoft.pyxyzygy.gui.wrappers.group;
	exports com.zarbosoft.pyxyzygy.gui.wrappers.truecolorimage;
}