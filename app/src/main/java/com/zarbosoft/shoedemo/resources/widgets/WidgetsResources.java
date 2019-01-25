package com.zarbosoft.shoedemo.resources.widgets;

import java.net.URI;

public class WidgetsResources {
	public static String get(String path) {
		return WidgetsResources.class.getResource("/" + path).toExternalForm();
	}
}
