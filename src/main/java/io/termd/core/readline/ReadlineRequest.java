package io.termd.core.readline;

import io.termd.core.Handler;
import io.termd.core.term.TermEvent;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface ReadlineRequest {

  int requestCount();

  String getData();

  void eventHandler(Handler<TermEvent> handler);

  /**
   * Write the specified string to the client
   *
   * @param s the string to write
   * @return this request object
   * @throws java.lang.IllegalStateException if the request is ended
   */
  ReadlineRequest write(String s);

  /**
   * Signal the request is processed
   */
  void end();

}
