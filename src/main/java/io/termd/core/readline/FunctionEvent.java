package io.termd.core.readline;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface FunctionEvent extends Event {

  /**
   * @return the action name
   */
  String getName();

}
