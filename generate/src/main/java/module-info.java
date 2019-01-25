module com.zarbosoft.shoedemo.internal.shoedemo_generate {
	requires transitive com.zarbosoft.internal.shoedemo_seed;
	requires com.zarbosoft.luxem;
	requires javapoet;
	requires ant;
	requires io.github.classgraph;
	requires java.compiler;
	requires com.google.common;
	requires javafx.base;
	exports com.zarbosoft.internal.shoedemo_generate;
}