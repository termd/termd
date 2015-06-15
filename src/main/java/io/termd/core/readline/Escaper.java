package io.termd.core.readline;

import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
interface Escaper extends Consumer<Integer> {

  void escaping();

  void escaped(int ch);

  void beginQuotes(int delim);

  void endQuotes(int delim);

}
