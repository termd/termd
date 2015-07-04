/*
 * Copyright 2015 Julien Viet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.termd.core.readline;

import io.termd.core.tty.TtyConnection;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Make this class thread safe as SSH will access this class with different threds [sic].
 *
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
   * @param conn the tty connection
   * @param requestHandler the requestHandler
   */
  public void readline(TtyConnection conn, String prompt, Consumer<String> requestHandler) {
    readline(conn, prompt, requestHandler, null);
  }

  /**
   * Read a line until a request can be processed.
   *
   * @param conn the tty connection
   * @param requestHandler the requestHandler
   */
  public void readline(TtyConnection conn, String prompt, Consumer<String> requestHandler, Consumer<Completion> completionHandler) {
    Consumer<int[]> previousEventHandler = conn.getReadHandler();
    Interaction interaction = new Interaction(conn, previousEventHandler, requestHandler, completionHandler);
    conn.setReadHandler(interaction);
    conn.write(prompt);
  }

  private enum LineStatus {
    LITERAL, ESCAPED, QUOTED
  }

  private class Interaction implements Consumer<int[]> {

    private final Consumer<String> requestHandler;
    private final TtyConnection term;
    private final Consumer<int[]> previousEventHandler;
    private final KeyDecoder decoder;
    private final Consumer<Completion> completionHandler;

    public Interaction(TtyConnection term, Consumer<int[]> previousEventHandler, Consumer<String> requestHandler, Consumer<Completion> completionHandler) {
      this.term = term;
      this.previousEventHandler = previousEventHandler;
      this.decoder = new KeyDecoder(keymap);
      this.requestHandler = requestHandler;
      this.completionHandler = completionHandler;
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

      if (completing) {
        throw new UnsupportedOperationException("Handle me gracefully");
      }

      LineBuffer copy = new LineBuffer(lineBuffer);
      if (event instanceof KeyEvent) {
        KeyEvent key = (KeyEvent) event;
        if (key.length() == 1 && key.getAt(0) == '\r') {
          for (int j : lineBuffer) {
            filter.accept(j);
          }
          if (lineStatus == LineStatus.ESCAPED) {
            filter.accept((int) '\r'); // Correct status
            term.write("\r\n> ");
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
              term.write("\r\n> ");
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
              term.write("\r\n");
              lineBuffer.setSize(0);
              term.setReadHandler(previousEventHandler);
              requestHandler.accept(raw.toString());
              return true;
            }
          }
        } else if (key.length() == 1 && key.getAt(0) == '\t') {
          if (completionHandler != null) {
            int index = lineBuffer.getCursor();
            while (index > 0 && lineBuffer.getAt(index - 1) != ' ') {
              index--;
            }
            int[] text = new int[lineBuffer.getCursor() - index];
            for (int i = 0; i < text.length;i++) {
              text[i] = lineBuffer.getAt(index + i);
            }
            completing = true;
            AtomicBoolean completed = new AtomicBoolean();
            completionHandler.accept(new Completion() {
              @Override
              public int[] text() {
                return text;
              }
              @Override
              public void complete(List<int[]> completions) {
                if (completed.compareAndSet(false, true)) {
                  if (completions.size() == 0) {
                    // Do nothing
                  } else if (completions.size() == 1) {
                    lineBuffer.insert(completions.get(0));
                  } else {
                    // To do
                  }
                  completing = false;

                  // That's a copy paste to sync with buffer -> improve that
                  term.writeHandler().accept(copy.compute(lineBuffer));
                }
              }
            });
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
      term.writeHandler().accept(copy.compute(lineBuffer));
      return false;
    }

    private final LinkedList<int[]> lines = new LinkedList<>();
    private final LineBuffer lineBuffer = new LineBuffer();
    private LinkedList<Integer> escaped = new LinkedList<>();
    private LineStatus lineStatus = LineStatus.LITERAL;
    private boolean completing = false;
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
