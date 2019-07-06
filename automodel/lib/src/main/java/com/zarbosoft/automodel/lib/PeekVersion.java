package com.zarbosoft.automodel.lib;

import com.zarbosoft.luxem.events.LTypeEvent;
import com.zarbosoft.luxem.read.BufferedRawReader;
import com.zarbosoft.luxem.read.RawReader;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.zarbosoft.rendaw.common.Common.uncheck;

public class PeekVersion {
  public static String check(Path path) {
    return uncheck(
        () -> {
          try (InputStream source = Files.newInputStream(path)) {
            return BufferedRawReader.streamEvents(source, new RawReader.DefaultEventFactory())
                .findFirst()
                .filter(p -> p.first instanceof LTypeEvent)
                .map(p -> ((LTypeEvent) p.first).value)
                .orElse(null);
          }
        });
  }
}
