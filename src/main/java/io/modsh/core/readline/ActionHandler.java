package io.modsh.core.readline;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ActionHandler {

  public void handle(Action action) {
    if (action instanceof KeyAction) {
      KeyAction key = (KeyAction) action;
      System.out.println("Key " + key);
    } else {
      FunctionAction fname = (FunctionAction) action;
      System.out.println("Function " + fname.getName());
    }
  }

}
