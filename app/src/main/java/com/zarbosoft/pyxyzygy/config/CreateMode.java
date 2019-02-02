package com.zarbosoft.pyxyzygy.config;

import com.zarbosoft.interface1.Configuration;

@Configuration
public enum CreateMode {
	@Configuration
	normal {
		public int tileSize() {
			return 200;
		}

		@Override
		public int defaultZoom() {
			return 0;
		}
	},
	@Configuration
	pixel {
		public int tileSize() {
			return 32;
		}

		@Override
		public int defaultZoom() {
			return 4;
		}
	};

	abstract public int tileSize();

	abstract public int defaultZoom();
}
