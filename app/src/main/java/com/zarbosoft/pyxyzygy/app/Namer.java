package com.zarbosoft.pyxyzygy.app;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Namer {
  public Map<String, Integer> names = new HashMap<>();

  public static Pattern nameCountPattern = Pattern.compile("(.*) \\([0-9]+\\)$");

  public static String splitName(String name) {
    return nameCountPattern.matcher(name).replaceAll(r -> r.group(1));
  }

  public void countUniqueName(String name) {
    uniqueName(splitName(name));
  }

  /**
   * Start at count 1 (unwritten) for machine generated names
   *
   * @param name
   * @return
   */
  public String uniqueName(String name) {
    name = splitName(name);
    int count = names.compute(name, (n, i) -> i == null ? 1 : i + 1);
    return count == 1 ? name : String.format("%s (%s)", name, count);
  }

  /**
   * Start at count 2 if the first element is not in the map (user decided name)
   *
   * @param name
   * @return
   */
  public String uniqueName1(String name) {
    name = splitName(name);
    int count = names.compute(name, (n, i) -> i == null ? 2 : i + 1);
    return count == 1 ? name : String.format("%s (%s)", name, count);
  }
}
