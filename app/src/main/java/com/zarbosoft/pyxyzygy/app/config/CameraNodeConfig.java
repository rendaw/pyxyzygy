package com.zarbosoft.pyxyzygy.app.config;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pyxyzygy.app.Global;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import javafx.beans.property.SimpleIntegerProperty;

@Configuration(name = "camera")
public class CameraNodeConfig extends GroupNodeConfig {
	public static String TOOL_VIEWPORT = "viewport";

	public CameraNodeConfig() {
		super();
	}

	public CameraNodeConfig(ProjectContext context) {
		super(context);
	}

	@Configuration
	public static enum RenderMode {
		@Configuration(name = "png") PNG {
			@Override
			public String human() {
				return "Single PNG";
			}
		},
		@Configuration(name = "webm") WEBM {
			@Override
			public String human() {
				return "WebM";
			}
		},
		@Configuration(name = "gif") GIF {
			@Override
			public String human() {
				return "Gif";
			}
		},
		@Configuration(name = "png-sequence") PNG_SEQUENCE {
			@Override
			public String human() {
				return "PNG Sequence";
			}
		};

		public abstract String human();
	}

	@Configuration
	public String renderDir = Global.appDirs.user_dir().toString();

	@Configuration
	public String renderName = "render";

	@Configuration
	public RenderMode renderMode = RenderMode.PNG;

	@Configuration(optional = true)
	public SimpleIntegerProperty renderScale = new SimpleIntegerProperty(1);
}
