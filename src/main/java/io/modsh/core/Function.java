package io.modsh.core;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Function<A, R> {

  /**
   * Call the function
   *
   * @param argument the argument
   * @return the returned value
   */
  R call(A argument);
}
