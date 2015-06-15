package io.termd.core.tty;

import io.termd.core.util.Dimension;

import java.util.function.Consumer;

/**
 * A connection to a tty.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface TtyConnection {

  Consumer<String> getTermHandler();

  void setTermHandler(Consumer<String> handler);

  Consumer<Dimension> getResizeHandler();

  void setResizeHandler(Consumer<Dimension> handler);

  Consumer<Signal> getSignalHandler();

  void setSignalHandler(Consumer<Signal> handler);

  Consumer<int[]> getReadHandler();

  /**
   * Set the read handler on this connection.
   *
   * @param handler the event handler
   */
  void setReadHandler(Consumer<int[]> handler);

  /**
   * @return the write handler of this connection
   */
  Consumer<int[]> writeHandler();

  /**
   * Schedule a task for execution.
   *
   * @param task the task to schedule
   */
  void schedule(Runnable task);

}
