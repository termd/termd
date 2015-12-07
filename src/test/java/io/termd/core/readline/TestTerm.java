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

import io.termd.core.TestBase;
import io.termd.core.readline.functions.*;
import io.termd.core.tty.TtyConnection;
import io.termd.core.tty.TtyEvent;
import io.termd.core.tty.TtyOutputMode;
import io.termd.core.util.Helper;
import io.termd.core.util.Vector;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
* @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
*/
class TestTerm {

  private TestBase readlineTest;
  private int[][] buffer = new int[10][];
  private int row;
  private int cursor;
  private int status = 0;
  private int acc = -1;
  private int bell;
  private int width = 40;
  Consumer<int[]> writeHandler = new Consumer<int[]>() {
    @Override
    public void accept(int[] event) {
      for (int i : event) {
        if (buffer[row] == null) {
          buffer[row] = new int[100];
        }
        switch (status) {
          case 0:
            if (i >= 32) {
              buffer[row][cursor] = i;
              forward();
            } else {
              switch (i) {
                case 7:
                  bell++;
                  break;
                case '\r':
                  cursor = 0;
                  break;
                case '\n':
                  row++;
                  break;
                case 27:
                  status = 1;
                  break;
                case '\b':
                  backward();
                  break;
              }
            }
            break;
          case 1:
            if (i == '[') {
              status = 2;
            } else {
              throw new UnsupportedOperationException();
            }
            break;
          case 2:
            if (i >= '0' && i <= '9') {
              if (acc == -1) {
                acc = i - '0';
              } else {
                acc = acc * 10 + (i - '0');
              }
            } else {
              switch (i) {
                case 'A':
                  while (acc-- > 0 && row > 0) {
                    row--;
                  }
                  break;
                case 'B':
                  while (acc-- > 0 && row < buffer.length) {
                    row++;
                  }
                  break;
                case 'C':
                  while (acc-- > 0) {
                    forward();
                  }
                  break;
                case 'D':
                  while (acc-- > 0) {
                    backward();
                  }
                  break;
                case 'K': {
                  if (acc != -1) {
                    throw new UnsupportedOperationException("Not yet implemented");
                  } else {
                    for (int j = cursor;j < buffer[row].length;j++) {
                      buffer[row][j] = 0;
                    }
                  }
                  break;
                }
                default:
                  throw new UnsupportedOperationException("Implement escape sequence " + i);
              }
              acc = -1;
              status = 0;
            }
            break;
          default:
            throw new UnsupportedOperationException("Unsupported cp=" + i + " with status=" + status);
        }
      }
    }

    private void backward() {
      if (cursor > 0) {
        cursor--;
      } else {
        throw new UnsupportedOperationException();
      }
    }

    private void forward() {
      cursor++;
      if (cursor == width) {
        cursor = 0;
        row++;
      }
    }
  };
  final Consumer<int[]> stdout = new TtyOutputMode(writeHandler);
  final Readline readline;

  Consumer<int[]> readHandler;
  Consumer<Vector> sizeHandler;
  BiConsumer<TtyEvent, Integer> eventHandler;
  private LinkedList<Runnable> tasks = new LinkedList<>();

  TtyConnection conn = new TtyConnection() {

    @Override
    public Charset inputCharset() {
      return StandardCharsets.UTF_8;
    }

    @Override
    public Charset outputCharset() {
      return StandardCharsets.UTF_8;
    }

    @Override
    public long lastAccessedTime() {
      return 0;
    }

    @Override
    public String terminalType() {
      return "xterm";
    }

    @Override
    public Vector size() {
      return new Vector(width, 20);
    }

    @Override
    public Consumer<String> getTerminalTypeHandler() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setTerminalTypeHandler(Consumer<String> handler) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Consumer<Vector> getSizeHandler() {
      return sizeHandler;
    }

    @Override
    public void setSizeHandler(Consumer<Vector> handler) {
      sizeHandler = handler;
      if (handler != null) {
        handler.accept(new Vector(40, 20));
      }
    }

    @Override
    public BiConsumer<TtyEvent, Integer> getEventHandler() {
      return eventHandler;
    }

    @Override
    public void setEventHandler(BiConsumer<TtyEvent, Integer> handler) {
      eventHandler = handler;
    }

    @Override
    public Consumer<int[]> getStdinHandler() {
      return readHandler;
    }

    @Override
    public void setStdinHandler(Consumer<int[]> handler) {
      readHandler = handler;
    }

    @Override
    public Consumer<int[]> stdoutHandler() {
      return stdout;
    }

    @Override
    public void execute(Runnable task) {
      tasks.add(task);
    }

    @Override
    public void schedule(Runnable task, long delay, TimeUnit unit) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setCloseHandler(Consumer<Void> closeHandler) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Consumer<Void> getCloseHandler() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
      throw new UnsupportedOperationException();
    }
  };

  public TestTerm(TestBase test) {
    this.readlineTest = test;
    Keymap keymap = InputrcParser.create();
    keymap.bindFunction(Keys.META_c.sequence, "capitalize-word");
    keymap.bindFunction(Keys.META_d.sequence, "kill-word");
    keymap.bindFunction(Keys.META_f.sequence, "forward-word");
    keymap.bindFunction(Keys.META_l.sequence, "downcase-word");
    keymap.bindFunction(Keys.META_u.sequence, "upcase-word");
    keymap.bindFunction(Keys.CTRL_U.sequence, "unix-line-discard");
    keymap.bindFunction(Keys.CTRL_Y.sequence, "yank");
    keymap.bindFunction(Keys.CTRL_UNDERSCORE.sequence, "undo");

    //meta-del, meta-backspace
    keymap.bindFunction(new int[]{27,8}, "backward-kill-word");

    readline = new Readline(keymap);
    readline.addFunction(new BackwardDeleteChar());
    readline.addFunction(new BackwardChar());
    readline.addFunction(new ForwardChar());
    readline.addFunction(new PreviousHistory());
    readline.addFunction(new NextHistory());
    readline.addFunction(new BeginningOfLine());
    readline.addFunction(new EndOfLine());
    readline.addFunction(new DeleteChar());
    readline.addFunction(new Complete());
    readline.addFunction(new KillLine());
    readline.addFunction(new MoveBackwardWord());
    readline.addFunction(new MoveForwardWord());
    readline.addFunction(new DeleteBackwardWord());
    readline.addFunction(new DeleteForwardWord());
    readline.addFunction(new DeleteStartOfLine());
    readline.addFunction(new DeleteEndOfLine());
    readline.addFunction(new CapitalizeForwardWord());
    readline.addFunction(new UpCaseForwardWord());
    readline.addFunction(new DownCaseForwardWord());
    readline.addFunction(new Undo());
    readline.addFunction(new Yank());
  }

  public void readlineFail() {
    readline(event -> readlineTest.fail("Was not accepting a call"));
  }

  public Supplier<String> readlineComplete() {
    final AtomicReference<String> queue = new AtomicReference<>();
    readline(event -> queue.compareAndSet(null, event));
    return () -> queue.get();
  }

  public void readline(Consumer<String> readlineHandler) {
    readline(readlineHandler, null);
  }

  public Supplier<String> readlineComplete(Consumer<Completion> completionHandler) {
    final AtomicReference<String> queue = new AtomicReference<>();
    readline(event -> queue.compareAndSet(null, event), completionHandler);
    return () -> queue.get();
  }

  public void readline(Consumer<String> readlineHandler, Consumer<Completion> completionHandler) {
    readline.readline(conn, "% ", readlineHandler, completionHandler);
  }

  public void executeTasks() {
    while (!tasks.isEmpty()) {
      Runnable task = tasks.removeFirst();
      task.run();
    }
  }

  public int getBellCount() {
    return bell;
  }

  public void resetBellCount() {
    bell = 0;
  }

  private List<String> render() {
    List<String> lines = new ArrayList<>();
    for (int[] row : buffer) {
      if (row == null) {
        break;
      }
      StringBuilder line = new StringBuilder();
      for (int codePoint : row) {
        if (codePoint < 32) {
          break;
        }
        line.appendCodePoint(codePoint);
      }
      lines.add(line.toString());
    }
    return lines;
  }

  void assertScreen(String... expected) {
    List<String> lines = render();
    readlineTest.assertEquals(Arrays.asList(expected), lines);
  }

  void assertAt(int row, int cursor) {
    readlineTest.assertEquals(row, this.row);
    readlineTest.assertEquals(cursor, this.cursor);
  }

  void read(int... data) {
    readHandler.accept(data);
  }

  void read(String data) {
    readHandler.accept(Helper.toCodePoints(data));
  }

  TestTerm setWidth(int width) {
    this.width = width;
    if (sizeHandler != null) {
      sizeHandler.accept(conn.size());
    }
    return this;
  }

}
