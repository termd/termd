package io.termd.core.readline;

import io.termd.core.Handler;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface RequestContext {
  

  String getRaw();

  void dataHandler(Handler<int[]> handler);

  /**
   * Write the specified string to the client
   *
   * @param s the string to write
   * @return this request object
   * @throws java.lang.IllegalStateException if the request is ended
   */
  RequestContext write(String s);

  /**
   * Signal the request is processed
   */
  void end();

}
