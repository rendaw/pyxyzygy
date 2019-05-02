package com.zarbosoft.pyxyzygy.app.config;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pyxyzygy.app.ConfigBase;
import javafx.beans.property.SimpleStringProperty;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RootGlobalConfig extends ConfigBase {
	@Configuration
	public long nextId = 0;

	@Configuration
	public static class Profile {
		@Configuration
		public final SimpleStringProperty name = new SimpleStringProperty();

		@Configuration
		public long id;
	}

	@Configuration
	public List<Profile> profiles = new ArrayList<>();

	@Configuration
	public long lastId = 0;

	@Configuration(optional = true)
	public long tileCacheSize = 1024;

	@Configuration(optional = true)
	public long onionSkinCacheSize = 1024;
}
