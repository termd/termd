package io.termd.core.util;

/**
 * Handle an event.
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Handler<E> {

  /**
   * Handle an event.
   *
   * @param event the event
   */
  void handle(E event);
}
