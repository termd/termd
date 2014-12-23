package io.modsh.core.writeline;

import java.util.function.IntConsumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Escaper extends IntConsumer {

  void beginEscape(int delimiter);

  void endEscape(int delimiter);

}
