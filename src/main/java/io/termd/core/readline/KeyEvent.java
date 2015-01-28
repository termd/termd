package io.termd.core.readline;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
interface KeyEvent extends Event {

  int getAt(int index) throws IndexOutOfBoundsException;

  int length();

}
