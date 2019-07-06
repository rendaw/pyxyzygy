package com.zarbosoft.pyxyzygy.app.config;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pyxyzygy.app.Context;
import com.zarbosoft.pyxyzygy.app.wrappers.group.GroupNodeCanvasHandle;
import javafx.beans.property.SimpleLongProperty;

@Configuration(name = "group")
public class GroupNodeConfig extends NodeConfig {
  public static final String TOOL_LAYER_MOVE = "layer_move";
  public static String TOOL_STAMP = "stamp";

  @Configuration public final SimpleLongProperty stampSource = new SimpleLongProperty(-1);

  public GroupNodeConfig() {
    super();
  }

  public GroupNodeConfig(Context context) {
    super(context);
  }
}
