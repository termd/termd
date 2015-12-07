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

import io.termd.core.readline.undo.UndoManager;
import io.termd.core.term.Device;
import io.termd.core.term.TermInfo;
import io.termd.core.tty.TtyConnection;
import io.termd.core.tty.TtyEvent;
import io.termd.core.util.Logging;
import io.termd.core.util.Vector;
import io.termd.core.util.Helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Make this class thread safe as SSH will access this class with different threds [sic].
 *
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class Readline {

  private final Device device;
  private final Map<String, Function> functions = new HashMap<>();
  private final EventQueue decoder;
  private Interaction interaction;
  private Vector size;
  private List<int[]> history;

  public Readline(Keymap keymap) {
    this.device = TermInfo.defaultInfo().getDevice("xterm"); // For now use xterm
    this.decoder = new EventQueue(keymap);
    this.history = new ArrayList<>();
    addFunction(ACCEPT_LINE);
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

  public Readline addFunctions(Iterable<Function> functions) {
    for (Function function : functions) {
      addFunction(function);
    }
    return this;
  }

  /**
   * Cancel the current readline interaction if there one, the request handler is called with {@code null}.
   */
  public boolean cancel() {
    Interaction interaction;
    synchronized (this) {
      interaction = this.interaction;
      if (interaction == null) {
        return false;
      }
    }
    return interaction.end(null);
  }

  private void deliver() {
    while (true) {
      Interaction handler;
      KeyEvent event;
      synchronized (this) {
        if (decoder.hasNext() && interaction != null && !interaction.paused) {
          event = decoder.next();
          handler = interaction;
        } else {
          return;
        }
      }
      handler.handle(event);
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
    synchronized (this) {
      if (interaction != null) {
        throw new IllegalStateException("Already reading a line");
      }
      interaction = new Interaction(conn, prompt, requestHandler, completionHandler);
    }
    interaction.install();
    conn.write(prompt);
    schedulePendingEvent();
  }

  /**
   * Schedule delivery of pending events in the event queue.
   */
  public void schedulePendingEvent() {
    TtyConnection conn;
    synchronized (this) {
      if (interaction == null) {
        throw new IllegalStateException("No interaction!");
      }
      if (decoder.hasNext()) {
        conn = interaction.conn;
      } else {
        return;
      }
    }
    conn.execute(Readline.this::deliver);
  }

  public synchronized Readline queueEvent(int[] codePoints) {
    decoder.append(codePoints);
    return this;
  }

  public synchronized boolean hasEvent() {
    return decoder.hasNext();
  }

  public synchronized KeyEvent nextEvent() {
    return decoder.next();
  }

  public class Interaction {

    final TtyConnection conn;
    private Consumer<int[]> prevReadHandler;
    private Consumer<Vector> prevSizeHandler;
    private BiConsumer<TtyEvent, Integer> prevEventHandler;
    private final String prompt;
    private final Consumer<String> requestHandler;
    private final Consumer<Completion> completionHandler;
    private final Map<String, Object> data;
    private final LineBuffer line = new LineBuffer();
    private final LineBuffer buffer = new LineBuffer();
    private int historyIndex = -1;
    private String currentPrompt;
    private boolean paused;
    private UndoManager undoManager;
    private PasteManager pasteManager;

    private Interaction(
        TtyConnection conn,
        String prompt,
        Consumer<String> requestHandler,
        Consumer<Completion> completionHandler) {
      this.conn = conn;
      this.prompt = prompt;
      this.data = new HashMap<>();
      this.currentPrompt = prompt;
      this.requestHandler = requestHandler;
      this.completionHandler = completionHandler;
      undoManager = new UndoManager();
      pasteManager = new PasteManager();
    }

    /**
     * End the current interaction with a callback.
     *
     * @param s the
     */
    private boolean end(String s) {
      synchronized (Readline.this) {
        if (interaction == null) {
          return false;
        }
        interaction = null;
        conn.setStdinHandler(prevReadHandler);
        conn.setSizeHandler(prevSizeHandler);
        conn.setEventHandler(prevEventHandler);
      }
      requestHandler.accept(s);
      return true;
    }

    private void handle(KeyEvent event) {

      // Very specific behavior that cannot be encapsulated in a function flow
      if (event.length() == 1) {
        if (event.getCodePointAt(0) == 4 && buffer.getSize() == 0) {
          // Specific behavior for Ctrl-D with empty line
          end(null);
          return;
        } else if (event.getCodePointAt(0) == 3) {
          // Specific behavior Ctrl-C
          line.clear();
          buffer.clear();
          data.clear();
          historyIndex = -1;
          currentPrompt = prompt;
          conn.stdoutHandler().accept(new int[]{'\n'});
          conn.write(interaction.prompt);
          return;
        }
      }
      if (event instanceof FunctionEvent) {
        FunctionEvent fname = (FunctionEvent) event;
        Function function = functions.get(fname.name());
        if (function != null) {
          synchronized (this) {
            paused = true;
          }
          function.apply(this);
        } else {
          Logging.READLINE.log(Level.WARNING, "Unimplemented function " + fname.name());
        }
      } else {
        LineBuffer buf = buffer.copy();
        for (int i = 0;i < event.length();i++) {
          int codePoint = event.getCodePointAt(i);
          try {
            buf.insert(codePoint);
          } catch (IllegalArgumentException e) {
            conn.stdoutHandler().accept(new int[]{'\007'});
          }
        }
        refresh(buf);
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
      refresh(new LineBuffer(), newWidth);
    }

    public Consumer<Completion> completionHandler() {
      return completionHandler;
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

    public LineBuffer line() {
      return line;
    }

    public LineBuffer buffer() {
      return buffer;
    }

    public String currentPrompt() {
      return currentPrompt;
    }

    public Vector size() {
      return size;
    }

    /**
     * Redraw the current line.
     */
    public void redraw() {
      LineBuffer toto = new LineBuffer();
      toto.insert(Helper.toCodePoints(currentPrompt));
      toto.insert(buffer.toArray());
      toto.setCursor(currentPrompt.length() + buffer.getCursor());
      LineBuffer abc = new LineBuffer();
      abc.update(toto, conn.stdoutHandler(), size.x());
    }

    /**
     * Refresh the current buffer with the argument buffer.
     *
     * @param buffer the new buffer
     */
    public Interaction refresh(LineBuffer buffer) {
      refresh(buffer, size.x());
      return this;
    }

    private void refresh(LineBuffer update, int width) {
      LineBuffer copy3 = new LineBuffer();
      copy3.insert(Helper.toCodePoints(currentPrompt));
      copy3.insert(buffer().toArray());
      copy3.setCursor(currentPrompt.length() + buffer().getCursor());
      LineBuffer copy2 = new LineBuffer();
      copy2.insert(Helper.toCodePoints(currentPrompt));
      copy2.insert(update.toArray());
      copy2.setCursor(currentPrompt.length() + update.getCursor());
      copy3.update(copy2, conn.stdoutHandler(), width);
      buffer.clear();
      buffer.insert(update.toArray());
      buffer.setCursor(update.getCursor());
    }

    public void resume() {
      synchronized (Readline.this) {
        if (!paused) {
          throw new IllegalStateException();
        }
        paused = false;
      }
      schedulePendingEvent();
    }

    private void install() {
      prevReadHandler = conn.getStdinHandler();
      prevSizeHandler = conn.getSizeHandler();
      prevEventHandler = conn.getEventHandler();
      conn.setStdinHandler(data -> {
        synchronized (Readline.this) {
          decoder.append(data);
        }
        deliver();
      });
      size = conn.size();
      conn.setSizeHandler(dim -> {
        if (size != null) {
          // Not supported for now
          // interaction.resize(size.width(), dim.width());
        }
        size = dim;
      });
      conn.setEventHandler(null);
    }

    public PasteManager getPasteManager() {
      return pasteManager;
    }

    public UndoManager getUndoManager() {
      return undoManager;
    }

  }

  // Need to access internal state
  private final Function ACCEPT_LINE = new Function() {

    @Override
    public String name() {
      return "accept-line";
    }

    @Override
    public void apply(Interaction interaction) {
      interaction.line.insert(interaction.buffer.toArray());
      LineStatus pb = new LineStatus();
      for (int i = 0;i < interaction.line.getSize();i++) {
        pb.accept(interaction.line.getAt(i));
      }
      interaction.buffer.clear();
      if (pb.isEscaping()) {
        interaction.line.delete(-1); // Remove \
        interaction.currentPrompt = "> ";
        interaction.conn.write("\n> ");
        interaction.resume();
      } else {
        if (pb.isQuoted()) {
          interaction.line.insert('\n');
          interaction.conn.write("\n> ");
          interaction.currentPrompt = "> ";
          interaction.resume();
        } else {
          String raw = interaction.line.toString();
          if (interaction.line.getSize() > 0) {
            history.add(0, interaction.line.toArray());
          }
          interaction.line.clear();
          interaction.conn.write("\n");
          interaction.end(raw);
        }
      }
    }
  };
}
