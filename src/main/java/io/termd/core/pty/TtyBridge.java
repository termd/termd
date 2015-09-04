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

package io.termd.core.pty;

import io.termd.core.readline.Keymap;
import io.termd.core.readline.Readline;
import io.termd.core.tty.TtyConnection;
import io.termd.core.tty.TtyEvent;
import io.termd.core.util.Helper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TtyBridge {

  Logger log = LoggerFactory.getLogger(TtyBridge.class);
  final TtyConnection conn;
  private Consumer<PtyMaster> processListener;
  private Consumer<int[]> processStdoutListener;
  private Consumer<String> processStdinListener;

  public TtyBridge(TtyConnection conn) {
    this.conn = conn;
  }

  public Consumer<PtyMaster> getProcessListener() {
    return processListener;
  }

  public TtyBridge setProcessListener(Consumer<PtyMaster> processListener) {
    this.processListener = processListener;
    return this;
  }

  public Consumer<String> getProcessStdinListener() {
    return processStdinListener;
  }

  public TtyBridge setProcessStdinListener(Consumer<String> processStdinListener) {
    this.processStdinListener = processStdinListener;
    return this;
  }

  public Consumer<int[]> getProcessStdoutListener() {
    return processStdoutListener;
  }

  public TtyBridge setProcessStdoutListener(Consumer<int[]> processStdoutListener) {
    this.processStdoutListener = processStdoutListener;
    return this;
  }

  public void readline() {
    InputStream inputrc = Keymap.class.getResourceAsStream("inputrc");
    Keymap keymap = new Keymap(inputrc);
    Readline readline = new Readline(keymap);
    for (io.termd.core.readline.Function function : Helper.loadServices(Thread.currentThread().getContextClassLoader(), io.termd.core.readline.Function.class)) {
      log.trace("Server is adding function to readline: {}", function);

      readline.addFunction(function);
    }
    conn.setTermHandler(term -> {
      // Not used yet but we should propagage this to the process builder
      System.out.println("CLIENT $TERM=" + term);
    });
    conn.stdoutHandler().accept(Helper.toCodePoints("Welcome sir\n"));
    read(conn, readline);
  }

  void read(final TtyConnection conn, final Readline readline) {
    readline.readline(conn, "% ", line -> {
      if (processStdinListener != null) {
        processStdinListener.accept(line);
      }
      PtyMaster task = new PtyMaster(line,
          buffer -> {
            conn.schedule(() -> {
              conn.stdoutHandler().accept(buffer);
            });
            if (processStdoutListener != null) {
              processStdoutListener.accept(buffer);
            }
          },
          v -> {
        conn.setEventHandler(null);
        conn.schedule(() -> read(conn, readline));
      });
      conn.setEventHandler((event,cp) -> {
        if (event == TtyEvent.INTR) {
          task.interruptProcess();
        }
      });
      if (processListener != null) {
        processListener.accept(task);
      }
      task.start();
    });
  }
}
