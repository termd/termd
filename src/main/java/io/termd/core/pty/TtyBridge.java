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

import io.termd.core.readline.KeyDecoder;
import io.termd.core.readline.Keymap;
import io.termd.core.readline.Readline;
import io.termd.core.tty.TtyConnection;
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
  final Consumer<PtyMaster> taskCreationListener;

  public TtyBridge(TtyConnection conn, Consumer<PtyMaster> taskCreationListener) {
    this.conn = conn;
    this.taskCreationListener = taskCreationListener;
  }

  public void handle() {
    InputStream inputrc = KeyDecoder.class.getResourceAsStream("inputrc");
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
    conn.writeHandler().accept(Helper.toCodePoints("Welcome sir\r\n"));
    read(conn, readline, conn.getInvokerContext());
  }

  public void read(final TtyConnection conn, final Readline readline, String invokerContext) {
    Consumer<String> requestHandler = new Consumer<String>() {
      @Override
      public void accept(String line) {
        PtyMaster task = new PtyMaster(TtyBridge.this, conn, readline, line, invokerContext);
        taskCreationListener.accept(task);
        task.start();
      }
    };
    readline.readline(conn, "% ", requestHandler);
  }
}
