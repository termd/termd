package io.modsh.core.readline;

import io.modsh.core.writeline.EscapeFilter;
import io.modsh.core.writeline.Escaper;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ActionHandler {

  public ActionHandler() {
    System.out.print("% ");
  }

  private EscapeFilter escapeFilter = new EscapeFilter(new Escaper() {

    private boolean escaped;

    @Override
    public void beginEscape(int delimiter) {
      escaped = true;
      for (char c : Character.toChars(delimiter)) {
        System.out.print(c);
      }
    }

    @Override
    public void endEscape(int delimiter) {
      escaped = false;
      if (delimiter != '\\') {
        for (char c : Character.toChars(delimiter)) {
          System.out.print(c);
        }
      }
    }

    @Override
    public void accept(int value) {
      if (value == '\r') {
        if (escaped) {
          System.out.println();
          System.out.print("> ");
        } else {
          System.out.println();
          System.out.print("% ");
        }
      } else {
        for (char c : Character.toChars(value)) {
          System.out.print(c);
        }
      }
    }
  });

  public void handle(Action action) {
    if (action instanceof KeyAction) {
      KeyAction key = (KeyAction) action;
      for (int i = 0;i < key.length();i++) {
        escapeFilter.accept(key.getAt(i));
      }
    } else {
      FunctionAction fname = (FunctionAction) action;
      System.out.println("Function " + fname.getName());
    }
  }

}
