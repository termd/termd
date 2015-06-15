package io.termd.core.readline;

import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Helper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Readline {

  private final Keymap keymap;
  private final Map<String, Function> functions = new HashMap<>();

  public Readline(Keymap keymap) {
    this.keymap = keymap;
  }

  public Readline addFunction(Function function) {
    functions.put(function.name(), function);
    return this;
  }

  /**
   * Read a line until a request can be processed.
   *
   * @param term the term to read from
   * @param requestHandler the requestHandler
   */
  public void readline(TtyConnection term, String prompt, Consumer<String> requestHandler) {
    Consumer<int[]> previousEventHandler = term.getReadHandler();
    Interaction interaction = new Interaction(term, previousEventHandler, requestHandler);
    term.setReadHandler(interaction);
    term.writeHandler().accept(Helper.toCodePoints(prompt));
  }

  private enum LineStatus {
    LITERAL, ESCAPED, QUOTED
  }

  private class Interaction implements Consumer<int[]> {

    private final Consumer<String> requestHandler;
    private final TtyConnection term;
    private final Consumer<int[]> previousEventHandler;
    private final KeyDecoder decoder;

    public Interaction(TtyConnection term, Consumer<int[]> previousEventHandler, Consumer<String> requestHandler) {
      this.term = term;
      this.previousEventHandler = previousEventHandler;
      this.decoder = new KeyDecoder(keymap);
      this.requestHandler = requestHandler;
    }

    @Override
    public void accept(int[] data) {
      decoder.append(data);
      while (decoder.hasNext()) {
        if (handle(decoder.next())) {
          break;
        }
      }
    }

    public boolean handle(final Event event) {
      LineBuffer copy = new LineBuffer(lineBuffer);
      if (event instanceof KeyEvent) {
        KeyEvent key = (KeyEvent) event;
        if (key.length() == 1 && key.getAt(0) == '\r') {
          for (int j : lineBuffer) {
            filter.accept(j);
          }
          if (lineStatus == LineStatus.ESCAPED) {
            filter.accept((int) '\r'); // Correct status
            term.writeHandler().accept(new int[]{'\r', '\n', '>', ' '});
            lineBuffer.setSize(0);
            copy.setSize(0);
          } else {
            int[] l = new int[this.escaped.size()];
            for (int index = 0;index < l.length;index++) {
              l[index] = this.escaped.get(index);
            }
            escaped.clear();
            lines.add(l);
            if (lineStatus == LineStatus.QUOTED) {
              term.writeHandler().accept(new int[]{'\r', '\n', '>', ' '});
              lineBuffer.setSize(0);
              copy.setSize(0);
            } else {
              final StringBuilder raw = new StringBuilder();
              for (int index = 0;index < lines.size();index++) {
                int[] a = lines.get(index);
                if (index > 0) {
                  raw.append('\n'); // Use \n for processing
                }
                for (int b : a) {
                  raw.appendCodePoint(b);
                }
              }
              lines.clear();
              escaped.clear();
              term.writeHandler().accept(new int[]{'\r', '\n'});
              lineBuffer.setSize(0);
              term.setReadHandler(previousEventHandler);
              requestHandler.accept(raw.toString());
              return true;
            }
          }
        } else {
          for (int i = 0;i < key.length();i++) {
            int codePoint = key.getAt(i);
            lineBuffer.insert(codePoint);
          }
        }
      } else {
        FunctionEvent fname = (FunctionEvent) event;
        Function function = functions.get(fname.name());
        if (function != null) {
          function.apply(lineBuffer);
        } else {
          System.out.println("Unimplemented function " + fname.name());
        }
      }
      LinkedList<Integer> a = copy.compute(lineBuffer);
      int[] t = new int[a.size()];
      for (int index = 0;index < a.size();index++) {
        t[index] = a.get(index);
      }
      term.writeHandler().accept(t);
      return false;
    }

    private final LinkedList<int[]> lines = new LinkedList<>();
    private final LineBuffer lineBuffer = new LineBuffer();
    private LinkedList<Integer> escaped = new LinkedList<>();
    private LineStatus lineStatus = LineStatus.LITERAL;
    private EscapeFilter filter = new EscapeFilter(new Escaper() {
      @Override
      public void escaping() {
        lineStatus = LineStatus.ESCAPED;
      }
      @Override
      public void escaped(int ch) {
        if (ch != '\r') {
          escaped.add((int) '\\');
          escaped.add(ch);
        }
        lineStatus = LineStatus.LITERAL;
      }
      @Override
      public void beginQuotes(int delim) {
        escaped.add(delim);
        lineStatus = LineStatus.QUOTED;
      }
      @Override
      public void endQuotes(int delim) {
        escaped.add(delim);
        lineStatus = LineStatus.LITERAL;
      }
      @Override
      public void accept(Integer event) {
        escaped.add(event);
      }
    });
  }
}
