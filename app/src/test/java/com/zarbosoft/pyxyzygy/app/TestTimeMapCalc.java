package com.zarbosoft.pyxyzygy.app;

import com.google.common.collect.ImmutableList;
import com.zarbosoft.automodel.lib.ModelBase;
import com.zarbosoft.luxem.read.StackReader;
import com.zarbosoft.pyxyzygy.app.parts.timeline.Timeline;
import com.zarbosoft.pyxyzygy.core.model.latest.GroupTimeFrame;
import com.zarbosoft.rendaw.common.Assertion;
import org.junit.Test;

import java.util.List;

public class TestTimeMapCalc {
  private static ModelBase context =
      new ModelBase(new ModelBase.TestMarkerArg()) {
        @Override
        public ModelBase migrate() {
          return this;
        }

        @Override
        public boolean needsMigrate() {
          return false;
        }

        @Override
        public void clearHistory() {}

        @Override
        public StackReader.State deserializeChange(String type) {
          throw new Assertion();
        }

        @Override
        public void close() {}
      };

  private static GroupTimeFrame inner(int length, int offset, int loop) {
    GroupTimeFrame out = GroupTimeFrame.create(context);
    out.initialLengthSet(context, length);
    out.initialInnerOffsetSet(context, offset);
    out.initialInnerLoopSet(context, loop);
    return out;
  }

  private static void compare(List<FrameMapEntry> got, List<FrameMapEntry> expected) {
    boolean hasErr = false;
    StringBuilder err = new StringBuilder();
    for (int i = 0; i < Math.max(got.size(), expected.size()); ++i) {
      FrameMapEntry got1 = i < got.size() ? got.get(i) : null;
      FrameMapEntry expected1 = i < expected.size() ? expected.get(i) : null;
      if (got1 == null) {
        hasErr = true;
        err.append(
            String.format("%s: got null, exp %s %s\n", i, expected1.length, expected1.innerOffset));
        continue;
      }
      if (expected1 == null) {
        hasErr = true;
        err.append(String.format("%s: got extra %s %s\n", i, got1.length, got1.innerOffset));
        continue;
      }
      if (got1.length != expected1.length || got1.innerOffset != expected1.innerOffset) {
        hasErr = true;
        err.append(
            String.format(
                "%s: got %s %s, exp %s %s\n",
                i, got1.length, got1.innerOffset, expected1.length, expected1.innerOffset));
      }
    }
    if (hasErr) throw new AssertionError("\n" + err.toString());
  }

  @Test
  public void testBase() {
    compare(
        Timeline.computeSubMap(
            ImmutableList.of(new FrameMapEntry(Global.NO_LENGTH, 0)),
            ImmutableList.of(inner(Global.NO_LENGTH, 0, Global.NO_LOOP))),
        ImmutableList.of(new FrameMapEntry(Global.NO_LENGTH, 0)));
  }

  @Test
  public void testOuterNoInner() {
    compare(
        Timeline.computeSubMap(
            ImmutableList.of(new FrameMapEntry(Global.NO_LENGTH, Global.NO_INNER)),
            ImmutableList.of(inner(Global.NO_LENGTH, 0, Global.NO_LOOP))),
        ImmutableList.of(new FrameMapEntry(Global.NO_LENGTH, Global.NO_INNER)));
  }

  @Test
  public void testInnerNoInner() {
    compare(
        Timeline.computeSubMap(
            ImmutableList.of(new FrameMapEntry(Global.NO_LENGTH, 0)),
            ImmutableList.of(inner(Global.NO_LENGTH, Global.NO_INNER, Global.NO_LOOP))),
        ImmutableList.of(new FrameMapEntry(Global.NO_LENGTH, Global.NO_INNER)));
  }

  @Test
  public void testTwoOuter() {
    compare(
        Timeline.computeSubMap(
            ImmutableList.of(new FrameMapEntry(10, 0), new FrameMapEntry(Global.NO_LENGTH, 0)),
            ImmutableList.of(inner(Global.NO_LENGTH, 0, Global.NO_LOOP))),
        ImmutableList.of(new FrameMapEntry(10, 0), new FrameMapEntry(Global.NO_LENGTH, 0)));
  }

  @Test
  public void testTwoInner() {
    compare(
        Timeline.computeSubMap(
            ImmutableList.of(new FrameMapEntry(Global.NO_LENGTH, 0)),
            ImmutableList.of(
                inner(10, 0, Global.NO_LOOP), inner(Global.NO_LENGTH, 0, Global.NO_LOOP))),
        ImmutableList.of(new FrameMapEntry(10, 0), new FrameMapEntry(Global.NO_LENGTH, 0)));
  }

  @Test
  public void testInnerOffset() {
    compare(
        Timeline.computeSubMap(
            ImmutableList.of(new FrameMapEntry(Global.NO_LENGTH, 30)),
            ImmutableList.of(
                inner(30, 5, Global.NO_LOOP), inner(Global.NO_LENGTH, 11, Global.NO_LOOP))),
        ImmutableList.of(new FrameMapEntry(Global.NO_LENGTH, 11)));
  }

  @Test
  public void testInnerOffsetSpanStart() {
    compare(
        Timeline.computeSubMap(
            ImmutableList.of(new FrameMapEntry(Global.NO_LENGTH, 35)),
            ImmutableList.of(
                inner(30, 5, Global.NO_LOOP), inner(Global.NO_LENGTH, 11, Global.NO_LOOP))),
        ImmutableList.of(new FrameMapEntry(Global.NO_LENGTH, 16)));
  }

  @Test
  public void testSpanInnerEnd() {
    compare(
        Timeline.computeSubMap(
            ImmutableList.of(new FrameMapEntry(Global.NO_LENGTH, 5)),
            ImmutableList.of(
                inner(10, 0, Global.NO_LOOP), inner(Global.NO_LENGTH, 0, Global.NO_LOOP))),
        ImmutableList.of(new FrameMapEntry(5, 5), new FrameMapEntry(Global.NO_LENGTH, 0)));
  }

  @Test
  public void testSpanInnerEndSum() {
    compare(
        Timeline.computeSubMap(
            ImmutableList.of(
                new FrameMapEntry(15, 0), new FrameMapEntry(Global.NO_LENGTH, Global.NO_INNER)),
            ImmutableList.of(
                inner(4, 0, Global.NO_LOOP),
                inner(5, 0, Global.NO_LOOP),
                inner(9, 0, Global.NO_LOOP),
                inner(Global.NO_LENGTH, 0, Global.NO_LOOP))),
        ImmutableList.of(
            new FrameMapEntry(4, 0),
            new FrameMapEntry(5, 0),
            new FrameMapEntry(6, 0),
            new FrameMapEntry(Global.NO_LENGTH, Global.NO_INNER)));
  }

  @Test
  public void testSpanInnerStart() {
    compare(
        Timeline.computeSubMap(
            ImmutableList.of(
                new FrameMapEntry(6, 5), new FrameMapEntry(Global.NO_LENGTH, Global.NO_INNER)),
            ImmutableList.of(
                inner(7, 0, Global.NO_LOOP),
                inner(10, 0, Global.NO_LOOP),
                inner(Global.NO_LENGTH, 0, Global.NO_LOOP))),
        ImmutableList.of(
            new FrameMapEntry(2, 5),
            new FrameMapEntry(4, 0),
            new FrameMapEntry(Global.NO_LENGTH, Global.NO_INNER)));
  }

  @Test
  public void testLoopNoLength() {
    compare(
        Timeline.computeSubMap(
            ImmutableList.of(new FrameMapEntry(Global.NO_LENGTH, 0)),
            ImmutableList.of(inner(Global.NO_LENGTH, 0, 10))),
        ImmutableList.of(
            new FrameMapEntry(10, 0),
            new FrameMapEntry(10, 0),
            new FrameMapEntry(10, 0),
            new FrameMapEntry(10, 0),
            new FrameMapEntry(10, 0),
            new FrameMapEntry(10, 0)));
  }

  @Test
  public void testLoopNoLengthSpanEnd() {
    compare(
        Timeline.computeSubMap(
            ImmutableList.of(new FrameMapEntry(Global.NO_LENGTH, 5)),
            ImmutableList.of(inner(Global.NO_LENGTH, 0, 10))),
        ImmutableList.of(
            new FrameMapEntry(5, 5),
            new FrameMapEntry(10, 0),
            new FrameMapEntry(10, 0),
            new FrameMapEntry(10, 0),
            new FrameMapEntry(10, 0),
            new FrameMapEntry(10, 0)));
  }

  @Test
  public void testLoopWithLength() {
    compare(
        Timeline.computeSubMap(
            ImmutableList.of(
                new FrameMapEntry(10, 0), new FrameMapEntry(Global.NO_LENGTH, Global.NO_INNER)),
            ImmutableList.of(inner(Global.NO_LENGTH, 0, 10))),
        ImmutableList.of(
            new FrameMapEntry(10, 0), new FrameMapEntry(Global.NO_LENGTH, Global.NO_INNER)));
  }

  @Test
  public void testLoopSpanEnd() {
    compare(
        Timeline.computeSubMap(
            ImmutableList.of(
                new FrameMapEntry(15, 5), new FrameMapEntry(Global.NO_LENGTH, Global.NO_INNER)),
            ImmutableList.of(inner(Global.NO_LENGTH, 0, 10))),
        ImmutableList.of(
            new FrameMapEntry(5, 5),
            new FrameMapEntry(10, 0),
            new FrameMapEntry(Global.NO_LENGTH, Global.NO_INNER)));
  }

  @Test
  public void testLoopSpanEndFar() {
    compare(
        Timeline.computeSubMap(
            ImmutableList.of(
                new FrameMapEntry(15, 25), new FrameMapEntry(Global.NO_LENGTH, Global.NO_INNER)),
            ImmutableList.of(inner(Global.NO_LENGTH, 0, 10))),
        ImmutableList.of(
            new FrameMapEntry(5, 5),
            new FrameMapEntry(10, 0),
            new FrameMapEntry(Global.NO_LENGTH, Global.NO_INNER)));
  }

  @Test
  public void skipEarly() {
    compare(
        Timeline.computeSubMap(
            ImmutableList.of(new FrameMapEntry(Global.NO_LENGTH, 10)),
            ImmutableList.of(
                inner(7, Global.NO_INNER, Global.NO_LOOP),
                inner(3, Global.NO_INNER, Global.NO_LOOP),
                inner(Global.NO_LENGTH, 0, Global.NO_LOOP))),
        ImmutableList.of(new FrameMapEntry(Global.NO_LENGTH, 0)));
  }
}
