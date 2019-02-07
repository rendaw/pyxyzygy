open module com.zarbosoft.pyxyzygy.app {
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
	requires com.zarbosoft.pidgooncommand;
	requires com.zarbosoft.pyxyzygy.nearestneighborimageview;
	exports com.zarbosoft.pyxyzygy.app;
	exports com.zarbosoft.pyxyzygy.app.config;
	exports com.zarbosoft.pyxyzygy.app.modelmirror;
	exports com.zarbosoft.pyxyzygy.app.parts.editor;
	exports com.zarbosoft.pyxyzygy.app.parts.structure;
	exports com.zarbosoft.pyxyzygy.app.parts.timeline;
	exports com.zarbosoft.pyxyzygy.app.widgets;
	exports com.zarbosoft.pyxyzygy.app.wrappers.camera;
	exports com.zarbosoft.pyxyzygy.app.wrappers.group;
	exports com.zarbosoft.pyxyzygy.app.wrappers.truecolorimage;
}