package com.zarbosoft.shoedemo.model;

import com.zarbosoft.rendaw.common.Assertion;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.apache.tools.ant.Project;

import javax.imageio.ImageIO;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static com.zarbosoft.rendaw.common.Common.uncheck;

/**
 * Shared, immutable
 */
public class Tile {
	public final static int tileSize = 256;

	public static Canvas tempCanvas;

	static {
		tempCanvas = new Canvas();
		tempCanvas.setHeight(tileSize);
		tempCanvas.setWidth(tileSize);
	}

	public static Set<Tile> dirty = new HashSet<>();

	public final long id;
	public final WritableImage data;
	public final int dataOffsetX;
	public final int dataOffsetY;

	int refCount;

	public Tile(Image data) {
		//this.data = data;

		// TODO
		this.data = null;
		id = 0;
		dataOffsetX = 0;
		dataOffsetY = 0;
	}

	public void refInc() {
		refCount += 1;
	}

	public void refDec(Project project) {
		this.refCount -= 1;
		if (this.refCount == 0) {
			uncheck(() -> Files.delete(path(project)));
		}
	}

	private static Path path(Project project, int id) {
		throw new Assertion(); // TODO
		//return context.project.tileDir.resolve(String.format("%s.png", id));
	}

	private Path path(Project project) {
		return path(project, id);
	}

	public void refDec(Project project) {
		refCount -= 1;
		if (refCount == 0) {
			dirty.remove(this);
			try {
				Files.delete(path(project));
			} catch (NoSuchFileException e) {

			} catch (Exception e) {
				throw uncheck(e);
			}
		}
	}

	public static Tile fromId(Project project, int id) {
		return new Tile(new Image(path(project, id).toString()));
	}

	/**
	 * x/y are in gc space
	 *
	 * @param gc
	 * @param x
	 * @param y
	 */
	public void draw(GraphicsContext gc, int x, int y) {
		gc.drawImage(data, dataOffsetX, dataOffsetY, tileSize, tileSize, x, y, tileSize, tileSize);
	}

	/**
	 * x/y are where the source is in tile space
	 *
	 * @param context
	 * @param source
	 * @param x
	 * @param y
	 * @return
	 */
	public Tile modify(Project project, Image source, int x, int y) {
		Canvas canvas = tempCanvas;
		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.clearRect(0, 0, tileSize, tileSize);
		draw(gc, 0, 0);
		gc.drawImage(source, x, y);
		WritableImage data = new WritableImage(tileSize, tileSize);
		canvas.snapshot(new SnapshotParameters(), data);
		return new Tile(data);
	}

	public void flush(Project project) {
		uncheck(() -> {
			Path tilePath = path(project);
			try (OutputStream out = Files.newOutputStream(tilePath)) {
				ImageIO.write(SwingFXUtils.fromFXImage(data, null), "png", out);
			}
		});
	}
}
