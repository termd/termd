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

import io.termd.core.term.Device;
import io.termd.core.term.TermInfo;
import io.termd.core.tty.TtyConnection;
import io.termd.core.tty.TtyEvent;
import io.termd.core.util.Vector;
import io.termd.core.util.Helper;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Make this class thread safe as SSH will access this class with different threds [sic].
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Readline {

  private final Keymap keymap;
  private final Device device;
  private final Map<String, Function> functions = new HashMap<>();
  private final EventQueue decoder;
  private Consumer<int[]> prevReadHandler;
  private Consumer<Vector> prevSizeHandler;
  private BiConsumer<TtyEvent, Integer> prevEventHandler;
  private Vector size;
  private Interaction interaction;
  private List<int[]> history;

  public Readline(Keymap keymap) {
    this.device = TermInfo.defaultInfo().getDevice("xterm"); // For now use xterm
    this.keymap = keymap;
    this.decoder = new EventQueue(keymap);
    this.history = new ArrayList<>();
  }

  public Interaction getInteraction() {
    return interaction;
  }

  public void setInteraction(Interaction interaction) {
    this.interaction = interaction;
  }

  /**
   * @return the current history
   */
  public List<int[]> getHistory() {
    return history;
  }

  /**
   * Set the history
   *
   * @param history the history
   */
  public void setHistory(List<int[]> history) {
    this.history = history;
  }

  /**
   * @return the last known size
   */
  public Vector size() {
    return size;
  }

  public Readline addFunction(Function function) {
    functions.put(function.name(), function);
    return this;
  }

  private void deliver() {
    while (decoder.hasNext() && interaction != null && ! interaction.completing) {
      interaction.handle(decoder.next());
    }
  }

  /**
   * Read a line until a request can be processed.
   *
   * @param requestHandler the requestHandler
   */
  public void readline(TtyConnection conn, String prompt, Consumer<String> requestHandler) {
    readline(conn, prompt, requestHandler, null);
  }

  /**
   * Read a line until a request can be processed.
   *
   * @param requestHandler the requestHandler
   */
  public void readline(TtyConnection conn, String prompt, Consumer<String> requestHandler, Consumer<Completion> completionHandler) {
    if (interaction != null) {
      throw new IllegalStateException("Already reading a line");
    }
    interaction = new Interaction(conn, prompt, requestHandler, completionHandler);
    interaction.install();
    conn.write(prompt);
    schedulePendingEvent();
  }

  /**
   * Schedule delivery of pending events in the event queue.
   */
  public void schedulePendingEvent() {
    if (interaction == null) {
      throw new IllegalStateException("No interaction!");
    }
    if (decoder.hasNext()) {
      interaction.conn.schedule(Readline.this::deliver);
    }
  }

  /**
   * Queue a {@link TtyEvent}.
   *
   * @param event the event
   * @param codePoint the code point that triggered this event
   * @return this object
   */
  public Readline queueEvent(TtyEvent event, int codePoint) {
    decoder.append(new FunctionEvent(event.name(), new int[]{codePoint}));
    return this;
  }

  public Readline queueEvent(int[] codePoints) {
    decoder.append(codePoints);
    return this;
  }

  public boolean hasEvent() {
    return decoder.hasNext();
  }

  public KeyEvent nextEvent() {
    return decoder.next();
  }

  public class Interaction {

    private final TtyConnection conn;
    private final String prompt;
    private final Consumer<String> requestHandler;
    private final Consumer<Completion> completionHandler;
    private final Map<String, Object> data;
    private final Map<IntBuffer, Runnable> handlers;
    private boolean completing;
    private final LinkedList<int[]> lines = new LinkedList<>();
    private final LineBuffer buffer = new LineBuffer();
    private final ParsedBuffer parsed = new ParsedBuffer();
    private int historyIndex = -1;
    private String currentPrompt;

    public Interaction(
        TtyConnection conn,
        String prompt,
        Consumer<String> requestHandler,
        Consumer<Completion> completionHandler) {
      this.conn = conn;
      this.prompt = prompt;
      this.handlers = new HashMap<>();
      this.data = new HashMap<>();
      this.currentPrompt = prompt;
      this.requestHandler = requestHandler;
      this.completionHandler = completionHandler;
      handlers.put(Keys.CTRL_M.buffer().asReadOnlyBuffer(), this::doEnter);
      handlers.put(Keys.CTRL_I.buffer().asReadOnlyBuffer(), this::doComplete);
    }

    private void doEnter() {
      for (int j : this.buffer) {
        parsed.accept(j);
      }
      this.buffer.setSize(0);
      if (parsed.escaped) {
        parsed.accept((int) '\r'); // Correct status
        currentPrompt = "> ";
        conn.write("\n> ");
      } else {
        int[] l = new int[parsed.buffer.size()];
        for (int index = 0;index < l.length;index++) {
          l[index] = parsed.buffer.get(index);
        }
        parsed.buffer.clear();
        lines.add(l);
        if (parsed.quoting == Quote.WEAK || parsed.quoting == Quote.STRONG) {
          conn.write("\n> ");
          currentPrompt = "> ";
        } else {
          final StringBuilder raw = new StringBuilder();
          ArrayList<Integer> hist = new ArrayList<>();
          for (int index = 0;index < lines.size();index++) {
            int[] a = lines.get(index);
            if (index > 0) {
              raw.append('\n'); // Use \n for processing
              hist.add((int) '\n');
            }
            for (int b : a) {
              raw.appendCodePoint(b);
              hist.add(b);
            }
          }
          lines.clear();
          history.add(0, hist.stream().mapToInt(Integer::intValue).toArray());
          parsed.buffer.clear();
          conn.write("\n");
          conn.setStdinHandler(prevReadHandler);
          conn.setSizeHandler(prevSizeHandler);
          conn.setEventHandler(prevEventHandler);
          interaction = null;
          requestHandler.accept(raw.toString());
        }
      }
    }

    private void doComplete() {
      if (completionHandler != null) {
        Vector dim = size; // Copy ref
        int index = this.buffer.getCursor();

        //
        while (index > 0 && this.buffer.getAt(index - 1) != ' ') {
          index--;
        }

        // Compute line : need to test full line :-)
        int linePos = this.buffer.getCursor();
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
        for (int i : this.buffer) {
          line_.accept(i);
        }
        int[] line = line_.buffer.stream().mapToInt(i -> i).toArray();

        // Compute prefix
        ParsedBuffer a = new ParsedBuffer();
        for (int i = index; i < this.buffer.getCursor();i++) {
          a.accept(this.buffer.getAt(i));
        }

        completing = true;
        LineBuffer copy = new LineBuffer(this.buffer);
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
          public Vector size() {
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
                        conn.write(currentPrompt);
                      } else {
                        conn.write("> ");
                      }
                      conn.stdoutHandler().accept(new LineBuffer().compute(Interaction.this.buffer));
                      break;
                  }
                  // Update status
                  completing = false;
                  // Schedule a delivery of pending data
                  schedulePendingEvent();
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
                            Interaction.this.buffer.insert('\\');
                            a.accept('\\');
                          }
                          Interaction.this.buffer.insert(z);
                          a.accept(z);
                          break;
                        default:
                          if (a.escaped) {
                            // Should beep
                          } else {
                            Interaction.this.buffer.insert(z);
                            a.accept(z);
                          }
                          break;
                      }
                      break;
                    case STRONG:
                      switch (z) {
                        case '\'':
                          Interaction.this.buffer.insert('\'', '\\', z, '\'');
                          a.accept('\'');
                          a.accept('\\');
                          a.accept(z);
                          a.accept('\'');
                          break;
                        default:
                          Interaction.this.buffer.insert(z);
                          a.accept(z);
                          break;
                      }
                      break;
                    case NONE:
                      if (a.escaped) {
                        Interaction.this.buffer.insert(z);
                        a.accept(z);
                      } else {
                        switch (z) {
                          case ' ':
                          case '"':
                          case '\'':
                          case '\\':
                            Interaction.this.buffer.insert('\\', z);
                            a.accept('\\');
                            a.accept(z);
                            break;
                          default:
                            Interaction.this.buffer.insert(z);
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
                        Interaction.this.buffer.insert('"', ' ');
                        a.accept('"');
                        a.accept(' ');
                      }
                      break;
                    case STRONG:
                      Interaction.this.buffer.insert('\'', ' ');
                      a.accept('\'');
                      a.accept(' ');
                      break;
                    case NONE:
                      if (a.escaped) {
                        // Do nothing emit bell
                      } else {
                        Interaction.this.buffer.insert(' ');
                        a.accept(' ');
                      }
                      break;
                  }
                }
                conn.stdoutHandler().accept(copy.compute(Interaction.this.buffer));
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
                    conn.write("\n");
                  }
                  conn.stdoutHandler().accept(text);
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
    }

    private void update(LineBuffer copy, int width) {
      LineBuffer copy3 = new LineBuffer();
      copy3.insert(Helper.toCodePoints(currentPrompt));
      copy3.insert(copy.toArray());
      copy3.setCursor(currentPrompt.length() + copy.getCursor());
      LineBuffer copy2 = new LineBuffer();
      copy2.insert(Helper.toCodePoints(currentPrompt));
      copy2.insert(buffer.toArray());
      copy2.setCursor(currentPrompt.length() + buffer.getCursor());
      copy3.update(copy2, conn.stdoutHandler(), width);
    }

    private void handle(KeyEvent event) {
      int width = conn.size().x();
      LineBuffer copy = new LineBuffer(buffer);
      if (event instanceof FunctionEvent) {
        FunctionEvent fname = (FunctionEvent) event;
        // Todo : bind ^D to exit shell somehow ?
        if (fname.name().equals(TtyEvent.INTR.name())) {
          interaction = new Interaction(conn, interaction.prompt, interaction.requestHandler, interaction.completionHandler);
          conn.stdoutHandler().accept(new int[]{'\n'});
          conn.write(interaction.prompt);
        } else {
          Function function = functions.get(fname.name());
          if (function != null) {
            function.apply(this);
          } else {
            System.out.println("Unimplemented function " + fname.name());
          }
          update(copy, width);
        }
      } else {
        Runnable handler = handlers.get(event.buffer());
        if (handler != null) {
          handler.run();
        } else {
          for (int i = 0;i < event.length();i++) {
            int codePoint = event.getCodePointAt(i);
            try {
              buffer.insert(codePoint);
            } catch (IllegalArgumentException e) {
              conn.stdoutHandler().accept(new int[]{'\007'});
            }
          }
          update(copy, width);
        }
      }
    }

    void resize(int oldWith, int newWidth) {

      // Erase screen
      LineBuffer abc = new LineBuffer();
      abc.insert(currentPrompt);
      abc.insert(buffer.toArray());
      abc.setCursor(currentPrompt.length() + buffer.getCursor());

      // Recompute new cursor
      Vector pos = abc.getCursorPosition(newWidth);
      int curWidth = pos.x();
      int curHeight = pos.y();

      // Recompute new end
      Vector end = abc.getPosition(abc.getSize(), oldWith);
      int endHeight = end.y() + end.x() / newWidth;

      // Position at the bottom / right
      Consumer<int[]> out = conn.stdoutHandler();
      out.accept(new int[]{'\r'});
      while (curHeight != endHeight) {
        if (curHeight > endHeight) {
          out.accept(new int[]{'\033','[','1','A'});
          curHeight--;
        } else {
          out.accept(new int[]{'\n'});
          curHeight++;
        }
      }

      // Now erase and redraw
      while (curHeight > 0) {
        out.accept(new int[]{'\033','[','1','K'});
        out.accept(new int[]{'\033','[','1','A'});
        curHeight--;
      }
      out.accept(new int[]{'\033','[','1','K'});

      // Now redraw
      out.accept(Helper.toCodePoints(currentPrompt));
      update(new LineBuffer(), newWidth);
    }

    public Map<String, Object> data() {
      return data;
    }

    public List<int[]> history() {
      return history;
    }

    public int getHistoryIndex() {
      return historyIndex;
    }

    public void setHistoryIndex(int historyIndex) {
      this.historyIndex = historyIndex;
    }

    public LineBuffer buffer() {
      return buffer;
    }

    private void install() {
      prevReadHandler = conn.getStdinHandler();
      prevSizeHandler = conn.getSizeHandler();
      prevEventHandler = conn.getEventHandler();
      conn.setStdinHandler(data -> {
        decoder.append(data);
        deliver();
      });
      conn.setSizeHandler(dim -> {
        if (size != null) {
          // Not supported for now
          // interaction.resize(size.width(), dim.width());
        }
        size = dim;
      });
      conn.setEventHandler((event,cp) -> {
        decoder.append(new FunctionEvent(event.name(), new int[]{cp}));
        deliver();
      });
    }
  }
}
