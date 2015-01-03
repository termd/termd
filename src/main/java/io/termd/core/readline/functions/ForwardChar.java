package io.termd.core.readline.functions;

import io.termd.core.readline.Function;
import io.termd.core.readline.LineBuffer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ForwardChar implements Function {

  @Override
  public String getName() {
    return "forward-char";
  }

  @Override
  public void call(LineBuffer buffer) {
    buffer.moveCursor(1);
  }
}
