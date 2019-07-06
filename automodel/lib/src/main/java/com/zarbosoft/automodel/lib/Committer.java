package com.zarbosoft.automodel.lib;

import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import static com.zarbosoft.automodel.lib.Logger.logger;

public class Committer {
  private final ReadWriteLock lock;

  /** This controls writing in UI thread vs reading from flush+render threads */
  public final AtomicReference<Timer> flushTimer = new AtomicReference<>();
  private final Map<Committable, Object> dirty = new ConcurrentHashMap<>();
  private final ModelBase model;

  public Committer(ReadWriteLock lock, ModelBase model) {
    this.lock = lock;
    this.model = model;
  }

  public void commitAll() {
    Timer timer = flushTimer.getAndSet(null);
    if (timer != null) timer.cancel();

    Lock readLock = lock.readLock();
    Iterator<Map.Entry<Committable, Object>> i = dirty.entrySet().iterator();
    while (i.hasNext()) {
      Committable dirty = i.next().getKey();
      i.remove();
      readLock.lock();
      try {
        dirty.commit(model);
      } finally {
        readLock.unlock();
      }
    }
  }

  public void setDirty(Committable dirty) {
    this.dirty.put(dirty, Object.class);
    Timer newTimer;
    String timerName = "dirty-flush";
    Timer oldTimer = flushTimer.getAndSet(newTimer = new Timer(timerName));
    if (oldTimer != null) oldTimer.cancel();
    newTimer.schedule(
      new TimerTask() {
        @Override
        public void run() {
          try {
            commitAll();
          } catch (Exception e) {
            logger.writeException(e, "Error during project flush");
          }
        }
      },
      5000);
  }
}
