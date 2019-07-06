package com.zarbosoft.automodel.lib;

import com.google.common.base.Throwables;
import com.zarbosoft.appdirsj.AppDirs;
import com.zarbosoft.rendaw.common.ChainComparator;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.zarbosoft.rendaw.common.Common.sublist;
import static com.zarbosoft.rendaw.common.Common.uncheck;

public abstract class Logger {
  public static Logger logger;  // User in charge of setting

  public abstract void flush();

  public static class File extends Logger {
    final PrintStream stream;

    public File(AppDirs appDirs) {
      stream =
          uncheck(
              () -> {
                Path logRoot = appDirs.user_log_dir(true);
                Files.createDirectories(logRoot);
                List<Path> oldLogs =
                    Files.list(logRoot)
                        .sorted(new ChainComparator<Path>().greaterFirst(Path::toString).build())
                        .collect(Collectors.toList());
                if (oldLogs.size() > 10)
                  sublist(oldLogs, 10 - oldLogs.size())
                      .forEach(p -> uncheck(() -> Files.delete(p)));
                return new PrintStream(
                    Files.newOutputStream(
                        logRoot.resolve(
                            LocalDateTime.now()
                                    .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                    .replaceAll("[:]", "_")
                                + ".txt")));
              });
    }

    public void write(String format, Object... args1) {
      Object[] args = Arrays.copyOf(args1, args1.length + 1);
      args[args.length - 1] = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
      stream.format("[!] %s: " + format + "\n", args);
    }

    public void writeException(Throwable e, String format, Object... args1) {
      Object[] args = Arrays.copyOf(args1, args1.length + 1);
      args[args.length - 1] = Throwables.getStackTraceAsString(e);
      write(format + "\n%s", args);
      stream.flush();
    }

    @Override
    public void flush() {
      stream.flush();
    }
  }

  public static class TerminalPlusFile extends File {
    public TerminalPlusFile(AppDirs appDirs) {
      super(appDirs);
    }

    @Override
    public void write(String format, Object... args) {
      System.out.format(format + "\n", args);
      super.write(format, args);
    }

    @Override
    public void writeException(Throwable e, String format, Object... args1) {
      System.out.flush();
      Object[] args = Arrays.copyOf(args1, args1.length + 1);
      args[args.length - 1] = Throwables.getStackTraceAsString(e);
      System.err.format(format + "\n%s\n", args);
      System.err.flush();
      super.writeException(e, format, args1);
    }

    @Override
    public void flush() {
      System.out.flush();
    }
  }

  public abstract void write(String format, Object... args);

  public abstract void writeException(Throwable e, String format, Object... args);
}
