package com.zarbosoft.pyxyzygy.wrappers.truecolorimage;

import com.sun.javafx.sg.prism.NGImageView;
import com.sun.prism.Graphics;
import com.zarbosoft.pyxyzygy.ProjectContext;
import com.zarbosoft.pyxyzygy.TrueColorImage;
import com.zarbosoft.pyxyzygy.model.Tile;
import com.zarbosoft.pyxyzygy.widgets.NearestNeighborGraphics;
import com.zarbosoft.rendaw.common.Assertion;
import javafx.scene.image.ImageView;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.ExceptionMethod;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.SuperMethodCall;

import java.io.File;
import java.lang.reflect.Constructor;

import static com.zarbosoft.pyxyzygy.widgets.HelperJFX.toImage;
import static com.zarbosoft.rendaw.common.Common.uncheck;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class WrapTile {
	private final static Constructor<ImageView> newNearestNeighborImageView;

	public static class NearestNeighborNGImageView extends NGImageView {
		public NearestNeighborNGImageView() {
			System.out.format("hi nearest neighbor ng\n");
		}

		@Override
		protected void renderContent(Graphics g) {
			System.out.format("BAH\n");
			super.renderContent(new NearestNeighborGraphics(g));
		}
	}

	static {
		newNearestNeighborImageView = uncheck(() -> {
			final DynamicType.Unloaded<ImageView> doCreatePeer = new ByteBuddy()
					.subclass(ImageView.class)
					.method(named("doCreatePeerPublic"))
					//.intercept(ExceptionMethod.throwing(Assertion.class))
					.intercept(MethodDelegation.toConstructor(NearestNeighborNGImageView.class))
					.make();
			doCreatePeer.saveIn(new File("bb.class"));
			return (Constructor<ImageView>) doCreatePeer
					.load(ClassLoader.getSystemClassLoader())
					.getLoaded()
					.getConstructor();
		});
	}

	final ImageView widget;

	WrapTile(ProjectContext context, Tile tile, int x, int y) {
		System.out.format("hi wrap tile\n");
		widget = uncheck(() -> newNearestNeighborImageView.newInstance());
		System.out.format("hi wrap tile 2 %s\n", widget.getClass().getName());
		widget.setMouseTransparent(true);
		widget.setLayoutX(x);
		widget.setLayoutY(y);
		widget.setManaged(false);
		update(context, tile);
	}

	public void update(ProjectContext context, Tile tile) {
		TrueColorImage image = tile.getData(context);
		widget.setImage(toImage(image));
	}
}
