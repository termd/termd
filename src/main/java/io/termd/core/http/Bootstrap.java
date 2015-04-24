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

package io.termd.core.http;

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
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class Bootstrap implements Consumer<TtyConnection> {

  Logger log = LoggerFactory.getLogger(Bootstrap.class);

  private TaskCreationListener taskCreationListener;

  public Bootstrap() {
    this((taskStatusUpdateEvent) -> {});
  }

  public Bootstrap(TaskCreationListener taskCreationListener) {
    this.taskCreationListener = taskCreationListener;
  }

  @Override
  public void accept(final TtyConnection conn) {
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
    read(conn, readline);
  }

  public void read(final TtyConnection conn, final Readline readline) {
    Consumer<String> requestHandler = new Consumer<String>() {
      @Override
      public void accept(String line) {
        Task task = new Task(Bootstrap.this, conn, readline, line);
        taskCreationListener.accept(task);
        task.start();
      }
    };
    readline.readline(conn, "% ", requestHandler);
  }
}
