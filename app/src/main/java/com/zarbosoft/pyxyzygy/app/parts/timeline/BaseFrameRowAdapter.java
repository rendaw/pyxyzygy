package com.zarbosoft.pyxyzygy.app.parts.timeline;

import com.zarbosoft.pyxyzygy.app.Window;
import com.zarbosoft.pyxyzygy.app.model.v0.ProjectContext;
import com.zarbosoft.pyxyzygy.app.wrappers.FrameFinder;
import com.zarbosoft.pyxyzygy.core.model.v0.ChangeStepBuilder;
import com.zarbosoft.rendaw.common.Assertion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.zarbosoft.pyxyzygy.app.Global.NO_INNER;
import static com.zarbosoft.pyxyzygy.app.Global.NO_LENGTH;

public abstract class BaseFrameRowAdapter<N, F> extends RowAdapter {
  Optional<RowFramesWidget> row = Optional.empty();
  public final Timeline timeline;

  protected BaseFrameRowAdapter(Timeline timeline) {
    this.timeline = timeline;
  }

  public abstract FrameFinder<N, F> getFrameFinder();

  @Override
  public final boolean duplicateFrame(
      ProjectContext context, Window window, ChangeStepBuilder change, int outer) {
    int inner = window.timeToInner(outer);
    if (inner == NO_INNER) return false;
    FrameFinder.Result<F> previous = getFrameFinder().findFrame(getNode(), inner);
    F source = null;
    if (timeline.selectedFrame.get() != null)
      source = getFrameFinder().frameGet(getNode(), timeline.selectedFrame.get().index);
    if (source == null) source = getFrameFinder().findFrame(getNode(), inner).frame;
    F newFrame = innerDuplicateFrame(context, source);
    if (previous.at != inner) return createFrame(context, change, inner, previous, newFrame);
    else return createFrameInsert(context, change, previous.frameIndex, newFrame);
  }

  @Override
  public final boolean createFrame(
      ProjectContext context, Window window, ChangeStepBuilder change, int outer) {
    int inner = window.timeToInner(outer);
    if (inner == NO_INNER) return false;
    FrameFinder.Result<F> previous = getFrameFinder().findFrame(getNode(), inner);
    F newFrame = innerCreateFrame(context, previous.frame);
    if (previous.at != inner) return createFrame(context, change, inner, previous, newFrame);
    else return createFrameInsert(context, change, previous.frameIndex, newFrame);
  }

  private boolean createFrameInsert(
      ProjectContext context, ChangeStepBuilder change, int frameIndex, F newFrame) {
    setFrameInitialLength(context, newFrame, 1);
    addFrame(change, frameIndex, newFrame);
    row.ifPresent(
        r ->
            r.frames.stream()
                .filter(f -> f.frame.id() == newFrame)
                .findFirst()
                .ifPresent(w -> timeline.select(w)));
    return true;
  }

  protected abstract F innerDuplicateFrame(ProjectContext context, F source);

  public boolean createFrame(
      ProjectContext context,
      ChangeStepBuilder change,
      int inner,
      FrameFinder.Result<F> previous,
      F newFrame) {
    int offset = inner - previous.at;
    if (offset <= 0) throw new Assertion();
    timeline.select(null);
    if (getFrameLength(previous.frame) == NO_LENGTH) {
      setFrameInitialLength(context, newFrame, NO_LENGTH);
    } else {
      setFrameInitialLength(context, newFrame, getFrameLength(previous.frame) - offset);
    }
    setFrameLength(change, previous.frame, offset);
    addFrame(change, previous.frameIndex + 1, newFrame);
    row.ifPresent(
        r ->
            r.frames.stream()
                .filter(f -> f.frame.id() == newFrame)
                .findFirst()
                .ifPresent(w -> timeline.select(w)));
    return true;
  }

  protected abstract N getNode();

  protected abstract F innerCreateFrame(ProjectContext context, F previousFrame);

  protected abstract void addFrame(ChangeStepBuilder change, int at, F frame);

  protected abstract void setFrameLength(ChangeStepBuilder change, F frame, int length);

  protected abstract void setFrameInitialLength(ProjectContext context, F frame, int length);

  protected abstract int getFrameLength(F frame);

  @Override
  public final boolean frameAt(Window window, int outer) {
    final int inner = window.timeToInner(outer);
    if (inner == NO_INNER) return false;
    FrameFinder.Result<F> previous = getFrameFinder().findFrame(getNode(), inner);
    return previous.at == inner;
  }

  @Override
  public final boolean hasFrames() {
    return true;
  }

  @Override
  public boolean hasNormalFrames() {
    return true;
  }

  @Override
  public void updateFrameMarker(ProjectContext context, Window window) {
    row.ifPresent(r -> r.updateFrameMarker(window));
  }

  public class AdapterFrame extends RowAdapterFrame {
    public final F f;
    public final int i;

    public AdapterFrame(F f, int i) {
      this.f = f;
      this.i = i;
    }

    @Override
    public Object id() {
      return f;
    }

    @Override
    public int length() {
      return getFrameLength(f);
    }

    @Override
    public void setLength(ProjectContext context, ChangeStepBuilder change, int length) {
      setFrameLength(change, f, length);
    }

    @Override
    public void remove(ProjectContext context, ChangeStepBuilder change) {
      int length = getFrameLength(getFrameFinder().frameGet(getNode(), i));
      removeFrame(change, i, 1);
      int previousIndex = i - 1;
      if (previousIndex > 0)
        setFrameLength(
            change,
            getFrameFinder().frameGet(getNode(), i - 1),
            length == -1
                ? -1
                : getFrameLength(getFrameFinder().frameGet(getNode(), i - 1)) + length);
    }

    @Override
    public void clear(ProjectContext context, ChangeStepBuilder change) {
      frameClear(change, f);
    }

    @Override
    public void moveLeft(ProjectContext context, ChangeStepBuilder change) {
      if (i == 0) return;
      F frameBefore = getFrameFinder().frameGet(getNode(), i - 1);
      moveFramesTo(change, i, 1, i - 1);
      final int lengthThis = getFrameLength(f);
      if (lengthThis == -1) {
        final int lengthBefore = getFrameLength(frameBefore);
        setFrameLength(change, f, lengthBefore);
        setFrameLength(change, frameBefore, lengthThis);
      }
    }

    @Override
    public void moveRight(ProjectContext context, ChangeStepBuilder change) {
      if (i == frameCount() - 1) return;
      F frameAfter = getFrameFinder().frameGet(getNode(), i + 1);
      moveFramesTo(change, i, 1, i + 1);
      final int lengthAfter = getFrameLength(frameAfter);
      if (lengthAfter == -1) {
        final int lengthThis = getFrameLength(f);
        setFrameLength(change, f, lengthAfter);
        setFrameLength(change, frameAfter, lengthThis);
      }
    }
  }

  @Override
  public final int updateTime(ProjectContext context, Window window) {
    return row.map(
            r -> {
              List<RowAdapterFrame> adapterFrames = new ArrayList<>();
              for (int i = 0; i < frameCount(); ++i) {
                adapterFrames.add(new AdapterFrame(getFrameFinder().frameGet(getNode(), i), i));
              }
              return r.updateTime(context, window, adapterFrames);
            })
        .orElse(0);
  }

  protected abstract void frameClear(ChangeStepBuilder change, F f);

  protected abstract int frameCount();

  protected abstract void removeFrame(ChangeStepBuilder change, int at, int count);

  protected abstract void moveFramesTo(ChangeStepBuilder change, int source, int count, int dest);

  @Override
  public final Object getData() {
    return getNode();
  }
}
