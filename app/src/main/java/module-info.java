open module com.zarbosoft.shoedemo {
	requires com.zarbosoft.internal.shoedemo_seed;
	requires javafx.graphics;
	requires javafx.controls;
	requires com.zarbosoft.appdirsj;
	requires java.desktop;
	exports com.zarbosoft.shoedemo;
	exports com.zarbosoft.shoedemo.config;
	exports com.zarbosoft.shoedemo.model;
	exports com.zarbosoft.shoedemo.modelmirror;
	exports com.zarbosoft.shoedemo.parts.editor;
	exports com.zarbosoft.shoedemo.parts.structure;
	exports com.zarbosoft.shoedemo.parts.timeline;
	exports com.zarbosoft.shoedemo.widgets;
	exports com.zarbosoft.shoedemo.wrappers.camera;
	exports com.zarbosoft.shoedemo.wrappers.group;
	exports com.zarbosoft.shoedemo.wrappers.truecolorimage;
}