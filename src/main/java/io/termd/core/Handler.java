package io.termd.core;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Handler<E> {
  void handle(E event);
}
