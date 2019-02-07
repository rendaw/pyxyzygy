package com.zarbosoft.pyxyzygy.gui.widgets;

import com.sun.javafx.sg.prism.NGImageView;
import com.sun.prism.Graphics;
import com.zarbosoft.pyxyzygy.CustomBinding;
import com.zarbosoft.pyxyzygy.Launch;
import com.zarbosoft.pyxyzygy.ProjectContext;
import com.zarbosoft.pyxyzygy.TrueColorImage;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.text.DecimalFormat;
import java.util.Optional;
import java.util.function.Function;

import static com.zarbosoft.rendaw.common.Common.getResource;
import static com.zarbosoft.rendaw.common.Common.uncheck;
import static net.bytebuddy.matcher.ElementMatchers.named;

public class HelperJFX {
	private final static Constructor<ImageView> newNearestNeighborImageView;

	public static class NearestNeighborNGImageView extends NGImageView {
		@Override
		protected void renderContent(Graphics g) {
			super.renderContent(new NearestNeighborGraphics(g));
		}
	}

	static {
		newNearestNeighborImageView = uncheck(() -> {
			return (Constructor<ImageView>) new ByteBuddy()
					.subclass(ImageView.class)
					.method(named("doCreatePeerPublic"))
					.intercept(MethodDelegation.toConstructor(NearestNeighborNGImageView.class))
					.make()
					.load(ClassLoader.getSystemClassLoader())
					.getLoaded()
					.getConstructor();
		});
	}

	public static ImageView nearestNeighborImageView() {
		return uncheck(() -> newNearestNeighborImageView.newInstance());
	}

	public static Node pad(Node node) {
		VBox out = new VBox();
		out.setPadding(new Insets(3));
		out.getChildren().add(node);
		return out;
	}

	public static Color c(java.awt.Color source) {
		return Color.rgb(source.getRed(), source.getGreen(), source.getBlue());
	}

	public static Pair<Node, SimpleIntegerProperty> nonlinerSlider(int min, int max, int precision, int divide) {
		Slider slider = new Slider();
		slider.setMin(0);
		slider.setMax(1);
		HBox.setHgrow(slider, Priority.ALWAYS);

		TextField text = new TextField();
		text.setMinWidth(50);
		text.setPrefWidth(50);
		HBox.setHgrow(text, Priority.NEVER);

		HBox out = new HBox();
		out.setSpacing(3);
		out.setAlignment(Pos.CENTER_LEFT);
		out.getChildren().addAll(text, slider);

		double range = max - min;
		Function<Double, Integer> fromNonlinear = v -> (int) (Math.pow(v, 2) * range + min);
		Function<Integer, Double> toNonlinear = v -> Math.pow((v - min) / range, 0.5);
		SimpleIntegerProperty value = new SimpleIntegerProperty();

		CustomBinding.bindBidirectional(value,
				slider.valueProperty(),
				v -> Optional.of(toNonlinear.apply(v.intValue())),
				v -> Optional.of(fromNonlinear.apply(v.doubleValue()))
		);
		DecimalFormat textFormat = new DecimalFormat();
		textFormat.setMaximumFractionDigits(precision);
		CustomBinding.bindBidirectional(value,
				text.textProperty(),
				v -> Optional.of(textFormat.format((double) v.intValue() / divide)),
				v -> {
					try {
						return Optional.of((int) (Double.parseDouble(v) * divide));
					} catch (NumberFormatException e) {
						return Optional.empty();
					}
				}
		);

		return new Pair<>(out, value);
	}

	public static Image icon(String resource) {
		InputStream s = getResource(Launch.class, "icons", resource);
		if (s == null)
			throw new Assertion(String.format("Can't find resource %s", resource));
		return ProjectContext.iconCache.computeIfAbsent(resource, r -> new Image(s));
	}

	public static MenuItem menuItem(String icon) {
		return new MenuItem(null, new ImageView(icon(icon)));
	}

	public static MenuButton menuButton(String icon) {
		return new MenuButton(null, new ImageView(icon(icon)));
	}

	public static class IconToggleButton extends ToggleButton {
		public IconToggleButton(String icon, String hint) {
			super(null, new ImageView(icon(icon)));
			Tooltip.install(this, new Tooltip(hint));
		}
	}

	public static Button button(String icon, String hint) {
		Button out = new Button(null, new ImageView(icon(icon)));
		Tooltip.install(out, new Tooltip(hint));
		return out;
	}

	public static WritableImage toImage(TrueColorImage image) {
		final int width = image.getWidth();
		final int height = image.getHeight();
		return new WritableImage(new PixelReader() {
			byte[] premultipliedData;
			byte[] data;

			@Override
			public PixelFormat getPixelFormat() {
				return PixelFormat.getByteBgraInstance();
			}

			@Override
			public int getArgb(int x, int y) {
				throw new Assertion();
			}

			@Override
			public Color getColor(int x, int y) {
				throw new Assertion();
			}

			@Override
			public <T extends Buffer> void getPixels(
					int x, int y, int w, int h, WritablePixelFormat<T> pixelformat, T buffer, int scanlineStride
			) {
				if (scanlineStride != w * 4)
					throw new Assertion();
				byte[] data = pixelformat.isPremultiplied() ? (
						premultipliedData == null ? premultipliedData = image.dataPremultiplied() : premultipliedData
				) : (this.data == null ? this.data = image.data() : this.data);
				if (w == width) {
					if (x != 0)
						throw new Assertion();
					((ByteBuffer) buffer).put(data, y * width * 4, h * width * 4);
				} else {
					if (x + w > width)
						throw new Assertion();
					if (y + h > height)
						throw new Assertion();
					for (int i = y; i < y + h; ++i) {
						((ByteBuffer) buffer).put(data, (i * width + x) * 4, scanlineStride);
					}
				}
			}

			@Override
			public void getPixels(
					int x,
					int y,
					int w,
					int h,
					WritablePixelFormat<ByteBuffer> pixelformat,
					byte[] buffer,
					int offset,
					int scanlineStride
			) {
				if (scanlineStride != w * 4)
					throw new Assertion();
				byte[] data = pixelformat.isPremultiplied() ? (
						premultipliedData == null ? premultipliedData = image.dataPremultiplied() : premultipliedData
				) : (this.data == null ? this.data = image.data() : this.data);
				if (w == width) {
					if (x != 0)
						throw new Assertion();
					System.arraycopy(data, y * width * 4, buffer, offset, h * width * 4);
				} else {
					if (x + w > width)
						throw new Assertion();
					if (y + h > height)
						throw new Assertion();
					for (int i = y; i < y + h; ++i) {
						System.arraycopy(data, (i * width + x) * 4, buffer, offset + i * w * 4, scanlineStride);
					}
				}
			}

			@Override
			public void getPixels(
					int x,
					int y,
					int w,
					int h,
					WritablePixelFormat<IntBuffer> pixelformat,
					int[] buffer,
					int offset,
					int scanlineStride
			) {
				throw new Assertion();
			}
		}, width, height);
	}
}
