package com.zarbosoft.pyxyzygy.generate.model.v0.premodel;

import com.zarbosoft.interface1.Configuration;

@Configuration
public class GroupTimeFrame extends ProjectObject {
  /** -1 = infinite length */
  @Configuration public int length;
  /** -1 = disabled */
  @Configuration public int innerOffset;

  @Configuration public int innerLoop;
}
