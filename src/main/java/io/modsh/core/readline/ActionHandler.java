package io.modsh.core.readline;

import io.modsh.core.Handler;
import io.modsh.core.writeline.EscapeFilter;
import io.modsh.core.writeline.Escaper;

import java.util.LinkedList;

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
    private LinkedList<String> lines = new LinkedList<>();
    private StringBuilder buffer = new StringBuilder();

    @Override
    public void beginQuotes(int delim) {
      escaped = true;
      output.handle(new int[]{delim});
    }

    @Override
    public void escaping() {
      output.handle(new int[]{'\\'});
    }

    @Override
    public void escaped(int ch) {
      if (ch == '\r') {
        output.handle(new int[]{'\r','\n','>',' '});
      } else {
        output.handle(new int[]{ch});
        buffer.appendCodePoint(ch);
      }
    }

    @Override
    public void endQuotes(int delim) {
      escaped = false;
      output.handle(new int[]{delim});
    }

    @Override
    public void handle(Integer value) {
      if (value == '\r') {
        lines.add(buffer.toString());
        if (escaped) {
          System.out.println("added:");
          System.out.println(">" + lines.peekLast() + "<");
          output.handle(new int[]{'\r', '\n', '>', ' '});
        } else {
          System.out.println("entered:");
          for (String line : lines) {
            System.out.println(">" + line + "<");
          }
          output.handle(new int[]{'\r','\n','%', ' '});
          lines.clear();
        }
        buffer.setLength(0);
      } else {
        output.handle(new int[]{value});
        buffer.appendCodePoint(value);
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
