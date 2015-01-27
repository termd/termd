package io.termd.core.term;

import io.termd.core.Handler;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface TermRequest {

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
  TermRequest write(String s);

  /**
   * Signal the request is processed
   */
  void end();

}
