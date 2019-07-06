package com.zarbosoft.automodel.lib;

import com.zarbosoft.luxem.events.LTypeEvent;
import com.zarbosoft.luxem.read.BufferedRawReader;
import com.zarbosoft.luxem.read.RawReader;
import com.zarbosoft.rendaw.common.Pair;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PeekVersion {
  public static String check(Path path) {
    try (InputStream source = Files.newInputStream(path)) {
      return BufferedRawReader.streamEvents(source, new RawReader.DefaultEventFactory())
          .map(p -> p.first)
          .findFirst()
          .filter(e -> e instanceof LTypeEvent)
          .map(e -> ((LTypeEvent) e).value)
          .orElse(null);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
