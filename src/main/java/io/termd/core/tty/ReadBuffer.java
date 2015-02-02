package io.termd.core.tty;

import io.termd.core.util.Handler;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ReadBuffer implements Handler<int[]> {

  private final Queue<int[]> queue = new ArrayDeque<>(10);
  private final Executor executor;
  private volatile Handler<int[]> readHandler;

  public ReadBuffer(Executor executor) {
    this.executor = executor;
  }

  @Override
  public void handle(int[] data) {
    queue.add(data);
    while (readHandler != null && queue.size() > 0) {
      data = queue.poll();
      if (data != null) {
        readHandler.handle(data);
      }
    }
  }

  public Handler<int[]> getReadHandler() {
    return readHandler;
  }

  public void setReadHandler(final Handler<int[]> readHandler) {
    if (readHandler != null) {
      if (this.readHandler != null) {
        this.readHandler = readHandler;
      } else {
        ReadBuffer.this.readHandler = readHandler;
        drainQueue();
      }
    } else {
      this.readHandler = null;
    }
  }

  private void drainQueue() {
    if (queue.size() > 0 && readHandler != null) {
      executor.execute(new Runnable() {
        @Override
        public void run() {
          if (readHandler != null) {
            final int[] data = queue.poll();
            if (data != null) {
              readHandler.handle(data);
              drainQueue();
            }
          }
        }
      });
    }
  }
}
