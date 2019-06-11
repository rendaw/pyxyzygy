package com.zarbosoft.pyxyzygy.app.config;

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
      return 8;
    }
  };

  public abstract int tileSize();

  public abstract int defaultZoom();
}