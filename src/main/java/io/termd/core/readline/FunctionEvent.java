package io.termd.core.readline;

/**
 * A function event.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
class FunctionEvent implements Event {

  private final String name;

  public FunctionEvent(String name) {
    this.name = name;
  }

  /**
   * @return the name of the function to apply.
   */
  String name() {
    return name;
  }
}
