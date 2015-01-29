package io.termd.core.term;

import io.termd.core.util.Handler;

/**
 * A connection to a term.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface TermConnection {

  /**
   * Set the event handler on this connection.
   *
   * @param handler the event handler
   */
  void eventHandler(Handler<TermEvent> handler);

  /**
   * @return the data handler of this connection
   */
  Handler<int[]> dataHandler();

  /**
   * Schedule a task for execution.
   *
   * @param task the task to schedule
   */
  void schedule(Runnable task);

}
