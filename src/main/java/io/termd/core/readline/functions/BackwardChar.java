package io.termd.core.readline.functions;

import io.termd.core.readline.Function;
import io.termd.core.readline.LineBuffer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class BackwardChar implements Function {

  @Override
  public String name() {
    return "backward-char";
  }

  @Override
  public void apply(LineBuffer buffer) {
    buffer.moveCursor(-1);
  }
}
