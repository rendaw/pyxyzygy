package com.zarbosoft.shoedemo;

import com.gojuno.morton.Morton64;
import com.zarbosoft.rendaw.common.Assertion;
import com.zarbosoft.shoedemo.model.*;
import com.zarbosoft.shoedemo.parts.timeline.Timeline;
import com.zarbosoft.shoedemo.structuretree.CameraWrapper;
import com.zarbosoft.shoedemo.structuretree.GroupLayerWrapper;
import com.zarbosoft.shoedemo.structuretree.GroupNodeWrapper;
import com.zarbosoft.shoedemo.structuretree.ImageNodeWrapper;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.ext.awt.image.codec.PNGEncodeParam;
import org.apache.batik.ext.awt.image.codec.PNGImageEncoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.SVGConstants;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

import static com.zarbosoft.rendaw.common.Common.uncheck;

public class Window {
	public static Map<String, Image> iconCache = new HashMap<>();
	public static Map<String, Integer> names = new HashMap<>();

	/**
	 * Start at count 1 (unwritten) for machine generated names
	 * @param name
	 * @return
	 */
	public static String uniqueName(String name) {
		int count = names.compute(name, (n, i) -> i == null ? 1 : i + 1);
		return count == 1 ? name : String.format("%s (%s)", name, count);
	}

	/**
	 * Start at count 2 if the first element is not in the map (user decided name)
	 * @param name
	 * @return
	 */
	public static String uniqueName1(String name) {
		int count = names.compute(name, (n, i) -> i == null ? 2 : i + 1);
		return count == 1 ? name : String.format("%s (%s)", name, count);
	}

	public void start(ProjectContext context, Stage primaryStage) {
		primaryStage.setOnCloseRequest(e -> {
			context.alive.set(false);
			context.flushSemaphore.release();
		});

		Structure structure = new Structure(context);

		Editor editor = new Editor(context);
		Timeline timeline = new Timeline(context);

		SplitPane specificLayout = new SplitPane();
		specificLayout.setOrientation(Orientation.VERTICAL);
		specificLayout.getItems().addAll(editor.getWidget(), timeline.getWidget());
		SplitPane.setResizableWithParent(timeline.getWidget(), false);
		specificLayout.setDividerPositions(0.7);

		SplitPane generalLayout = new SplitPane();
		generalLayout.setOrientation(Orientation.HORIZONTAL);
		generalLayout.getItems().addAll(structure.getWidget(), specificLayout);
		SplitPane.setResizableWithParent(structure.getWidget(), false);
		generalLayout.setDividerPositions(0.3);

		primaryStage.setTitle("Shoe Demo 2");
		Scene scene = new Scene(generalLayout, 1200, 800);

		scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
			if (e.getCode() == KeyCode.U || (e.isControlDown() && e.getCode() == KeyCode.Z)) {
				context.history.undo();
			} else if (e.isControlDown() && (e.getCode() == KeyCode.R || e.getCode() == KeyCode.Y)) {
				context.history.redo();
			}
		});

		primaryStage.setScene(scene);
		primaryStage.show();
	}

	public static Morton64 morton = new Morton64(2, 32);

	public static List<Wrapper> getAncestors(Wrapper start, Wrapper target) {
		List<Wrapper> ancestors = new ArrayList<>();
		Wrapper at = target.getParent();
		while (at != start) {
			ancestors.add(at);
			at = at.getParent();
		}
		Collections.reverse(ancestors);
		return ancestors;
	}

	public static DoubleVector toLocal(Wrapper wrapper, DoubleVector v) {
		for (Wrapper parent : getAncestors(null, wrapper)) {
			v = parent.toInner(v);
		}
		return v;
	}

	public static Wrapper createNode(ProjectContext context, Wrapper parent, int parentIndex, ProjectObject node) {
		if (false) {
			throw new Assertion();
		} else if (node instanceof Camera) {
			return new CameraWrapper(context, parent, parentIndex, (Camera) node);
		} else if (node instanceof GroupNode) {
			return new GroupNodeWrapper(context, parent, parentIndex, (GroupNode) node);
		} else if (node instanceof GroupLayer) {
			return new GroupLayerWrapper(context, parent, parentIndex, (GroupLayer) node);
		} else if (node instanceof ImageNode) {
			return new ImageNodeWrapper(context, parent, parentIndex, (ImageNode) node);
		} else
			throw new Assertion();
	}

	public static Image iconSize(String resource, int width, int height) {
		return uncheck(() -> {
			TranscodingHints hints = new TranscodingHints();
			hints.put(ImageTranscoder.KEY_WIDTH, (float) width); //your image width
			hints.put(ImageTranscoder.KEY_HEIGHT, (float) height); //your image height
			hints.put(ImageTranscoder.KEY_DOM_IMPLEMENTATION, SVGDOMImplementation.getDOMImplementation());
			hints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI, SVGConstants.SVG_NAMESPACE_URI);
			hints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT, SVGConstants.SVG_SVG_TAG);
			hints.put(ImageTranscoder.KEY_XML_PARSER_VALIDATING, false);
			final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			ImageTranscoder transcoder = new ImageTranscoder() {
				@Override
				public BufferedImage createImage(int i, int i1) {
					return image;
				}

				@Override
				public void writeImage(
						BufferedImage bufferedImage, TranscoderOutput transcoderOutput
				) throws TranscoderException {

				}
			};
			transcoder.setTranscodingHints(hints);
			uncheck(() -> transcoder.transcode(new TranscoderInput(Window.class.getResourceAsStream("icons/" + resource)),
					null
			));
			ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
			new PNGImageEncoder(pngOut, PNGEncodeParam.getDefaultEncodeParam(image)).encode(image);
			return new Image(new ByteArrayInputStream(pngOut.toByteArray()));
		});
	}

	public static Image icon(String resource) {
		return iconCache.computeIfAbsent(resource, r -> iconSize(resource, 16, 16));
	}

	public static MenuItem menuItem(String icon) {
		return new MenuItem(null, new ImageView(icon(icon)));
	}

	public static MenuButton menuButton(String icon) {
		return new MenuButton(null, new ImageView(icon(icon)));
	}

	public static Button button(String icon, String hint) {
		Button out = new Button(null, new ImageView(icon(icon)));
		Tooltip.install(out, new Tooltip(hint));
		return out;
	}
}
