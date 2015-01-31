package io.termd.core.tty;

import io.termd.core.util.Handler;

import java.util.LinkedList;
import java.util.concurrent.Executor;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ReadBuffer implements Handler<int[]> {

  private final LinkedList<int[]> events = new LinkedList<>();
  private final Executor executor;
  private Handler<int[]> readHandler;

  public ReadBuffer(Executor executor) {
    this.executor = executor;
  }

  @Override
  public void handle(int[] data) {
    if (readHandler == null) {
      events.addLast(data);
    } else {
      readHandler.handle(data);
    }
  }

  public Handler<int[]> getReadHandler() {
    return readHandler;
  }

  // Bug with setHandler when dispatching we need some kind of flag to either forbid it
  // or handle it properly
  public void setReadHandler(final Handler<int[]> readHandler) {
    if (readHandler != null) {
      if (this.readHandler != null) {
        this.readHandler = readHandler;
      } else {
        Runnable task = new Runnable() {
          @Override
          public void run() {
            if (events.size() > 0) {
              int[] data = events.removeFirst();
              readHandler.handle(data);
              executor.execute(this);
            } else {
              ReadBuffer.this.readHandler = readHandler;
            }
          }
        };
        executor.execute(task);
      }
    } else {
      this.readHandler = null;
    }
  }
}
