package com.zarbosoft.pyxyzygy.nearestneighborimageview;

import com.sun.javafx.sg.prism.NGImageView;
import com.sun.prism.Graphics;
import javafx.scene.image.ImageView;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;

import java.lang.reflect.Constructor;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class NearestNeighborImageView {
  private static final Constructor<ImageView> newNearestNeighborImageView;

  public static class NearestNeighborNGImageView extends NGImageView {
    @Override
    protected void renderContent(Graphics g) {
      super.renderContent(new NearestNeighborGraphics(g));
    }
  }

  static {
    try {
      newNearestNeighborImageView =
          (Constructor<ImageView>)
              new ByteBuddy()
                  .subclass(ImageView.class)
                  .method(named("doCreatePeerPublic"))
                  .intercept(MethodDelegation.toConstructor(NearestNeighborNGImageView.class))
                  .make()
                  .load(ClassLoader.getSystemClassLoader())
                  .getLoaded()
                  .getConstructor();
    } catch (NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  public static ImageView create() {
    try {
      return newNearestNeighborImageView.newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
