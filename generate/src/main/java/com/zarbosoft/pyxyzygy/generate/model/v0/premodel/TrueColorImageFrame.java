package com.zarbosoft.pyxyzygy.generate.model.v0.premodel;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pyxyzygy.seed.model.v0.Vector;

import java.util.Map;

@Configuration
public class TrueColorImageFrame extends ProjectObject {
  @Configuration public int length;
  @Configuration public Vector offset;
  @Configuration public Map<Long, TrueColorTileBase> tiles;
}
