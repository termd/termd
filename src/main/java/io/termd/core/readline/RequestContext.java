package io.termd.core.readline;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class RequestContext {

  final String raw;

  public RequestContext(String raw) {
    this.raw = raw;
  }

  /**
   * Signal the request is processed
   */
  public void end() {
  }
}
