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
    Consumer<Dimension> prevResizeHandler = conn.sizeHandler();
    Interaction interaction = new Interaction(conn, prompt, prevEventHandler, prevResizeHandler, requestHandler, completionHandler);
    conn.write(prompt);
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
      conn.setSizeHandler(dim -> this.size = dim);

      handlers.put(Keys.CTRL_M.buffer().asReadOnlyBuffer(), () -> {
        for (int j : lineBuffer) {
          parsed.accept(j);
        }
        lineBuffer.setSize(0);
        if (parsed.escaped) {
          parsed.accept((int) '\r'); // Correct status
          conn.write("\r\n> ");
        } else {
          int[] l = new int[parsed.buffer.size()];
          for (int index = 0;index < l.length;index++) {
            l[index] = parsed.buffer.get(index);
          }
          parsed.buffer.clear();
          lines.add(l);
          if (parsed.quoting == Quote.WEAK || parsed.quoting == Quote.STRONG) {
            conn.write("\r\n> ");
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
            parsed.buffer.clear();
            conn.write("\r\n");
            conn.setReadHandler(previousEventHandler);
            conn.setSizeHandler(previousSizeHandler);
            status = Status.PAUSED;
            requestHandler.accept(raw.toString());
          }
        }
      });

      handlers.put(Keys.CTRL_I.buffer().asReadOnlyBuffer(), () -> {
        if (completionHandler != null) {
          Dimension dim = size; // Copy ref
          int index = lineBuffer.getCursor();

          //
          while (index > 0 && lineBuffer.getAt(index - 1) != ' ') {
            index--;
          }

          // Compute line : need to test full line :-)
          int linePos = lineBuffer.getCursor();
          ParsedBuffer line_ = new ParsedBuffer();
          for (int[] l : lines) {
            for (int j : l) {
              line_.accept(j);
            }
            line_.accept('\n');
          }
          for (int i : parsed.buffer) {
            line_.accept(i);
          }
          for (int i : lineBuffer) {
            line_.accept(i);
          }
          int[] line = line_.buffer.stream().mapToInt(i -> i).toArray();

          // Compute prefix
          ParsedBuffer a = new ParsedBuffer();
          for (int i = index; i < lineBuffer.getCursor();i++) {
            a.accept(lineBuffer.getAt(i));
          }

          status = Status.COMPLETING;
          LineBuffer copy = new LineBuffer(lineBuffer);
          final AtomicReference<CompletionStatus> status = new AtomicReference<>(CompletionStatus.PENDING);
          completionHandler.accept(new Completion() {

            @Override
            public int[] line() {
              return line;
            }

            @Override
            public int[] prefix() {
              return a.buffer.stream().mapToInt(i -> i).toArray();
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
                        if (lines.size() == 0 && parsed.buffer.size() == 0) {
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
            public Completion complete(int[] text, boolean terminal) {
              if (status.compareAndSet(CompletionStatus.PENDING, CompletionStatus.INLINING)) {
                if (text.length > 0 || terminal) {
                  for (int z : text) {
                    if (z < 32) {
                      // Todo support \n with $'\n'
                      throw new UnsupportedOperationException("todo");
                    }
                    switch (a.quoting) {
                      case WEAK:
                        switch (z) {
                          case '\\':
                          case '"':
                            if (!a.escaped) {
                              lineBuffer.insert('\\');
                              a.accept('\\');
                            }
                            lineBuffer.insert(z);
                            a.accept(z);
                            break;
                          default:
                            if (a.escaped) {
                              // Should beep
                            } else {
                              lineBuffer.insert(z);
                              a.accept(z);
                            }
                            break;
                        }
                        break;
                      case STRONG:
                        switch (z) {
                          case '\'':
                            lineBuffer.insert('\'', '\\', z, '\'');
                            a.accept('\'');
                            a.accept('\\');
                            a.accept(z);
                            a.accept('\'');
                            break;
                          default:
                            lineBuffer.insert(z);
                            a.accept(z);
                            break;
                        }
                        break;
                      case NONE:
                        if (a.escaped) {
                          lineBuffer.insert(z);
                          a.accept(z);
                        } else {
                          switch (z) {
                            case ' ':
                            case '"':
                            case '\'':
                            case '\\':
                              lineBuffer.insert('\\', z);
                              a.accept('\\');
                              a.accept(z);
                              break;
                            default:
                              lineBuffer.insert(z);
                              a.accept(z);
                              break;
                          }
                        }
                        break;
                      default:
                        throw new UnsupportedOperationException("Todo " + a.quoting);
                    }
                  }
                  if (terminal) {
                    switch (a.quoting) {
                      case WEAK:
                        if (a.escaped) {
                          // Do nothing emit bell
                        } else {
                          lineBuffer.insert('"', ' ');
                          a.accept('"');
                          a.accept(' ');
                        }
                        break;
                      case STRONG:
                        lineBuffer.insert('\'', ' ');
                        a.accept('\'');
                        a.accept(' ');
                        break;
                      case NONE:
                        if (a.escaped) {
                          // Do nothing emit bell
                        } else {
                          lineBuffer.insert(' ');
                          a.accept(' ');
                        }
                        break;
                    }
                  }
                  conn.writeHandler().accept(copy.compute(lineBuffer));
                }
              } else {
                throw new IllegalStateException();
              }
              return this;
            }

            @Override
            public Completion suggest(int[] text) {
              while (true) {
                CompletionStatus current = status.get();
                if ((current == CompletionStatus.PENDING || current == CompletionStatus.COMPLETING)) {
                  if (status.compareAndSet(current, CompletionStatus.COMPLETING)) {
                    if (current == CompletionStatus.PENDING) {
                      conn.write("\r\n");
                    }
                    conn.writeHandler().accept(text);
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
    private final ParsedBuffer parsed = new ParsedBuffer();
  }
}
