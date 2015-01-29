package io.termd.core.util;

/**
 * Provide elements.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Provider<E> {

  /**
   * Provide an element.
   */
  E provide();
}
