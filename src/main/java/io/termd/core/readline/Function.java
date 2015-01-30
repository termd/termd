package io.termd.core.readline;

/**
 * A readline function.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Function {

  /**
   * The function name, for instance <i>backward-char</i>.
   */
  String name();

  /**
   * Apply the function on the line buffer.
   *
   * @param buffer the buffer to update
   */
  void apply(LineBuffer buffer);

}
