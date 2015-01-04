package io.termd.core.readline;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface RequestContext {

  String getRaw();

  /**
   * Signal the request is processed
   */
  void end();

}
