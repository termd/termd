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
import io.termd.core.util.Dimension;

import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Make this class thread safe as SSH will access this class with different threds [sic].
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Readline {

  static enum Status {
    PAUSED, ACTIVE, COMPLETING
  }

  private final Keymap keymap;
  private final Map<String, Function> functions = new HashMap<>();
  private final KeyDecoder decoder;
  private Status status = Status.PAUSED;

  public Readline(Keymap keymap) {
    this.keymap = keymap;
    this.decoder = new KeyDecoder(keymap);
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
    if (status != Status.PAUSED) {
      throw new IllegalStateException("Wrong status " + status);
    }
    status = Status.ACTIVE;
    Consumer<int[]> prevEventHandler = conn.readHandler();
    Consumer<Dimension> prevResizeHandler = conn.resizeHandler();
    Interaction interaction = new Interaction(conn, prompt, prevEventHandler, prevResizeHandler, requestHandler, completionHandler);
    conn.write(prompt);
  }

  private enum LineStatus {
    LITERAL, ESCAPED, QUOTED
  }

  private class Interaction implements Consumer<int[]> {

    private final Consumer<String> requestHandler;
    private final TtyConnection conn;
    private final String prompt;
    private final Consumer<int[]> prevEventHandler;
    private final Consumer<Dimension> prevSizeHandler;
    private final Consumer<Completion> completionHandler;
    private final Map<IntBuffer, Runnable> handlers;
    private Dimension size;

    public Interaction(
        TtyConnection conn,
        String prompt,
        Consumer<int[]> previousEventHandler,
        Consumer<Dimension> previousSizeHandler,
        Consumer<String> requestHandler,
        Consumer<Completion> completionHandler) {
      this.conn = conn;
      this.prompt = prompt;
      this.prevEventHandler = previousEventHandler;
      this.requestHandler = requestHandler;
      this.completionHandler = completionHandler;
      this.prevSizeHandler = previousSizeHandler;
      this.handlers = new HashMap<>();

      this.conn.setReadHandler(this);
      conn.setResizeHandler(dim -> this.size = dim);

      handlers.put(Keys.CTRL_M.buffer().asReadOnlyBuffer(), () -> {
        LineBuffer copy = new LineBuffer(lineBuffer);
        for (int j : lineBuffer) {
          filter.accept(j);
        }
        if (lineStatus == LineStatus.ESCAPED) {
          filter.accept((int) '\r'); // Correct status
          conn.write("\r\n> ");
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
            conn.write("\r\n> ");
            lineBuffer.setSize(0);
            copy.setSize(0);
          }  else {
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
            conn.write("\r\n");
            lineBuffer.setSize(0);
            conn.setReadHandler(previousEventHandler);
            status = Status.PAUSED;
            requestHandler.accept(raw.toString());
          }
        }
      });

      handlers.put(Keys.CTRL_I.buffer().asReadOnlyBuffer(), () -> {
        if (completionHandler != null) {
          Dimension dim = size; // Copy ref
          int index = lineBuffer.getCursor();
          while (index > 0 && lineBuffer.getAt(index - 1) != ' ') {
            index--;
          }
          int[] text = new int[lineBuffer.getCursor() - index];
          for (int i = 0; i < text.length;i++) {
            text[i] = lineBuffer.getAt(index + i);
          }
          status = Status.COMPLETING;
          LineBuffer copy = new LineBuffer(lineBuffer);
          final AtomicReference<CompletionStatus> status = new AtomicReference<>(CompletionStatus.PENDING);
          completionHandler.accept(new Completion() {

            @Override
            public int[] text() {
              return text;
            }

            @Override
            public Dimension size() {
              return dim;
            }

            @Override
            public void end() {
              while (true) {
                CompletionStatus current = status.get();
                if (current != CompletionStatus.COMPLETED) {
                  if (status.compareAndSet(current, CompletionStatus.COMPLETED)) {
                    switch (current) {
                      case COMPLETING:
                        // Redraw last line with correct prompt
                        if (lines.size() == 0 && escaped.size() == 0) {
                          conn.write(prompt);
                        } else {
                          conn.write("> ");
                        }
                        conn.writeHandler().accept(new LineBuffer().compute(lineBuffer));
                        break;
                    }
                    // Update status
                    Readline.this.status = Status.ACTIVE;
                    // Schedule a resume
                    conn.schedule(Interaction.this::handle);
                    break;
                  }
                  // Try again
                } else {
                  throw new IllegalStateException();
                }
              }
            }

            @Override
            public Completion inline(int[] line) {
              if (status.compareAndSet(CompletionStatus.PENDING, CompletionStatus.INLINING)) {
                if (line.length > 0) {
                  lineBuffer.insert(line);
                  conn.writeHandler().accept(copy.compute(lineBuffer));
                }
              } else {
                throw new IllegalStateException();
              }
              return this;
            }

            @Override
            public Completion write(int[] data) {
              while (true) {
                CompletionStatus current = status.get();
                if ((current == CompletionStatus.PENDING || current == CompletionStatus.COMPLETING)) {
                  if (status.compareAndSet(current, CompletionStatus.COMPLETING)) {
                    if (current == CompletionStatus.PENDING) {
                      conn.write("\r\n");
                    }
                    conn.writeHandler().accept(data);
                    return this;
                  }
                  // Try again
                } else {
                  throw new IllegalStateException();
                }
              }
            }
          });
        }
      });
    }

    @Override
    public void accept(int[] data) {
      decoder.append(data);
      handle();
    }

    private void handle() {

      while (status == Status.ACTIVE && decoder.hasNext()) {
        Event event = decoder.next();
        LineBuffer copy = new LineBuffer(lineBuffer);
        if (event instanceof KeyEvent) {
          KeyEvent key = (KeyEvent) event;
          Runnable handler = handlers.get(key.buffer());
          if (handler != null) {
            handler.run();
          } else {
            for (int i = 0;i < key.length();i++) {
              int codePoint = key.getAt(i);
              lineBuffer.insert(codePoint);
            }
            conn.writeHandler().accept(copy.compute(lineBuffer));
          }
        } else {
          FunctionEvent fname = (FunctionEvent) event;
          Function function = functions.get(fname.name());
          if (function != null) {
            function.apply(lineBuffer);
          } else {
            System.out.println("Unimplemented function " + fname.name());
          }
          conn.writeHandler().accept(copy.compute(lineBuffer));
        }
      }
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
