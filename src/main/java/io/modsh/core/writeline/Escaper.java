package io.modsh.core.writeline;

import io.modsh.core.Handler;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Escaper extends Handler<Integer> {

  void beginEscape(int delimiter);

  void endEscape(int delimiter);

}
