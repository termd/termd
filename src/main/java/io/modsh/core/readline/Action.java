package io.modsh.core.readline;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public interface Action {

  int getAt(int index) throws IndexOutOfBoundsException;

  int length();

}
