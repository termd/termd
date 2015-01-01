package io.modsh.core.writeline;

import io.modsh.core.Handler;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Escaper extends Handler<Integer> {

  void escaping();

  void escaped(int ch);

  void beginQuotes(int delim);

  void endQuotes(int delim);

}
