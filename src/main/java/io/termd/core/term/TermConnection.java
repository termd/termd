package io.termd.core.term;

import io.termd.core.Handler;

import java.util.Map;

/**
 * A connection to a term.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface TermConnection {

  void eventHandler(Handler<TermEvent> handler);

  /**
   * @return the chars handler of this connection
   */
  Handler<int[]> charsHandler();


  void schedule(Runnable task);

}
