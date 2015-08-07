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

import io.termd.core.readline.functions.BackwardChar;
import io.termd.core.readline.functions.BackwardDeleteChar;
import io.termd.core.readline.functions.ForwardChar;
import io.termd.core.readline.functions.NextHistory;
import io.termd.core.readline.functions.PreviousHistory;
import io.termd.core.telnet.TestBase;
import io.termd.core.tty.TtyConnection;
import io.termd.core.tty.TtyEvent;
import io.termd.core.util.Dimension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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
  Consumer<int[]> writeHandler = new Consumer<int[]>() {
    @Override
    public void accept(int[] event) {
      for (int i : event) {
        if (buffer[row] == null) {
          buffer[row] = new int[100];
        }
        if (i >= 32) {
          buffer[row][cursor++] = i;
        } else {
          switch (i) {
            case '\r':
              cursor = 0;
              break;
            case '\n':
              row++;
              break;
            case '\b':
              if (cursor > 0) {
                cursor--;
              } else {
                throw new UnsupportedOperationException();
              }
              break;
          }
        }
      }
    }
  };
  final Readline readline;

  Consumer<int[]> readHandler;
  Consumer<Dimension> sizeHandler;
  private LinkedList<Runnable> tasks = new LinkedList<>();

  TtyConnection conn = new TtyConnection() {

    @Override
    public Consumer<String> getTermHandler() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setTermHandler(Consumer<String> handler) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Consumer<Dimension> getSizeHandler() {
      return sizeHandler;
    }

    @Override
    public void setSizeHandler(Consumer<Dimension> handler) {
      sizeHandler = handler;
      if (handler != null) {
        handler.accept(new Dimension(40, 20));
      }
    }

    @Override
    public Consumer<TtyEvent> getEventHandler() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setEventHandler(Consumer<TtyEvent> handler) {
      throw new UnsupportedOperationException();
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
      return writeHandler;
    }

    @Override
    public void schedule(Runnable task) {
      tasks.add(task);
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
    readline = new Readline(keymap);
    readline.addFunction(new BackwardDeleteChar());
    readline.addFunction(new BackwardChar());
    readline.addFunction(new ForwardChar());
    readline.addFunction(new PreviousHistory());
    readline.addFunction(new NextHistory());
    readline.install(conn);
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
    readline.readline("% ", readlineHandler, completionHandler);
  }

  public void executeTasks() {
    while (!tasks.isEmpty()) {
      Runnable task = tasks.removeFirst();
      task.run();
    }
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

}
