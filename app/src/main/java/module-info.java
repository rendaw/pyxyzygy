open module com.zarbosoft.pyxyzygy {
	requires com.zarbosoft.internal.pyxyzygy_seed;
	requires javafx.graphics;
	requires javafx.controls;
	requires com.zarbosoft.appdirsj;
	requires java.desktop;
	requires net.bytebuddy;
	requires net.bytebuddy.agent;
	requires java.instrument;
	requires jdk.attach;
	requires java.management;
	exports com.zarbosoft.pyxyzygy;
	exports com.zarbosoft.pyxyzygy.config;
	exports com.zarbosoft.pyxyzygy.model;
	exports com.zarbosoft.pyxyzygy.modelmirror;
	exports com.zarbosoft.pyxyzygy.parts.editor;
	exports com.zarbosoft.pyxyzygy.parts.structure;
	exports com.zarbosoft.pyxyzygy.parts.timeline;
	exports com.zarbosoft.pyxyzygy.widgets;
	exports com.zarbosoft.pyxyzygy.wrappers.camera;
	exports com.zarbosoft.pyxyzygy.wrappers.group;
	exports com.zarbosoft.pyxyzygy.wrappers.truecolorimage;
}