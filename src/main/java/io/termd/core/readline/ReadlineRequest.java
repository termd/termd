package io.termd.core.readline;

import io.termd.core.util.Handler;
import io.termd.core.term.TermEvent;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface ReadlineRequest {

  /**
   * @return the request count.
   */
  int requestCount();

  /**
   * @return the line to process.
   */
  String line();

  /**
   * Set a term  event handler to catch term event occuring during the lifetime of this request. This handler
   * override the {@link io.termd.core.readline.ReadlineHandler} events.
   *
   * @param handler the event handler
   */
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
