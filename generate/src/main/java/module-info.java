open module com.zarbosoft.shoedemo.internal.shoedemo_generate {
	requires transitive com.zarbosoft.internal.shoedemo_seed;
	requires com.zarbosoft.interface1;
	requires javapoet;
	requires ant;
	requires reflections;
	requires java.compiler;
	requires guava;
	requires javafx.base;
	exports com.zarbosoft.internal.shoedemo_generate;
}