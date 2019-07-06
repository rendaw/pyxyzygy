package com.zarbosoft.pyxyzygy.app.config;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pyxyzygy.app.Context;
import javafx.beans.property.SimpleIntegerProperty;

@Configuration(name = "truecolorimage")
public class TrueColorImageNodeConfig extends NodeConfig {
  public TrueColorImageNodeConfig(Context context) {
    super(context);
    tool.set(TOOL_BRUSH);
  }

  public TrueColorImageNodeConfig() {
    tool.set(TOOL_BRUSH);
  }

  public static final String TOOL_FRAME_MOVE = "frame_move";
  public static final String TOOL_SELECT = "select";
  public static final String TOOL_BRUSH = "brush";

  @Configuration public final SimpleIntegerProperty brush = new SimpleIntegerProperty(1);

  @Configuration public int lastBrush = 0;
}
