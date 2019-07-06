package com.zarbosoft.pyxyzygy.app;

import com.zarbosoft.automodel.lib.Logger;
import com.zarbosoft.automodel.lib.ModelBase;
import com.zarbosoft.automodel.lib.ProjectObject;
import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.luxem.write.RawWriter;
import com.zarbosoft.pidgooncommand.Command;
import com.zarbosoft.pyxyzygy.core.TrueColorImage;
import com.zarbosoft.pyxyzygy.core.model.ModelVersions;
import com.zarbosoft.pyxyzygy.core.model.latest.Camera;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupLayer;
import com.zarbosoft.pyxyzygy.core.model.latest.Model;
import com.zarbosoft.pyxyzygy.core.model.latest.Project;
import com.zarbosoft.pyxyzygy.core.model.latest.ProjectLayer;
import com.zarbosoft.pyxyzygy.core.model.latest.TrueColorImageLayer;
import com.zarbosoft.pyxyzygy.seed.Rectangle;
import com.zarbosoft.rendaw.common.Assertion;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.zarbosoft.pyxyzygy.app.Global.appDirs;
import static com.zarbosoft.rendaw.common.Common.sublist;

public class CLIMain {

  public static class Commandline {
    @Configuration(name = "--help")
    @Command.Argument(shortName = "-h", earlyExit = true, description = "Show this help text")
    public boolean help = false;

    @Configuration(optional = true)
    public Subcommand subcommand;
  }

  @Configuration
  public abstract static class Subcommand {
    @Configuration
    @Command.Argument(index = 0, description = "Path to a pyxyzygy project")
    public String path;

    public void run() {
      ModelBase.DeserializeResult result = ModelVersions.deserialize(Paths.get(path), 0);
      if (result.model.needsMigrate())
        throw new RuntimeException(
            "This file needs to be updated for this versio nof the software - use the GUI and confirm that the automatic update is correct.");
      runImpl(new Context((Model) result.model));
    }

    public abstract void runImpl(Context context);
  }

  @Configuration(name = "list")
  @Command.Argument(description = "Output project object structure and path element ids")
  public static class ListSubcommand extends Subcommand {

    @Override
    public void runImpl(Context context) {
      RawWriter w = new RawWriter(System.out, (byte) ' ', 4);
      w.recordBegin();
      walk(w, context.project);
      w.recordEnd();
      System.out.flush();
    }

    private void walk(RawWriter w, ProjectObject o) {
      if (false) {
        throw new Assertion();
      } else if (o instanceof Project) {
        for (int i = 0; i < ((Project) o).topLength(); ++i) {
          w.key(Integer.toString(i));
          walk(w, ((Project) o).topGet(i));
        }
      } else if (o instanceof GroupLayer) {
        for (int i = 0; i < ((GroupLayer) o).childrenLength(); ++i) {
          w.key(Integer.toString(i));
          ProjectObject inner = ((GroupLayer) o).childrenGet(i).inner();
          if (inner == null) w.primitive("null".getBytes(StandardCharsets.UTF_8));
          else walk(w, inner);
        }
      } else if (o instanceof TrueColorImageLayer) {
        w.primitive(((TrueColorImageLayer) o).name().getBytes(StandardCharsets.UTF_8));
      } else throw new Assertion();
    }
  }

  @Configuration(name = "render")
  @Command.Argument(description = "Export a project subtree as png, png offset relative to origin")
  public static class RenderSubcommand extends Subcommand {
    @Configuration
    @Command.Argument(
        index = 1,
        description = "List of indexes to a specific project subtree to export")
    public List<Integer> selector = new ArrayList<>();

    @Configuration
    @Command.Argument(index = 2, description = "Output filename")
    public String output;

    @Configuration(optional = true)
    @Command.Argument(description = "Frame to render")
    public int frame = 0;

    @Override
    public void runImpl(Context context) {
      ProjectLayer found = find(context.project, selector);
      Rectangle bounds;
      if (found instanceof Camera) {
        bounds =
            new Rectangle(
                -((Camera) found).width() / 2,
                -((Camera) found).height() / 2,
                ((Camera) found).width(),
                ((Camera) found).height());
      } else {
        bounds = Render.findBounds(context, frame, found);
      }
      TrueColorImage out = TrueColorImage.create(bounds.width, bounds.height);
      Render.render(context, found, out, frame, bounds, 1);
      out.serialize(output);
      new RawWriter(System.out)
          .primitive(Integer.toString(bounds.x))
          .primitive(Integer.toString(bounds.y));
      System.out.flush();
    }

    private ProjectLayer find(ProjectObject o, List<Integer> selector) {
      if (selector.isEmpty()) return (ProjectLayer) o;
      int next = selector.get(0);
      List<Integer> remaining = sublist(selector, 1);
      if (false) {
        throw new Assertion();
      } else if (o instanceof Project) {
        return find(((Project) o).topGet(next), remaining);
      } else if (o instanceof GroupLayer) {
        return find(((GroupLayer) o).childrenGet(next).inner(), remaining);
      } else if (o instanceof TrueColorImageLayer) {
        return (ProjectLayer) o;
      } else throw new Assertion();
    }
  }

  @Configuration(name = "render-anim")
  @Command.Argument(description = "Export a camera node as a series of png frames")
  public static class RenderAnimSubcommand extends Subcommand {
    @Configuration
    @Command.Argument(
        index = 1,
        description = "List of indexes to a specific project subtree to export")
    public List<Integer> selector = new ArrayList<>();

    @Configuration
    @Command.Argument(index = 2, description = "Output directory")
    public String output;

    @Override
    public void runImpl(Context context) {
      ProjectLayer found = find(context.project, selector);
      if (!(found instanceof Camera)) throw new RuntimeException("Selected node is not a camera");
      Camera node = (Camera) found;
      Rectangle bounds =
          new Rectangle(-node.width() / 2, -node.height() / 2, node.width(), node.height());
      TrueColorImage canvas = TrueColorImage.create(bounds.width, bounds.height);
      Path renderPath = Paths.get(output);
      for (int i = node.frameStart(); i < node.frameStart() + node.frameLength(); ++i) {
        if (i != node.frameStart()) canvas.clear();
        Render.render(context, node, canvas, i, bounds, 1.0);
        canvas.serialize(renderPath.resolve(String.format("frame%06d.png", i)).toString());
      }
      new RawWriter(System.out)
          .primitive(Integer.toString(bounds.x))
          .primitive(Integer.toString(bounds.y));
      System.out.flush();
    }

    private ProjectLayer find(ProjectObject o, List<Integer> selector) {
      if (selector.isEmpty()) return (ProjectLayer) o;
      int next = selector.get(0);
      List<Integer> remaining = sublist(selector, 1);
      if (false) {
        throw new Assertion();
      } else if (o instanceof Project) {
        return find(((Project) o).topGet(next), remaining);
      } else if (o instanceof GroupLayer) {
        return find(((GroupLayer) o).childrenGet(next).inner(), remaining);
      } else if (o instanceof TrueColorImageLayer) {
        return (ProjectLayer) o;
      } else throw new Assertion();
    }
  }

  public static void main(String[] args) {
    Logger.logger = new Logger.File(appDirs);
    ScanResult scan =
        new ClassGraph().enableAllInfo().whitelistPackages("com.zarbosoft.pyxyzygy.cli").scan();
    Commandline got = Command.<Commandline>parse(scan, Commandline.class, args);
    if (got.help || got.subcommand == null) {
      Command.showHelp(scan, Commandline.class, args[0]);
    }
  }
}
