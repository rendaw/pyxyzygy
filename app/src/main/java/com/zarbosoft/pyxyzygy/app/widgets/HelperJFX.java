package com.zarbosoft.pyxyzygy.app.widgets;

import com.google.common.base.Throwables;
import com.zarbosoft.pyxyzygy.app.CustomBinding;
import com.zarbosoft.pyxyzygy.app.GUILaunch;
import com.zarbosoft.pyxyzygy.app.Global;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.core.PaletteColors;
import com.zarbosoft.pyxyzygy.core.PaletteImage;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.seed.model.v0.TrueColor;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.rendaw.common.Pair;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.text.DecimalFormat;
import java.util.Optional;
import java.util.function.Function;

import static com.zarbosoft.pyxyzygy.app.Misc.opt;
import static com.zarbosoft.rendaw.common.Common.getResource;

public class HelperJFX {
	public static Node pad(Node node) {
		VBox out = new VBox();
		out.setPadding(new Insets(3));
		out.getChildren().add(node);
		return out;
	}

	public static Color c(java.awt.Color source) {
		return Color.rgb(source.getRed(), source.getGreen(), source.getBlue(), source.getAlpha() / 255.0);
	}

	public static Cursor centerCursor(String res) {
		Image image = icon(res);
		return new ImageCursor(image, image.getWidth() / 2, image.getHeight() / 2);
	}
	public static Cursor topCenterCursor(String res) {
		Image image = icon(res);
		return new ImageCursor(image, image.getWidth() / 2, 0);
	}

	public static Cursor cornerCursor(String res) {
		Image image = icon(res);
		return new ImageCursor(image, 0, 0);
	}

	public static Pair<Node, SimpleObjectProperty<Integer>> nonlinearSlider(
			int min, int max, int precision, int divide
	) {
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
		Function<Number, Integer> fromNonlinear = v -> (int) (Math.pow(v.doubleValue(), 2) * range + min);
		Function<Integer, Number> toNonlinear = v -> Math.pow((v - min) / range, 0.5);
		SimpleObjectProperty<Integer> value = new SimpleObjectProperty<>(0);

		CustomBinding.bindBidirectional(new CustomBinding.PropertyBinder<>(value),
				new CustomBinding.PropertyBinder<>(slider.valueProperty()).<Integer>bimap(n -> opt(n).map(fromNonlinear),
						toNonlinear
				)
		);
		DecimalFormat textFormat = new DecimalFormat();
		textFormat.setMaximumFractionDigits(precision);
		CustomBinding.bindBidirectional(new CustomBinding.PropertyBinder<>(value),
				new CustomBinding.PropertyBinder<>(text.textProperty()).bimap(v -> {
					try {
						return opt((int) (Double.parseDouble(v) * divide));
					} catch (NumberFormatException e) {
						return Optional.empty();
					}
				}, v -> textFormat.format((double) v.intValue() / divide))
		);

		return new Pair<>(out, value);
	}

	public static Image icon(String resource) {
		InputStream s = getResource(GUILaunch.class, "icons", resource);
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

	public static Label title(String title) {
		Label out = new Label(title);
		out.getStyleClass().add("h2");
		return out;
	}

	private abstract static class NativeReader implements PixelReader {
		private final int width;
		private final int height;
		byte[] premultipliedData;
		byte[] data;

		protected NativeReader(int width, int height) {
			this.width = width;
			this.height = height;
		}

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

		abstract byte[] getNativeDataPremultiplied();

		abstract byte[] getNativeData();

		@Override
		public <T extends Buffer> void getPixels(
				int x, int y, int w, int h, WritablePixelFormat<T> pixelformat, T buffer, int scanlineStride
		) {
			if (scanlineStride != w * 4)
				throw new Assertion();
			byte[] data = pixelformat.isPremultiplied() ? (
					premultipliedData == null ? premultipliedData = getNativeDataPremultiplied() : premultipliedData
			) : (this.data == null ? this.data = getNativeData() : this.data);
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
					premultipliedData == null ? premultipliedData = getNativeDataPremultiplied() : premultipliedData
			) : (this.data == null ? this.data = getNativeData() : this.data);
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
	}

	public static Image toImage(PaletteImage image, PaletteColors palette) {
		final int width = image.getWidth();
		final int height = image.getHeight();
		return new WritableImage(new NativeReader(width, height) {
			@Override
			byte[] getNativeDataPremultiplied() {
				return image.dataPremultiplied(palette);
			}

			@Override
			byte[] getNativeData() {
				return image.data(palette);
			}
		}, width, height);
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
		return new WritableImage(new NativeReader(width, height) {
			@Override
			byte[] getNativeDataPremultiplied() {
				return image.dataPremultiplied();
			}

			@Override
			byte[] getNativeData() {
				return image.data();
			}
		}, width, height);
	}

	public static WritableImage toImage(TrueColorImage image, TrueColor tint) {
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
				byte[] data = pixelformat.isPremultiplied() ? getPremultipliedData() : getData();
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

			protected byte[] getData() {
				return this.data == null ? this.data = image.dataTint(tint.r, tint.g, tint.b) : this.data;
			}

			protected byte[] getPremultipliedData() {
				return premultipliedData == null ?
						premultipliedData = image.dataPremultipliedTint(tint.r, tint.g, tint.b) :
						premultipliedData;
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
				byte[] data = pixelformat.isPremultiplied() ? getPremultipliedData() : getData();
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

	public static void exceptionPopup(Stage stage, Throwable e, String message, String shortDescription) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.initOwner(stage);
		alert.setTitle(String.format("%s - error", Global.nameHuman));
		alert.setHeaderText(message);
		alert.setContentText(shortDescription);
		TextArea textArea = new TextArea(Throwables.getStackTraceAsString(e));
		textArea.setEditable(false);
		textArea.setWrapText(true);
		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);
		alert.getDialogPane().setExpandableContent(textArea);
		alert.showAndWait();
	}

	public static Runnable bindStyle(Node node, String styleClass, CustomBinding.HalfBinder<Boolean> source) {
		return source.addListener(b -> {
			if (b) {
				if (!node.getStyleClass().contains(styleClass))
					node.getStyleClass().add(styleClass);
			} else
				node.getStyleClass().remove(styleClass);
		});
	}
}
