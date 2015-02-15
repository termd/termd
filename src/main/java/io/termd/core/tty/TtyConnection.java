package io.termd.core.tty;

import io.termd.core.util.Dimension;
import io.termd.core.util.Handler;

/**
 * A connection to a tty.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface TtyConnection {

  Handler<String> getTermHandler();

  void setTermHandler(Handler<String> handler);

  Handler<Dimension> getResizeHandler();

  void setResizeHandler(Handler<Dimension> handler);

  Handler<Signal> getSignalHandler();

  void setSignalHandler(Handler<Signal> handler);

  Handler<int[]> getReadHandler();

  /**
   * Set the read handler on this connection.
   *
   * @param handler the event handler
   */
  void setReadHandler(Handler<int[]> handler);

  /**
   * @return the write handler of this connection
   */
  Handler<int[]> writeHandler();

  /**
   * Schedule a task for execution.
   *
   * @param task the task to schedule
   */
  void schedule(Runnable task);

}
