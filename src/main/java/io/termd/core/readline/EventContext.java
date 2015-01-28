package io.termd.core.readline;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
interface EventContext {

  Event getEvent();

  void end();

}
