open module com.zarbosoft.internal.pyxyzygy_seed {
	requires transitive com.zarbosoft.luxem;
	requires transitive net.bytebuddy;
	requires transitive java.instrument;
	requires javafx.graphics;
	exports com.zarbosoft.internal.pyxyzygy_seed.model;
	exports com.zarbosoft.internal.pyxyzygy_seed.deserialize;
}