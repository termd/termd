package io.modsh.core.readline;

import io.modsh.core.Handler;
import io.modsh.core.writeline.EscapeFilter;
import io.modsh.core.writeline.Escaper;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class ActionHandler implements Handler<Action> {

  final Handler<int[]> output;

  public ActionHandler(Handler<int[]> output) {
    output.handle(new int[]{'%', ' '});
    this.output = output;
  }

  private EscapeFilter escapeFilter = new EscapeFilter(new Escaper() {

    private boolean escaped;

    @Override
    public void beginEscape(int delimiter) {
      escaped = true;
      output.handle(new int[]{delimiter});
    }

    @Override
    public void endEscape(int delimiter) {
      escaped = false;
      if (delimiter != '\\') {
        output.handle(new int[]{delimiter});
      }
    }

    @Override
    public void handle(Integer value) {
      if (value == '\r') {
        if (escaped) {
          output.handle(new int[]{'\r','\n','>',' '});
        } else {
          output.handle(new int[]{'\r','\n','%', ' '});
        }
      } else {
        output.handle(new int[]{value});
      }
    }
  });

  public void handle(Action action) {
    if (action instanceof KeyAction) {
      KeyAction key = (KeyAction) action;
      for (int i = 0;i < key.length();i++) {
        escapeFilter.handle(key.getAt(i));
      }
    } else {
      FunctionAction fname = (FunctionAction) action;
      System.out.println("Function " + fname.getName());
    }
  }

}
