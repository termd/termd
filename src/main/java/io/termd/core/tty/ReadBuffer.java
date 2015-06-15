package io.termd.core.tty;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ReadBuffer implements Consumer<int[]> {

  private final Queue<int[]> queue = new ArrayDeque<>(10);
  private final Executor executor;
  private volatile Consumer<int[]> readHandler;

  public ReadBuffer(Executor executor) {
    this.executor = executor;
  }

  @Override
  public void accept(int[] data) {
    queue.add(data);
    while (readHandler != null && queue.size() > 0) {
      data = queue.poll();
      if (data != null) {
        readHandler.accept(data);
      }
    }
  }

  public Consumer<int[]> getReadHandler() {
    return readHandler;
  }

  public void setReadHandler(final Consumer<int[]> readHandler) {
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
      executor.execute(() -> {
        if (readHandler != null) {
          final int[] data = queue.poll();
          if (data != null) {
            readHandler.accept(data);
            drainQueue();
          }
        }
      });
    }
  }
}
