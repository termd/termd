package io.modsh.core.term;

import io.modsh.core.Handler;

import java.util.Map;

/**
 * A connection to a term.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface TermConnection {

  /**
   * Set an handler to receive window size change events.
   *
   * @param handler the size handler.
   */
  void sizeHandler(Handler<Map.Entry<Integer, Integer>> handler);

  /**
   * Set an handler for receiving chars events.
   *
   * @param handler the chars handler
   */
  void charsHandler(Handler<int[]> handler);

}
